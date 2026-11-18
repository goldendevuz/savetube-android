package io.github.aedev.flow.data.wellbeing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The minutes played in the current session (since the last break of five minutes), for the timer in the player. */
object SessionClock {
    private val _minutes = MutableStateFlow(0)
    val minutes: StateFlow<Int> = _minutes.asStateFlow()

    internal fun set(minutes: Int) {
        _minutes.value = minutes
    }
}
