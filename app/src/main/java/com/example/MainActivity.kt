package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.SkyPulseRoot
import com.example.ui.UiState
import com.example.ui.theme.SkyPulseTheme
import java.util.Locale

private class LocalizedActivityContextWrapper(
    base: Context,
    private val localizedResources: Resources,
    private val localizedAssets: AssetManager
) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedResources
    override fun getAssets(): AssetManager = localizedAssets
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
            val isNight = (weatherState as? UiState.Success)?.data?.current?.isDay == false

            val baseContext = LocalContext.current
            val baseConfig = LocalConfiguration.current
            val localizedConfig = remember(prefs.language.code, baseConfig) {
                val locale = Locale.forLanguageTag(prefs.language.code)
                Locale.setDefault(locale)
                Configuration(baseConfig).apply { setLocale(locale) }
            }
            val localizedContext = remember(prefs.language.code, baseContext, localizedConfig) {
                val configCtx = baseContext.createConfigurationContext(localizedConfig)
                LocalizedActivityContextWrapper(
                    base = this@MainActivity,
                    localizedResources = configCtx.resources,
                    localizedAssets = configCtx.assets
                )
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfig,
                LocalActivityResultRegistryOwner provides this@MainActivity,
                LocalOnBackPressedDispatcherOwner provides this@MainActivity
            ) {
                SkyPulseTheme(
                    appearance = prefs.themeAppearance,
                    fontStyle = prefs.fontStyle,
                    isNight = isNight
                ) {
                    SkyPulseRoot(viewModel = viewModel)
                }
            }
        }
    }
}
