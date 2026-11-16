package io.github.aedev.flow.data.wellbeing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** What one device counted on one day: played time, the extra time Keep watching gave, and the two flags. */
data class DeviceCount(
    val day: Long,
    val playedMs: Long = 0L,
    val extraMs: Long = 0L,
    val reached: Boolean = false,
    val warned: Boolean = false,
)

/**
 * Today's count as every device keeps it. A device only ever changes its own entry, and the total is the sum of
 * the entries of the day, so two devices playing at once never overwrite each other. The entries travel as one
 * setting and a receiving device merges them into its own instead of replacing them.
 */
object SharedTally {
    /** Entries older than this many days are dropped. */
    private const val KEEP_DAYS = 7L

    fun parse(raw: String?): Map<String, DeviceCount> {
        if (raw.isNullOrBlank()) return emptyMap()
        val root = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return emptyMap()
        val out = HashMap<String, DeviceCount>()
        for ((id, value) in root) {
            val entry = (value as? JsonObject) ?: continue
            val day = entry["d"]?.jsonPrimitive()?.longOrNull ?: continue
            out[id] =
                DeviceCount(
                    day = day,
                    playedMs = (entry["p"]?.jsonPrimitive()?.longOrNull ?: 0L).coerceAtLeast(0L),
                    extraMs = (entry["x"]?.jsonPrimitive()?.longOrNull ?: 0L).coerceAtLeast(0L),
                    reached = entry["r"]?.jsonPrimitive()?.booleanOrNull ?: false,
                    warned = entry["w"]?.jsonPrimitive()?.booleanOrNull ?: false,
                )
        }
        return out
    }

    fun encode(shared: Map<String, DeviceCount>): String =
        buildJsonObject {
            for (id in shared.keys.sorted()) {
                val c = shared.getValue(id)
                put(
                    id,
                    buildJsonObject {
                        put("d", c.day)
                        put("p", c.playedMs)
                        put("x", c.extraMs)
                        put("r", c.reached)
                        put("w", c.warned)
                    },
                )
            }
        }.toString()

    /** One device's entry from two copies: the later day wins; on the same day the larger count and either flag. */
    private fun merge(
        a: DeviceCount,
        b: DeviceCount,
    ): DeviceCount =
        when {
            a.day != b.day -> if (a.day > b.day) a else b
            else ->
                DeviceCount(
                    day = a.day,
                    playedMs = maxOf(a.playedMs, b.playedMs),
                    extraMs = maxOf(a.extraMs, b.extraMs),
                    reached = a.reached || b.reached,
                    warned = a.warned || b.warned,
                )
        }

    fun merge(
        a: Map<String, DeviceCount>,
        b: Map<String, DeviceCount>,
    ): Map<String, DeviceCount> {
        val out = HashMap(a)
        for ((id, count) in b) out[id] = out[id]?.let { merge(it, count) } ?: count
        return prune(out)
    }

    /** Drops entries more than [KEEP_DAYS] days older than the newest one. */
    fun prune(shared: Map<String, DeviceCount>): Map<String, DeviceCount> {
        val newest = shared.values.maxOfOrNull { it.day } ?: return shared
        return shared.filterValues { it.day >= newest - KEEP_DAYS }
    }

    /** The newest day any device has counted, or null when none has. */
    fun latestDay(shared: Map<String, DeviceCount>): Long? = shared.values.maxOfOrNull { it.day }

    /** All devices' count for [day] as one tally. */
    fun aggregate(
        shared: Map<String, DeviceCount>,
        day: Long,
    ): DailyTally {
        val today = shared.values.filter { it.day == day }
        return DailyTally(
            day = day,
            playedMs = today.sumOf { it.playedMs },
            extraMs = today.sumOf { it.extraMs },
            warned = today.any { it.warned },
            reached = today.any { it.reached },
        )
    }

    /**
     * Records [tally], the all-devices count for its day, as this device's own entry: whatever it grew by since
     * the stored total is this device's. Another device's entry is never touched.
     */
    fun withOwn(
        shared: Map<String, DeviceCount>,
        deviceId: String,
        tally: DailyTally,
    ): Map<String, DeviceCount> {
        val total = aggregate(shared, tally.day)
        val own = shared[deviceId]?.takeIf { it.day == tally.day } ?: DeviceCount(tally.day)
        val next =
            own.copy(
                playedMs = own.playedMs + (tally.playedMs - total.playedMs).coerceAtLeast(0L),
                extraMs = own.extraMs + (tally.extraMs - total.extraMs).coerceAtLeast(0L),
                reached = own.reached || tally.reached,
                warned = own.warned || tally.warned,
            )
        return prune(shared + (deviceId to next))
    }

    private fun kotlinx.serialization.json.JsonElement.jsonPrimitive(): JsonPrimitive? = this as? JsonPrimitive
}
