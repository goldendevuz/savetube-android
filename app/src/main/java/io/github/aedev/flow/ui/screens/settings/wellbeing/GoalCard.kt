package io.github.aedev.flow.ui.screens.settings.wellbeing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R
import io.github.aedev.flow.data.wellbeing.DailyGoal

/** One thing to get done today, written by the user and ticked off when done. */
@Composable
fun GoalCard(
    goal: DailyGoal?,
    onSave: (text: String, done: Boolean) -> Unit,
) {
    // another device changed the goal, or the day turned over
    var draft by remember(goal?.text) { mutableStateOf(goal?.text.orEmpty()) }
    val commit = { if (draft.trim() != goal?.text.orEmpty()) onSave(draft, goal?.done ?: false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.goal_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Checkbox(checked = goal?.done ?: false, enabled = goal != null, onCheckedChange = { goal?.let { g -> onSave(g.text, it) } })
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(DailyGoal.MAX_LENGTH) },
                placeholder = { Text(stringResource(R.string.goal_placeholder)) },
                singleLine = true,
                textStyle =
                    MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (goal?.done == true) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) commit() },
            )
            if (goal != null) {
                TextButton(onClick = { draft = ""; onSave("", false) }) { Text(stringResource(R.string.goal_clear)) }
            }
        }
        Text(
            text = stringResource(R.string.goal_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
