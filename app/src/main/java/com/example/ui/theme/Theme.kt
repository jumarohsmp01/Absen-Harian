package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = SleekBlueDark,
        onPrimary = Color(0xFF003258),
        primaryContainer = SleekBlueActive,
        onPrimaryContainer = SleekBlueContainer,
        secondary = SleekBlueDark,
        onSecondary = Color(0xFF003258),
        secondaryContainer = Color(0xFF00497D),
        onSecondaryContainer = SleekBlueContainer,
        background = SleekBackgroundDark,
        onBackground = SleekTextPrimaryDark,
        surface = SleekSurfaceDark,
        onSurface = SleekTextPrimaryDark,
        surfaceVariant = Color(0xFF282A2E),
        onSurfaceVariant = SleekTextSecondaryDark,
        outline = SleekBorderDark,
        outlineVariant = Color(0xFF33353A),
        error = Color(0xFFFFB4AB),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = SleekBluePrimary,
        onPrimary = Color.White,
        primaryContainer = SleekBlueContainer,
        onPrimaryContainer = SleekOnBlueContainer,
        secondary = SleekBluePrimary,
        onSecondary = Color.White,
        secondaryContainer = SleekSecondaryContainer,
        onSecondaryContainer = SleekOnBlueContainer,
        background = SleekBackgroundLight,
        onBackground = SleekTextPrimaryLight,
        surface = SleekSurfaceLight,
        onSurface = SleekTextPrimaryLight,
        surfaceVariant = SleekSurfaceContainer,
        onSurfaceVariant = SleekTextSecondaryLight,
        outline = SleekOutline,
        outlineVariant = SleekBorder,
        error = ErrorRed,
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
