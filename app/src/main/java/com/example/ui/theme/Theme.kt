package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyberTeal,
    secondary = ElectricBlue,
    tertiary = NeonPurple,
    background = DeepSpace,
    surface = CardSlate,
    onPrimary = DeepSpace,
    onSecondary = DeepSpace,
    onBackground = TextSilver,
    onSurface = TextSilver,
    error = CrimsonError
)

private val HighDensityColorScheme = lightColorScheme(
    primary = HighDensityPrimary,
    secondary = HighDensityPurpleDark,
    tertiary = HighDensityBlueText,
    background = HighDensityBg,
    surface = HighDensityContainer,
    onPrimary = HighDensityButtonText,
    onSecondary = HighDensityButtonText,
    onBackground = HighDensityText,
    onSurface = HighDensityText,
    error = CrimsonError
)

private val LightColorScheme = HighDensityColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Default to false/light to showcase High Density UI
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> HighDensityColorScheme // Show High Density light theme by default
    }


    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
