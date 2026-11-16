package io.github.aedev.flow.data.wellbeing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimeZonesTest {
    @Test
    fun tashkentIsTheDefaultAndABadNameFallsBackToIt() {
        assertThat(TimeZones.DEFAULT).isEqualTo("Asia/Tashkent")
        assertThat(TimeZones.zoneOf(null).id).isEqualTo("Asia/Tashkent")
        assertThat(TimeZones.zoneOf("").id).isEqualTo("Asia/Tashkent")
        assertThat(TimeZones.zoneOf("Mars/Olympus").id).isEqualTo("Asia/Tashkent")
        assertThat(TimeZones.zoneOf("Europe/Berlin").id).isEqualTo("Europe/Berlin")
    }

    @Test
    fun utcAndItsAliasesAreNotChosenAndNotListed() {
        for (utc in listOf("UTC", "Etc/UTC", "GMT", "Etc/GMT", "Zulu")) {
            assertThat(TimeZones.zoneOf(utc).id).isEqualTo("Asia/Tashkent")
            assertThat(TimeZones.selectable()).doesNotContain(utc)
        }
    }

    @Test
    fun theListHoldsTheZonesOfPlaces() {
        val zones = TimeZones.selectable()
        assertThat(zones).contains("Asia/Tashkent")
        assertThat(zones).contains("Europe/Berlin")
        assertThat(zones.size).isGreaterThan(100)
        assertThat(zones).isInOrder()
    }
}
