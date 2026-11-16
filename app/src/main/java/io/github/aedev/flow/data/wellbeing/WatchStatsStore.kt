package io.github.aedev.flow.data.wellbeing

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aedev.flow.data.local.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every device's published watch statistics, in the profile's settings store. This device publishes its own entry
 * and sync brings the others; sync merges what arrives into what is stored instead of replacing it.
 */
@Singleton
class WatchStatsStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dailyLimitStore: DailyLimitStore,
    ) {
        /** The statistics of the other devices, by device id. This device's own entry is left out. */
        val others: Flow<Map<String, DeviceStats>> =
            context.dataStore.data.map { prefs ->
                val own = prefs[stringPreferencesKey(DailyLimitStore.DEVICE_ID_KEY)]
                SharedStats.parse(prefs[STATS]).filterKeys { it != own }
            }

        /** Publishes this device's entry, keeping what other devices published. */
        suspend fun publish(entry: DeviceStats) {
            val id = dailyLimitStore.deviceId()
            context.dataStore.edit { prefs ->
                val merged = SharedStats.merge(SharedStats.parse(prefs[STATS]), mapOf(id to entry))
                prefs[STATS] = SharedStats.encode(merged)
            }
        }

        /** The text sync sends, or null before anything was published or received. */
        suspend fun exportText(): String? = context.dataStore.data.first()[STATS]

        /** Merges the statistics a peer sent into the stored ones. */
        suspend fun mergeIn(theirs: String) {
            context.dataStore.edit { prefs ->
                val merged = SharedStats.merge(SharedStats.parse(prefs[STATS]), SharedStats.parse(theirs))
                prefs[STATS] = SharedStats.encode(merged)
            }
        }

        companion object {
            private val STATS = stringPreferencesKey("watch_stats")

            /** The name of the statistics on the wire, the same as in the store. */
            val KEY: String = STATS.name
        }
    }
