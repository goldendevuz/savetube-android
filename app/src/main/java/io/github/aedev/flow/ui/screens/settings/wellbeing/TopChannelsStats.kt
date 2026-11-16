package io.github.aedev.flow.ui.screens.settings.wellbeing

import androidx.compose.runtime.Immutable
import io.github.aedev.flow.data.stats.RankedItem
import io.github.aedev.flow.data.stats.VideoRecap
import io.github.aedev.flow.data.stats.ViewFormat

internal const val TOP_CHANNEL_COUNT = 5

/** The channels this month's watching went to, most time first, and the share of video time that was Shorts. */
@Immutable
data class TopChannelsSummary(
    val channels: List<RankedItem>,
    /** From 0 to 1, or null when nothing was watched. */
    val shortsShare: Float?,
)

internal fun summarizeTopChannels(video: VideoRecap): TopChannelsSummary =
    TopChannelsSummary(
        channels = video.topChannels.sortedByDescending { it.durationMs }.take(TOP_CHANNEL_COUNT),
        shortsShare = shortsShare(video.formatMs),
    )

/** Shorts time over all video time, or null when no video time was counted. */
internal fun shortsShare(formatMs: Map<ViewFormat, Long>): Float? {
    val total = (formatMs[ViewFormat.LONG] ?: 0L) + (formatMs[ViewFormat.SHORT] ?: 0L) + (formatMs[ViewFormat.LIVE] ?: 0L)
    if (total <= 0L) return null
    return (formatMs[ViewFormat.SHORT] ?: 0L).toFloat() / total
}

/** A share as a whole percentage, such as "35%". */
internal fun percentLabel(share: Float): String = "${Math.round(share * 100)}%"
