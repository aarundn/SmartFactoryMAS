package com.example.smartfactorymas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Tailwind Material 3 Theme Palette from HTML
val SurfaceBright = Color(0xFFF7F9FB)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF2F4F6)
val SurfaceContainer = Color(0xFFECEEF0)
val SurfaceContainerHigh = Color(0xFFE6E8EA)
val SurfaceContainerHighest = Color(0xFFE0E3E5)
val SurfaceVariant = Color(0xFFE0E3E5)
val SurfaceDim = Color(0xFFD8DADC)

val OnSurface = Color(0xFF191C1E)
val OnSurfaceVariant = Color(0xFF454650)
val OutlineVariant = Color(0xFFC6C5D2)
val Outline = Color(0xFF767681)

val Primary = Color(0xFF4B5A9C)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFA5B4FC)
val OnPrimaryContainer = Color(0xFF354484)
val PrimaryFixed = Color(0xFFDDE1FF)
val PrimaryFixedDim = Color(0xFFB8C4FF)
val OnPrimaryFixed = Color(0xFF001354)
val OnPrimaryFixedVariant = Color(0xFF334282)

val Secondary = Color(0xFF855316)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFFFBC76)
val OnSecondaryContainer = Color(0xFF79490B)
val SecondaryFixed = Color(0xFFFFDCBD)
val SecondaryFixedDim = Color(0xFFFCB973)
val OnSecondaryFixed = Color(0xFF2C1600)
val OnSecondaryFixedVariant = Color(0xFF683C00)

val Tertiary = Color(0xFF8F4953)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFF79FAA)
val OnTertiaryContainer = Color(0xFF75333E)
val TertiaryFixed = Color(0xFFFFD9DC)
val TertiaryFixedDim = Color(0xFFFFB2BB)
val OnTertiaryFixed = Color(0xFF3B0613)
val OnTertiaryFixedVariant = Color(0xFF73323D)

val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF93000A)

val White = Color(0xFFFFFFFF)

// ─── Material 3 Light Color Scheme ────────────────────────────────────────────
val SmartFactoryColorScheme = lightColorScheme(
    background       = SurfaceBright,
    surface          = SurfaceContainerLowest,
    surfaceVariant   = SurfaceContainerLow,
    primary          = Primary,
    primaryContainer = PrimaryContainer,
    secondary        = Secondary,
    secondaryContainer = SecondaryContainer,
    tertiary         = Tertiary,
    tertiaryContainer = TertiaryContainer,
    onBackground     = OnSurface,
    onSurface        = OnSurface,
    onPrimary        = OnPrimary,
    onSecondary      = OnSecondary,
    onTertiary       = OnTertiary,
    error            = Error,
    errorContainer   = ErrorContainer,
    onError          = OnError,
    onErrorContainer = OnErrorContainer
)

/**
 * Provides the Smart Factory light theme to the composition tree.
 */
@Composable
fun SmartFactoryTheme(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = SmartFactoryColorScheme,
        content = content
    )
}

@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .shadow(2.dp, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(SurfaceContainerLowest, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .border(1.dp, OutlineVariant, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(20.dp)
    ) {
        androidx.compose.foundation.layout.Column(content = content)
    }
}
