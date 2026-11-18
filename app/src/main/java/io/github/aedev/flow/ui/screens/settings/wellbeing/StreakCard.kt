package io.github.aedev.flow.ui.screens.settings.wellbeing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R
import io.github.aedev.flow.data.wellbeing.LimitStreak

private val BarHeight = 6.dp

/** Days in a row the daily limit held, the best run, the next badge, and the badges earned. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreakCard(
    days: Int,
    best: Int,
) {
    val shownBest = maxOf(best, days)
    val next = LimitStreak.nextMilestone(days)
    val previous = LimitStreak.MILESTONES.lastOrNull { it <= days } ?: 0
    val progress = if (next == null) 1f else (days - previous).toFloat() / (next - previous)
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.streak_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = days.toString(), style = MaterialTheme.typography.displaySmall)
            Text(
                text = stringResource(R.string.streak_unit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 18.dp),
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
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(BarHeight)
                        .clip(RoundedCornerShape(BarHeight))
                        .background(MaterialTheme.colorScheme.primary),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.streak_best, shownBest),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (next == null) stringResource(R.string.streak_all) else stringResource(R.string.streak_next, next),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LimitStreak.MILESTONES.forEach { milestone ->
                val earned = shownBest >= milestone
                Text(
                    text = milestone.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (earned) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
    }
}
