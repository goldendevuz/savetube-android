package io.github.aedev.flow.data.wellbeing

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dagger.Lazy
import io.github.aedev.flow.di.ApplicationScope
import io.github.aedev.flow.player.EnhancedMusicPlayerManager
import io.github.aedev.flow.player.EnhancedPlayerManager
import io.github.aedev.flow.player.shorts.ShortsPlayerPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Follows the playing time of this phone: the hours of the day it falls in (for the "when you watch" card) and the
 * current session, which ends after five minutes without playing (for the timer in the player). Nothing runs while
 * nothing plays.
 */
@Singleton
@OptIn(UnstableApi::class)
class WatchTracker
    @Inject
    constructor(
        private val store: DailyLimitStore,
        @ApplicationScope private val scope: CoroutineScope,
        private val videoPlayer: Lazy<EnhancedPlayerManager>,
        private val shortsPlayers: Lazy<ShortsPlayerPool>,
    ) {
        private var sessionMs = 0L

        fun install() {
            scope.launch(Dispatchers.Main.immediate) {
                val video = videoPlayer.get().playerState.map { it.isPlaying }
                val music = EnhancedMusicPlayerManager.playerState.map { it.isPlaying }
                combine(video, shortsPlayers.get().anyPlaying, music) { v, s, m -> v || s || m }
                    .distinctUntilChanged()
                    .collectLatest { playing -> if (playing) track() else endSessionAfterBreak() }
            }
        }

        private suspend fun endSessionAfterBreak() {
            delay(SESSION_BREAK_MS)
            sessionMs = 0L
            SessionClock.set(0)
        }

        private suspend fun track() {
            var since = SystemClock.elapsedRealtime()
            val pending = ArrayList<Triple<Long, Int, Long>>()
            var lastSaved = since

            fun book(now: Long, zone: java.time.ZoneId) {
                val elapsed = (now - since).coerceAtLeast(0L)
                since = now
                val at = ZonedDateTime.now(zone)
                pending.add(Triple(LocalDate.now(zone).toEpochDay(), at.hour, elapsed))
                sessionMs += elapsed
                SessionClock.set((sessionMs / 60_000L).toInt())
            }

            try {
                while (true) {
                    delay(TICK_MS)
                    val now = SystemClock.elapsedRealtime()
                    book(now, store.zone())
                    if (now - lastSaved >= SAVE_EVERY_MS) {
                        store.addHours(pending.toList())
                        pending.clear()
                        lastSaved = now
                    }
                }
            } finally {
                withContext(NonCancellable) {
                    book(SystemClock.elapsedRealtime(), store.zone())
                    store.addHours(pending.toList())
                }
            }
        }

        private companion object {
            const val TICK_MS = 5_000L
            const val SAVE_EVERY_MS = 60_000L
            const val SESSION_BREAK_MS = 5 * 60_000L
        }
    }
