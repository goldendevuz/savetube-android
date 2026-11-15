package io.github.aedev.flow.ui.screens.home

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.local.VideoHistoryEntry
import io.github.aedev.flow.data.local.WatchedThreshold
import io.github.aedev.flow.data.model.Video
import org.junit.Test

class HomeContentFiltersTest {
    private fun video(
        title: String = "A video",
        channel: String = "Channel",
        duration: Int = 0,
    ) = Video(
        id = title,
        title = title,
        channelName = channel,
        channelId = "c",
        thumbnailUrl = "",
        duration = duration,
        viewCount = 1000,
        uploadDate = "1 day ago",
    )

    @Test
    fun `songs lyrics and nasheeds are music in several languages`() {
        assertThat(looksLikeMusic("Ummi Summa Ummi Nasheed(Lyrics) / Dar Al-Maqama", "Dar Al-Maqama")).isTrue()
        assertThat(looksLikeMusic("Darbuna Darbun ( Slowed + Reverb )", "LeE.7")).isTrue()
        assertThat(looksLikeMusic("Wedding Nasheed - Muhammad Al Muqit", "Laa")).isTrue()
        assertThat(looksLikeMusic("Любовь | текст песни", "Канал")).isTrue()
        assertThat(looksLikeMusic("Yangi qoʻshiq 2026", "Jon")).isTrue()
        assertThat(looksLikeMusic("Yeni şarkı sözleri", "Kanal")).isTrue()
        assertThat(looksLikeMusic("Eng sara taronalar (2026)", "Jon")).isTrue()
        assertThat(looksLikeMusic("Anything", "Artist - Topic")).isTrue()
        assertThat(looksLikeMusic("Anything", "Sony Music")).isTrue()
    }

    @Test
    fun `other videos are not music`() {
        assertThat(looksLikeMusic("5 Cybersecurity Certificates You Need", "UnixGuy")).isFalse()
        assertThat(looksLikeMusic("My first post about Rust", "Dev")).isFalse()
        assertThat(looksLikeMusic("Songbird migration in spring", "Nature")).isFalse()
    }

    @Test
    fun `several length groups keep a video that fits any of them`() {
        val groups = parseDurationGroups("g2,g4")
        assertThat(isOutsideDurationGroups(video(duration = 120), groups)).isFalse()
        assertThat(isOutsideDurationGroups(video(duration = 900), groups)).isFalse()
        assertThat(isOutsideDurationGroups(video(duration = 400), groups)).isTrue()
        assertThat(isOutsideDurationGroups(video(duration = 5_000), groups)).isTrue()
        assertThat(isOutsideDurationGroups(video(duration = 0), groups)).isFalse()
    }

    @Test
    fun `a saved length list skips unknown keys, any and the old g1`() {
        assertThat(parseDurationGroups("any")).isEmpty()
        assertThat(parseDurationGroups("g1")).isEmpty()
        assertThat(parseDurationGroups("g8, g3 ,g9")).containsExactly("g3", "g8").inOrder()
        assertThat(serializeDurationGroups(listOf("g5", "g2"))).isEqualTo("g2,g5")
        assertThat(serializeDurationGroups(emptyList())).isEqualTo("any")
    }

    @Test
    fun `the filters use every chosen group`() {
        val filters = HomeContentFilters(durationGroup = "g3,g5")
        assertThat(filters.isActive).isTrue()
        assertThat(filters.hides(video(duration = 400))).isFalse()
        assertThat(filters.hides(video(duration = 1_500))).isFalse()
        assertThat(filters.hides(video(duration = 100))).isTrue()
    }

    @Test
    fun `continue watching drops songs only when music is hidden`() {
        val history =
            listOf(
                entry("talk", "Security talk", "Dev"),
                entry("nasheed", "Arabic Nasheed Medley", "ITHADchannel"),
            )
        fun ids(hide: Boolean) =
            filterHomeHistory(
                history = history,
                hideWatchedVideos = false,
                watchedThreshold = WatchedThreshold.PERCENT_90,
                continueWatchingEnabled = true,
                hideWatchedShorts = false,
                hideMusicVideos = hide,
            ).continueWatchingVideos.map { it.videoId }
        assertThat(ids(hide = true)).containsExactly("talk")
        assertThat(ids(hide = false)).containsExactly("talk", "nasheed")
    }

    private fun entry(
        id: String,
        title: String,
        channel: String,
    ) = VideoHistoryEntry(
        videoId = id,
        position = 300_000,
        duration = 600_000,
        timestamp = 1,
        title = title,
        thumbnailUrl = "",
        channelName = channel,
    )
}
