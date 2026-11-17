package io.github.aedev.flow.data.focus

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aedev.flow.data.local.dataStore
import io.github.aedev.flow.data.wellbeing.SettingsExport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** The focus options: what to hide, blur or turn to grey. All off by default. */
data class FocusSettings(
    val hideThumbnails: Boolean = false,
    val blurThumbnails: Boolean = false,
    val hideTopBar: Boolean = false,
    val disableEndCards: Boolean = false,
    val blackWhite: Boolean = false,
    val hideSubscriptionsNav: Boolean = false,
)

/**
 * The focus options, in the profile's settings store. They go into a settings backup and travel with sync under the
 * same names as on the desktop app (see [exportSettings] and [restoreSettings]).
 */
class FocusStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val settings: Flow<FocusSettings> =
            context.dataStore.data.map { prefs ->
                FocusSettings(
                    hideThumbnails = prefs[HIDE_THUMBNAILS] ?: false,
                    blurThumbnails = prefs[BLUR_THUMBNAILS] ?: false,
                    hideTopBar = prefs[HIDE_TOP_BAR] ?: false,
                    disableEndCards = prefs[DISABLE_END_CARDS] ?: false,
                    blackWhite = prefs[BLACK_WHITE] ?: false,
                    hideSubscriptionsNav = prefs[HIDE_SUBSCRIPTIONS_NAV] ?: false,
                )
            }

        suspend fun set(
            key: androidx.datastore.preferences.core.Preferences.Key<Boolean>,
            value: Boolean,
        ) {
            context.dataStore.edit { prefs -> prefs[key] = value }
        }

        /** What sync sends: the options that have been set. */
        suspend fun exportSettings(): SettingsExport {
            val prefs = context.dataStore.data.first()
            val booleans = buildMap { ALL.forEach { key -> prefs[key]?.let { put(key.name, it) } } }
            return SettingsExport(booleans, emptyMap(), emptyMap())
        }

        /** Writes what arrived by sync; options that did not arrive keep their value. */
        suspend fun restoreSettings(incoming: SettingsExport) {
            if (incoming.booleans.keys.none { it in SETTING_NAMES }) return
            context.dataStore.edit { prefs ->
                ALL.forEach { key -> incoming.booleans[key.name]?.let { prefs[key] = it } }
            }
        }

        companion object {
            val HIDE_THUMBNAILS = booleanPreferencesKey("hide_thumbnails")
            val BLUR_THUMBNAILS = booleanPreferencesKey("blur_thumbnails")
            val HIDE_TOP_BAR = booleanPreferencesKey("hide_top_bar")
            val DISABLE_END_CARDS = booleanPreferencesKey("disable_end_cards")
            val BLACK_WHITE = booleanPreferencesKey("black_white_mode")
            val HIDE_SUBSCRIPTIONS_NAV = booleanPreferencesKey("hide_subscriptions_nav")

            private val ALL =
                listOf(HIDE_THUMBNAILS, BLUR_THUMBNAILS, HIDE_TOP_BAR, DISABLE_END_CARDS, BLACK_WHITE, HIDE_SUBSCRIPTIONS_NAV)

            /** The keys a settings backup and sync carry. */
            val SETTING_NAMES: Set<String> = ALL.map { it.name }.toSet()
        }
    }
