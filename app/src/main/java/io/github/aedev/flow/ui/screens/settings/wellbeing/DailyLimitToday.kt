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
import io.github.aedev.flow.data.wellbeing.DailyLimitPolicy
import io.github.aedev.flow.data.wellbeing.DailyLimitSettings
import io.github.aedev.flow.data.wellbeing.DailyTally
import io.github.aedev.flow.data.wellbeing.SharedTally
import io.github.aedev.flow.ui.components.stats.spentTimeLabel

private val CardPadding = 20.dp
private val BarHeight = 6.dp

/** Today's time on all devices together against the limit, then each device's share of it. */
@Composable
fun DailyLimitToday(
    limit: DailyLimitSettings,
    tally: DailyTally?,
    devices: List<SharedTally.DeviceShare>,
) {
    if (tally == null) return
    val allowanceMs = DailyLimitPolicy.allowanceMs(limit.minutes, tally)
    val leftMs = DailyLimitPolicy.remainingMs(limit.minutes, tally)
    Column(
        modifier = Modifier.fillMaxWidth().padding(CardPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.daily_limit_today_total, spentTimeLabel(tally.playedMs), spentTimeLabel(allowanceMs)),
            style = MaterialTheme.typography.bodyMedium,
        )
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
                        .fillMaxWidth((tally.playedMs.toFloat() / allowanceMs.coerceAtLeast(1L)).coerceIn(0f, 1f))
                        .height(BarHeight)
                        .clip(RoundedCornerShape(BarHeight))
                        .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text(
            text = stringResource(R.string.daily_limit_left, spentTimeLabel(leftMs)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        devices.forEach { device ->
            val name = device.name.ifEmpty { stringResource(R.string.daily_limit_device_unnamed) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (device.own) stringResource(R.string.daily_limit_device_own, name) else name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(text = spentTimeLabel(device.playedMs), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
