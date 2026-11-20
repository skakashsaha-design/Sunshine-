package com.example.domain.calc

import android.app.ActivityManager
import android.content.Context
import com.example.domain.model.AnimationLevel
import com.example.domain.model.MetricGlossaryEntry
import com.example.domain.model.PerformanceMode

data class RuntimePerformanceProfile(
    val isLowRamActive: Boolean,
    val totalRamMb: Long,
    val effectiveMode: PerformanceMode,
    val maxRadarFrames: Int,
    val maxDisasterMarkers: Int,
    val effectiveAnimation: AnimationLevel
)

object PerformanceHelper {

    fun resolveProfile(context: Context, mode: PerformanceMode, requestedAnim: AnimationLevel): RuntimePerformanceProfile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        val totalMb = runCatching {
            am?.getMemoryInfo(memInfo)
            memInfo.totalMem / (1024L * 1024L)
        }.getOrDefault(4096L)

        val systemLowRam = runCatching { am?.isLowRamDevice == true }.getOrDefault(false) ||
            (totalMb in 1..1536)

        val resolvedMode = when (mode) {
            PerformanceMode.AUTO -> if (systemLowRam) PerformanceMode.LOW_RAM_1GB else PerformanceMode.BALANCED
            else -> mode
        }

        val lowRamActive = resolvedMode == PerformanceMode.LOW_RAM_1GB
        val effAnim = when {
            lowRamActive && requestedAnim == AnimationLevel.FULL -> AnimationLevel.MINIMAL
            lowRamActive && requestedAnim == AnimationLevel.REDUCED -> AnimationLevel.OFF
            else -> requestedAnim
        }

        return RuntimePerformanceProfile(
            isLowRamActive = lowRamActive,
            totalRamMb = totalMb,
            effectiveMode = resolvedMode,
            maxRadarFrames = if (lowRamActive) 6 else 16,
            maxDisasterMarkers = if (lowRamActive) 20 else 60,
            effectiveAnimation = effAnim
        )
    }
}

object MetricGlossary {
    val entries: List<MetricGlossaryEntry> = listOf(
        MetricGlossaryEntry(
            key = "temperature",
            titleEn = "Temperature",
            explanationEn = "How hot or cold the air is at 2 meters above the ground.",
            titleBn = "তাপমাত্রা",
            explanationBn = "ভূমি থেকে ২ মিটার উচ্চতায় বাতাস কতটা গরম বা ঠান্ডা তার পরিমাপ।"
        ),
        MetricGlossaryEntry(
            key = "feels_like",
            titleEn = "Feels-Like Temperature",
            explanationEn = "How warm or cold the weather feels on human skin after factoring humidity, wind, and sunshine.",
            titleBn = "অনুভূত তাপমাত্রা",
            explanationBn = "আর্দ্রতা, বাতাস এবং রোদের কারণে মানুষের শরীরে তাপমাত্রা যেমন অনুভূত হয়।"
        ),
        MetricGlossaryEntry(
            key = "dew_point",
            titleEn = "Dew Point",
            explanationEn = "The temperature to which air must cool to become saturated with water vapor. Above 24°C feels oppressive; below 10°C feels crisp.",
            titleBn = "শিশিরাঙ্ক (Dew Point)",
            explanationBn = "যে তাপমাত্রায় বাতাস জলীয় বাষ্পে সম্পৃক্ত হয়ে শিশির বা কুয়াশা তৈরি করে। ২৪°সে-এর বেশি হলে খুব ভ্যাপসা গরম লাগে।"
        ),
        MetricGlossaryEntry(
            key = "wet_bulb",
            titleEn = "Wet-Bulb Temperature",
            explanationEn = "Lowest temperature cooled by evaporation. Measures combined heat-humidity strain; sustained values near 31–35°C are dangerous.",
            titleBn = "ওয়েট-বাল্ব তাপমাত্রা",
            explanationBn = "ঘাম বাষ্পীভূত হয়ে শরীর কতটা ঠান্ডা হতে পারে তার পরিমাপ। এটি বেশি হলে হিট স্ট্রোকের ঝুঁকি বাড়ে।"
        ),
        MetricGlossaryEntry(
            key = "humidity",
            titleEn = "Humidity",
            explanationEn = "The amount of water vapor present in the air compared to the maximum amount it can hold at that temperature.",
            titleBn = "আপেক্ষিক আর্দ্রতা",
            explanationBn = "নির্দিষ্ট তাপমাত্রায় বাতাসে কত শতাংশ জলীয় বাষ্প রয়েছে তার পরিমাপ।"
        ),
        MetricGlossaryEntry(
            key = "pressure",
            titleEn = "Atmospheric Pressure",
            explanationEn = "Weight of the air column above you. Rapid drops can signal approaching low-pressure systems or storms (does not guarantee a single outcome).",
            titleBn = "বায়ুচাপ",
            explanationBn = "বায়ুমণ্ডলের ওজন। দ্রুত চাপ কমলে নিম্নচাপ বা ঝড়বৃষ্টির সম্ভাবনা বাড়তে পারে।"
        ),
        MetricGlossaryEntry(
            key = "wind_direction",
            titleEn = "Wind & Direction",
            explanationEn = "Wind speed measures sustained 10m air movement; Gust is a brief spike. Wind direction means the direction FROM which the wind is coming.",
            titleBn = "বাতাস ও দিক",
            explanationBn = "বাতাসের দিক বলতে বোঝায় কোন দিক থেকে বাতাস প্রবাহিত হয়ে আসছে (যেমন: দক্ষিণ মানে দক্ষিণ দিক থেকে আসা বাতাস)।"
        ),
        MetricGlossaryEntry(
            key = "visibility",
            titleEn = "Visibility",
            explanationEn = "Approximate horizontal distance at which objects can be clearly seen; reduced by fog, haze, smoke, or heavy rain.",
            titleBn = "দৃশ্যমানতা",
            explanationBn = "কুয়াশা, ধোঁয়া বা ভারী বৃষ্টির কারণে কত দূর পর্যন্ত স্পষ্ট দেখা যায় তার পরিমাপ।"
        ),
        MetricGlossaryEntry(
            key = "uv",
            titleEn = "UV Index",
            explanationEn = "Strength of ultraviolet solar radiation reaching the ground. Values 6+ require sunscreen and shade.",
            titleBn = "ইউভি (UV) সূচক",
            explanationBn = "সূর্যের অতিবেগুনি রশ্মির তীব্রতা। ৬ বা তার বেশি হলে সানস্ক্রিন ও ছায়া ব্যবহার করা উচিত।"
        ),
        MetricGlossaryEntry(
            key = "aqi",
            titleEn = "AQI & PM2.5",
            explanationEn = "Air Quality Index summarizes pollutant health risk (US and EU scales differ). PM2.5 are fine particles ≤2.5 micrometers that can enter lungs.",
            titleBn = "বায়ুর মান সূচক (AQI ও PM2.5)",
            explanationBn = "বাতাসে দূষণ ও সূক্ষ্ম ধূলিকণার (PM2.5) পরিমাণ, যা স্বাস্থ্যের ওপর প্রভাব নির্দেশ করে।"
        ),
        MetricGlossaryEntry(
            key = "cape",
            titleEn = "CAPE (Convective Energy)",
            explanationEn = "Atmospheric instability energy available for rising updrafts. High CAPE (>1500 J/kg) favors strong thunderstorms and Kalboishakhi squalls.",
            titleBn = "CAPE (বজ্রঝড়ের শক্তি)",
            explanationBn = "বাতাসের অস্থিরতা ও ঊর্ধ্বমুখী শক্তির পরিমাপ। এটি বেশি হলে কালবৈশাখী ও বজ্রঝড়ের ঝুঁকি বাড়ে।"
        )
    )

    fun find(key: String): MetricGlossaryEntry? = entries.firstOrNull { it.key == key }
}
