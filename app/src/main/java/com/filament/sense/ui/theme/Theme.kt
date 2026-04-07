package com.filament.sense.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary            = Primary,
    onPrimary          = OnPrimary,
    primaryContainer   = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary          = Secondary,
    onSecondary        = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    background         = Background,
    surface            = Surface,
    surfaceVariant     = SurfaceVariant,
    onBackground       = OnBackground,
    onSurface          = OnSurface,
    onSurfaceVariant   = OnSurfaceVariant,
    error              = Error,
    onError            = OnError,
    outline            = Outline,
)

@Composable
fun FilamentSenseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = FilamentSenseTypography,
        content = content,
    )
}