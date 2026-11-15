package io.github.aedev.flow.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R

/** The Home settings that can leave the feed empty, as the viewer chose them. */
internal data class HomeFilterChoices(
    val minViews: Long = 0L,
    val minSubscribers: Long = 0L,
    val durationGroup: String = "any",
    val hideMusicVideos: Boolean = false,
    val feedScope: String = "off",
    val hideWatched: Boolean = false,
) {
    /** Whether any of them is on. The old "g1" length choice is read as any length (it left Home empty). */
    val hasActiveFilter: Boolean
        get() =
            minViews > 0L ||
                minSubscribers > 0L ||
                (durationGroup in DURATION_GROUPS && durationGroup != "g1") ||
                hideMusicVideos ||
                feedScope == "subscribed" ||
                feedScope == "unsubscribed" ||
                hideWatched
}

/**
 * Shown when Home has nothing to show and the viewer has filters on: it lists them, so a strict filter that
 * was forgotten is easy to find.
 */
@Composable
internal fun HomeEmptyState(
    choices: HomeFilterChoices,
    modifier: Modifier = Modifier,
) {
    val filters =
        buildList {
            if (choices.minViews > 0L) add(stringResource(R.string.home_filter_min_views, "%,d".format(choices.minViews)))
            if (choices.minSubscribers > 0L) {
                add(stringResource(R.string.home_filter_min_subscribers, "%,d".format(choices.minSubscribers)))
            }
            val lengthLabel = durationGroupName(choices.durationGroup)
            if (lengthLabel != null) add(stringResource(R.string.home_filter_length, lengthLabel))
            if (choices.hideMusicVideos) add(stringResource(R.string.home_filter_music))
            if (choices.feedScope == "subscribed") add(stringResource(R.string.home_filter_scope_subscribed))
            if (choices.feedScope == "unsubscribed") add(stringResource(R.string.home_filter_scope_unsubscribed))
            if (choices.hideWatched) add(stringResource(R.string.home_filter_seen))
        }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.home_empty_filters_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            filters.forEach { filter ->
                Text(
                    text = "• $filter",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = stringResource(R.string.home_empty_filters_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The label of a length group in the empty-state list; null for any length (and the old "g1"). */
@Composable
private fun durationGroupName(group: String): String? =
    when (group) {
        "g2" -> stringResource(R.string.content_settings_home_duration_g2)
        "g3" -> stringResource(R.string.content_settings_home_duration_g3)
        "g4" -> stringResource(R.string.content_settings_home_duration_g4)
        "g5" -> stringResource(R.string.content_settings_home_duration_g5)
        "g6" -> stringResource(R.string.content_settings_home_duration_g6)
        "g7" -> stringResource(R.string.content_settings_home_duration_g7)
        "g8" -> stringResource(R.string.content_settings_home_duration_g8)
        else -> null
    }
