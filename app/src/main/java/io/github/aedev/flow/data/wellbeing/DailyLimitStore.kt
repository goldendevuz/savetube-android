package io.github.aedev.flow.data.wellbeing

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aedev.flow.data.local.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import java.util.UUID
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

        /** The zone the daily limit and the bedtime reminder keep their days and clock in. */
        val timeZone: Flow<ZoneId> = context.dataStore.data.map { prefs -> TimeZones.zoneOf(prefs[TIME_ZONE]) }

        suspend fun zone(): ZoneId = timeZone.first()

        suspend fun setTimeZone(name: String) {
            context.dataStore.edit { prefs -> prefs[TIME_ZONE] = TimeZones.zoneOf(name).id }
        }

        /** The count of the newest day, all devices together, or null before anything was counted. */
        val tally: Flow<DailyTally?> =
            context.dataStore.data.map { prefs ->
                val shared = SharedTally.parse(prefs[SHARED])
                SharedTally.latestDay(shared)?.let { SharedTally.aggregate(shared, it) }
            }

        suspend fun setSettings(settings: DailyLimitSettings) {
            context.dataStore.edit { prefs ->
                prefs[ENABLED] = settings.enabled
                prefs[MINUTES] = DailyLimitPolicy.validMinutes(settings.minutes)
                prefs[COUNT_MUSIC] = settings.countMusic
            }
        }

        /** The daily limit streak (see [LimitStreak]), or null before the limit was ever watched. */
        val streak: Flow<LimitStreak?> = context.dataStore.data.map { prefs -> LimitStreak.parse(prefs[STREAK]) }

        /** Reads the saved streak, lets [transform] change it, and saves the result if it differs. */
        suspend fun updateStreak(transform: (LimitStreak?) -> LimitStreak) {
            context.dataStore.edit { prefs ->
                val saved = LimitStreak.parse(prefs[STREAK])
                val next = transform(saved)
                if (next != saved) prefs[STREAK] = next.encode()
            }
        }

        /** Today's goal record (see [DailyGoal]), or null when none was ever written. */
        val goal: Flow<DailyGoal?> = context.dataStore.data.map { prefs -> DailyGoal.parse(prefs[GOAL]) }

        suspend fun setGoal(goal: DailyGoal) {
            context.dataStore.edit { prefs -> prefs[GOAL] = DailyGoal.merge(DailyGoal.parse(prefs[GOAL]), goal)?.encode() ?: goal.encode() }
        }

        /** This device's playing time by day and hour (see [WatchHours]); never synced. */
        val hours: Flow<Map<Long, List<Long>>> = context.dataStore.data.map { prefs -> WatchHours.parse(prefs[HOURS]) }

        /** Adds playing time, as (epoch day, hour, ms), to the hours record. */
        suspend fun addHours(entries: List<Triple<Long, Int, Long>>) {
            if (entries.isEmpty()) return
            context.dataStore.edit { prefs ->
                var ledger = WatchHours.parse(prefs[HOURS])
                for ((day, hour, ms) in entries) ledger = WatchHours.add(ledger, day, hour, ms)
                prefs[HOURS] = WatchHours.encode(ledger)
            }
        }

        /** Each device's share of the newest day, the biggest first, with this device marked. */
        val devices: Flow<List<SharedTally.DeviceShare>> =
            context.dataStore.data.map { prefs ->
                val shared = SharedTally.parse(prefs[SHARED])
                SharedTally.latestDay(shared)?.let { SharedTally.shares(shared, it, prefs[DEVICE_ID].orEmpty()) }.orEmpty()
            }

        /** This device's id in the shared counts: made once and kept; it is never synced. */
        suspend fun deviceId(): String {
            context.dataStore.data.first()[DEVICE_ID]?.let { return it }
            var id = ""
            context.dataStore.edit { prefs ->
                id = prefs[DEVICE_ID] ?: UUID.randomUUID().toString().take(8).also { prefs[DEVICE_ID] = it }
            }
            return id
        }

        /** What this phone is called in the list of devices. */
        fun deviceName(): String = Build.MODEL.orEmpty().ifBlank { Build.DEVICE.orEmpty() }

        /** What sync sends: the settings that have been set, and the shared count. Settings never set stay out. */
        suspend fun exportSettings(): SettingsExport {
            val prefs = context.dataStore.data.first()
            val booleans =
                buildMap {
                    prefs[ENABLED]?.let { put(ENABLED.name, it) }
                    prefs[COUNT_MUSIC]?.let { put(COUNT_MUSIC.name, it) }
                }
            val ints = buildMap { prefs[MINUTES]?.let { put(MINUTES.name, it) } }
            val strings =
                buildMap {
                    prefs[SHARED]?.let { put(SHARED.name, it) }
                    prefs[TIME_ZONE]?.let { put(TIME_ZONE.name, it) }
                    prefs[STREAK]?.let { put(STREAK.name, it) }
                    prefs[GOAL]?.let { put(GOAL.name, it) }
                }
            return SettingsExport(booleans, ints, strings)
        }

        /** Writes what arrived by sync. Settings that did not arrive keep their value; the counts are merged in. */
        suspend fun restoreSettings(incoming: SettingsExport) {
            val theirs = incoming.strings[SHARED.name]
            val hasSettings = incoming.booleans.keys.any { it in SETTING_NAMES } || incoming.ints.keys.any { it in SETTING_NAMES }
            val zone = incoming.strings[TIME_ZONE.name]
            val streak = incoming.strings[STREAK.name]
            val goal = incoming.strings[GOAL.name]
            if (!hasSettings && theirs == null && zone == null && streak == null && goal == null) return
            context.dataStore.edit { prefs ->
                incoming.booleans[ENABLED.name]?.let { prefs[ENABLED] = it }
                incoming.booleans[COUNT_MUSIC.name]?.let { prefs[COUNT_MUSIC] = it }
                incoming.ints[MINUTES.name]?.let { prefs[MINUTES] = DailyLimitPolicy.validMinutes(it) }
                zone?.let { prefs[TIME_ZONE] = TimeZones.zoneOf(it).id }
                if (goal != null) {
                    // the copy with the newer edit time wins
                    DailyGoal.merge(DailyGoal.parse(prefs[GOAL]), DailyGoal.parse(goal))?.let { prefs[GOAL] = it.encode() }
                }
                if (streak != null) {
                    // a broken day is never forgotten: the arriving record is merged into this device's own
                    LimitStreak.merge(LimitStreak.parse(prefs[STREAK]), LimitStreak.parse(streak))?.let { prefs[STREAK] = it.encode() }
                }
                if (theirs != null) {
                    val merged = SharedTally.merge(SharedTally.parse(prefs[SHARED]), SharedTally.parse(theirs))
                    prefs[SHARED] = SharedTally.encode(merged)
                }
            }
        }

        /** Books [tally], the all-devices count of its day, as this device's own share of it. */
        suspend fun setTally(tally: DailyTally) {
            context.dataStore.edit { prefs ->
                val deviceId = prefs[DEVICE_ID] ?: UUID.randomUUID().toString().take(8).also { prefs[DEVICE_ID] = it }
                prefs[SHARED] = SharedTally.encode(SharedTally.withOwn(SharedTally.parse(prefs[SHARED]), deviceId, deviceName(), tally))
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
            private val TIME_ZONE = stringPreferencesKey("time_zone")
            private val SHARED = stringPreferencesKey("daily_time")
            private val STREAK = stringPreferencesKey("limit_streak")
            private val GOAL = stringPreferencesKey("daily_goal")
            private val HOURS = stringPreferencesKey("watch_hours")
            /** The name this device's id is kept under. */
            const val DEVICE_ID_KEY = "daily_limit_device_id"
            private val DEVICE_ID = stringPreferencesKey(DEVICE_ID_KEY)

            /** The name of the shared count, which is also its name on the wire. */
            val SHARED_KEY: String = SHARED.name

            /** The keys a settings backup carries. */
            val SETTING_NAMES: Set<String> = setOf(ENABLED.name, MINUTES.name, COUNT_MUSIC.name, TIME_ZONE.name, STREAK.name, GOAL.name)

            /** Today's running count, which a backup leaves behind. */
            val TALLY_NAMES: Set<String> =
                setOf(
                    TALLY_DAY.name,
                    TALLY_PLAYED_MS.name,
                    TALLY_EXTRA_MS.name,
                    TALLY_WARNED.name,
                    TALLY_REACHED.name,
                    SHARED.name,
                    DEVICE_ID.name,
                    // The published watch statistics (see WatchStatsStore), rebuilt from the ledger.
                    "watch_stats",
                    // This device's playing time by hour of day (see WatchHours).
                    HOURS.name,
                )
        }
    }

/** The daily limit's part of a sync: booleans, whole numbers and text, by name. */
data class SettingsExport(
    val booleans: Map<String, Boolean> = emptyMap(),
    val ints: Map<String, Int> = emptyMap(),
    val strings: Map<String, String> = emptyMap(),
)
