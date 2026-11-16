package io.github.aedev.flow.data.wellbeing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** A channel and the time a device spent on it this month. */
data class ChannelTime(
    val id: String,
    val name: String,
    val ms: Long,
)

/**
 * What one device publishes about its own watching, so every device can show the same numbers for all of them:
 * the last days (date to ms, watching and listening), and this month's channels and format split of video. A
 * device only ever publishes its own entry; for each device the newest publication stands.
 */
data class DeviceStats(
    val name: String,
    val publishedAt: Long,
    /** The month [channels] and the format times are for, such as "2026-11". */
    val month: String,
    /** Local date ("2026-11-16") to time. */
    val days: Map<String, Long> = emptyMap(),
    val channels: List<ChannelTime> = emptyList(),
    val longMs: Long = 0L,
    val shortMs: Long = 0L,
    val liveMs: Long = 0L,
)

object SharedStats {
    /** A device not heard from for this long, counted from the newest publication, is dropped. */
    private const val KEEP_MS = 45L * 24 * 60 * 60 * 1000

    private fun kotlinx.serialization.json.JsonElement.primitive(): JsonPrimitive? = this as? JsonPrimitive

    fun parse(raw: String?): Map<String, DeviceStats> {
        if (raw.isNullOrBlank()) return emptyMap()
        val root = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return emptyMap()
        val out = HashMap<String, DeviceStats>()
        for ((id, value) in root) {
            val entry = (value as? JsonObject) ?: continue
            val publishedAt = entry["u"]?.primitive()?.longOrNull ?: continue
            val days = HashMap<String, Long>()
            (entry["days"] as? JsonObject)?.forEach { (date, ms) ->
                ms.primitive()?.longOrNull?.takeIf { it >= 0 }?.let { days[date] = it }
            }
            val channels =
                (entry["ch"] as? JsonArray)?.mapNotNull { row ->
                    val cells = row as? JsonArray ?: return@mapNotNull null
                    val channelId = cells.getOrNull(0)?.primitive()?.contentOrNull ?: return@mapNotNull null
                    val ms = cells.getOrNull(2)?.primitive()?.longOrNull ?: return@mapNotNull null
                    ChannelTime(channelId, cells.getOrNull(1)?.primitive()?.contentOrNull.orEmpty(), ms)
                }.orEmpty()
            val fmt = (entry["fmt"] as? JsonArray)?.map { it.primitive()?.longOrNull ?: 0L }.orEmpty()
            out[id] =
                DeviceStats(
                    name = entry["n"]?.primitive()?.contentOrNull.orEmpty(),
                    publishedAt = publishedAt,
                    month = entry["m"]?.primitive()?.contentOrNull.orEmpty(),
                    days = days,
                    channels = channels,
                    longMs = fmt.getOrElse(0) { 0L },
                    shortMs = fmt.getOrElse(1) { 0L },
                    liveMs = fmt.getOrElse(2) { 0L },
                )
        }
        return out
    }

    fun prune(shared: Map<String, DeviceStats>): Map<String, DeviceStats> {
        val newest = shared.values.maxOfOrNull { it.publishedAt } ?: return shared
        return shared.filterValues { it.publishedAt + KEEP_MS >= newest }
    }

    /** The text that is saved and sent: ids in order, so the same statistics always give the same text. */
    fun encode(shared: Map<String, DeviceStats>): String =
        buildJsonObject {
            val kept = prune(shared)
            for (id in kept.keys.sorted()) {
                val d = kept.getValue(id)
                put(
                    id,
                    buildJsonObject {
                        put("n", d.name)
                        put("u", d.publishedAt)
                        put("m", d.month)
                        put("days", buildJsonObject { d.days.toSortedMap().forEach { (date, ms) -> put(date, ms) } })
                        put(
                            "ch",
                            buildJsonArray {
                                d.channels.forEach { c ->
                                    add(buildJsonArray { add(JsonPrimitive(c.id)); add(JsonPrimitive(c.name)); add(JsonPrimitive(c.ms)) })
                                }
                            },
                        )
                        put("fmt", buildJsonArray { add(JsonPrimitive(d.longMs)); add(JsonPrimitive(d.shortMs)); add(JsonPrimitive(d.liveMs)) })
                    },
                )
            }
        }.toString()

    /** For each device, the newest publication. */
    fun merge(
        a: Map<String, DeviceStats>,
        b: Map<String, DeviceStats>,
    ): Map<String, DeviceStats> {
        val out = HashMap(a)
        for ((id, entry) in b) {
            val known = out[id]
            if (known == null || entry.publishedAt > known.publishedAt) out[id] = entry
        }
        return prune(out)
    }
}
