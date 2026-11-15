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

    @Test
    fun `entertainment and game titles are caught`() {
        assertThat(looksLikeEntertainment("Hulk Vs Surtur Demon (4K Blu-ray) Fight Scenes | Marvel Movie Clips")).isTrue()
        assertThat(looksLikeEntertainment("Best anime of 2026")).isTrue()
        assertThat(looksLikeEntertainment("Лучшие моменты из фильма")).isTrue()
        assertThat(looksLikeEntertainment("Rust ownership explained")).isFalse()
        assertThat(looksLikeGaming("Minecraft survival gameplay #12")).isTrue()
        assertThat(looksLikeGaming("Прохождение Call of Duty")).isTrue()
        assertThat(looksLikeGaming("Yangi o\u02bbyin sharhi")).isTrue()
        assertThat(looksLikeGaming("Game theory in economics")).isFalse()
    }

    @Test
    fun `the language of a title is guessed from its script and telling words`() {
        assertThat(guessLanguage("Your Life as Every Rank in Cyber")).isEqualTo("en")
        assertThat(guessLanguage("Eng sara taronalar (2026)")).isEqualTo("uz")
        assertThat(guessLanguage("O'zbek tilida dars")).isEqualTo("uz")
        assertThat(guessLanguage("Nas\u0131l yap\u0131l\u0131r? \u00c7ok kolay ve h\u0131zl\u0131")).isEqualTo("tr")
        assertThat(guessLanguage("Лучшие песни 2026")).isEqualTo("ru")
        assertThat(guessLanguage("Ўзбек тилида дарс")).isEqualTo("uz")
        assertThat(guessLanguage("Қазақ тілінде сабақ")).isEqualTo("kk")
        assertThat(guessLanguage("Тоҷикӣ суруд")).isEqualTo("tg")
        assertThat(guessLanguage("أفضل الأناشيد")).isEqualTo("ar")
        assertThat(guessLanguage("최고의 노래")).isEqualTo("ko")
        assertThat(guessLanguage("2026")).isNull()
        assertThat(guessLanguage("Hulk Surtur Demon")).isNull()
    }

    @Test
    fun `only the chosen languages stay and an unknown one is never hidden`() {
        val languages = parseHomeLanguages("uz,en")
        assertThat(isOutsideLanguages("Best of the year", languages)).isFalse()
        assertThat(isOutsideLanguages("Yangi dars bilan", languages)).isFalse()
        assertThat(isOutsideLanguages("Лучшие песни 2026", languages)).isTrue()
        assertThat(isOutsideLanguages("2026", languages)).isFalse()
        assertThat(isOutsideLanguages("Лучшие песни 2026", emptyList())).isFalse()
        assertThat(parseHomeLanguages("en, uz,xx,UZ")).containsExactly("uz", "en").inOrder()
        assertThat(serializeHomeLanguages(listOf("ru", "uz"))).isEqualTo("uz,ru")
        assertThat(serializeHomeLanguages(emptyList())).isEqualTo("any")
    }

    @Test
    fun `the filters apply the new rules only when set`() {
        val clip = video(title = "Marvel Movie Clips")
        assertThat(HomeContentFilters(hideEntertainment = true).hides(clip)).isTrue()
        assertThat(HomeContentFilters().hides(clip)).isFalse()
        assertThat(HomeContentFilters(hideGaming = true).hides(video(title = "Fortnite highlights"))).isTrue()
        assertThat(HomeContentFilters(languages = "en").hides(video(title = "Лучшие песни 2026"))).isTrue()
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
