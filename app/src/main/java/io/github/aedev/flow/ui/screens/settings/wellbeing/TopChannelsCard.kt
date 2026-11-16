package io.github.aedev.flow.ui.screens.settings.wellbeing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R
import io.github.aedev.flow.ui.components.stats.spentTimeLabel

private val CardPadding = 20.dp
private val BarHeight = 6.dp
private const val MIN_BAR_FRACTION = 0.04f

/** The channels this month's watching went to, with a bar each, and the share that was Shorts. */
@Composable
fun TopChannelsCard(summary: TopChannelsSummary?) {
    if (summary == null) return
    Column(
        modifier = Modifier.fillMaxWidth().padding(CardPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.top_channels_month_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (summary.channels.isEmpty()) {
            Text(
                text = stringResource(R.string.no_watch_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val longest = summary.channels.first().durationMs.coerceAtLeast(1L)
            summary.channels.forEachIndexed { index, channel ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "${index + 1}  ${channel.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = spentTimeLabel(channel.durationMs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(BarHeight)
                                .clip(RoundedCornerShape(BarHeight))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth((channel.durationMs.toFloat() / longest).coerceIn(MIN_BAR_FRACTION, 1f))
                                    .height(BarHeight)
                                    .clip(RoundedCornerShape(BarHeight))
                                    .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
        summary.shortsShare?.let { share ->
            Text(
                text = stringResource(R.string.shorts_share_label, percentLabel(share)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
