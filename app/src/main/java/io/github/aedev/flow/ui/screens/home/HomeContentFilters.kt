package io.github.aedev.flow.ui.screens.home

import io.github.aedev.flow.data.model.Video

/** Phrases that mark a title as a song or its video, matched in lower case. */
private val MUSIC_TITLE_MARKERS =
    listOf(
        "official music video",
        "official video",
        "official audio",
        "official lyric",
        "official mv",
        "official clip",
        "lyric video",
        "lyrics",
        "music video",
        "mood video",
        "visualizer",
        "(audio)",
        "[audio]",
        "(mv)",
        "[mv]",
        " m/v",
        "(feat.",
        "[feat.",
        "(ft.",
        "[ft.",
        " feat. ",
        " ft. ",
        "клип",
        "klip",
    )

/**
 * A best guess at which videos are music videos: the card carries no category, so this goes by what the
 * title and channel say. It catches the usual labels, VEVO and "- Topic" channels, and misses a song
 * whose title says nothing of the kind.
 */
internal fun looksLikeMusicVideo(video: Video): Boolean {
    val channel = video.channelName.lowercase()
    if (channel.endsWith(" - topic") || channel.endsWith("vevo") || channel.contains("vevo ")) return true
    val title = video.title.lowercase()
    return MUSIC_TITLE_MARKERS.any { title.contains(it) }
}

/** The video length groups of the CRM's G1 to G8 filter, in seconds: from (included) to (excluded, null for no limit). */
internal val DURATION_GROUPS: Map<String, Pair<Int, Int?>> =
    mapOf(
        "g1" to (1 to 60),
        "g2" to (60 to 300),
        "g3" to (300 to 600),
        "g4" to (600 to 1_200),
        "g5" to (1_200 to 2_400),
        "g6" to (2_400 to 3_600),
        "g7" to (3_600 to 7_200),
        "g8" to (7_200 to null),
    )

/** Whether [video] is outside [group] ("g1" to "g8"). Live and upcoming videos and ones with no known length are never ruled out. */
internal fun isOutsideDurationGroup(
    video: Video,
    group: String,
): Boolean {
    val (from, to) = DURATION_GROUPS[group] ?: return false
    if (video.isLive || video.isUpcoming || video.duration <= 0) return false
    return video.duration < from || (to != null && video.duration >= to)
}

/**
 * What the viewer chose to keep off Home: videos with few views, channels with few subscribers, music
 * videos. A channel whose subscriber count is not known yet stays hidden until it has been looked up.
 * Live and upcoming videos have no views yet, so the views minimum leaves them alone.
 */
internal data class HomeContentFilters(
    val minViews: Long = 0L,
    val minSubscribers: Long = 0L,
    val hideMusicVideos: Boolean = false,
    val durationGroup: String = "any",
    val subscribers: Map<String, Long> = emptyMap(),
) {
    val isActive: Boolean
        get() = minViews > 0L || minSubscribers > 0L || hideMusicVideos || durationGroup in DURATION_GROUPS

    fun hides(video: Video): Boolean {
        if (!isActive) return false
        if (minViews > 0L && !video.isLive && !video.isUpcoming && video.viewCount < minViews) return true
        if (durationGroup in DURATION_GROUPS && isOutsideDurationGroup(video, durationGroup)) return true
        if (minSubscribers > 0L) {
            val count = subscribers[video.channelId]
            if (count == null || count < minSubscribers) return true
        }
        return hideMusicVideos && looksLikeMusicVideo(video)
    }
}
