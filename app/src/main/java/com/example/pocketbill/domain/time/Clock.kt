package com.example.pocketbill.domain.time

import java.time.LocalDate
import java.time.ZoneId

/**
 * Where "today" and "now" come from (ARCHITECTURE.md section 4). Code that depends on the date,
 * such as overdue rules and autosave, takes a Clock so tests can fix the date.
 */
interface Clock {
    /** Today's date on the phone, as an epoch day (DATA_MODEL.md section 1: calendar dates). */
    fun todayEpochDay(): Long

    /** Current time in epoch milliseconds (UTC), for createdAt / updatedAt timestamps. */
    fun nowMillis(): Long
}

/** The real clock: the phone's local date and the system time. */
class DeviceClock(private val zone: () -> ZoneId = { ZoneId.systemDefault() }) : Clock {
    override fun todayEpochDay(): Long = LocalDate.now(zone()).toEpochDay()

    override fun nowMillis(): Long = System.currentTimeMillis()
}

/** A clock that always returns the same moment. For tests. */
class FixedClock(private val epochDay: Long, private val millis: Long = epochDay * 86_400_000L) : Clock {
    override fun todayEpochDay(): Long = epochDay

    override fun nowMillis(): Long = millis
}
