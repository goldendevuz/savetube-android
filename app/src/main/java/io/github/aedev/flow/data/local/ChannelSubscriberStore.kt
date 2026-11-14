package io.github.aedev.flow.data.local

import android.content.Context

/**
 * Subscriber counts of channels Home has looked up, kept for a month. Home's minimum-subscribers
 * choice needs a count for every channel it might show, and a count costs a channel request.
 */
class ChannelSubscriberStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("channel_subscribers", Context.MODE_PRIVATE)

    /** Counts read within the last month, by channel id. */
    fun fresh(now: Long = System.currentTimeMillis()): Map<String, Long> {
        val counts = HashMap<String, Long>()
        for ((channelId, raw) in prefs.all) {
            val parts = (raw as? String)?.split('|') ?: continue
            val count = parts.getOrNull(0)?.toLongOrNull() ?: continue
            val readAt = parts.getOrNull(1)?.toLongOrNull() ?: continue
            if (now - readAt in 0..MAX_AGE_MS) counts[channelId] = count
        }
        return counts
    }

    fun remember(
        counts: Map<String, Long>,
        now: Long = System.currentTimeMillis(),
    ) {
        if (counts.isEmpty()) return
        prefs.edit().apply { counts.forEach { (channelId, count) -> putString(channelId, "$count|$now") } }.apply()
    }

    private companion object {
        const val MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L
    }
}
