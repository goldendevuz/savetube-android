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

/**
 * What the viewer chose to keep off Home: videos with few views, channels with few subscribers, music
 * videos. A channel whose subscriber count is not known yet stays hidden until it has been looked up.
 * Live and upcoming videos have no views yet, so the views minimum leaves them alone.
 */
internal data class HomeContentFilters(
    val minViews: Long = 0L,
    val minSubscribers: Long = 0L,
    val hideMusicVideos: Boolean = false,
    val subscribers: Map<String, Long> = emptyMap(),
) {
    val isActive: Boolean
        get() = minViews > 0L || minSubscribers > 0L || hideMusicVideos

    fun hides(video: Video): Boolean {
        if (!isActive) return false
        if (minViews > 0L && !video.isLive && !video.isUpcoming && video.viewCount < minViews) return true
        if (minSubscribers > 0L) {
            val count = subscribers[video.channelId]
            if (count == null || count < minSubscribers) return true
        }
        return hideMusicVideos && looksLikeMusicVideo(video)
    }
}
