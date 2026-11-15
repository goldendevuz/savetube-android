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
data class HomeFilterChoices(
    val minViews: Long = 0L,
    val minSubscribers: Long = 0L,
    val durationGroup: String = "any",
    val hideMusicVideos: Boolean = false,
    val feedScope: String = "off",
    val hideWatched: Boolean = false,
    val hideEntertainment: Boolean = false,
    val hideGaming: Boolean = false,
    val languages: String = "any",
) {
    /** Whether any of them is on. The old "g1" length choice is read as any length (it left Home empty). */
    val hasActiveFilter: Boolean
        get() =
            minViews > 0L ||
                minSubscribers > 0L ||
                parseDurationGroups(durationGroup).isNotEmpty() ||
                hideMusicVideos ||
                hideEntertainment ||
                hideGaming ||
                parseHomeLanguages(languages).isNotEmpty() ||
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
            val lengthLabels = parseDurationGroups(choices.durationGroup).mapNotNull { durationGroupName(it) }
            if (lengthLabels.isNotEmpty()) add(stringResource(R.string.home_filter_length, lengthLabels.joinToString(", ")))
            if (choices.hideMusicVideos) add(stringResource(R.string.home_filter_music))
            if (choices.hideEntertainment) add(stringResource(R.string.home_filter_media))
            if (choices.hideGaming) add(stringResource(R.string.home_filter_gaming))
            val languageNames = parseHomeLanguages(choices.languages).map { homeLanguageName(it) }
            if (languageNames.isNotEmpty()) add(stringResource(R.string.home_filter_languages, languageNames.joinToString(", ")))
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

/** The name of a language in its own language, for the settings and the empty-state list. */
@Composable
internal fun homeLanguageName(code: String): String =
    stringResource(
        when (code) {
            "uz" -> R.string.language_name_uz
            "ru" -> R.string.language_name_ru
            "en" -> R.string.language_name_en
            "tr" -> R.string.language_name_tr
            "ar" -> R.string.language_name_ar
            "fa" -> R.string.language_name_fa
            "kk" -> R.string.language_name_kk
            "tg" -> R.string.language_name_tg
            "ky" -> R.string.language_name_ky
            else -> R.string.language_name_ko
        },
    )
