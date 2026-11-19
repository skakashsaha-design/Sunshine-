package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SkyPulseApp
import com.example.data.repository.AlertsAndDisastersBundle
import com.example.domain.calc.AstronomyCalculator
import com.example.domain.model.*
import com.example.ui.theme.ThemeAppearance
import com.example.widget.SkyPulseWidgetUpdater
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SkyPulseApp
    val container = app.container

    val preferences: StateFlow<UserPreferences> = container.preferencesManager.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    val savedLocations: StateFlow<List<SavedLocation>> = container.locationRepository.savedLocationsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val searchHistory: StateFlow<List<SearchLocationResult>> = container.locationRepository.searchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeLocation = MutableStateFlow<SavedLocation?>(null)
    val activeLocation: StateFlow<SavedLocation?> = _activeLocation.asStateFlow()

    private val _weatherState = MutableStateFlow<UiState<WeatherBundle>>(UiState.Loading)
    val weatherState: StateFlow<UiState<WeatherBundle>> = _weatherState.asStateFlow()

    private val _aqiState = MutableStateFlow<UiState<AirQualityData>>(UiState.Idle)
    val aqiState: StateFlow<UiState<AirQualityData>> = _aqiState.asStateFlow()

    private val _astronomyState = MutableStateFlow<AstronomySnapshot?>(null)
    val astronomyState: StateFlow<AstronomySnapshot?> = _astronomyState.asStateFlow()

    private val _alertsState = MutableStateFlow<UiState<AlertsAndDisastersBundle>>(UiState.Idle)
    val alertsState: StateFlow<UiState<AlertsAndDisastersBundle>> = _alertsState.asStateFlow()

    private val _radarTimeline = MutableStateFlow<RadarTimeline?>(null)
    val radarTimeline: StateFlow<RadarTimeline?> = _radarTimeline.asStateFlow()

    private val _comparisonState = MutableStateFlow<UiState<List<ProviderComparisonEntry>>>(UiState.Idle)
    val comparisonState: StateFlow<UiState<List<ProviderComparisonEntry>>> = _comparisonState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchLocationResult>>(emptyList())
    val searchResults: StateFlow<List<SearchLocationResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedDay = MutableStateFlow<DailyForecast?>(null)
    val selectedDay: StateFlow<DailyForecast?> = _selectedDay.asStateFlow()

    private val _selectedHour = MutableStateFlow<HourlyForecast?>(null)
    val selectedHour: StateFlow<HourlyForecast?> = _selectedHour.asStateFlow()

    private val _selectedAlert = MutableStateFlow<WeatherAlert?>(null)
    val selectedAlert: StateFlow<WeatherAlert?> = _selectedAlert.asStateFlow()

    private val _mapFocusCoords = MutableStateFlow<Pair<Double, Double>?>(null)
    val mapFocusCoords: StateFlow<Pair<Double, Double>?> = _mapFocusCoords.asStateFlow()

    private val _pointWeatherPreview = MutableStateFlow<String?>(null)
    val pointWeatherPreview: StateFlow<String?> = _pointWeatherPreview.asStateFlow()

    val isSoundPlaying: StateFlow<Boolean> = container.soundEngine.isPlaying
    val activeSound: StateFlow<AmbientSoundType> = container.soundEngine.activeSound

    init {
        viewModelScope.launch {
            container.locationRepository.ensureInitialLocations()
            val locs = container.locationRepository.savedLocationsFlow.first { it.isNotEmpty() }
            val def = locs.firstOrNull { it.isDefault } ?: locs.first()
            selectLocation(def, forceRefresh = false)
        }
    }

    fun selectLocation(location: SavedLocation, forceRefresh: Boolean = false) {
        _activeLocation.value = location
        _mapFocusCoords.value = location.latitude to location.longitude
        _astronomyState.value = AstronomyCalculator.calculate(
            location.latitude,
            location.longitude,
            location.timezoneId
        )
        viewModelScope.launch {
            container.locationRepository.setDefaultLocation(location.id)
            refreshWeatherInternal(location, forceRefresh)
        }
    }

    fun refreshWeather() {
        val loc = _activeLocation.value ?: return
        viewModelScope.launch {
            refreshWeatherInternal(loc, forceRefresh = true)
            loadAqi(forceRefresh = true)
            loadAlertsAndDisasters()
        }
    }

    private suspend fun refreshWeatherInternal(location: SavedLocation, forceRefresh: Boolean) {
        _weatherState.value = UiState.Loading
        val prefs = preferences.value
        val res = container.weatherRepository.getWeather(location, prefs, forceRefresh)
        res.fold(
            onSuccess = { bundle ->
                _weatherState.value = UiState.Success(bundle)
                _astronomyState.value = AstronomyCalculator.calculate(
                    location.latitude,
                    location.longitude,
                    location.timezoneId
                )
                // Lightweight lazy AQI load for home summary badge & widget
                val aqiRes = container.weatherRepository.getAirQuality(location, forceRefresh = false)
                aqiRes.getOrNull()?.let { aqi ->
                    _aqiState.value = UiState.Success(aqi)
                    SkyPulseWidgetUpdater.updateAllWidgets(
                        context = app,
                        weather = bundle,
                        aqi = aqi,
                        alerts = (alertsState.value as? UiState.Success)?.data?.alerts.orEmpty(),
                        unit = prefs.temperatureUnit
                    )
                } ?: run {
                    SkyPulseWidgetUpdater.updateAllWidgets(
                        context = app,
                        weather = bundle,
                        aqi = null,
                        alerts = emptyList(),
                        unit = prefs.temperatureUnit
                    )
                }
            },
            onFailure = { err ->
                _weatherState.value = UiState.Error(err.message ?: "Unable to fetch weather")
            }
        )
    }

    fun loadAqi(forceRefresh: Boolean = false) {
        val loc = _activeLocation.value ?: return
        if (!forceRefresh && _aqiState.value is UiState.Success) return
        viewModelScope.launch {
            _aqiState.value = UiState.Loading
            container.weatherRepository.getAirQuality(loc, forceRefresh).fold(
                onSuccess = { _aqiState.value = UiState.Success(it) },
                onFailure = { _aqiState.value = UiState.Error(it.message ?: "AQI unavailable") }
            )
        }
    }

    fun refreshAstronomy() {
        val loc = _activeLocation.value ?: return
        _astronomyState.value = AstronomyCalculator.calculate(
            loc.latitude,
            loc.longitude,
            loc.timezoneId
        )
    }

    fun loadAlertsAndDisasters() {
        val loc = _activeLocation.value ?: return
        val weather = (_weatherState.value as? UiState.Success)?.data
        val prefs = preferences.value
        viewModelScope.launch {
            _alertsState.value = UiState.Loading
            val bundle = container.disasterAlertRepository.loadAlertsAndDisasters(
                loc,
                weather,
                prefs.personalKeys.bmdCustomFeedUrl
            )
            _alertsState.value = UiState.Success(bundle)
            if (weather != null) {
                val aqi = (_aqiState.value as? UiState.Success)?.data
                container.notificationHelper.evaluateAndDispatchAlerts(
                    settings = prefs.notificationSettings,
                    weather = weather,
                    aqi = aqi,
                    alerts = bundle.alerts,
                    disasters = bundle.disasters,
                    unit = prefs.temperatureUnit
                )
            }
        }
    }

    fun loadRadarTimeline() {
        viewModelScope.launch {
            container.weatherRepository.getRadarTimeline().onSuccess {
                _radarTimeline.value = it
            }
        }
    }

    fun loadProviderComparison() {
        val loc = _activeLocation.value ?: return
        viewModelScope.launch {
            _comparisonState.value = UiState.Loading
            val entries = container.weatherRepository.compareAllProviders(loc, preferences.value)
            _comparisonState.value = UiState.Success(entries)
        }
    }

    fun searchCities(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }
            _isSearching.value = true
            _searchResults.value = container.locationRepository.searchCities(query)
            _isSearching.value = false
        }
    }

    fun addAndSelectLocation(result: SearchLocationResult) {
        viewModelScope.launch {
            val saved = container.locationRepository.saveLocation(result, makeDefault = true)
            _searchResults.value = emptyList()
            selectLocation(saved, forceRefresh = true)
        }
    }

    fun requestGpsLocation(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val gps = container.locationRepository.fetchCurrentGpsLocation()
            if (gps != null) {
                selectLocation(gps, forceRefresh = true)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun renameLocation(id: String, name: String) {
        viewModelScope.launch { container.locationRepository.renameLocation(id, name) }
    }

    fun deleteLocation(id: String) {
        viewModelScope.launch { container.locationRepository.deleteLocation(id) }
    }

    fun moveLocation(id: String, dir: Int) {
        viewModelScope.launch { container.locationRepository.moveLocation(id, dir) }
    }

    fun inspectMapPoint(lat: Double, lon: Double) {
        _mapFocusCoords.value = lat to lon
        viewModelScope.launch {
            _pointWeatherPreview.value = "Inspecting ${String.format(java.util.Locale.US, "%.3f, %.3f", lat, lon)}..."
            val res = container.openMeteoProvider.fetchWeather(lat, lon, "UTC", null)
            _pointWeatherPreview.value = when (res) {
                is com.example.data.remote.ProviderResult.Success -> {
                    val c = res.snapshot.current
                    val temp = com.example.domain.calc.GeoUtils.formatTemperature(c.temperatureC, preferences.value.temperatureUnit)
                    "Point ${String.format(java.util.Locale.US, "%.2f, %.2f", lat, lon)}: $temp · ${c.conditionLabel} · Wind ${c.windSpeedKmh?.toInt() ?: "--"} km/h · RH ${c.humidityPercent ?: "--"}%"
                }
                else -> "Point ${String.format(java.util.Locale.US, "%.2f, %.2f", lat, lon)} (weather lookup unavailable offline)"
            }
        }
    }

    fun saveMapPointAsLocation(lat: Double, lon: Double) {
        addAndSelectLocation(
            SearchLocationResult(
                name = "Map (${String.format(java.util.Locale.US, "%.2f, %.2f", lat, lon)})",
                region = "Map Selection",
                country = "",
                countryCode = "",
                latitude = lat,
                longitude = lon,
                timezoneId = "UTC"
            )
        )
    }

    fun selectDay(day: DailyForecast?) { _selectedDay.value = day }
    fun selectHour(hour: HourlyForecast?) { _selectedHour.value = hour }
    fun selectAlert(alert: WeatherAlert?) { _selectedAlert.value = alert }
    fun focusMapOn(lat: Double, lon: Double) { _mapFocusCoords.value = lat to lon }

    fun toggleAmbientSound(type: AmbientSoundType) {
        val prefs = preferences.value
        if (container.soundEngine.isPlaying.value && container.soundEngine.activeSound.value == type) {
            container.soundEngine.stop()
            viewModelScope.launch { container.preferencesManager.updateAmbientSettings(AmbientSoundType.NONE, prefs.ambientVolume, prefs.ambientAutoStopMinutes) }
        } else {
            if (type == AmbientSoundType.NONE) {
                container.soundEngine.stop()
            } else {
                container.soundEngine.start(type, prefs.ambientVolume, prefs.ambientAutoStopMinutes)
            }
            viewModelScope.launch { container.preferencesManager.updateAmbientSettings(type, prefs.ambientVolume, prefs.ambientAutoStopMinutes) }
        }
    }

    fun setAmbientVolume(volume: Float) {
        container.soundEngine.setVolume(volume)
        val prefs = preferences.value
        viewModelScope.launch { container.preferencesManager.updateAmbientSettings(prefs.ambientSound, volume, prefs.ambientAutoStopMinutes) }
    }

    fun setAmbientAutoStop(minutes: Int) {
        val prefs = preferences.value
        viewModelScope.launch { container.preferencesManager.updateAmbientSettings(prefs.ambientSound, prefs.ambientVolume, minutes) }
        if (container.soundEngine.isPlaying.value && prefs.ambientSound != AmbientSoundType.NONE) {
            container.soundEngine.start(prefs.ambientSound, prefs.ambientVolume, minutes)
        }
    }

    fun sendQuickViewNotificationNow() {
        val weather = (_weatherState.value as? UiState.Success)?.data ?: return
        val aqi = (_aqiState.value as? UiState.Success)?.data
        viewModelScope.launch {
            container.notificationHelper.dispatchQuickViewNotification(weather, aqi, preferences.value.temperatureUnit)
        }
    }

    // Settings mutators
    fun updateLanguage(lang: AppLanguage) { viewModelScope.launch { container.preferencesManager.updateLanguage(lang) } }
    fun updateTheme(theme: ThemeAppearance) { viewModelScope.launch { container.preferencesManager.updateTheme(theme) } }
    fun updateAnimationLevel(level: AnimationLevel) { viewModelScope.launch { container.preferencesManager.updateAnimationLevel(level) } }
    fun updateTempUnit(unit: TemperatureUnit) { viewModelScope.launch { container.preferencesManager.updateTemperatureUnit(unit) } }
    fun updateWindUnit(unit: WindUnit) { viewModelScope.launch { container.preferencesManager.updateWindUnit(unit) } }
    fun updatePressureUnit(unit: PressureUnit) { viewModelScope.launch { container.preferencesManager.updatePressureUnit(unit) } }
    fun updateProvider(auto: Boolean, id: ProviderId) {
        viewModelScope.launch {
            container.preferencesManager.updateProviderSelection(auto, id)
            _activeLocation.value?.let { refreshWeatherInternal(it, forceRefresh = true) }
        }
    }
    fun updateNotificationSettings(s: NotificationSettings) { viewModelScope.launch { container.preferencesManager.updateNotificationSettings(s) } }
    fun updatePersonalKeys(keys: PersonalApiKeys) { viewModelScope.launch { container.preferencesManager.updatePersonalKeys(keys) } }
    fun updateWallpaperStaticFallback(staticOnly: Boolean) { viewModelScope.launch { container.preferencesManager.updateWallpaperMode(staticOnly) } }
}
