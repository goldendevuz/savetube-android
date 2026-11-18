package io.github.aedev.flow.data.wellbeing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * The streak: how many days in a row the daily limit held, the same rules and the same stored text as the
 * desktop app. Only the days it broke are remembered (a day is broken once the limit ran out or "Keep watching"
 * was used); every other day since the streak began is a good one, also a day the app was never opened.
 */
data class LimitStreak(
    /** The first day (epoch day) the limit was watched. */
    val start: Long,
    /** Days the limit broke, newest first. */
    val broken: List<Long> = emptyList(),
    /** The longest streak so far, in days. */
    val best: Int = 0,
    /** The highest milestone already announced. */
    val announced: Int = 0,
) {
    /** The streak on [today]: the days since the last broken one, today included. A broken today is zero. */
    fun days(today: Long): Int {
        val lastBroken = broken.firstOrNull() ?: (start - 1)
        return if (lastBroken >= today) 0 else (today - lastBroken).toInt()
    }

    fun encode(): String =
        JsonObject(
            mapOf(
                "s" to JsonPrimitive(start),
                "b" to JsonArray(broken.map { JsonPrimitive(it) }),
                "best" to JsonPrimitive(best),
                "a" to JsonPrimitive(announced),
            ),
        ).toString()

    companion object {
        val MILESTONES = listOf(3, 7, 14, 30, 60, 100, 200, 365)
        private const val KEEP_BROKEN = 120

        fun parse(text: String?): LimitStreak? {
            if (text.isNullOrBlank()) return null
            return try {
                val obj = Json.parseToJsonElement(text).jsonObject
                val start = obj["s"]?.jsonPrimitive?.longOrNull ?: return null
                val broken = obj["b"]?.jsonArray?.mapNotNull { it.jsonPrimitive.longOrNull }.orEmpty()
                LimitStreak(
                    start = start,
                    broken = broken.distinct().sortedDescending().take(KEEP_BROKEN),
                    best = obj["best"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                    announced = obj["a"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                )
            } catch (e: Exception) {
                null
            }
        }

        /** Two copies in one: the earliest start, every broken day, the best of each count. */
        fun merge(a: LimitStreak?, b: LimitStreak?): LimitStreak? {
            if (a == null) return b
            if (b == null) return a
            return LimitStreak(
                start = minOf(a.start, b.start),
                broken = (a.broken + b.broken).distinct().sortedDescending().take(KEEP_BROKEN),
                best = maxOf(a.best, b.best),
                announced = maxOf(a.announced, b.announced),
            )
        }

        /**
         * The record after [today] was seen: started if it was new, today marked broken when [exceeded], the best
         * raised. The same object comes back when nothing changed, so the caller can skip saving.
         */
        fun record(streak: LimitStreak?, today: Long, exceeded: Boolean): LimitStreak {
            var next = streak ?: LimitStreak(start = today)
            if (exceeded && today !in next.broken) {
                // the run that ends today still counts for the best, also when the app was closed on those days
                val before = LimitStreak(next.start, next.broken).days(today - 1)
                next = next.copy(best = maxOf(next.best, before), broken = (listOf(today) + next.broken).sortedDescending().take(KEEP_BROKEN))
            }
            val days = next.days(today)
            if (days > next.best) next = next.copy(best = days)
            return if (streak != null && next == streak) streak else next
        }

        /** The milestone reached today that was not announced yet, if any. */
        fun newMilestone(streak: LimitStreak?, today: Long): Int? {
            if (streak == null) return null
            val days = streak.days(today)
            return MILESTONES.filter { it <= days && it > streak.announced }.maxOrNull()
        }

        /** The next milestone after [days], or null once all are reached. */
        fun nextMilestone(days: Int): Int? = MILESTONES.firstOrNull { it > days }
    }
}
