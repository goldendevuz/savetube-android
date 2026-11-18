package io.github.aedev.flow.data.wellbeing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class LimitStreakTest {
    @Test
    fun `starts at one day and grows with every day without a break`() {
        val first = LimitStreak.record(null, 100, false)
        assertEquals(1, first.days(100))
        assertEquals(5, first.days(104))
    }

    @Test
    fun `a day the app was not opened is a good day`() {
        assertEquals(11, LimitStreak.record(null, 10, false).days(20))
    }

    @Test
    fun `resets to zero on the day the limit breaks and starts again the day after`() {
        var s = LimitStreak.record(null, 10, false)
        s = LimitStreak.record(s, 14, true)
        assertEquals(0, s.days(14))
        assertEquals(1, s.days(15))
        assertEquals(4, s.best)
    }

    @Test
    fun `keeps the best streak after a break`() {
        var s = LimitStreak.record(null, 1, false)
        s = LimitStreak.record(s, 8, false)
        s = LimitStreak.record(s, 8, true)
        assertEquals(8, s.best)
        assertEquals(1, s.days(9))
    }

    @Test
    fun `returns the same record when nothing changed`() {
        val s = LimitStreak.record(null, 5, false)
        assertSame(s, LimitStreak.record(s, 5, false))
    }

    @Test
    fun `merges two devices without forgetting a broken day`() {
        val a = LimitStreak(5, listOf(20), 4, 3)
        val b = LimitStreak(3, listOf(25, 20), 9, 7)
        assertEquals(LimitStreak(3, listOf(25, 20), 9, 7), LimitStreak.merge(a, b))
        assertSame(a, LimitStreak.merge(null, a))
    }

    @Test
    fun `announces a milestone once`() {
        var s = LimitStreak.record(null, 1, false)
        assertNull(LimitStreak.newMilestone(s, 2))
        assertEquals(3, LimitStreak.newMilestone(s, 3))
        s = s.copy(announced = 3)
        assertNull(LimitStreak.newMilestone(s, 3))
        assertEquals(7, LimitStreak.newMilestone(s, 7))
    }

    @Test
    fun `knows the next milestone`() {
        assertEquals(3, LimitStreak.nextMilestone(0))
        assertEquals(14, LimitStreak.nextMilestone(7))
        assertNull(LimitStreak.nextMilestone(400))
    }

    @Test
    fun `round trips and rejects junk`() {
        val s = LimitStreak.record(null, 9, true)
        assertEquals(s, LimitStreak.parse(s.encode()))
        assertNull(LimitStreak.parse("nope"))
        assertNull(LimitStreak.parse(""))
    }

    @Test
    fun `reads the text the desktop app writes`() {
        val s = LimitStreak.parse("""{"s":120,"b":[130,125],"best":9,"a":7}""")
        assertEquals(LimitStreak(120, listOf(130, 125), 9, 7), s)
    }
}
