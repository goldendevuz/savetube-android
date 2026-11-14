package io.github.aedev.flow.ui.screens.subscriptions

import io.github.aedev.flow.R
import io.github.aedev.flow.data.local.dao.WatchHistoryDao
import io.github.aedev.flow.data.model.Channel
import io.github.aedev.flow.data.model.Video

internal fun SubscriptionSortMode.labelRes(): Int =
    when (this) {
        SubscriptionSortMode.DEFAULT -> R.string.subscriptions_sort_default
        SubscriptionSortMode.NAME_ASC -> R.string.subscriptions_sort_name
        SubscriptionSortMode.RECENTLY_UPDATED -> R.string.subscriptions_sort_recent
        SubscriptionSortMode.MOST_WATCHED -> R.string.subscriptions_sort_most_watched
        SubscriptionSortMode.SUBSCRIBERS_DESC -> R.string.subscriptions_sort_subscribers_desc
        SubscriptionSortMode.SUBSCRIBERS_ASC -> R.string.subscriptions_sort_subscribers_asc
        SubscriptionSortMode.VIDEOS_DESC -> R.string.subscriptions_sort_videos_desc
        SubscriptionSortMode.VIDEOS_ASC -> R.string.subscriptions_sort_videos_asc
        SubscriptionSortMode.ADDED_NEWEST -> R.string.subscriptions_sort_added_newest
        SubscriptionSortMode.ADDED_OLDEST -> R.string.subscriptions_sort_added_oldest
    }

internal fun sortSubscriptions(
    channels: List<Channel>,
    sortMode: SubscriptionSortMode,
    recentVideos: List<Video>,
    watchCounts: Map<String, Int> = emptyMap(),
    subscriberCounts: Map<String, Long> = emptyMap(),
    videoCounts: Map<String, Long> = emptyMap(),
    subscribedAt: Map<String, Long> = emptyMap(),
): List<Channel> =
    when (sortMode) {
        SubscriptionSortMode.DEFAULT -> {
            channels
        }

        SubscriptionSortMode.NAME_ASC -> {
            channels.sortedBy { it.name.lowercase() }
        }

        SubscriptionSortMode.RECENTLY_UPDATED -> {
            val latestUploadByChannel =
                recentVideos
                    .groupBy { it.channelId }
                    .mapValues { (_, videos) -> videos.maxOf { it.timestamp } }
            channels.sortedByDescending { latestUploadByChannel[it.id] ?: 0L }
        }

        SubscriptionSortMode.MOST_WATCHED -> {
            channels.sortedByDescending { watchCounts[it.id] ?: 0 }
        }

        SubscriptionSortMode.SUBSCRIBERS_DESC -> {
            channels.sortedByFigure(descending = true) { subscriberCounts[it.id] }
        }

        SubscriptionSortMode.SUBSCRIBERS_ASC -> {
            channels.sortedByFigure(descending = false) { subscriberCounts[it.id] }
        }

        SubscriptionSortMode.VIDEOS_DESC -> {
            channels.sortedByFigure(descending = true, tieBreak = { subscriberCounts[it.id] }) { videoCounts[it.id] }
        }

        SubscriptionSortMode.VIDEOS_ASC -> {
            channels.sortedByFigure(descending = false, tieBreak = { subscriberCounts[it.id] }) { videoCounts[it.id] }
        }

        SubscriptionSortMode.ADDED_NEWEST -> {
            channels.sortedByFigure(descending = true) { subscribedAt[it.id] }
        }

        SubscriptionSortMode.ADDED_OLDEST -> {
            channels.sortedByFigure(descending = false) { subscribedAt[it.id] }
        }
    }

/**
 * Orders by [figure], largest first when [descending]. A channel whose figure is not known yet goes last,
 * whichever way the list runs; equal figures go to the channel with more subscribers ([tieBreak]), then by name.
 */
private fun List<Channel>.sortedByFigure(
    descending: Boolean,
    tieBreak: (Channel) -> Long? = { null },
    figure: (Channel) -> Long?,
): List<Channel> {
    val (known, unknown) = partition { figure(it) != null }
    val ordered =
        known.sortedWith(
            compareBy<Channel> { if (descending) -(figure(it) ?: 0L) else (figure(it) ?: 0L) }
                .thenByDescending { tieBreak(it) ?: -1L }
                .thenBy { it.name.lowercase() },
        )
    return ordered + unknown.sortedBy { it.name.lowercase() }
}

/**
 * Video channels lead the quick-access row and music channels follow, so the two kinds stay
 * grouped no matter which sort the user picked.
 */
internal fun quickAccessOrder(channels: List<Channel>): List<Channel> =
    (channels.filterNot { it.isMusic } + channels.filter { it.isMusic }).distinctBy(Channel::id)

/** Watched videos per channel, for the Most watched sort. */
internal suspend fun WatchHistoryDao.channelWatchCounts(): Map<String, Int> =
    watchedCountByChannel().associate { count -> count.channelId to count.watched }
