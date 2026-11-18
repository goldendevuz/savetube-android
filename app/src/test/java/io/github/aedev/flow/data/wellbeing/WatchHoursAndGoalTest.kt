package io.github.aedev.flow.data.wellbeing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchHoursAndGoalTest {
    @Test
    fun `adds time to the hour of the day`() {
        var ledger = WatchHours.add(emptyMap(), 100, 21, 60_000)
        ledger = WatchHours.add(ledger, 100, 21, 30_000)
        ledger = WatchHours.add(ledger, 101, 8, 10_000)
        assertEquals(90_000L, ledger[100]!![21])
        assertEquals(10_000L, ledger[101]!![8])
    }

    @Test
    fun `drops days older than 28`() {
        val ledger = WatchHours.add(WatchHours.add(emptyMap(), 10, 1, 1000), 40, 1, 1000)
        assertEquals(setOf(40L), ledger.keys)
    }

    @Test
    fun `finds the peak hour over all days`() {
        var ledger = WatchHours.add(emptyMap(), 1, 9, 5000)
        ledger = WatchHours.add(ledger, 2, 22, 4000)
        ledger = WatchHours.add(ledger, 3, 22, 4000)
        val summary = WatchHours.summarize(ledger)
        assertEquals(22, summary.peakHour)
        assertEquals(13_000L, summary.total)
        assertNull(WatchHours.summarize(emptyMap()).peakHour)
    }

    @Test
    fun `hours round trip and junk is ignored`() {
        val ledger = WatchHours.add(emptyMap(), 5, 3, 1000)
        assertEquals(ledger, WatchHours.parse(WatchHours.encode(ledger)))
        assertEquals(emptyMap<Long, List<Long>>(), WatchHours.parse("nope"))
        assertEquals(emptyMap<Long, List<Long>>(), WatchHours.parse("""{"5":[1,2]}"""))
    }

    @Test
    fun `shows only today's goal`() {
        val goal = DailyGoal.write(50, "Finish the course video", false, 1000)
        assertEquals("Finish the course video", DailyGoal.forDay(goal, 50)?.text)
        assertNull(DailyGoal.forDay(goal, 51))
        assertNull(DailyGoal.forDay(DailyGoal.write(50, "   ", false, 1), 50))
    }

    @Test
    fun `keeps the newer goal copy`() {
        val older = DailyGoal.write(50, "a", false, 100)
        val newer = DailyGoal.write(50, "a", true, 200)
        assertEquals(newer, DailyGoal.merge(older, newer))
        assertEquals(newer, DailyGoal.merge(newer, older))
        assertEquals(older, DailyGoal.merge(null, older))
    }

    @Test
    fun `limits the goal length and round trips, reading the desktop text`() {
        val goal = DailyGoal.write(1, "x".repeat(200), false, 1)
        assertEquals(80, goal.text.length)
        assertEquals(goal, DailyGoal.parse(goal.encode()))
        assertEquals(DailyGoal(7, "Read", true, 9), DailyGoal.parse("""{"d":7,"t":"Read","done":true,"u":9}"""))
        assertNull(DailyGoal.parse("nope"))
    }
}
