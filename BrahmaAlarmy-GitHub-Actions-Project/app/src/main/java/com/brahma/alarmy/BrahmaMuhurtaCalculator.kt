package com.brahma.alarmy

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.*

data class BrahmaWindow(
    val date: LocalDate,
    val sunriseTime: LocalTime,
    val brahmaStart: LocalTime,
    val brahmaEnd: LocalTime,
    val triggerEpochMillis: Long
)

object BrahmaMuhurtaCalculator {

    /**
     * Finds the next upcoming Brahma Muhurta window based on current time and location.
     * If today's Brahma Muhurta has already passed, it calculates tomorrow's window.
     */
    fun getNextBrahmaWindow(
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): BrahmaWindow {
        val now = ZonedDateTime.now(zoneId)
        val todayWindow = calculateForDate(now.toLocalDate(), latitude, longitude, zoneId)

        return if (todayWindow.triggerEpochMillis > System.currentTimeMillis() + 30_000L) {
            todayWindow
        } else {
            calculateForDate(now.toLocalDate().plusDays(1), latitude, longitude, zoneId)
        }
    }

    fun calculateForDate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): BrahmaWindow {
        val dayOfYear = date.dayOfYear

        // Fractional year (gamma) in radians
        val gamma = 2.0 * PI / 365.0 * (dayOfYear - 1)

        // Equation of time (in minutes)
        val eqTime = 229.18 * (0.000075 +
                0.001868 * cos(gamma) -
                0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) -
                0.040849 * sin(2 * gamma))

        // Solar declination angle (in radians)
        val decl = 0.006918 -
                0.399912 * cos(gamma) +
                0.070257 * sin(gamma) -
                0.006758 * cos(2 * gamma) +
                0.000907 * sin(2 * gamma) -
                0.002697 * cos(3 * gamma) +
                0.00148 * sin(3 * gamma)

        val latRad = Math.toRadians(latitude)
        val zenith = Math.toRadians(90.833) // Official astronomical zenith for sunrise

        // Hour angle at sunrise
        val cosHourAngle = (cos(zenith) / (cos(latRad) * cos(decl))) - tan(latRad) * tan(decl)
        val clampedCos = cosHourAngle.coerceIn(-1.0, 1.0)
        val hourAngleDeg = Math.toDegrees(acos(clampedCos))

        // UTC sunrise in minutes from midnight
        val sunriseUtcMinutes = 720.0 - 4.0 * (longitude + hourAngleDeg) - eqTime

        // Convert to local timezone offset
        val offsetSeconds = zoneId.rules.getOffset(date.atStartOfDay()).totalSeconds
        val localSunriseMinutes = (sunriseUtcMinutes + (offsetSeconds / 60.0) + 1440.0) % 1440.0

        val sunriseHour = (localSunriseMinutes / 60).toInt()
        val sunriseMinute = (localSunriseMinutes % 60).roundToInt().coerceIn(0, 59)

        val sunriseLocalTime = LocalTime.of(sunriseHour, sunriseMinute)
        // Brahma Muhurta starts 96 minutes (1 hr 36 min) before sunrise
        val brahmaStart = sunriseLocalTime.minusMinutes(96)
        // Ends 48 minutes before sunrise
        val brahmaEnd = sunriseLocalTime.minusMinutes(48)

        val triggerDateTime = LocalDateTime.of(date, brahmaStart).atZone(zoneId)

        return BrahmaWindow(
            date = date,
            sunriseTime = sunriseLocalTime,
            brahmaStart = brahmaStart,
            brahmaEnd = brahmaEnd,
            triggerEpochMillis = triggerDateTime.toInstant().toEpochMilli()
        )
    }
}
