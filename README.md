# SkyPulse — Meteorological, Environmental, Astronomy & Global Hazard Intelligence Platform

<p align="center">
  <strong>Native Kotlin · Jetpack Compose · Material Design 3 · Offline-First · 1 GB Low-RAM Optimized</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%20(API%2024%E2%80%9336)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android API 24-36" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose Material 3" />
  <img src="https://img.shields.io/badge/Widgets-Jetpack%20Glance-0F9D58?style=for-the-badge" alt="Jetpack Glance" />
  <img src="https://img.shields.io/badge/Build-Gradle%209.3.1-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle 9.3.1" />
</p>

---

## Overview

**SkyPulse** is a native Kotlin Android weather, air-quality, radar, astronomy, and global hazard intelligence platform engineered for worldwide coverage with specialized companion support for **Bangladesh (BMD & BWOT Watch)**, **India (IMD)**, and **Japan (JMA)**.

Built cleanly with MVVM + Repository architecture, Room offline caching, DataStore preferences, Retrofit + Moshi DTOs, WorkManager background alerting, interactive Canvas graphs, tiled radar/satellite maps, and Jetpack Glance home-screen widgets.

---

## Core Capabilities

| Domain | Highlights |
| --- | --- |
| **Live Weather & 30+ Parameters** | Temperature (°C / °F / K), Feels-Like, Heat Index, Wind Chill, Dew Point, Wet-Bulb, Pressure + Trend, Wind Speed/Gust/16-point Direction + Beaufort Scale, Low/Mid/High Cloud Layers, Rain/Snow/CAPE, UV, Solar Radiation, Soil Temp/Moisture |
| **Today AI Analysis & Daily Advisor** | Multi-phase daytime analysis (Morning, Afternoon, Evening, Night), thunderstorm/rain timing, comfort index, and actionable clothing/commute/AQI guidance |
| **Trip & Event Host Planner** | Evaluate any future date and outdoor event type (Wedding, Cricket, Hiking, Travel, Boating, Photography) against forecast probability, heat, wind gusts, and storm energy; persisted locally in Room |
| **Multi-Model & Source Comparison** | Live parallel comparisons across **ECMWF IFS**, **NOAA GFS**, **DWD ICON**, **JMA**, and **CMC GEM** with spread/agreement metrics |
| **BWOT & Worldwide Alerts** | Bangladesh convective / Kalboishakhi & heatwave threshold companion alongside global **NOAA/NWS**, **USGS**, **NASA EONET v3**, **GDACS**, and **GloFAS** hazard feeds |
| **Radar & Multi-Layer GIS Map** | Animated RainViewer precipitation radar playback with opacity, timeline scrubber, OSM / Carto Dark / Esri Satellite / OpenTopo base tiles, and live disaster pins |
| **Air Quality & Analytics** | US AQI & European AQI (kept strictly distinct), PM2.5, PM10, O₃, NO₂, SO₂, CO, dust, pollen, and 18+ interactive time-series charts with bilingual EN/BN metric glossary |
| **Aviation, Marine & Space Weather** | **NOAA AviationWeather** METAR & TAF lookup (supports IATA/ICAO e.g. `DAC` → `VGHS`), **Open-Meteo Marine** waves/swell/period/SST, **NOAA SWPC** Kp index & solar wind |
| **Astronomy & Emergency Directory** | Exact solar/lunar calculations (twilights, solar noon, moon phase/illumination) + verified country-flagged Emergency & Useful numbers across 18 countries (`BD`, `IN`, `JP`, `US`, `GB`, `AU`, etc.) |
| **Home Screen Widgets & Live Notification** | Jetpack Glance Material 3 widget + RemoteViews widgets (`Current`, `Hourly`, `4x2 Detail`, `Compact`) and optional persistent live weather notification |
| **Themes, Custom Fonts & 1 GB RAM Mode** | 5 bundled font presets (`Plus Jakarta Sans`, `Inter`, `Space Grotesk`, `Outfit`, `JetBrains Mono`), 8 theme appearances, 5 atmospheric backgrounds, bilingual **English / বাংলা**, and automatic 1 GB Low-RAM scaling |

---

## Architecture

```text
app/src/main/java/com/example/
├── data/
│   ├── local/        # Room Database (v2), DAO, DataStore Preferences, ProviderRegistry, EmergencyNumbersDataset
│   ├── remote/       # Retrofit (OpenMeteoApiService + DTOs), OkHttp NetworkClient, Multi-provider adapters, ExtendedWeatherService
│   ├── repository/   # Offline-first WeatherRepository, LocationRepository, AlertsRepository
│   └── worker/       # WorkManager periodic alert & live notification worker
├── domain/
│   ├── calc/         # MeteorologyCalculator, AstronomyCalculator, GeoUtils, TripPlannerCalculator, PerformanceHelper
│   └── model/        # WeatherModels, AlertModels, SettingsModels, ExtendedModels
├── ui/
│   ├── components/   # DynamicWeatherBackground, WeatherIcons & SkyPulseLogo, WeatherCharts, SplashOverlay
│   ├── screens/      # Home, Forecast, Map, Alerts, MoreHub, WeatherDetail, TripPlanner, ExtendedScreens, Settings, PermissionManager, About
│   └── theme/        # Bundled Google Fonts Typography, ColorSchemes, Shapes
└── widget/           # SkyPulseGlanceWidget (Jetpack Glance M3) + Classic RemoteViews Widgets
```

---

## Data Providers & Transparency

SkyPulse never fabricates official warnings or simulates commercial APIs when keys are absent:

- **Zero-Key Public Providers (Active Out-of-the-Box):**
  - Open-Meteo (Weather, Multi-Model ECMWF/GFS/ICON/JMA/GEM, CAMS AQI, Marine, GloFAS Flood)
  - MET Norway (`Locationforecast 2.0`), RainViewer Radar, USGS Earthquakes, NASA EONET v3, UN/EC GDACS, NOAA/NWS Alerts, NOAA AviationWeather (METAR/TAF), NOAA SWPC Space Weather
- **Optional Keyed Providers (Disabled until Configured):**
  - `WEATHERAPI_KEY`, `TOMORROW_IO_KEY`, `WEATHERBIT_KEY`, `ACCUWEATHER_KEY`, `GOOGLE_MAPS_KEY`, `GOOGLE_WEATHER_KEY`, `NASA_FIRMS_KEY`, `GEMINI_API_KEY`
  - Configure keys safely via **AI Studio Secrets Panel** or `.env` (never committed to source control or `local.properties`).

---

## Building, Testing & APK Packaging

### Prerequisites
- JDK 21
- Android SDK (compileSdk / targetSdk 36, minSdk 24)
- Gradle 9.3.1

### Commands

```bash
# Run full unit & Robolectric UI/repository test suite
gradle :app:testDebugUnitTest

# Build Debug APK
gradle :app:assembleDebug

# Output artifact
# app/build/outputs/apk/debug/app-debug.apk
```

### Continuous Integration
GitHub Actions workflow `.github/workflows/android-apk.yml` automatically runs `:app:testDebugUnitTest`, assembles `:app:assembleDebug`, and uploads both test reports and `skypulse-debug-apk` artifacts on every push or manual dispatch.

---

## Developer

**Arko Saha**
- Email: `skakashsaha@gmail.com`
- Origin: Bangladesh
