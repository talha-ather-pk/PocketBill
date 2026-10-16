package com.example.pocketbill.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ClockTest {

    @Test fun `fixed clock returns what it was given`() {
        val day = LocalDate.of(2026, 10, 30).toEpochDay()
        val clock = FixedClock(day, millis = 123L)
        assertEquals(day, clock.todayEpochDay())
        assertEquals(123L, clock.nowMillis())
    }

    @Test fun `device clock uses the phone's time zone for today`() {
        // Whatever the zone, "today" must equal the local date there, not the UTC date.
        for (zone in listOf("Pacific/Kiritimati", "UTC", "Pacific/Pago_Pago").map(ZoneId::of)) {
            val before = LocalDate.now(zone).toEpochDay()
            val today = DeviceClock { zone }.todayEpochDay()
            val after = LocalDate.now(zone).toEpochDay()
            assertTrue("$zone: $today not in $before..$after", today in before..after)
        }
    }

    @Test fun `device clock now is the system time`() {
        val before = Instant.now().toEpochMilli()
        val now = DeviceClock().nowMillis()
        val after = Instant.now().toEpochMilli()
        assertTrue(now in before..after)
    }
}
