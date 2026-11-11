package com.dealfinder.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB9B7FF),
    onPrimary = Color(0xFF25226A),
    primaryContainer = PrimaryDarkBlue,
    onPrimaryContainer = Color(0xFFE1E0FF),
    secondary = Color(0xFFFFC663),
    onSecondary = Color(0xFF422C00),
    secondaryContainer = Color(0xFF6B4A00),
    onSecondaryContainer = Color(0xFFFFDEA0),
    tertiary = Color(0xFF67D9BA),
    onTertiary = Color(0xFF00382C),
    tertiaryContainer = Color(0xFF00513F),
    onTertiaryContainer = Color(0xFF86F8D8),
    background = Color(0xFF0F1220),
    onBackground = Color(0xFFE5E1F2),
    surface = Color(0xFF151827),
    onSurface = Color(0xFFE5E1F2),
    surfaceVariant = Color(0xFF292D3D),
    onSurfaceVariant = Color(0xFFC4C5D2),
    outline = Color(0xFF90919F),
    outlineVariant = Color(0xFF454756)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLightBlue,
    onPrimaryContainer = PrimaryDarkBlue,
    secondary = SecondaryOrange,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryLightOrange,
    onSecondaryContainer = SecondaryDarkOrange,
    tertiary = AccentGreen,
    onTertiary = OnPrimary,
    tertiaryContainer = Color(0xFFC7F3E7),
    onTertiaryContainer = Color(0xFF075E4A),
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Color(0xFFD5D9E5),
    outlineVariant = Color(0xFFE1E4ED)
)

@Composable
fun DealfinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ and keeps the app native to the device.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
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
        shapes = DealfinderShapes,
        content = content
    )
}
