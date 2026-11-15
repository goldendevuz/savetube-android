package io.github.aedev.flow.ui.screens.home

import io.github.aedev.flow.data.local.VideoHistoryEntry
import io.github.aedev.flow.data.local.WatchedThreshold
import io.github.aedev.flow.data.shorts.isShortComplete

/** What [filterHomeHistory] reads from the history and the settings, bundled so the flows can be combined. */
internal data class HomeHistoryInputs(
    val history: List<VideoHistoryEntry>,
    val hideWatched: Boolean,
    val threshold: WatchedThreshold,
    val continueWatchingEnabled: Boolean,
    val hideWatchedShorts: Boolean,
)

internal data class HomeHistoryFilterResult(
    val watchedVideoIds: Set<String>,
    val continueWatchingVideos: List<VideoHistoryEntry>,
    /** Filters the Shorts shelf, under its own setting rather than the videos one. */
    val watchedShortIds: Set<String>,
)

internal fun filterHomeHistory(
    history: List<VideoHistoryEntry>,
    hideWatchedVideos: Boolean,
    watchedThreshold: WatchedThreshold,
    continueWatchingEnabled: Boolean,
    hideWatchedShorts: Boolean,
    hideMusicVideos: Boolean = false,
    hideEntertainment: Boolean = false,
    hideGaming: Boolean = false,
): HomeHistoryFilterResult {
    val watchedIds by lazy(LazyThreadSafetyMode.NONE) {
        history
            .asSequence()
            .filter { watchedThreshold.isWatched(it.position, it.duration) }
            .mapTo(HashSet()) { it.videoId }
    }
    val watchedVideoIds = if (hideWatchedVideos) watchedIds else emptySet()

    val continueWatchingVideos =
        if (continueWatchingEnabled) {
            history
                .asSequence()
                .filter { !it.isShort && it.progressPercentage >= 3f }
                // Songs stay out of the shelf when the viewer hides music: the same rule as the feed.
                .filter { !(hideMusicVideos && (it.isMusic || looksLikeMusic(it.title, it.channelName))) }
                .filter { !(hideEntertainment && looksLikeEntertainment(it.title)) }
                .filter { !(hideGaming && looksLikeGaming(it.title)) }
                .filter { entry ->
                    if (hideWatchedVideos) {
                        entry.videoId !in watchedVideoIds
                    } else {
                        entry.progressPercentage <= 90f
                    }
                }.sortedByDescending { it.timestamp }
                .take(20)
                .toList()
        } else {
            emptyList()
        }

    return HomeHistoryFilterResult(
        watchedVideoIds = watchedVideoIds,
        continueWatchingVideos = continueWatchingVideos,
        watchedShortIds =
            if (hideWatchedShorts) {
                history.filter { isShortComplete(it.position, it.duration) }.mapTo(HashSet()) { it.videoId }
            } else {
                emptySet()
            },
    )
}
