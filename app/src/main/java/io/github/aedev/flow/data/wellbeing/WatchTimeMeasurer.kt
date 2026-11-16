package io.github.aedev.flow.data.wellbeing

import io.github.aedev.flow.data.local.ViewHistory
import io.github.aedev.flow.data.recommendation.music.MusicBrainEngine
import io.github.aedev.flow.data.recommendation.music.MusicStatsStorage
import io.github.aedev.flow.data.stats.RecapAggregates
import io.github.aedev.flow.data.stats.RecapPeriod
import io.github.aedev.flow.data.stats.RecapSummary
import io.github.aedev.flow.data.stats.VideoStatsRecorder
import io.github.aedev.flow.ui.screens.settings.wellbeing.WEEK_DAYS
import io.github.aedev.flow.ui.screens.settings.wellbeing.WatchRecord
import io.github.aedev.flow.ui.screens.settings.wellbeing.WatchTimeSummary
import io.github.aedev.flow.ui.screens.settings.wellbeing.summarizeLedgerTime
import io.github.aedev.flow.ui.screens.settings.wellbeing.summarizeWatchTime
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Measures this device's own watching: the last week, and this month's recap. Shared by the page and the publisher. */
@Singleton
class WatchTimeMeasurer
    @Inject
    constructor(
        private val videoStats: VideoStatsRecorder,
        private val musicBrain: MusicBrainEngine,
        private val viewHistory: ViewHistory,
    ) {
        /**
         * The recap ledgers' real watching time is used once they cover the whole week; until then the
         * history estimate (last position on the last-watched day) stands in.
         */
        suspend fun week(
            today: LocalDate,
            zone: ZoneId,
        ): WatchTimeSummary {
            val video = runCatching { videoStats.snapshot() }.getOrNull()
            val ledger =
                video?.let {
                    val music = runCatching { musicBrain.listeningStats() }.getOrDefault(MusicStatsStorage.SerializableStats())
                    val days = RecapAggregates.dailyTime(it, music, today.minusDays((WEEK_DAYS - 1).toLong()), today)
                    summarizeLedgerTime(days, RecapAggregates.videoLedgerStart(it), today)
                }
            return ledger ?: summarizeWatchTime(
                viewHistory.getAllHistory().first().map { WatchRecord(it.timestamp, it.position) },
                today,
                zone,
            )
        }

        /** This month's recap, or null when the ledger cannot be read. */
        suspend fun month(): RecapSummary? {
            val video = runCatching { videoStats.snapshot() }.getOrNull() ?: return null
            val music = runCatching { musicBrain.listeningStats() }.getOrDefault(MusicStatsStorage.SerializableStats())
            return RecapAggregates.summarize(RecapPeriod.Month(YearMonth.now()), video, music)
        }
    }
