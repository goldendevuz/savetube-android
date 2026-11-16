package io.github.aedev.flow.ui.screens.settings.wellbeing

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.stats.ViewFormat
import org.junit.Test

class TopChannelsStatsTest {
    @Test
    fun shortsShareIsTheShortsTimeOverAllVideoTime() {
        val share = shortsShare(mapOf(ViewFormat.LONG to 60_000L, ViewFormat.SHORT to 20_000L, ViewFormat.LIVE to 20_000L))
        assertThat(share).isWithin(0.0001f).of(0.2f)
        assertThat(shortsShare(mapOf(ViewFormat.SHORT to 5_000L))).isEqualTo(1f)
        assertThat(shortsShare(mapOf(ViewFormat.LONG to 5_000L))).isEqualTo(0f)
    }

    @Test
    fun shortsShareIsNullWhenNothingWasWatched() {
        assertThat(shortsShare(emptyMap())).isNull()
        assertThat(shortsShare(mapOf(ViewFormat.LONG to 0L, ViewFormat.SHORT to 0L))).isNull()
    }

    @Test
    fun percentLabelRoundsToAWholePercentage() {
        assertThat(percentLabel(0.2f)).isEqualTo("20%")
        assertThat(percentLabel(0.3449f)).isEqualTo("34%")
        assertThat(percentLabel(1f)).isEqualTo("100%")
    }
}
