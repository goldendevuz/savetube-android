package io.github.aedev.flow.ui.screens.settings.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aedev.flow.R
import io.github.aedev.flow.data.local.SharedLinkAction
import io.github.aedev.flow.data.local.WatchedThreshold
import io.github.aedev.flow.ui.components.settings.SettingsDestination
import io.github.aedev.flow.ui.components.settings.SettingsPage
import io.github.aedev.flow.ui.components.settings.SettingsTarget
import io.github.aedev.flow.ui.components.settings.choice
import io.github.aedev.flow.ui.components.settings.nav
import io.github.aedev.flow.ui.components.settings.switch
import io.github.aedev.flow.ui.components.shared.FlowChoice
import io.github.aedev.flow.ui.components.shared.FlowChoiceDialog
import io.github.aedev.flow.ui.components.shared.FlowSwitchRow
import io.github.aedev.flow.ui.screens.settings.index.ContentIndex
import io.github.aedev.flow.ui.screens.home.parseDurationGroups
import io.github.aedev.flow.ui.screens.home.serializeDurationGroups
import io.github.aedev.flow.ui.screens.settings.index.DestinationIndex

/**
 * What shows up in the feeds. How it is laid out lives in Appearance, and what the players do in
 * Player & playback.
 */
@Composable
internal fun ContentSettingsScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
    onNavigate: (SettingsTarget) -> Unit,
    viewModel: ContentSettingsViewModel = hiltViewModel(),
) {
    val shortsContent by viewModel.shortsContent.collectAsStateWithLifecycle()
    val channelShortsTab by viewModel.channelShortsTab.collectAsStateWithLifecycle()
    val hideWatchedHome by viewModel.hideWatchedHome.collectAsStateWithLifecycle()
    val hideWatchedSubs by viewModel.hideWatchedSubs.collectAsStateWithLifecycle()
    val hideWatchedShorts by viewModel.hideWatchedShorts.collectAsStateWithLifecycle()
    val removeWatchedWatchLater by viewModel.removeWatchedWatchLater.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val linkHandlingLabel = rememberLinkHandlingLabel()
    var showThresholdDialog by rememberSaveable { mutableStateOf(false) }
    var showSharedLinksDialog by rememberSaveable { mutableStateOf(false) }
    var showMinViewsDialog by rememberSaveable { mutableStateOf(false) }
    var showMinSubscribersDialog by rememberSaveable { mutableStateOf(false) }
    var showDurationGroupDialog by rememberSaveable { mutableStateOf(false) }
    var showFeedScopeDialog by rememberSaveable { mutableStateOf(false) }

    SettingsPage(
        title = stringResource(R.string.settings_content_title),
        onBack = onBack,
        highlight = highlight,
    ) {
        group(key = "content.home", header = R.string.settings_section_home) {
            switch(ContentIndex.homeFeed, viewModel.homeFeed, viewModel::setHomeFeed)
            switch(ContentIndex.refreshOnReselect, viewModel.refreshOnReselect, viewModel::setRefreshOnReselect)
            switch(ContentIndex.homeChips, viewModel.homeChips, viewModel::setHomeChips)
            switch(ContentIndex.homeNewVideosButton, viewModel.homeNewVideosButton, viewModel::setHomeNewVideosButton)
            switch(ContentIndex.continueWatching, viewModel.continueWatching, viewModel::setContinueWatching)
            switch(ContentIndex.homeShortsShelf, viewModel.homeShortsShelf, viewModel::setHomeShortsShelf, enabled = shortsContent)
            choice(ContentIndex.homeSubscriptions, onClick = { showFeedScopeDialog = true }) {
                val value by viewModel.homeFeedScope.collectAsStateWithLifecycle()
                feedScopeLabel(value)
            }
            switch(ContentIndex.hideWatchedHome, viewModel.hideWatchedHome, viewModel::setHideWatchedHome)
            choice(ContentIndex.homeMinViews, onClick = { showMinViewsDialog = true }) {
                val value by viewModel.homeMinViews.collectAsStateWithLifecycle()
                minCountLabel(value)
            }
            choice(ContentIndex.homeMinSubscribers, onClick = { showMinSubscribersDialog = true }) {
                val value by viewModel.homeMinSubscribers.collectAsStateWithLifecycle()
                minCountLabel(value)
            }
            choice(ContentIndex.homeDurationGroup, onClick = { showDurationGroupDialog = true }) {
                val value by viewModel.homeDurationGroup.collectAsStateWithLifecycle()
                durationGroupsLabel(value)
            }
            switch(ContentIndex.homeHideMusic, viewModel.homeHideMusic, viewModel::setHomeHideMusic)
            switch(ContentIndex.homePinFreshSubs, viewModel.homePinFreshSubs, viewModel::setHomePinFreshSubs)
        }
        group(key = "content.music", header = R.string.screen_title_music) {
            nav(
                DestinationIndex.entry(SettingsDestination.MUSIC_HOME),
                onClick = { onNavigate(SettingsTarget(SettingsDestination.MUSIC_HOME)) },
            )
        }
        group(key = "content.library", header = R.string.library) {
            nav(
                DestinationIndex.entry(SettingsDestination.LIBRARY),
                onClick = { onNavigate(SettingsTarget(SettingsDestination.LIBRARY)) },
            )
        }
        group(key = "content.subscriptions", header = R.string.settings_section_subscriptions) {
            switch(ContentIndex.subsVideos, viewModel.subsVideos, viewModel::setSubsVideos)
            switch(ContentIndex.subsShorts, viewModel.subsShorts, viewModel::setSubsShorts, enabled = shortsContent)
            switch(ContentIndex.subsShortsShelf, viewModel.subsShortsShelf, viewModel::setSubsShortsShelf, enabled = shortsContent)
            switch(ContentIndex.subsLive, viewModel.subsLive, viewModel::setSubsLive)
            switch(ContentIndex.subsUpcoming, viewModel.subsUpcoming, viewModel::setSubsUpcoming)
            switch(ContentIndex.subsCollaborations, viewModel.subsCollaborations, viewModel::setSubsCollaborations)
            switch(ContentIndex.subsPostsChip, viewModel.subsPostsChip, viewModel::setSubsPostsChip)
            switch(ContentIndex.subsUngrouped, viewModel.subsUngrouped, viewModel::setSubsUngrouped)
            switch(ContentIndex.hideWatchedSubs, viewModel.hideWatchedSubs, viewModel::setHideWatchedSubs)
            switch(ContentIndex.hideUnplayableSubs, viewModel.hideUnplayableSubs, viewModel::setHideUnplayableSubs)
            switch(ContentIndex.subsRefreshOnStartup, viewModel.subsRefreshOnStartup, viewModel::setSubsRefreshOnStartup)
            switch(ContentIndex.subsCheckedCount, viewModel.subsCheckedCount, viewModel::setSubsCheckedCount)
        }
        group(key = "content.shorts", header = R.string.content_settings_header_shorts) {
            switch(ContentIndex.shortsContent, viewModel.shortsContent, viewModel::setShortsContent)
            row(ContentIndex.channelShortsTab.key) { shape ->
                FlowSwitchRow(
                    title = stringResource(ContentIndex.channelShortsTab.title),
                    supportingText = stringResource(R.string.content_settings_channel_shorts_tab_subtitle),
                    checked = channelShortsTab || shortsContent,
                    onCheckedChange = viewModel::setChannelShortsTab,
                    enabled = !shortsContent,
                    shape = shape,
                )
            }
            switch(ContentIndex.hideWatchedShorts, viewModel.hideWatchedShorts, viewModel::setHideWatchedShorts, enabled = shortsContent)
        }
        group(key = "content.watch_later", header = R.string.watch_later) {
            switch(ContentIndex.removeWatchedWatchLater, viewModel.removeWatchedWatchLater, viewModel::setRemoveWatchedWatchLater)
        }
        if (hideWatchedHome || hideWatchedSubs || hideWatchedShorts || removeWatchedWatchLater) {
            group(key = "content.watched", header = R.string.settings_section_watched) {
                choice(ContentIndex.watchedThreshold, onClick = { showThresholdDialog = true }) {
                    val threshold by viewModel.watchedThreshold.collectAsStateWithLifecycle()
                    watchedThresholdLabel(threshold)
                }
            }
        }
        group(key = "content.notes", header = R.string.content_settings_notes_title) {
            switch(ContentIndex.notes, viewModel.notes, viewModel::setNotes)
            switch(ContentIndex.channelNotes, viewModel.channelNotes, viewModel::setChannelNotes, enabled = notes)
            switch(ContentIndex.videoNotes, viewModel.videoNotes, viewModel::setVideoNotes, enabled = notes)
        }
        group(key = "content.sharing", header = R.string.settings_section_sharing) {
            switch(ContentIndex.shareWithoutText, viewModel.shareWithoutText, viewModel::setShareWithoutText)
        }
        group(key = "content.links", header = R.string.settings_section_links) {
            nav(ContentIndex.openLinks, onClick = { openLinkSettings(context) }, value = linkHandlingLabel)
            choice(ContentIndex.sharedLinks, onClick = { showSharedLinksDialog = true }) {
                val action by viewModel.sharedLinkAction.collectAsStateWithLifecycle()
                sharedLinkActionLabel(action)
            }
        }
    }

    if (showThresholdDialog) {
        val threshold by viewModel.watchedThreshold.collectAsStateWithLifecycle()
        FlowChoiceDialog(
            title = stringResource(R.string.content_settings_watched_threshold_title),
            description = stringResource(R.string.content_settings_watched_threshold_dialog_body),
            options =
                listOf(
                    WatchedThreshold.ALMOST_FINISHED,
                    WatchedThreshold.PERCENT_99,
                    WatchedThreshold.PERCENT_95,
                    WatchedThreshold.PERCENT_90,
                ).map { FlowChoice(it, watchedThresholdLabel(it)) },
            selected = threshold,
            onSelect = viewModel::setWatchedThreshold,
            onDismiss = { showThresholdDialog = false },
        )
    }

    if (showMinViewsDialog) {
        val value by viewModel.homeMinViews.collectAsStateWithLifecycle()
        FlowChoiceDialog(
            title = stringResource(R.string.content_settings_home_min_views_title),
            description = stringResource(R.string.content_settings_home_min_views_dialog_body),
            options = MIN_VIEWS_OPTIONS.map { FlowChoice(it, minCountLabel(it)) },
            selected = value,
            onSelect = viewModel::setHomeMinViews,
            onDismiss = { showMinViewsDialog = false },
        )
    }

    if (showFeedScopeDialog) {
        val value by viewModel.homeFeedScope.collectAsStateWithLifecycle()
        FlowChoiceDialog(
            title = stringResource(R.string.content_settings_home_feed_scope_title),
            description = stringResource(R.string.content_settings_home_feed_scope_dialog_body),
            options = FEED_SCOPE_KEYS.map { FlowChoice(it, feedScopeLabel(it)) },
            selected = value,
            onSelect = viewModel::setHomeFeedScope,
            onDismiss = { showFeedScopeDialog = false },
        )
    }

    if (showDurationGroupDialog) {
        val value by viewModel.homeDurationGroup.collectAsStateWithLifecycle()
        DurationGroupsDialog(
            selected = parseDurationGroups(value),
            onChange = { viewModel.setHomeDurationGroup(serializeDurationGroups(it)) },
            onDismiss = { showDurationGroupDialog = false },
        )
    }

    if (showMinSubscribersDialog) {
        val value by viewModel.homeMinSubscribers.collectAsStateWithLifecycle()
        FlowChoiceDialog(
            title = stringResource(R.string.content_settings_home_min_subscribers_title),
            description = stringResource(R.string.content_settings_home_min_subscribers_dialog_body),
            options = MIN_SUBSCRIBERS_OPTIONS.map { FlowChoice(it, minCountLabel(it)) },
            selected = value,
            onSelect = viewModel::setHomeMinSubscribers,
            onDismiss = { showMinSubscribersDialog = false },
        )
    }

    if (showSharedLinksDialog) {
        val action by viewModel.sharedLinkAction.collectAsStateWithLifecycle()
        FlowChoiceDialog(
            title = stringResource(R.string.content_settings_shared_links_title),
            description = stringResource(R.string.content_settings_shared_links_dialog_body),
            options = SharedLinkAction.entries.map { FlowChoice(it, sharedLinkActionLabel(it)) },
            selected = action,
            onSelect = viewModel::setSharedLinkAction,
            onDismiss = { showSharedLinksDialog = false },
        )
    }
}

@Composable
private fun sharedLinkActionLabel(action: SharedLinkAction): String =
    when (action) {
        SharedLinkAction.PLAY -> stringResource(R.string.content_settings_shared_links_play)
        SharedLinkAction.ASK_WHILE_PLAYING -> stringResource(R.string.content_settings_shared_links_ask_while_playing)
        SharedLinkAction.ALWAYS_ASK -> stringResource(R.string.content_settings_shared_links_always_ask)
    }

@Composable
private fun watchedThresholdLabel(threshold: WatchedThreshold): String =
    when (threshold) {
        WatchedThreshold.PERCENT_90 -> stringResource(R.string.content_settings_watched_threshold_90)
        WatchedThreshold.PERCENT_95 -> stringResource(R.string.content_settings_watched_threshold_95)
        WatchedThreshold.PERCENT_99 -> stringResource(R.string.content_settings_watched_threshold_99)
        WatchedThreshold.ALMOST_FINISHED -> stringResource(R.string.content_settings_watched_threshold_almost)
    }

private val MIN_VIEWS_OPTIONS =
    listOf(0L, 100L, 1_000L, 5_000L, 10_000L, 50_000L, 100_000L, 500_000L, 1_000_000L, 5_000_000L, 10_000_000L, 100_000_000L, 1_000_000_000L)

private val MIN_SUBSCRIBERS_OPTIONS =
    listOf(0L, 1_000L, 10_000L, 100_000L, 500_000L, 1_000_000L, 5_000_000L, 10_000_000L, 50_000_000L, 100_000_000L, 500_000_000L, 1_000_000_000L)

@Composable
private fun minCountLabel(value: Long): String =
    if (value <= 0L) {
        stringResource(R.string.content_settings_home_min_off)
    } else {
        when {
            value >= 1_000_000_000L -> "${value / 1_000_000_000L}B"
            value >= 1_000_000L -> "${value / 1_000_000L}M"
            value >= 1_000L -> "${value / 1_000L}K"
            else -> value.toString()
        }
    }

// "Up to 1 minute" (g1) is not offered: Home holds no Shorts, so it always left the page empty.
private val DURATION_GROUP_KEYS = listOf("g2", "g3", "g4", "g5", "g6", "g7", "g8")

/** The row's value: every chosen length range, or "Any length". */
@Composable
private fun durationGroupsLabel(value: String): String {
    val groups = parseDurationGroups(value)
    return if (groups.isEmpty()) {
        durationGroupLabel("any")
    } else {
        groups.joinToString(", ") { durationGroupLabel(it) }
    }
}

/** Pick one or several length ranges; a video is kept when it fits any of them. Nothing picked is any length. */
@Composable
private fun DurationGroupsDialog(
    selected: List<String>,
    onChange: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.content_settings_home_duration_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.content_settings_home_duration_dialog_body),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                DurationGroupRow(
                    label = durationGroupLabel("any"),
                    checked = selected.isEmpty(),
                    onClick = { onChange(emptyList()) },
                )
                DURATION_GROUP_KEYS.forEach { key ->
                    DurationGroupRow(
                        label = durationGroupLabel(key),
                        checked = key in selected,
                        onClick = { onChange(if (key in selected) selected - key else selected + key) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun DurationGroupRow(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onClick() })
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun durationGroupLabel(key: String): String =
    stringResource(
        when (key) {
            "g1" -> R.string.content_settings_home_duration_g1
            "g2" -> R.string.content_settings_home_duration_g2
            "g3" -> R.string.content_settings_home_duration_g3
            "g4" -> R.string.content_settings_home_duration_g4
            "g5" -> R.string.content_settings_home_duration_g5
            "g6" -> R.string.content_settings_home_duration_g6
            "g7" -> R.string.content_settings_home_duration_g7
            "g8" -> R.string.content_settings_home_duration_g8
            else -> R.string.content_settings_home_duration_any
        },
    )

private val FEED_SCOPE_KEYS = listOf("off", "subscribed", "unsubscribed")

@Composable
private fun feedScopeLabel(key: String): String =
    stringResource(
        when (key) {
            "subscribed" -> R.string.content_settings_home_feed_scope_subscribed
            "unsubscribed" -> R.string.content_settings_home_feed_scope_unsubscribed
            else -> R.string.content_settings_home_feed_scope_off
        },
    )
