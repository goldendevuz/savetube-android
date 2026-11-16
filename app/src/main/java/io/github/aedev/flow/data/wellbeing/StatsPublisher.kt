package io.github.aedev.flow.data.wellbeing

import android.util.Log
import io.github.aedev.flow.data.stats.ViewFormat
import io.github.aedev.flow.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Publishes this device's watch statistics a little after the app starts and every ten minutes while it runs. */
@Singleton
class StatsPublisher
    @Inject
    constructor(
        private val measurer: WatchTimeMeasurer,
        private val store: WatchStatsStore,
        private val dailyLimitStore: DailyLimitStore,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        fun install() {
            scope.launch {
                delay(FIRST_DELAY_MS)
                while (true) {
                    runCatching { publish() }.onFailure { Log.w(TAG, "Could not publish the watch statistics", it) }
                    delay(EVERY_MS)
                }
            }
        }

        suspend fun publish() {
            val zone = ZoneId.systemDefault()
            val week = measurer.week(LocalDate.now(zone), zone)
            val month = measurer.month()
            val video = month?.video
            store.publish(
                DeviceStats(
                    name = dailyLimitStore.deviceName(),
                    publishedAt = System.currentTimeMillis(),
                    month = YearMonth.now().toString(),
                    days = week.days.associate { it.date.toString() to it.millis },
                    channels =
                        video
                            ?.topChannels
                            ?.sortedByDescending { it.durationMs }
                            ?.take(CHANNELS)
                            ?.map { ChannelTime(it.id, it.name, it.durationMs) }
                            .orEmpty(),
                    longMs = video?.formatMs?.get(ViewFormat.LONG) ?: 0L,
                    shortMs = video?.formatMs?.get(ViewFormat.SHORT) ?: 0L,
                    liveMs = video?.formatMs?.get(ViewFormat.LIVE) ?: 0L,
                ),
            )
        }

        private companion object {
            const val TAG = "StatsPublisher"
            const val FIRST_DELAY_MS = 20_000L
            const val EVERY_MS = 10 * 60_000L
            const val CHANNELS = 15
        }
    }
