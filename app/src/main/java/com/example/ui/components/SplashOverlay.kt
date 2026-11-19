package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AnimationLevel
import com.example.domain.model.WeatherCondition
import com.example.ui.theme.SkyCobalt
import com.example.ui.theme.SkyDeepNavy
import kotlinx.coroutines.delay

@Composable
fun SplashOverlay(animationLevel: AnimationLevel) {
    var visible by remember { mutableStateOf(animationLevel != AnimationLevel.OFF) }
    LaunchedEffect(Unit) {
        if (visible) {
            delay(if (animationLevel == AnimationLevel.REDUCED) 450L else 950L)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(SkyDeepNavy, SkyCobalt))),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedWeatherIcon(
                    condition = WeatherCondition.PARTLY_CLOUDY,
                    isDay = true,
                    animationLevel = animationLevel,
                    size = 96.dp
                )
                Text(
                    text = "SkyPulse",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Meteorology · Radar · BMD · Disasters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBAE6FD)
                )
            }
        }
    }
}
