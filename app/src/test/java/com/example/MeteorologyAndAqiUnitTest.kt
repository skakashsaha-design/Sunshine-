package com.example

import com.example.data.remote.*
import com.example.domain.calc.*
import com.example.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class MeteorologyAndAqiUnitTest {

    @Test
    fun `dew point and wet bulb calculations match physical expectations`() {
        val dew = MeteorologyCalculator.calculateDewPointC(30.0, 70)
        assertTrue("Expected dew point ~23.9C, got $dew", dew in 23.0..25.0)

        val wetBulb = MeteorologyCalculator.calculateWetBulbC(30.0, 70)
        assertTrue("Expected wet bulb ~25.5C, got $wetBulb", wetBulb in 24.5..26.5)

        val heatIndex = MeteorologyCalculator.calculateHeatIndexC(34.0, 65)
        assertNotNull(heatIndex)
        assertTrue("Heat index must exceed air temp", heatIndex!! > 34.0)

        assertNull("Wind chill is null above 10C", MeteorologyCalculator.calculateWindChillC(15.0, 20.0))
        val chill = MeteorologyCalculator.calculateWindChillC(0.0, 25.0)
        assertNotNull(chill)
        assertTrue("Wind chill must be below 0C", chill!! < 0.0)
    }

    @Test
    fun `aqi breakpoints and outdoor windows compute accurately`() {
        assertEquals(50, AqiCalculator.pm25ToUsAqi(9.0))
        assertTrue(AqiCalculator.pm25ToUsAqi(55.4) in 148..151)
        assertEquals(AqiCategory.GOOD, AqiCalculator.categoryFromUsAqi(35))
        assertEquals(AqiCategory.HAZARDOUS, AqiCalculator.categoryFromUsAqi(350))

        val hourly = listOf(
            HourlyAqi(1, "2026-11-19T06:00", 30, 20, 7.0, 12.0, 30.0),
            HourlyAqi(2, "2026-11-19T07:00", 35, 22, 8.0, 14.0, 32.0),
            HourlyAqi(3, "2026-11-19T08:00", 40, 25, 9.0, 16.0, 34.0),
            HourlyAqi(4, "2026-11-19T09:00", 160, 80, 65.0, 110.0, 45.0),
            HourlyAqi(5, "2026-11-19T10:00", 170, 85, 70.0, 120.0, 50.0),
            HourlyAqi(6, "2026-11-19T11:00", 165, 82, 68.0, 115.0, 48.0)
        )
        val (best, worst) = AqiCalculator.analyzeWindows(hourly)
        assertNotNull(best)
        assertNotNull(worst)
        assertTrue(best!!.avgAqi < worst!!.avgAqi)
        assertEquals("06:00 – 08:00", best.timeRange)
        assertEquals("09:00 – 11:00", worst.timeRange)
    }

    @Test
    fun `astronomy calculator produces valid Dhaka solar and lunar ephemeris`() {
        val snap = AstronomyCalculator.calculate(
            latitude = 23.8103,
            longitude = 90.4125,
            timezoneId = "Asia/Dhaka",
            epochMs = 1763550000000L
        )
        assertNotEquals("N/A", snap.sun.sunriseTime)
        assertNotEquals("N/A", snap.sun.sunsetTime)
        assertTrue(snap.sun.dayLengthMinutes in 550..850)
        assertTrue(snap.moon.illuminationPercent in 0.0..100.0)
        assertTrue(snap.moon.moonDistanceKm in 350000..410000)
    }

    @Test
    fun `geo utils distance, bangladesh detection, unit conversion, and tile math`() {
        val dist = GeoUtils.haversineKm(23.8103, 90.4125, 22.3569, 91.7832)
        assertTrue("Dhaka to Chattogram ~210km, got $dist", dist in 200.0..230.0)
        assertTrue(GeoUtils.isBangladesh("BD", "Bangladesh", 23.81, 90.41))
        assertFalse(GeoUtils.isBangladesh("JP", "Japan", 35.67, 139.65))
        assertEquals("🇧🇩", GeoUtils.countryCodeToFlagEmoji("BD"))
        assertEquals("77°F", GeoUtils.formatTemperature(25.0, TemperatureUnit.FAHRENHEIT))
        assertEquals("N", GeoUtils.compassDirection(358))

        val (tx, ty) = GeoUtils.latLonToTileDouble(23.8103, 90.4125, 6)
        val (latBack, lonBack) = GeoUtils.tileDoubleToLatLon(tx, ty, 6)
        assertEquals(23.8103, latBack, 0.01)
        assertEquals(90.4125, lonBack, 0.01)
    }
}
