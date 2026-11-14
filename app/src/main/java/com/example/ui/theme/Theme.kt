package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TempleDarkColorScheme = darkColorScheme(
    primary = FlameOrange,
    onPrimary = WarmParchment,
    secondary = AzurePhotoBlue,
    onSecondary = WarmParchment,
    tertiary = TempleGold,
    onTertiary = ObsidianNight,
    background = ObsidianNight,
    onBackground = WarmParchment,
    surface = TempleStoneDark,
    onSurface = WarmParchment,
    surfaceVariant = TempleStoneCard,
    onSurfaceVariant = MutedSand,
    error = CrimsonHazard,
    onError = WarmParchment
)

@Composable
fun TempleRunTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TempleDarkColorScheme,
        typography = Typography,
        content = content
    )
}
