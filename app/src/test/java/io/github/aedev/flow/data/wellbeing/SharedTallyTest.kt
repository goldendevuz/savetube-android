package io.github.aedev.flow.data.wellbeing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SharedTallyTest {
    private val minute = 60_000L

    @Test
    fun theTotalIsTheSumOfTheDevicesOfTheDay() {
        val shared =
            mapOf(
                "phone" to DeviceCount(day = 10, playedMs = 30 * minute, extraMs = 15 * minute),
                "laptop" to DeviceCount(day = 10, playedMs = 20 * minute, reached = true),
                "old" to DeviceCount(day = 9, playedMs = 90 * minute),
            )
        val total = SharedTally.aggregate(shared, 10)
        assertThat(total.playedMs).isEqualTo(50 * minute)
        assertThat(total.extraMs).isEqualTo(15 * minute)
        assertThat(total.reached).isTrue()
        assertThat(total.warned).isFalse()
    }

    @Test
    fun mergingKeepsEachDevicesLargerCountAndNeverAddsTwice() {
        val a = mapOf("phone" to DeviceCount(10, 30 * minute), "laptop" to DeviceCount(10, 5 * minute))
        val b = mapOf("phone" to DeviceCount(10, 10 * minute), "laptop" to DeviceCount(10, 25 * minute))
        val merged = SharedTally.merge(a, b)
        assertThat(merged.getValue("phone").playedMs).isEqualTo(30 * minute)
        assertThat(merged.getValue("laptop").playedMs).isEqualTo(25 * minute)
        assertThat(SharedTally.merge(merged, merged)).isEqualTo(merged)
    }

    @Test
    fun aLaterDayReplacesAnEarlierOneAndFlagsAreKeptOnTheSameDay() {
        val merged =
            SharedTally.merge(
                mapOf("phone" to DeviceCount(10, 99 * minute, reached = true)),
                mapOf("phone" to DeviceCount(11, 1 * minute)),
            )
        assertThat(merged.getValue("phone")).isEqualTo(DeviceCount(11, 1 * minute))
        val sameDay =
            SharedTally.merge(
                mapOf("phone" to DeviceCount(10, 5 * minute, warned = true)),
                mapOf("phone" to DeviceCount(10, 6 * minute, reached = true)),
            )
        assertThat(sameDay.getValue("phone").warned).isTrue()
        assertThat(sameDay.getValue("phone").reached).isTrue()
    }

    @Test
    fun aDeviceBooksOnlyItsOwnGrowth() {
        val shared = mapOf("laptop" to DeviceCount(10, 20 * minute))
        // The phone is told the all-devices total is now 25 minutes: 5 of them are its own.
        val next = SharedTally.withOwn(shared, "phone", "Phone", DailyTally(day = 10, playedMs = 25 * minute))
        assertThat(next.getValue("laptop").playedMs).isEqualTo(20 * minute)
        assertThat(next.getValue("phone").playedMs).isEqualTo(5 * minute)
        assertThat(SharedTally.aggregate(next, 10).playedMs).isEqualTo(25 * minute)
    }

    @Test
    fun theEntriesSurviveTheTripThroughText() {
        val shared = mapOf("phone" to DeviceCount(10, 3 * minute, 15 * minute, reached = true, warned = true))
        assertThat(SharedTally.parse(SharedTally.encode(shared))).isEqualTo(shared)
        assertThat(SharedTally.parse("not json")).isEmpty()
        assertThat(SharedTally.parse("{\"day\":1,\"playedMs\":2}")).isEmpty()
    }
}
