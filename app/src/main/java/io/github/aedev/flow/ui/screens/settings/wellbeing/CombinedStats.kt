package io.github.aedev.flow.ui.screens.settings.wellbeing

import androidx.compose.runtime.Immutable
import io.github.aedev.flow.data.stats.RankedItem
import io.github.aedev.flow.data.stats.RankedKind
import io.github.aedev.flow.data.stats.ViewFormat
import io.github.aedev.flow.data.wellbeing.DeviceStats

/** One device's part of the week, for the list under the chart. */
@Immutable
data class DeviceTotal(
    val name: String,
    val millis: Long,
    val own: Boolean,
)

/** This device's days with the time the other devices published for the same dates added. */
internal fun combineDays(
    own: WatchTimeSummary,
    others: Collection<DeviceStats>,
): WatchTimeSummary =
    WatchTimeSummary(own.days.map { day -> day.copy(millis = day.millis + others.sumOf { it.days[day.date.toString()] ?: 0L }) })

/** This device's part of the week first-class, then the others, the one with the most time first. */
internal fun deviceTotals(
    own: WatchTimeSummary,
    ownName: String,
    others: Collection<DeviceStats>,
): List<DeviceTotal> {
    val dates = own.days.map { it.date.toString() }
    val rows =
        listOf(DeviceTotal(ownName, own.weekMillis, own = true)) +
            others.map { device -> DeviceTotal(device.name, dates.sumOf { device.days[it] ?: 0L }, own = false) }
    return rows.filter { it.millis > 0L || it.own }.sortedByDescending { it.millis }
}

/** This month's channels and Shorts share over this device and the others. */
internal fun combineTopChannels(
    own: TopChannelsSummary?,
    month: String,
    others: Collection<DeviceStats>,
): TopChannelsSummary? {
    if (own == null) return null
    val totals = LinkedHashMap<String, RankedItem>()
    own.channels.forEach { totals[it.id] = it }
    val sameMonth = others.filter { it.month == month }
    for (device in sameMonth) {
        for (c in device.channels) {
            val known = totals[c.id]
            totals[c.id] =
                if (known != null) {
                    known.copy(durationMs = known.durationMs + c.ms)
                } else {
                    RankedItem(id = c.id, name = c.name, count = 0, durationMs = c.ms, kind = RankedKind.CHANNEL)
                }
        }
    }
    val longMs = (own.formatMs[ViewFormat.LONG] ?: 0L) + sameMonth.sumOf { it.longMs }
    val shortMs = (own.formatMs[ViewFormat.SHORT] ?: 0L) + sameMonth.sumOf { it.shortMs }
    val liveMs = (own.formatMs[ViewFormat.LIVE] ?: 0L) + sameMonth.sumOf { it.liveMs }
    return TopChannelsSummary(
        channels = totals.values.sortedByDescending { it.durationMs }.take(TOP_CHANNEL_COUNT),
        shortsShare = shortsShare(mapOf(ViewFormat.LONG to longMs, ViewFormat.SHORT to shortMs, ViewFormat.LIVE to liveMs)),
        formatMs = own.formatMs,
    )
}
