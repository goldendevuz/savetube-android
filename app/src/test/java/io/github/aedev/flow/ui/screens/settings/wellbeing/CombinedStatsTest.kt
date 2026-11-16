package io.github.aedev.flow.ui.screens.settings.wellbeing

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.stats.RankedItem
import io.github.aedev.flow.data.stats.RankedKind
import io.github.aedev.flow.data.stats.ViewFormat
import io.github.aedev.flow.data.wellbeing.ChannelTime
import io.github.aedev.flow.data.wellbeing.DeviceStats
import org.junit.Test
import java.time.LocalDate

class CombinedStatsTest {
    private val minute = 60_000L
    private val today = LocalDate.of(2026, 11, 16)

    private fun device(
        name: String,
        days: Map<String, Long> = emptyMap(),
        channels: List<ChannelTime> = emptyList(),
        month: String = "2026-11",
        longMs: Long = 0L,
        shortMs: Long = 0L,
    ) = DeviceStats(name, 1L, month, days, channels, longMs, shortMs, 0L)

    private val ownWeek =
        WatchTimeSummary(
            (6 downTo 0).map { DayWatchTime(today.minusDays(it.toLong()), if (it == 0) 20 * minute else 0L) },
        )

    @Test
    fun theOtherDevicesTimeIsAddedDayByDay() {
        val phone = device("Phone", days = mapOf("2026-11-16" to 35 * minute, "2026-11-10" to 99 * minute))
        val combined = combineDays(ownWeek, listOf(phone))
        assertThat(combined.days.last().millis).isEqualTo(55 * minute)
        assertThat(combined.weekMillis).isEqualTo(55 * minute)
    }

    @Test
    fun eachDevicesPartOfTheWeekIsListedBiggestFirst() {
        val phone = device("Phone", days = mapOf("2026-11-16" to 35 * minute))
        val rows = deviceTotals(ownWeek, "Pixel 5", listOf(phone))
        assertThat(rows.map { it.name }).containsExactly("Phone", "Pixel 5").inOrder()
        assertThat(rows.first { it.own }.millis).isEqualTo(20 * minute)
    }

    @Test
    fun channelsAndTheShortsShareAreAddedOverDevices() {
        val own =
            TopChannelsSummary(
                channels = listOf(RankedItem("a", "Alpha", 1, 12 * minute, kind = RankedKind.CHANNEL), RankedItem("b", "Beta", 1, 6 * minute, kind = RankedKind.CHANNEL)),
                shortsShare = 0f,
                formatMs = mapOf(ViewFormat.LONG to 18 * minute),
            )
        val phone = device("Phone", channels = listOf(ChannelTime("a", "Alpha", 5 * minute), ChannelTime("c", "Gamma", 30 * minute)), longMs = 20 * minute, shortMs = 15 * minute)
        val elsewhere = device("Other month", month = "2026-10", channels = listOf(ChannelTime("z", "Zeta", 99 * minute)), shortMs = 999 * minute)
        val combined = combineTopChannels(own, "2026-11", listOf(phone, elsewhere))!!
        assertThat(combined.channels.map { it.name to it.durationMs }).containsExactly("Gamma" to 30 * minute, "Alpha" to 17 * minute, "Beta" to 6 * minute).inOrder()
        assertThat(combined.shortsShare).isWithin(0.0001f).of(15f / 53f)
    }

    @Test
    fun withoutAnOwnMonthNothingIsShown() {
        assertThat(combineTopChannels(null, "2026-11", emptyList())).isNull()
    }
}
