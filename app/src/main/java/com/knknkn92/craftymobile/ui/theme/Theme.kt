package com.knknkn92.craftymobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// アプリは常にダークテーマ固定 (Material You / Pixel 9 Pro XL inspired)
private val AppColorScheme = darkColorScheme(
    primary          = MaterialYouBlue,
    onPrimary        = SurfaceDark,
    primaryContainer = MaterialYouBlueDark,
    secondary        = MaterialYouPurple,
    onSecondary      = SurfaceDark,
    background       = SurfaceDark,
    surface          = SurfaceDark,
    surfaceContainer = SurfaceContainerDark,
    onBackground     = OnSurfaceDark,
    onSurface        = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline          = OutlineDark,
    error            = ErrorDark,
)

@Composable
fun CraftyMobileTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = Typography,
        content     = content
    )
}
