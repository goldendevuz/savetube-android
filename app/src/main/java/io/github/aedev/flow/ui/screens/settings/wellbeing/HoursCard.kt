package io.github.aedev.flow.ui.screens.settings.wellbeing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R
import io.github.aedev.flow.data.wellbeing.WatchHours
import io.github.aedev.flow.ui.components.stats.spentTimeLabel

private val LabelHours = listOf(0, 6, 12, 18, 23)

/** The 24 hours of the day, the bar of each as tall as the time played then, over the last 28 days. */
@Composable
fun HoursCard(summary: WatchHours.Summary) {
    val peak = (summary.byHour.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.hours_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val peakHour = summary.peakHour
        if (peakHour == null) {
            Text(
                text = stringResource(R.string.hours_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        Text(
            text = stringResource(R.string.hours_peak, "$peakHour:00–${(peakHour + 1) % 24}:00"),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            summary.byHour.forEachIndexed { hour, ms ->
                val color = MaterialTheme.colorScheme.primary
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight((ms.toFloat() / peak).coerceIn(0.05f, 1f))
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (hour == peakHour) color else color.copy(alpha = 0.4f)),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LabelHours.forEach { hour ->
                Text(text = "$hour:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text = stringResource(R.string.hours_total, spentTimeLabel(summary.total)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
