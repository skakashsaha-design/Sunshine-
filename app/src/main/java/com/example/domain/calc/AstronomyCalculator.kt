package com.example.domain.calc

import com.example.domain.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

object AstronomyCalculator {

    private const val SYNODIC_MONTH = 29.530588853
    // Known New Moon epoch: 2000-01-06 18:14 UTC
    private const val NEW_MOON_EPOCH_MS = 947182440000L

    fun calculate(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        epochMs: Long = System.currentTimeMillis()
    ): AstronomySnapshot {
        val tz = runCatching { TimeZone.getTimeZone(timezoneId) }.getOrDefault(TimeZone.getDefault())
        val cal = Calendar.getInstance(tz).apply { timeInMillis = epochMs }

        val sunPos = calculateSolarPosition(latitude, longitude, epochMs)
        val sunriseMin = calculateSunEventMinutes(latitude, longitude, cal, tz, zenithDeg = 90.833, isSunrise = true)
        val sunsetMin = calculateSunEventMinutes(latitude, longitude, cal, tz, zenithDeg = 90.833, isSunrise = false)
        val civilDawnMin = calculateSunEventMinutes(latitude, longitude, cal, tz, zenithDeg = 96.0, isSunrise = true)
        val civilDuskMin = calculateSunEventMinutes(latitude, longitude, cal, tz, zenithDeg = 96.0, isSunrise = false)
        val goldenEndMorningMin = calculateSunEventMinutes(latitude, longitude, cal, tz, zenithDeg = 84.0, isSunrise = true)
        val goldenStartEveningMin = calculateSunEventMinutes(latitude, longitude, cal, tz, zenithDeg = 84.0, isSunrise = false)

        val solarNoonMin = if (sunriseMin != null && sunsetMin != null) {
            (sunriseMin + sunsetMin) / 2
        } else {
            (720 - (longitude * 4.0).roundToInt() + tz.getOffset(epochMs) / 60000).coerceIn(0, 1439)
        }

        val dayLengthMin = if (sunriseMin != null && sunsetMin != null) {
            (sunsetMin - sunriseMin).coerceIn(0, 1440)
        } else if (sunPos.first > 0) 1440 else 0

        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val isDay = sunriseMin != null && sunsetMin != null && nowMin in sunriseMin..sunsetMin
        val remainingMin = when {
            sunriseMin == null || sunsetMin == null -> 0
            nowMin < sunriseMin -> dayLengthMin
            nowMin <= sunsetMin -> (sunsetMin - nowMin).coerceAtLeast(0)
            else -> 0
        }

        val progress = if (sunriseMin != null && sunsetMin != null && sunsetMin > sunriseMin) {
            ((nowMin - sunriseMin).toFloat() / (sunsetMin - sunriseMin).toFloat()).coerceIn(0f, 1f)
        } else 0f

        val sunData = SunData(
            sunriseTime = formatMinutes(sunriseMin),
            sunsetTime = formatMinutes(sunsetMin),
            solarNoonTime = formatMinutes(solarNoonMin),
            civilDawnTime = formatMinutes(civilDawnMin),
            civilDuskTime = formatMinutes(civilDuskMin),
            goldenHourMorning = if (sunriseMin != null && goldenEndMorningMin != null) "${formatMinutes(sunriseMin)} – ${formatMinutes(goldenEndMorningMin)}" else "N/A",
            goldenHourEvening = if (goldenStartEveningMin != null && sunsetMin != null) "${formatMinutes(goldenStartEveningMin)} – ${formatMinutes(sunsetMin)}" else "N/A",
            blueHourMorning = if (civilDawnMin != null && sunriseMin != null) "${formatMinutes(civilDawnMin)} – ${formatMinutes(sunriseMin)}" else "N/A",
            blueHourEvening = if (sunsetMin != null && civilDuskMin != null) "${formatMinutes(sunsetMin)} – ${formatMinutes(civilDuskMin)}" else "N/A",
            dayLengthFormatted = "${dayLengthMin / 60}h ${dayLengthMin % 60}m",
            dayLengthMinutes = dayLengthMin,
            remainingDaylightFormatted = "${remainingMin / 60}h ${remainingMin % 60}m",
            sunProgressFraction = progress,
            sunAltitudeDeg = sunPos.first,
            sunAzimuthDeg = sunPos.second,
            isDaylight = isDay,
            isTwilight = !isDay && sunPos.first > -6.0
        )

        val moonData = calculateMoon(latitude, longitude, tz, epochMs)

        val constellations = listOf(
            StarConstellationInfo("Orion the Hunter", "21:00 – 04:00", "East to South", "Prominent winter constellation featuring Betelgeuse and Rigel."),
            StarConstellationInfo("Ursa Major (Big Dipper)", "All Night (Circumpolar)", "North", "Reliable navigational asterism pointing to Polaris."),
            StarConstellationInfo("Sirius (Alpha Canis Majoris)", "22:00 – 05:00", "Southeast", "Brightest star in the night sky in Canis Major."),
            StarConstellationInfo("Cassiopeia", "All Night", "Northeast", "Distinctive W-shaped northern constellation.")
        )

        val events = listOf(
            AstronomyEventInfo("Geminids Meteor Shower Peak", "Dec 13–14", "Up to 120 multicolored meteors per hour under dark skies."),
            AstronomyEventInfo("Winter Solstice", "Dec 21", "Shortest daylight period of the year in the Northern Hemisphere."),
            AstronomyEventInfo("Quadrantids Meteor Shower", "Jan 3–4", "Active meteor shower with sharp peak rate."),
            AstronomyEventInfo("Spring Equinox", "Mar 20", "Equal duration of day and night worldwide.")
        )

        return AstronomySnapshot(
            sun = sunData,
            moon = moonData,
            constellations = constellations,
            events = events,
            calculatedAtEpochMs = epochMs,
            origin = DataOrigin.CALCULATED
        )
    }

    /**
     * Returns (altitudeDeg, azimuthDeg) for the Sun at the given epochMs.
     */
    fun calculateSolarPosition(latitude: Double, longitude: Double, epochMs: Long): Pair<Double, Double> {
        val jd = epochMs / 86400000.0 + 2440587.5
        val d = jd - 2451545.0
        val g = Math.toRadians((357.529 + 0.98560028 * d) % 360.0)
        val q = (280.459 + 0.98564736 * d) % 360.0
        val l = Math.toRadians((q + 1.915 * sin(g) + 0.020 * sin(2 * g)) % 360.0)
        val e = Math.toRadians(23.439 - 0.00000036 * d)

        val ra = atan2(cos(e) * sin(l), cos(l))
        val dec = asin(sin(e) * sin(l))

        val gmstHours = (18.697374558 + 24.06570982441908 * d) % 24.0
        val lstRad = Math.toRadians(((gmstHours * 15.0 + longitude) % 360.0 + 360.0) % 360.0)
        val ha = lstRad - ra
        val latRad = Math.toRadians(latitude)

        val alt = asin(sin(latRad) * sin(dec) + cos(latRad) * cos(dec) * cos(ha))
        val az = atan2(-sin(ha), tan(dec) * cos(latRad) - sin(latRad) * cos(ha))
        val azDeg = (Math.toDegrees(az) + 360.0) % 360.0
        return Math.toDegrees(alt) to azDeg
    }

    private fun calculateSunEventMinutes(
        latitude: Double,
        longitude: Double,
        cal: Calendar,
        tz: TimeZone,
        zenithDeg: Double,
        isSunrise: Boolean
    ): Int? {
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val lngHour = longitude / 15.0
        val t = if (isSunrise) dayOfYear + ((6.0 - lngHour) / 24.0) else dayOfYear + ((18.0 - lngHour) / 24.0)
        val m = (0.9856 * t) - 3.289
        var l = m + (1.916 * sin(Math.toRadians(m))) + (0.020 * sin(Math.toRadians(2 * m))) + 282.634
        l = ((l % 360.0) + 360.0) % 360.0

        var ra = Math.toDegrees(atan(0.91764 * tan(Math.toRadians(l))))
        ra = ((ra % 360.0) + 360.0) % 360.0
        val lQuad = floor(l / 90.0) * 90.0
        val raQuad = floor(ra / 90.0) * 90.0
        ra = (ra + (lQuad - raQuad)) / 15.0

        val sinDec = 0.39782 * sin(Math.toRadians(l))
        val cosDec = cos(asin(sinDec))
        val cosH = (cos(Math.toRadians(zenithDeg)) - (sinDec * sin(Math.toRadians(latitude)))) /
            (cosDec * cos(Math.toRadians(latitude)))
        if (cosH > 1.0 || cosH < -1.0) return null

        val h = if (isSunrise) {
            (360.0 - Math.toDegrees(acos(cosH))) / 15.0
        } else {
            Math.toDegrees(acos(cosH)) / 15.0
        }

        val localMeanTime = h + ra - (0.06571 * t) - 6.622
        var utcHours = (localMeanTime - lngHour) % 24.0
        if (utcHours < 0) utcHours += 24.0

        val offsetHours = tz.getOffset(cal.timeInMillis) / 3600000.0
        var localHours = (utcHours + offsetHours) % 24.0
        if (localHours < 0) localHours += 24.0
        return (localHours * 60.0).roundToInt().coerceIn(0, 1439)
    }

    private fun calculateMoon(
        latitude: Double,
        longitude: Double,
        tz: TimeZone,
        epochMs: Long
    ): MoonData {
        val daysSinceEpoch = (epochMs - NEW_MOON_EPOCH_MS) / 86400000.0
        val cycles = daysSinceEpoch / SYNODIC_MONTH
        val phaseFraction = cycles - floor(cycles)
        val moonAgeDays = phaseFraction * SYNODIC_MONTH
        val illumination = (1.0 - cos(2.0 * PI * phaseFraction)) / 2.0 * 100.0

        val phaseType = when {
            phaseFraction < 0.03 || phaseFraction >= 0.97 -> MoonPhaseType.NEW_MOON
            phaseFraction < 0.22 -> MoonPhaseType.WAXING_CRESCENT
            phaseFraction < 0.28 -> MoonPhaseType.FIRST_QUARTER
            phaseFraction < 0.47 -> MoonPhaseType.WAXING_GIBBOUS
            phaseFraction < 0.53 -> MoonPhaseType.FULL_MOON
            phaseFraction < 0.72 -> MoonPhaseType.WANING_GIBBOUS
            phaseFraction < 0.78 -> MoonPhaseType.LAST_QUARTER
            else -> MoonPhaseType.WANING_CRESCENT
        }

        val jd = epochMs / 86400000.0 + 2440587.5
        val d = jd - 2451545.0
        val l0 = Math.toRadians((218.316 + 13.176396 * d) % 360.0)
        val mMoon = Math.toRadians((134.963 + 13.064993 * d) % 360.0)
        val fMoon = Math.toRadians((93.272 + 13.229350 * d) % 360.0)

        val lonEcl = l0 + Math.toRadians(6.289 * sin(mMoon))
        val latEcl = Math.toRadians(5.128 * sin(fMoon))
        val distanceKm = (385001.0 - 20905.0 * cos(mMoon)).roundToInt()

        val eps = Math.toRadians(23.4393)
        val ra = atan2(sin(lonEcl) * cos(eps) - tan(latEcl) * sin(eps), cos(lonEcl))
        val dec = asin(sin(latEcl) * cos(eps) + cos(latEcl) * sin(eps) * sin(lonEcl))

        val gmstHours = (18.697374558 + 24.06570982441908 * d) % 24.0
        val lstRad = Math.toRadians(((gmstHours * 15.0 + longitude) % 360.0 + 360.0) % 360.0)
        val ha = lstRad - ra
        val latRad = Math.toRadians(latitude)

        val altDeg = Math.toDegrees(asin(sin(latRad) * sin(dec) + cos(latRad) * cos(dec) * cos(ha)))
        val azDeg = (Math.toDegrees(atan2(-sin(ha), tan(dec) * cos(latRad) - sin(latRad) * cos(ha))) + 360.0) % 360.0

        // Approximate moonrise/moonset by sampling the local day in 10-minute intervals
        val dayStartCal = Calendar.getInstance(tz).apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        var riseMin: Int? = null
        var setMin: Int? = null
        var prevAlt = moonAltAt(latitude, longitude, dayStartCal.timeInMillis)
        for (m in 10..1440 step 10) {
            val curAlt = moonAltAt(latitude, longitude, dayStartCal.timeInMillis + m * 60000L)
            if (prevAlt <= 0.0 && curAlt > 0.0 && riseMin == null) riseMin = m - 5
            if (prevAlt >= 0.0 && curAlt < 0.0 && setMin == null) setMin = m - 5
            prevAlt = curAlt
        }

        val daysToFull = if (phaseFraction <= 0.5) (0.5 - phaseFraction) * SYNODIC_MONTH else (1.5 - phaseFraction) * SYNODIC_MONTH
        val daysToNew = (1.0 - phaseFraction) * SYNODIC_MONTH
        val dateFmt = SimpleDateFormat("MMM dd", Locale.US).apply { timeZone = tz }

        return MoonData(
            phaseType = phaseType,
            phaseFraction = phaseFraction,
            illuminationPercent = illumination,
            moonAgeDays = moonAgeDays,
            moonriseTime = formatMinutes(riseMin),
            moonsetTime = formatMinutes(setMin),
            moonAltitudeDeg = altDeg,
            moonAzimuthDeg = azDeg,
            moonDistanceKm = distanceKm,
            nextFullMoonDate = dateFmt.format(Date(epochMs + (daysToFull * 86400000.0).toLong())),
            nextNewMoonDate = dateFmt.format(Date(epochMs + (daysToNew * 86400000.0).toLong()))
        )
    }

    private fun moonAltAt(latitude: Double, longitude: Double, epochMs: Long): Double {
        val jd = epochMs / 86400000.0 + 2440587.5
        val d = jd - 2451545.0
        val l0 = Math.toRadians((218.316 + 13.176396 * d) % 360.0)
        val mMoon = Math.toRadians((134.963 + 13.064993 * d) % 360.0)
        val fMoon = Math.toRadians((93.272 + 13.229350 * d) % 360.0)
        val lonEcl = l0 + Math.toRadians(6.289 * sin(mMoon))
        val latEcl = Math.toRadians(5.128 * sin(fMoon))
        val eps = Math.toRadians(23.4393)
        val ra = atan2(sin(lonEcl) * cos(eps) - tan(latEcl) * sin(eps), cos(lonEcl))
        val dec = asin(sin(latEcl) * cos(eps) + cos(latEcl) * sin(eps) * sin(lonEcl))
        val gmstHours = (18.697374558 + 24.06570982441908 * d) % 24.0
        val lstRad = Math.toRadians(((gmstHours * 15.0 + longitude) % 360.0 + 360.0) % 360.0)
        val ha = lstRad - ra
        val latRad = Math.toRadians(latitude)
        return Math.toDegrees(asin(sin(latRad) * sin(dec) + cos(latRad) * cos(dec) * cos(ha)))
    }

    private fun formatMinutes(minutes: Int?): String {
        if (minutes == null) return "N/A"
        val h = (minutes / 60) % 24
        val m = minutes % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }
}
