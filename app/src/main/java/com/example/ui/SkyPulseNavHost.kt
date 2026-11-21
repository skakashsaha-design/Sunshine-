package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.domain.model.WeatherCondition
import com.example.ui.components.DynamicWeatherBackground
import com.example.ui.components.SplashOverlay
import com.example.ui.screens.*

object Routes {
    const val HOME = "home"
    const val FORECAST = "forecast"
    const val MAP = "map"
    const val ALERTS = "alerts"
    const val MORE = "more"
    const val WEATHER_DETAIL = "weather_detail"
    const val ALERT_DETAIL = "alert_detail"
    const val AQI = "aqi"
    const val ASTRONOMY = "astronomy"
    const val DISASTERS = "disasters"
    const val LOCATIONS = "locations"
    const val PROVIDERS = "providers"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val TRIP_PLANNER = "trip_planner"
    const val MODELS = "models"
    const val MARINE = "marine"
    const val AVIATION = "aviation"
    const val SPACE_WEATHER = "space_weather"
    const val EMERGENCY_NUMBERS = "emergency_numbers"
    const val PERMISSIONS = "permissions"
}

private data class TopTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topTabs = listOf(
    TopTab(Routes.HOME, R.string.nav_home, Icons.Filled.WbSunny, Icons.Outlined.WbSunny, "nav_home"),
    TopTab(Routes.FORECAST, R.string.nav_forecast, Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday, "nav_forecast"),
    TopTab(Routes.MAP, R.string.nav_map, Icons.Filled.Map, Icons.Outlined.Map, "nav_map"),
    TopTab(Routes.ALERTS, R.string.nav_alerts, Icons.Filled.Warning, Icons.Outlined.WarningAmber, "nav_alerts"),
    TopTab(Routes.MORE, R.string.nav_more, Icons.Filled.Apps, Icons.Outlined.Apps, "nav_more")
)

@Composable
fun SkyPulseRoot(viewModel: MainViewModel) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val runtimePerf by viewModel.runtimePerformance.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val aqiState by viewModel.aqiState.collectAsStateWithLifecycle()
    val astronomy by viewModel.astronomyState.collectAsStateWithLifecycle()
    val alertsState by viewModel.alertsState.collectAsStateWithLifecycle()
    val radarTimeline by viewModel.radarTimeline.collectAsStateWithLifecycle()
    val comparisonState by viewModel.comparisonState.collectAsStateWithLifecycle()
    val modelComparisonState by viewModel.modelComparisonState.collectAsStateWithLifecycle()
    val marineState by viewModel.marineState.collectAsStateWithLifecycle()
    val aviationState by viewModel.aviationState.collectAsStateWithLifecycle()
    val spaceWeatherState by viewModel.spaceWeatherState.collectAsStateWithLifecycle()
    val tripPlans by viewModel.tripPlans.collectAsStateWithLifecycle()
    val savedLocations by viewModel.savedLocations.collectAsStateWithLifecycle()
    val activeLocation by viewModel.activeLocation.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val selectedHour by viewModel.selectedHour.collectAsStateWithLifecycle()
    val selectedAlert by viewModel.selectedAlert.collectAsStateWithLifecycle()
    val mapFocus by viewModel.mapFocusCoords.collectAsStateWithLifecycle()
    val pointPreview by viewModel.pointWeatherPreview.collectAsStateWithLifecycle()
    val isSoundPlaying by viewModel.isSoundPlaying.collectAsStateWithLifecycle()
    val activeSound by viewModel.activeSound.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Routes.HOME

    val curWeather = (weatherState as? UiState.Success)?.data?.current
    val bgCondition = curWeather?.condition ?: WeatherCondition.PARTLY_CLOUDY
    val bgIsDay = curWeather?.isDay ?: true

    val showBottomOrRail = currentRoute in topTabs.map { it.route }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicWeatherBackground(
            condition = bgCondition,
            isDay = bgIsDay,
            animationLevel = runtimePerf.effectiveAnimation,
            themeAppearance = prefs.themeAppearance,
            backgroundStyle = prefs.backgroundStyle
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 840.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (useRail && showBottomOrRail) {
                        NavigationRail(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
                        ) {
                            Spacer(Modifier.height(12.dp))
                            topTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopTab(tab.route) },
                                    icon = { Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, contentDescription = stringResource(tab.labelRes)) },
                                    label = { Text(stringResource(tab.labelRes)) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        containerColor = if (currentRoute == Routes.HOME) Color.Transparent else MaterialTheme.colorScheme.background,
                        contentColor = if (currentRoute == Routes.HOME) LocalContentColor.current else MaterialTheme.colorScheme.onBackground,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        bottomBar = {
                            if (!useRail && showBottomOrRail) {
                                NavigationBar {
                                    topTabs.forEach { tab ->
                                        val selected = currentRoute == tab.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTopTab(tab.route) },
                                            icon = {
                                                Icon(
                                                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = stringResource(tab.labelRes)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = stringResource(tab.labelRes),
                                                    maxLines = 1
                                                )
                                            },
                                            modifier = Modifier.testTag(tab.testTag)
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Routes.HOME,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Routes.HOME) {
                                HomeScreen(
                                    weatherState = weatherState,
                                    aqiState = aqiState,
                                    astronomy = astronomy,
                                    alertsState = alertsState,
                                    savedLocations = savedLocations,
                                    activeLocation = activeLocation,
                                    prefs = prefs,
                                    onRefresh = { viewModel.refreshWeather() },
                                    onSelectLocation = { viewModel.selectLocation(it) },
                                    onRequestGps = { viewModel.requestGpsLocation() },
                                    onOpenLocations = { navController.navigate(Routes.LOCATIONS) },
                                    onOpenDetail = { navController.navigate(Routes.WEATHER_DETAIL) },
                                    onOpenForecast = { navigateTopTab(Routes.FORECAST) },
                                    onSelectHour = { h ->
                                        viewModel.selectHour(h)
                                        navigateTopTab(Routes.FORECAST)
                                    },
                                    onOpenAqi = { navController.navigate(Routes.AQI) },
                                    onOpenAstronomy = { navController.navigate(Routes.ASTRONOMY) },
                                    onOpenAlerts = { navigateTopTab(Routes.ALERTS) },
                                    onSelectAlert = { alert ->
                                        viewModel.selectAlert(alert)
                                        navController.navigate(Routes.ALERT_DETAIL)
                                    },
                                    onOpenTripPlanner = { navController.navigate(Routes.TRIP_PLANNER) }
                                )
                            }

                            composable(Routes.FORECAST) {
                                ForecastScreen(
                                    weatherState = weatherState,
                                    aqiState = aqiState,
                                    prefs = prefs,
                                    selectedHour = selectedHour,
                                    selectedDay = selectedDay,
                                    onSelectHour = { viewModel.selectHour(it) },
                                    onSelectDay = { viewModel.selectDay(it) }
                                )
                            }

                            composable(Routes.MAP) {
                                MapScreen(
                                    activeLocation = activeLocation,
                                    focusCoords = mapFocus,
                                    weatherState = weatherState,
                                    alertsState = alertsState,
                                    radarTimeline = radarTimeline,
                                    pointWeatherPreview = pointPreview,
                                    networkClient = viewModel.container.networkClient,
                                    onLoadRadar = { viewModel.loadRadarTimeline() },
                                    onLoadDisasters = { viewModel.loadAlertsAndDisasters() },
                                    onInspectPoint = { lat, lon -> viewModel.inspectMapPoint(lat, lon) },
                                    onSavePoint = { lat, lon -> viewModel.saveMapPointAsLocation(lat, lon) },
                                    onOpenLocations = { navController.navigate(Routes.LOCATIONS) },
                                    lowRamMode = runtimePerf.isLowRamActive
                                )
                            }

                            composable(Routes.ALERTS) {
                                AlertsScreen(
                                    alertsState = alertsState,
                                    onRefresh = { viewModel.loadAlertsAndDisasters() },
                                    onSelectAlert = { alert ->
                                        viewModel.selectAlert(alert)
                                        navController.navigate(Routes.ALERT_DETAIL)
                                    },
                                    onOpenDisasters = { navController.navigate(Routes.DISASTERS) }
                                )
                            }

                            composable(Routes.MORE) {
                                MoreHubScreen(
                                    prefs = prefs,
                                    isSoundPlaying = isSoundPlaying,
                                    activeSound = activeSound,
                                    onOpenTripPlanner = { navController.navigate(Routes.TRIP_PLANNER) },
                                    onOpenModels = { navController.navigate(Routes.MODELS) },
                                    onOpenAqi = { navController.navigate(Routes.AQI) },
                                    onOpenAstronomy = { navController.navigate(Routes.ASTRONOMY) },
                                    onOpenMarine = { navController.navigate(Routes.MARINE) },
                                    onOpenAviation = { navController.navigate(Routes.AVIATION) },
                                    onOpenSpaceWeather = { navController.navigate(Routes.SPACE_WEATHER) },
                                    onOpenDisasters = { navController.navigate(Routes.DISASTERS) },
                                    onOpenEmergencyNumbers = { navController.navigate(Routes.EMERGENCY_NUMBERS) },
                                    onOpenLocations = { navController.navigate(Routes.LOCATIONS) },
                                    onOpenProviders = { navController.navigate(Routes.PROVIDERS) },
                                    onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                                    onToggleSound = { viewModel.toggleAmbientSound(it) },
                                    onSendTestNotification = { viewModel.sendQuickViewNotificationNow() }
                                )
                            }

                            composable(Routes.TRIP_PLANNER) {
                                TripPlannerScreen(
                                    weatherState = weatherState,
                                    savedPlans = tripPlans,
                                    prefs = prefs,
                                    evaluateDay = { day, act, win -> viewModel.evaluateTripDay(day, act, win) },
                                    onSavePlan = { title, eval -> viewModel.saveTripPlan(title, eval) },
                                    onDeletePlan = { viewModel.deleteTripPlan(it) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.MODELS) {
                                ModelComparisonScreen(
                                    modelState = modelComparisonState,
                                    cityName = activeLocation?.name ?: "Location",
                                    prefs = prefs,
                                    onRefresh = { viewModel.loadModelComparison() },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.MARINE) {
                                MarineWeatherScreen(
                                    marineState = marineState,
                                    cityName = activeLocation?.name ?: "Location",
                                    prefs = prefs,
                                    onRefresh = { viewModel.loadMarineWeather() },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.AVIATION) {
                                AviationWeatherScreen(
                                    aviationState = aviationState,
                                    service = viewModel.container.extendedWeatherService,
                                    onSearchAirport = { viewModel.searchAviationAirport(it) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.SPACE_WEATHER) {
                                SpaceWeatherScreen(
                                    spaceState = spaceWeatherState,
                                    onRefresh = { viewModel.loadSpaceWeather() },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.EMERGENCY_NUMBERS) {
                                EmergencyNumbersScreen(
                                    activeCountryCode = activeLocation?.countryCode,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.PERMISSIONS) {
                                PermissionManagerScreen(onBack = { navController.popBackStack() })
                            }

                            composable(Routes.WEATHER_DETAIL) {
                                val bundle = (weatherState as? UiState.Success)?.data
                                if (bundle != null) {
                                    WeatherDetailScreen(
                                        bundle = bundle,
                                        prefs = prefs,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }

                            composable(Routes.ALERT_DETAIL) {
                                selectedAlert?.let { alert ->
                                    AlertDetailScreen(
                                        alert = alert,
                                        onBack = { navController.popBackStack() },
                                        onShowOnMap = { lat, lon ->
                                            viewModel.focusMapOn(lat, lon)
                                            navigateTopTab(Routes.MAP)
                                        }
                                    )
                                }
                            }

                            composable(Routes.AQI) {
                                AqiScreen(
                                    aqiState = aqiState,
                                    cityName = activeLocation?.name ?: "Location",
                                    onRefresh = { viewModel.loadAqi(forceRefresh = true) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.ASTRONOMY) {
                                AstronomyScreen(
                                    astronomy = astronomy,
                                    cityName = activeLocation?.name ?: "Location",
                                    animationLevel = runtimePerf.effectiveAnimation,
                                    onRefresh = { viewModel.refreshAstronomy() },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.DISASTERS) {
                                DisasterCenterScreen(
                                    alertsState = alertsState,
                                    onRefresh = { viewModel.loadAlertsAndDisasters() },
                                    onBack = { navController.popBackStack() },
                                    onShowOnMap = { lat, lon ->
                                        viewModel.focusMapOn(lat, lon)
                                        navigateTopTab(Routes.MAP)
                                    }
                                )
                            }

                            composable(Routes.LOCATIONS) {
                                LocationsScreen(
                                    savedLocations = savedLocations,
                                    searchResults = searchResults,
                                    searchHistory = searchHistory,
                                    isSearching = isSearching,
                                    onSearchQuery = { viewModel.searchCities(it) },
                                    onAddCity = { viewModel.addAndSelectLocation(it) },
                                    onSelectLocation = { viewModel.selectLocation(it) },
                                    onRequestGps = { viewModel.requestGpsLocation() },
                                    onRenameLocation = { id, name -> viewModel.renameLocation(id, name) },
                                    onMoveLocation = { id, dir -> viewModel.moveLocation(id, dir) },
                                    onDeleteLocation = { viewModel.deleteLocation(it) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.PROVIDERS) {
                                ProviderComparisonScreen(
                                    comparisonState = comparisonState,
                                    cityName = activeLocation?.name ?: "Location",
                                    prefs = prefs,
                                    onRefresh = { viewModel.loadProviderComparison() },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.SETTINGS) {
                                SettingsScreen(
                                    prefs = prefs,
                                    runtimePerformance = runtimePerf,
                                    onUpdateLanguage = { viewModel.updateLanguage(it) },
                                    onUpdateTheme = { viewModel.updateTheme(it) },
                                    onUpdateFontStyle = { viewModel.updateFontStyle(it) },
                                    onUpdateBackgroundStyle = { viewModel.updateBackgroundStyle(it) },
                                    onUpdateAnimation = { viewModel.updateAnimationLevel(it) },
                                    onUpdatePerformanceMode = { viewModel.updatePerformanceMode(it) },
                                    onUpdateTempUnit = { viewModel.updateTempUnit(it) },
                                    onUpdateWindUnit = { viewModel.updateWindUnit(it) },
                                    onUpdatePressureUnit = { viewModel.updatePressureUnit(it) },
                                    onUpdateProvider = { auto, id -> viewModel.updateProvider(auto, id) },
                                    onUpdateNotifications = { viewModel.updateNotificationSettings(it) },
                                    onUpdateAmbientVolume = { viewModel.setAmbientVolume(it) },
                                    onUpdateAmbientTimer = { viewModel.setAmbientAutoStop(it) },
                                    onUpdateWallpaperStatic = { viewModel.updateWallpaperStaticFallback(it) },
                                    onUpdateIconPack = { viewModel.updateIconPack(it) },
                                    onUpdatePersonalKeys = { viewModel.updatePersonalKeys(it) },
                                    onSendTestNotification = { viewModel.sendQuickViewNotificationNow() },
                                    onSendMorningBriefing = { viewModel.sendMorningBriefingNow() },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.ABOUT) {
                                AboutScreen(onBack = { navController.popBackStack() })
                            }
                        }
                    }
                }
            }
        }

        SplashOverlay(animationLevel = runtimePerf.effectiveAnimation)
    }
}
