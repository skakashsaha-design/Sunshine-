package com.example.di

import android.content.Context
import com.example.audio.WeatherSoundEngine
import com.example.data.local.PreferencesManager
import com.example.data.local.SkyPulseDatabase
import com.example.data.remote.*
import com.example.data.repository.DisasterAlertRepository
import com.example.data.repository.LocationRepository
import com.example.data.repository.WeatherRepository
import com.example.notification.NotificationHelper

class AppContainer(context: Context) {

    val networkClient = NetworkClient(context.cacheDir)
    val database: SkyPulseDatabase = SkyPulseDatabase.create(context)
    val preferencesManager = PreferencesManager(context)

    val openMeteoProvider = OpenMeteoProvider(networkClient)
    val metNorwayProvider = MetNorwayProvider(networkClient)
    val weatherApiProvider = WeatherApiProvider(networkClient)
    val accuWeatherProvider = AccuWeatherProvider(networkClient)
    val tomorrowIoProvider = TomorrowIoProvider(networkClient)
    val weatherbitProvider = WeatherbitProvider(networkClient)
    val googleWeatherProvider = GoogleWeatherProvider(networkClient)
    val bmdProvider = BmdProvider(networkClient)
    val disasterService = DisasterAndAlertService(networkClient)
    val radarService = RadarService(networkClient)
    val extendedWeatherService = ExtendedWeatherService(networkClient)
    val personalAiClient = PersonalAiAdvisorClient(networkClient)

    val weatherRepository = WeatherRepository(
        dao = database.dao(),
        openMeteo = openMeteoProvider,
        metNorway = metNorwayProvider,
        weatherApi = weatherApiProvider,
        accuWeather = accuWeatherProvider,
        tomorrowIo = tomorrowIoProvider,
        weatherbit = weatherbitProvider,
        googleWeather = googleWeatherProvider,
        personalAiClient = personalAiClient,
        radarService = radarService
    )

    val disasterAlertRepository = DisasterAlertRepository(
        dao = database.dao(),
        disasterService = disasterService,
        openMeteo = openMeteoProvider,
        bmdProvider = bmdProvider
    )

    val locationRepository = LocationRepository(
        context = context,
        dao = database.dao(),
        openMeteo = openMeteoProvider
    )

    val notificationHelper = NotificationHelper(context, database.dao())
    val soundEngine = WeatherSoundEngine()
}
