package io.github.aedev.flow.data.wellbeing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SharedStatsTest {
    private val minute = 60_000L

    private fun stats(
        name: String = "",
        at: Long = 1_000L,
        month: String = "2026-11",
        days: Map<String, Long> = emptyMap(),
        channels: List<ChannelTime> = emptyList(),
    ) = DeviceStats(name = name, publishedAt = at, month = month, days = days, channels = channels, longMs = 1L, shortMs = 2L, liveMs = 3L)

    @Test
    fun theNewestPublicationOfEachDeviceStandsAndMergingTwiceChangesNothing() {
        val a = mapOf("phone" to stats("old", 2_000), "laptop" to stats("Laptop", 5_000))
        val b = mapOf("phone" to stats("new", 3_000), "laptop" to stats("stale", 4_000))
        val merged = SharedStats.merge(a, b)
        assertThat(merged.getValue("phone").name).isEqualTo("new")
        assertThat(merged.getValue("laptop").name).isEqualTo("Laptop")
        assertThat(SharedStats.merge(merged, merged)).isEqualTo(merged)
    }

    @Test
    fun aDeviceNotHeardFromForWeeksIsDropped() {
        val day = 24L * 60 * 60 * 1000
        val merged = SharedStats.merge(mapOf("gone" to stats(at = 1_000)), mapOf("here" to stats(at = 60 * day)))
        assertThat(merged.keys).containsExactly("here")
    }

    @Test
    fun theStatisticsSurviveTheTripThroughTextAndDamagedTextIsIgnored() {
        val shared =
            mapOf(
                "me" to stats("Pixel", 7, days = mapOf("2026-11-16" to 5 * minute), channels = listOf(ChannelTime("c1", "One", minute))),
            )
        assertThat(SharedStats.parse(SharedStats.encode(shared))).isEqualTo(shared)
        assertThat(SharedStats.parse("not json")).isEmpty()
        assertThat(SharedStats.parse("{\"x\":{\"no\":1}}")).isEmpty()
        assertThat(SharedStats.parse(null)).isEmpty()
    }
}
