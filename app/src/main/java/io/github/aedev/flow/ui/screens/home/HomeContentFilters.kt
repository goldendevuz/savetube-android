package io.github.aedev.flow.ui.screens.home

import io.github.aedev.flow.data.model.Video

/** Phrases that mark a title as a song or its video, matched anywhere in the lower-case title. */
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
        "full album",
        "karaoke",
        "nightcore",
        "mashup",
        "medley",
        "nasheed",
        "nashid",
        "anasheed",
        "нашид",
        "lofi",
        "lo-fi",
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
        "текст песни",
        "слова песни",
        "песн",
        "музык",
        "ремикс",
        "кавер",
        "караоке",
        "саундтрек",
        "klip",
        "qo'shiq",
        "qo'shig",
        "qoshiq",
        "musiqa",
        "ashula",
        "tarona",
        "konsert",
        "şarkı",
        "müzik",
        "sözleri",
        "♪",
        "♫",
        "🎵",
        "🎶",
    )

/** Words that mark a title as a song when they stand alone (so "ost" is not found inside "post"). */
private val MUSIC_TITLE_WORDS =
    setOf(
        "song",
        "songs",
        "lyric",
        "remix",
        "acoustic",
        "instrumental",
        "soundtrack",
        "ost",
        "album",
        "concert",
        "slowed",
        "mp3",
    )

/** Parts of a channel name that only publish music. */
private val MUSIC_CHANNEL_MARKERS = listOf("vevo", "music", "musiqa", "музык", "müzik")

/** The lower-case form text is matched in: the different apostrophes Uzbek is typed with become `'`. */
private fun normalizedForMusic(text: String): String =
    text.lowercase().replace('\u02bb', '\'').replace('\u02bc', '\'').replace('\u2019', '\'').replace('\u2018', '\'').replace('`', '\'').replace('\u00b4', '\'')

/**
 * A best guess at whether a title and channel are a song or a music video: the card carries no category, so
 * this goes by what they say. The same rule, with the same lists, runs on every device and for every viewer
 * who turns on "Hide music videos" (desktop: home_feed/music_video.rs).
 */
internal fun looksLikeMusic(
    title: String,
    channelName: String,
): Boolean {
    val channel = normalizedForMusic(channelName)
    if (channel.endsWith(" - topic") || MUSIC_CHANNEL_MARKERS.any { channel.contains(it) }) return true
    val lowerTitle = normalizedForMusic(title)
    if (MUSIC_TITLE_MARKERS.any { lowerTitle.contains(it) }) return true
    return lowerTitle.split(Regex("[^\\p{L}\\p{N}]+")).any { it in MUSIC_TITLE_WORDS }
}

internal fun looksLikeMusicVideo(video: Video): Boolean = looksLikeMusic(video.title, video.channelName)

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

/** The groups in a saved choice: keys joined by commas ("g2,g4"). "any", empty and unknown keys, and the old "g1", give none. */
internal fun parseDurationGroups(value: String): List<String> {
    val chosen = value.split(',').map { it.trim().lowercase() }.toSet()
    return DURATION_GROUPS.keys.filter { it != "g1" && it in chosen }
}

/** The saved choice for a list of groups, in length order: "any" when there is none. */
internal fun serializeDurationGroups(groups: Collection<String>): String =
    DURATION_GROUPS.keys.filter { it != "g1" && it in groups }.joinToString(",").ifEmpty { "any" }

/** Whether [video] is outside every group in [groups]. Live and upcoming videos and ones with no known length are never ruled out. */
internal fun isOutsideDurationGroups(
    video: Video,
    groups: List<String>,
): Boolean {
    if (groups.isEmpty() || video.isLive || video.isUpcoming || video.duration <= 0) return false
    return groups.none { key ->
        val (from, to) = DURATION_GROUPS[key] ?: return@none false
        video.duration >= from && (to == null || video.duration < to)
    }
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
    private val durationGroups: List<String> = parseDurationGroups(durationGroup)

    val isActive: Boolean
        get() = minViews > 0L || minSubscribers > 0L || hideMusicVideos || durationGroups.isNotEmpty()

    fun hides(video: Video): Boolean {
        if (!isActive) return false
        if (minViews > 0L && !video.isLive && !video.isUpcoming && video.viewCount < minViews) return true
        if (isOutsideDurationGroups(video, durationGroups)) return true
        if (minSubscribers > 0L) {
            val count = subscribers[video.channelId]
            if (count == null || count < minSubscribers) return true
        }
        return hideMusicVideos && looksLikeMusicVideo(video)
    }
}
