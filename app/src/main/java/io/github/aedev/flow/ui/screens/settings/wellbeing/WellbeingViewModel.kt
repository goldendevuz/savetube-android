package io.github.aedev.flow.ui.screens.settings.wellbeing

import android.content.Context
import android.os.SystemClock
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aedev.flow.data.local.LocalDataManager
import io.github.aedev.flow.data.local.ViewHistory
import io.github.aedev.flow.data.recommendation.music.MusicBrainEngine
import io.github.aedev.flow.data.stats.VideoStatsRecorder
import io.github.aedev.flow.data.wellbeing.DailyLimitPolicy
import io.github.aedev.flow.data.wellbeing.DailyLimitSettings
import io.github.aedev.flow.data.wellbeing.DailyLimitStore
import io.github.aedev.flow.data.wellbeing.TimeZones
import io.github.aedev.flow.data.wellbeing.WatchStatsStore
import io.github.aedev.flow.data.wellbeing.WatchTimeMeasurer
import io.github.aedev.flow.notification.ReminderManager
import io.github.aedev.flow.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/** A daily limit change held back until [waitEndsAt], an elapsedRealtime. */
data class PendingLimitChange(
    val next: DailyLimitSettings,
    val waitEndsAt: Long,
)

@HiltViewModel
class WellbeingViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val viewHistory: ViewHistory,
        private val localData: LocalDataManager,
        private val videoStats: VideoStatsRecorder,
        private val musicBrain: MusicBrainEngine,
        private val dailyLimitStore: DailyLimitStore,
        private val measurer: WatchTimeMeasurer,
        private val watchStatsStore: WatchStatsStore,
    ) : SettingsViewModel() {
        /** This device's own week, measured once per visit and again at midnight so the week ends on the new today. */
        private val ownWeek =
            flow {
                while (true) {
                    val now = ZonedDateTime.now()
                    emit(measurer.week(now.toLocalDate(), now.zone))
                    delay(millisUntilNextDay(now))
                }
            }.flowOn(Dispatchers.Default)

        /** This device's own month of video, measured once per visit. */
        private val ownMonth =
            flow { emit(measurer.month()?.let { summarizeTopChannels(it.video) }) }
                .flowOn(Dispatchers.Default)

        /** The week of all devices together: this device's own plus what the others published. */
        val summary =
            combine(ownWeek, watchStatsStore.others) { own, others -> combineDays(own, others.values) }
                .asState(null)

        /** Each device's part of the week. */
        val deviceTotals =
            combine(ownWeek, watchStatsStore.others) { own, others ->
                deviceTotals(own, dailyLimitStore.deviceName(), others.values)
            }.asState(emptyList())

        /** This month's most watched channels over all devices. */
        val topChannels =
            combine(ownMonth, watchStatsStore.others) { own, others ->
                combineTopChannels(own, YearMonth.now().toString(), others.values)
            }.asState(null)

        val bedtimeEnabled = localData.bedtimeReminder.asState(false)
        val breakEnabled = localData.breakReminder.asState(false)
        val bedtimeStart = timeOf(localData.bedtimeStartHour, localData.bedtimeStartMinute, DEFAULT_BEDTIME)
        val bedtimeEnd = timeOf(localData.bedtimeEndHour, localData.bedtimeEndMinute, DEFAULT_WAKE)
        val breakMinutes = localData.breakFrequency.asState(DEFAULT_BREAK_MINUTES)

        fun setBedtimeEnabled(enabled: Boolean) =
            write {
                localData.setBedtimeReminder(enabled)
                scheduleBedtime(enabled, bedtimeStart.value)
            }

        fun setBedtimeStart(time: LocalTime) =
            write {
                val end = bedtimeEnd.value
                localData.setBedtimeSchedule(time.hour, time.minute, end.hour, end.minute)
                if (bedtimeEnabled.value) scheduleBedtime(true, time)
            }

        fun setBedtimeEnd(time: LocalTime) =
            write {
                val start = bedtimeStart.value
                localData.setBedtimeSchedule(start.hour, start.minute, time.hour, time.minute)
            }

        fun setBreakEnabled(enabled: Boolean) = write { localData.setBreakReminder(enabled) }

        fun setBreakMinutes(minutes: Int) = write { localData.setBreakFrequency(minutes) }

        val dailyLimit = dailyLimitStore.settings.asState(DailyLimitSettings())

        /** The zone the daily limit and the bedtime reminder keep their days and clock in. */
        val timeZone = dailyLimitStore.timeZone.map { it.id }.asState(TimeZones.DEFAULT)

        fun setTimeZone(name: String) =
            write {
                dailyLimitStore.setTimeZone(name)
                if (bedtimeEnabled.value) scheduleBedtime(true, bedtimeStart.value)
            }

        /** Today's count of all devices together, and each device's share of it. */
        val dailyTally = dailyLimitStore.tally.asState(null)
        val dailyDevices = dailyLimitStore.devices.asState(emptyList())

        private val _pendingLimit = MutableStateFlow<PendingLimitChange?>(null)

        /** A change that loosens a limit already used up today, waiting out its pause before it can be applied. */
        val pendingLimit: StateFlow<PendingLimitChange?> = _pendingLimit.asStateFlow()

        fun changeDailyLimit(next: DailyLimitSettings) =
            write {
                val current = dailyLimitStore.settings.first()
                val tally = dailyLimitStore.tally.first()
                if (DailyLimitPolicy.changeNeedsWait(current, next, tally, LocalDate.now(dailyLimitStore.zone()).toEpochDay())) {
                    _pendingLimit.value = PendingLimitChange(next, SystemClock.elapsedRealtime() + DailyLimitPolicy.WAIT_MS)
                } else {
                    dailyLimitStore.setSettings(next)
                }
            }

        fun confirmPendingLimit() {
            val pending = _pendingLimit.value ?: return
            if (SystemClock.elapsedRealtime() < pending.waitEndsAt) return
            _pendingLimit.value = null
            write { dailyLimitStore.setSettings(pending.next) }
        }

        fun dismissPendingLimit() {
            _pendingLimit.value = null
        }

        private fun timeOf(
            hour: Flow<Int>,
            minute: Flow<Int>,
            initial: LocalTime,
        ) = combine(hour, minute) { h, m -> LocalTime.of(h.coerceIn(0, 23), m.coerceIn(0, 59)) }
            .asState(initial)

        private suspend fun scheduleBedtime(
            enabled: Boolean,
            start: LocalTime,
        ) = runCatching {
            if (enabled) {
                ReminderManager.scheduleBedtimeReminder(context, start.hour, start.minute, dailyLimitStore.zone())
            } else {
                ReminderManager.cancelBedtimeReminder(context)
            }
        }.onFailure { Log.w(TAG, "Bedtime reminder could not be scheduled", it) }

        private companion object {
            const val TAG = "WellbeingViewModel"
            const val DEFAULT_BREAK_MINUTES = 30
            val DEFAULT_BEDTIME: LocalTime = LocalTime.of(23, 0)
            val DEFAULT_WAKE: LocalTime = LocalTime.of(7, 0)
        }
    }
