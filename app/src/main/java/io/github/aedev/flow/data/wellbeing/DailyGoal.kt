package io.github.aedev.flow.data.wellbeing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Today's goal: one thing to get done today, written by the user and ticked off when done. It belongs to one day
 * (an epoch day) and is simply not shown the next day. The newest edit wins when two devices disagree. The same
 * rules and stored text as the desktop app.
 */
data class DailyGoal(
    /** The day (epoch day) the goal is for. */
    val day: Long,
    val text: String,
    val done: Boolean,
    /** When it was last changed, in ms, to tell which copy is newer. */
    val updated: Long,
) {
    fun encode(): String =
        JsonObject(
            mapOf(
                "d" to JsonPrimitive(day),
                "t" to JsonPrimitive(text),
                "done" to JsonPrimitive(done),
                "u" to JsonPrimitive(updated),
            ),
        ).toString()

    companion object {
        const val MAX_LENGTH = 80

        fun parse(text: String?): DailyGoal? {
            if (text.isNullOrBlank()) return null
            return try {
                val obj = Json.parseToJsonElement(text).jsonObject
                DailyGoal(
                    day = obj["d"]?.jsonPrimitive?.longOrNull ?: return null,
                    text = (obj["t"]?.jsonPrimitive?.contentOrNull ?: return null).take(MAX_LENGTH),
                    done = obj["done"]?.jsonPrimitive?.booleanOrNull ?: false,
                    updated = obj["u"]?.jsonPrimitive?.longOrNull ?: 0L,
                )
            } catch (e: Exception) {
                null
            }
        }

        /** The goal that counts for [today], or null when there is none (or it is from another day). */
        fun forDay(
            goal: DailyGoal?,
            today: Long,
        ): DailyGoal? = goal?.takeIf { it.day == today && it.text.isNotBlank() }

        /** The newer of two copies. */
        fun merge(
            a: DailyGoal?,
            b: DailyGoal?,
        ): DailyGoal? = if (a == null) b else if (b == null) a else if (b.updated > a.updated) b else a

        fun write(
            today: Long,
            text: String,
            done: Boolean,
            now: Long,
        ): DailyGoal = DailyGoal(today, text.take(MAX_LENGTH), done, now)
    }
}
