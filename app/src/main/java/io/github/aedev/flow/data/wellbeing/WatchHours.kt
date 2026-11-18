package io.github.aedev.flow.data.wellbeing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * When the user watches: this device's playing time by day and hour of day, for the last 28 days, in the same text
 * form as the desktop app. It is kept on this device only.
 */
object WatchHours {
    const val KEEP_DAYS = 28

    /** Epoch day to 24 amounts of ms, one for each hour of the day. */
    fun parse(text: String?): Map<Long, List<Long>> {
        if (text.isNullOrBlank()) return emptyMap()
        return try {
            Json.parseToJsonElement(text).jsonObject.mapNotNull { (day, hours) ->
                val key = day.toLongOrNull() ?: return@mapNotNull null
                val values = hours.jsonArray.map { it.jsonPrimitive.longOrNull?.coerceAtLeast(0L) ?: 0L }
                if (values.size == 24) key to values else null
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun encode(ledger: Map<Long, List<Long>>): String =
        JsonObject(ledger.entries.associate { (day, hours) -> day.toString() to JsonArray(hours.map { JsonPrimitive(it) }) }).toString()

    /** [ms] more playing time in [hour] of [day]; days older than the kept window are dropped. */
    fun add(
        ledger: Map<Long, List<Long>>,
        day: Long,
        hour: Int,
        ms: Long,
    ): Map<Long, List<Long>> {
        if (ms <= 0 || hour !in 0..23) return ledger
        val kept = ledger.filterKeys { it > day - KEEP_DAYS }.toMutableMap()
        val hours = (kept[day] ?: List(24) { 0L }).toMutableList()
        hours[hour] = hours[hour] + ms
        kept[day] = hours
        return kept
    }

    data class Summary(
        /** Time at each hour of the day, all days together. */
        val byHour: List<Long>,
        val total: Long,
        /** The hour with the most time, or null when nothing was counted. */
        val peakHour: Int?,
    )

    fun summarize(ledger: Map<Long, List<Long>>): Summary {
        val byHour = MutableList(24) { 0L }
        ledger.values.forEach { hours -> hours.forEachIndexed { hour, ms -> byHour[hour] = byHour[hour] + ms } }
        val total = byHour.sum()
        return Summary(byHour, total, if (total > 0) byHour.indexOf(byHour.max()) else null)
    }
}
