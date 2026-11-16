package io.github.aedev.flow.data.wellbeing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aedev.flow.data.local.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The daily limit's settings and today's count, in the profile's own settings store next to the break reminder. The
 * settings go into a settings backup like the reminders do and travel with sync (see [exportSettings] and
 * [restoreSettings]); the count does not, it stays on each device.
 */
class DailyLimitStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val settings: Flow<DailyLimitSettings> =
            context.dataStore.data.map { prefs ->
                DailyLimitSettings(
                    enabled = prefs[ENABLED] ?: false,
                    minutes = DailyLimitPolicy.validMinutes(prefs[MINUTES]),
                    countMusic = prefs[COUNT_MUSIC] ?: false,
                )
            }

        /** The stored count, or null before anything was counted. */
        val tally: Flow<DailyTally?> =
            context.dataStore.data.map { prefs ->
                prefs[TALLY_DAY]?.let { day ->
                    DailyTally(
                        day = day,
                        playedMs = prefs[TALLY_PLAYED_MS] ?: 0L,
                        extraMs = prefs[TALLY_EXTRA_MS] ?: 0L,
                        warned = prefs[TALLY_WARNED] ?: false,
                        reached = prefs[TALLY_REACHED] ?: false,
                    )
                }
            }

        suspend fun setSettings(settings: DailyLimitSettings) {
            context.dataStore.edit { prefs ->
                prefs[ENABLED] = settings.enabled
                prefs[MINUTES] = DailyLimitPolicy.validMinutes(settings.minutes)
                prefs[COUNT_MUSIC] = settings.countMusic
            }
        }

        /** The settings that have been set, by name, for sync. Settings never set are left out so they stay as they are. */
        suspend fun exportSettings(): Pair<Map<String, Boolean>, Map<String, Int>> {
            val prefs = context.dataStore.data.first()
            val booleans = buildMap {
                prefs[ENABLED]?.let { put(ENABLED.name, it) }
                prefs[COUNT_MUSIC]?.let { put(COUNT_MUSIC.name, it) }
            }
            val ints = buildMap { prefs[MINUTES]?.let { put(MINUTES.name, it) } }
            return booleans to ints
        }

        /** Writes the settings that arrived by sync; names that did not arrive keep their value. */
        suspend fun restoreSettings(
            booleans: Map<String, Boolean>,
            ints: Map<String, Int>,
        ) {
            if (booleans.keys.none { it in SETTING_NAMES } && ints.keys.none { it in SETTING_NAMES }) return
            context.dataStore.edit { prefs ->
                booleans[ENABLED.name]?.let { prefs[ENABLED] = it }
                booleans[COUNT_MUSIC.name]?.let { prefs[COUNT_MUSIC] = it }
                ints[MINUTES.name]?.let { prefs[MINUTES] = DailyLimitPolicy.validMinutes(it) }
            }
        }

        suspend fun setTally(tally: DailyTally) {
            context.dataStore.edit { prefs ->
                prefs[TALLY_DAY] = tally.day
                prefs[TALLY_PLAYED_MS] = tally.playedMs
                prefs[TALLY_EXTRA_MS] = tally.extraMs
                prefs[TALLY_WARNED] = tally.warned
                prefs[TALLY_REACHED] = tally.reached
            }
        }

        companion object {
            private val ENABLED = booleanPreferencesKey("daily_limit")
            private val MINUTES = intPreferencesKey("daily_limit_minutes")
            private val COUNT_MUSIC = booleanPreferencesKey("daily_limit_music")
            private val TALLY_DAY = longPreferencesKey("daily_tally_day")
            private val TALLY_PLAYED_MS = longPreferencesKey("daily_tally_played_ms")
            private val TALLY_EXTRA_MS = longPreferencesKey("daily_tally_extra_ms")
            private val TALLY_WARNED = booleanPreferencesKey("daily_tally_warned")
            private val TALLY_REACHED = booleanPreferencesKey("daily_tally_reached")

            /** The keys a settings backup carries. */
            val SETTING_NAMES: Set<String> = setOf(ENABLED.name, MINUTES.name, COUNT_MUSIC.name)

            /** Today's running count, which a backup leaves behind. */
            val TALLY_NAMES: Set<String> =
                setOf(TALLY_DAY.name, TALLY_PLAYED_MS.name, TALLY_EXTRA_MS.name, TALLY_WARNED.name, TALLY_REACHED.name)
        }
    }
