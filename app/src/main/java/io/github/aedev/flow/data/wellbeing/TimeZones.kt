package io.github.aedev.flow.data.wellbeing

import java.time.ZoneId

/**
 * The time zone the daily limit and the bedtime reminder keep their days and clock in. It is a setting, Asia/Tashkent
 * unless changed, so a day starts at the same moment on every device whatever their own clocks say.
 */
object TimeZones {
    const val DEFAULT = "Asia/Tashkent"

    /** UTC and its aliases: the list leaves them out, the clock of a place is what is wanted. */
    private val UtcNames =
        setOf(
            "UTC", "Etc/UTC", "Etc/UCT", "UCT", "Etc/Universal", "Universal", "Etc/Zulu", "Zulu",
            "Etc/GMT", "GMT", "Etc/Greenwich", "Greenwich", "Z",
        )

    /** The zone for a stored name, or the default for none, an unknown name or UTC. */
    fun zoneOf(name: String?): ZoneId =
        name
            ?.takeIf { it.isNotBlank() && it !in UtcNames }
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneId.of(DEFAULT)

    /** Every zone to choose from, by name, as places ("Europe/Berlin"), without UTC. */
    fun selectable(): List<String> =
        ZoneId
            .getAvailableZoneIds()
            .filter { it.contains('/') && !it.startsWith("Etc/") && !it.startsWith("SystemV/") && it !in UtcNames }
            .toSortedSet()
            .toList()
}
