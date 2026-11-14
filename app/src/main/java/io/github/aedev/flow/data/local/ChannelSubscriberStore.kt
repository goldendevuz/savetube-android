package io.github.aedev.flow.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subscriber and video counts of channels the app has looked up, kept for a month. Home's
 * minimum-subscribers choice and the subscription sorts need a count for every channel they might
 * show, and a count costs a channel request.
 */
@Singleton
class ChannelSubscriberStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences("channel_subscribers", Context.MODE_PRIVATE)

        /** Subscriber counts read within the last month, by channel id. */
        fun fresh(now: Long = System.currentTimeMillis()): Map<String, Long> = read(VIDEOS_PREFIX, now, wanted = false)

        /** Video counts read within the last month, by channel id. */
        fun freshVideoCounts(now: Long = System.currentTimeMillis()): Map<String, Long> = read(VIDEOS_PREFIX, now, wanted = true)

        fun remember(
            counts: Map<String, Long>,
            now: Long = System.currentTimeMillis(),
        ) = write("", counts, now)

        fun rememberVideoCounts(
            counts: Map<String, Long>,
            now: Long = System.currentTimeMillis(),
        ) = write(VIDEOS_PREFIX, counts, now)

        private fun read(
            prefix: String,
            now: Long,
            wanted: Boolean,
        ): Map<String, Long> {
            val counts = HashMap<String, Long>()
            for ((key, raw) in prefs.all) {
                if (key.startsWith(prefix) != wanted) continue
                val parts = (raw as? String)?.split('|') ?: continue
                val count = parts.getOrNull(0)?.toLongOrNull() ?: continue
                val readAt = parts.getOrNull(1)?.toLongOrNull() ?: continue
                if (now - readAt in 0..MAX_AGE_MS) counts[if (wanted) key.removePrefix(prefix) else key] = count
            }
            return counts
        }

        private fun write(
            prefix: String,
            counts: Map<String, Long>,
            now: Long,
        ) {
            if (counts.isEmpty()) return
            prefs.edit().apply { counts.forEach { (channelId, count) -> putString(prefix + channelId, "$count|$now") } }.apply()
        }

        private companion object {
            const val MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L
            const val VIDEOS_PREFIX = "v:"
        }
    }
