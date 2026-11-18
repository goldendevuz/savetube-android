package io.github.aedev.flow.data.wellbeing

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aedev.flow.R
import io.github.aedev.flow.di.ApplicationScope
import io.github.aedev.flow.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the daily limit streak up to date while the limit is on: starts it, marks a day broken once the limit ran
 * out (or "Keep watching" was used), and announces a new badge once. It follows the count, so nothing runs while
 * nothing is played.
 */
@Singleton
class LimitStreakKeeper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val store: DailyLimitStore,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        fun install() {
            scope.launch(Dispatchers.Default) {
                store.settings.map { it.enabled }.distinctUntilChanged().collectLatest { enabled ->
                    if (!enabled) return@collectLatest
                    store.tally.distinctUntilChanged().collect { tally -> update(tally) }
                }
            }
        }

        private suspend fun update(tally: DailyTally?) {
            val today = LocalDate.now(store.zone()).toEpochDay()
            var milestone: Int? = null
            store.updateStreak { saved ->
                var next = LimitStreak.record(saved, today, false)
                // the limit ran out on the day of the count, which can be yesterday just after midnight
                if (tally != null && (tally.reached || tally.extraMs > 0) && tally.day >= next.start && tally.day <= today) {
                    next = LimitStreak.record(next, tally.day, true)
                }
                milestone = LimitStreak.newMilestone(next, today)
                milestone?.let { next = next.copy(announced = it) }
                next
            }
            milestone?.let { days ->
                NotificationHelper.showReminderNotification(
                    context,
                    context.getString(R.string.streak_milestone_title, days),
                    context.getString(R.string.streak_milestone_body, days),
                )
            }
        }
    }
