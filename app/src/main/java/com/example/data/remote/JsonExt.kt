package com.example.data.remote

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    val v = optDouble(key, Double.NaN)
    return if (v.isNaN()) null else v
}

fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    val v = optDouble(key, Double.NaN)
    return if (v.isNaN()) null else v.toInt()
}

fun JSONArray.optDoubleOrNull(index: Int): Double? {
    if (index !in 0 until length() || isNull(index)) return null
    val v = optDouble(index, Double.NaN)
    return if (v.isNaN()) null else v
}

fun JSONArray.optIntOrNull(index: Int): Int? {
    if (index !in 0 until length() || isNull(index)) return null
    val v = optDouble(index, Double.NaN)
    return if (v.isNaN()) null else v.toInt()
}

fun JSONArray.optLongOrNull(index: Int): Long? {
    if (index !in 0 until length() || isNull(index)) return null
    return optLong(index)
}

fun epochToLocalIso(epochSec: Long, utcOffsetSec: Int): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
        timeZone = SimpleTimeZone(utcOffsetSec * 1000, "LOCAL")
    }
    return sdf.format(Date(epochSec * 1000L))
}

fun epochToLocalDate(epochSec: Long, utcOffsetSec: Int): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = SimpleTimeZone(utcOffsetSec * 1000, "LOCAL")
    }
    return sdf.format(Date(epochSec * 1000L))
}

fun epochToLocalTime(epochSec: Long, utcOffsetSec: Int): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.US).apply {
        timeZone = SimpleTimeZone(utcOffsetSec * 1000, "LOCAL")
    }
    return sdf.format(Date(epochSec * 1000L))
}
