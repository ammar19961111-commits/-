package com.example.ui.theme

import android.app.Activity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.AppThemeStyle

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Immutable
data class AdhkarExtendedColors(
    val completed: Color,
    val goldHalo: Color,
    val isDark: Boolean
)

val LocalAdhkarColors = staticCompositionLocalOf {
    AdhkarExtendedColors(
        completed = CompletedGreenLight,
        goldHalo = SoftGoldHaloLight,
        isDark = false
    )
}

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmerald,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = SageSecondary,
    onSecondary = OnSageSecondary,
    secondaryContainer = SageContainer,
    onSecondaryContainer = OnSageContainer,
    tertiary = AntiqueGold,
    onTertiary = OnAntiqueGold,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = OnGoldContainer,
    background = ParchmentBackground,
    onBackground = OnParchment,
    surface = CreamSurface,
    onSurface = OnParchment,
    surfaceVariant = SoftParchmentVariant,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
    outline = OutlineWarm,
    outlineVariant = OutlineSoft,
    error = ErrorRedLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    scrim = ScrimColor
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkOnEmerald,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkOnEmeraldContainer,
    secondary = DarkSage,
    onSecondary = DarkOnSage,
    secondaryContainer = DarkSageContainer,
    onSecondaryContainer = DarkOnSageContainer,
    tertiary = DarkGold,
    onTertiary = DarkOnGold,
    tertiaryContainer = DarkGoldContainer,
    onTertiaryContainer = DarkOnGoldContainer,
    background = MidnightBackground,
    onBackground = MidnightOnBackground,
    surface = MidnightSurface,
    onSurface = MidnightOnBackground,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = MidnightMuted,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    outline = MidnightOutline,
    outlineVariant = MidnightOutlineSoft,
    error = ErrorRedDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    scrim = ScrimColor
)

private val MeccanGoldLightColorScheme = LightColorScheme.copy(
    primary = Color(0xFF8C6214),
    onPrimary = Color(0xFFFFFBF2),
    primaryContainer = Color(0xFFF6E3B4),
    onPrimaryContainer = Color(0xFF342202),
    secondary = Color(0xFF6B5939),
    secondaryContainer = Color(0xFFEEE2C8),
    onSecondaryContainer = Color(0xFF281F0F),
    tertiary = Color(0xFF1F6351),
    tertiaryContainer = Color(0xFFCDE9DF),
    background = Color(0xFFFDF8EC),
    surface = Color(0xFFFFFCF4),
    surfaceVariant = Color(0xFFF2E8D2),
    surfaceContainerLow = Color(0xFFF9F2E2),
    surfaceContainer = Color(0xFFF4EAD5),
    surfaceContainerHigh = Color(0xFFEDE0C6)
)

private val MeccanGoldDarkColorScheme = DarkColorScheme.copy(
    primary = Color(0xFFE9C36B),
    onPrimary = Color(0xFF3E2900),
    primaryContainer = Color(0xFF5A3F09),
    onPrimaryContainer = Color(0xFFFBE7B2),
    secondary = Color(0xFFD6C4A1),
    secondaryContainer = Color(0xFF443924),
    onSecondaryContainer = Color(0xFFF1E3C5),
    tertiary = Color(0xFF7BD3BA),
    tertiaryContainer = Color(0xFF164A3D),
    background = Color(0xFF14110B),
    surface = Color(0xFF1A160E),
    surfaceVariant = Color(0xFF2A2418),
    surfaceContainerLow = Color(0xFF1F1A11),
    surfaceContainer = Color(0xFF241F15),
    surfaceContainerHigh = Color(0xFF2F281C)
)

private val MadinahTealLightColorScheme = LightColorScheme.copy(
    primary = Color(0xFF0E6E74),
    onPrimary = Color(0xFFF4FCFC),
    primaryContainer = Color(0xFFC2ECEE),
    onPrimaryContainer = Color(0xFF032A2D),
    secondary = Color(0xFF426365),
    secondaryContainer = Color(0xFFCDE5E7),
    onSecondaryContainer = Color(0xFF112628),
    tertiary = Color(0xFFB58932),
    tertiaryContainer = Color(0xFFF6E7C4),
    background = Color(0xFFF3FAFA),
    surface = Color(0xFFFAFEFE),
    surfaceVariant = Color(0xFFE0EEEF),
    surfaceContainerLow = Color(0xFFEAF5F6),
    surfaceContainer = Color(0xFFE2F0F1),
    surfaceContainerHigh = Color(0xFFD6E8E9)
)

private val MadinahTealDarkColorScheme = DarkColorScheme.copy(
    primary = Color(0xFF72D9DF),
    onPrimary = Color(0xFF00373A),
    primaryContainer = Color(0xFF0D4E53),
    onPrimaryContainer = Color(0xFFC2F5F8),
    secondary = Color(0xFFACCACC),
    secondaryContainer = Color(0xFF274042),
    onSecondaryContainer = Color(0xFFD3ECED),
    tertiary = Color(0xFFE5C16C),
    background = Color(0xFF091416),
    surface = Color(0xFF0F1D1F),
    surfaceVariant = Color(0xFF1A2D30),
    surfaceContainerLow = Color(0xFF122124),
    surfaceContainer = Color(0xFF16282B),
    surfaceContainerHigh = Color(0xFF1E3337)
)

private val FajrSapphireLightColorScheme = LightColorScheme.copy(
    primary = Color(0xFF284785),
    onPrimary = Color(0xFFF7F9FF),
    primaryContainer = Color(0xFFD6E2FF),
    onPrimaryContainer = Color(0xFF0B1F48),
    secondary = Color(0xFF515E78),
    secondaryContainer = Color(0xFFDCE2F2),
    onSecondaryContainer = Color(0xFF172133),
    tertiary = Color(0xFFC59B43),
    tertiaryContainer = Color(0xFFF7E8C3),
    background = Color(0xFFF4F7FC),
    surface = Color(0xFFFAFBFD),
    surfaceVariant = Color(0xFFE3E8F4),
    surfaceContainerLow = Color(0xFFEDF1FA),
    surfaceContainer = Color(0xFFE5EBF6),
    surfaceContainerHigh = Color(0xFFDCE3F0)
)

private val FajrSapphireDarkColorScheme = DarkColorScheme.copy(
    primary = Color(0xFF9BB8FF),
    onPrimary = Color(0xFF0E295E),
    primaryContainer = Color(0xFF1D3A72),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFB6C2DE),
    secondaryContainer = Color(0xFF2B354B),
    onSecondaryContainer = Color(0xFFDCE3F5),
    tertiary = Color(0xFFE8C472),
    background = Color(0xFF0A101D),
    surface = Color(0xFF101829),
    surfaceVariant = Color(0xFF1C273D),
    surfaceContainerLow = Color(0xFF131D30),
    surfaceContainer = Color(0xFF18243A),
    surfaceContainerHigh = Color(0xFF212F49)
)

private val DamascusRoseLightColorScheme = LightColorScheme.copy(
    primary = Color(0xFF8B3B56),
    onPrimary = Color(0xFFFFF8FA),
    primaryContainer = Color(0xFFF8D8E2),
    onPrimaryContainer = Color(0xFF3E0E20),
    secondary = Color(0xFF6E535D),
    secondaryContainer = Color(0xFFEFDDE3),
    onSecondaryContainer = Color(0xFF2A181F),
    tertiary = Color(0xFFC49257),
    tertiaryContainer = Color(0xFFF7E5CE),
    background = Color(0xFFFDF5F7),
    surface = Color(0xFFFFFAFB),
    surfaceVariant = Color(0xFFF1E3E7),
    surfaceContainerLow = Color(0xFFF8ECF0),
    surfaceContainer = Color(0xFFF3E4E9),
    surfaceContainerHigh = Color(0xFFECDAE0)
)

private val DamascusRoseDarkColorScheme = DarkColorScheme.copy(
    primary = Color(0xFFF4A8BE),
    onPrimary = Color(0xFF54142B),
    primaryContainer = Color(0xFF6E243D),
    onPrimaryContainer = Color(0xFFFFD9E4),
    secondary = Color(0xFFDABEC7),
    secondaryContainer = Color(0xFF452E37),
    onSecondaryContainer = Color(0xFFF5DDE5),
    tertiary = Color(0xFFE9BF89),
    background = Color(0xFF160D11),
    surface = Color(0xFF1E1318),
    surfaceVariant = Color(0xFF2F2026),
    surfaceContainerLow = Color(0xFF24171D),
    surfaceContainer = Color(0xFF291B22),
    surfaceContainerHigh = Color(0xFF35242C)
)

private val HighContrastNightColorScheme = darkColorScheme(
    primary = NightEmeraldPrimary,
    onPrimary = NightOnEmerald,
    primaryContainer = NightEmeraldContainer,
    onPrimaryContainer = NightOnEmeraldContainer,
    secondary = NightSage,
    onSecondary = NightOnSage,
    secondaryContainer = NightSageContainer,
    onSecondaryContainer = NightOnSageContainer,
    tertiary = NightGold,
    onTertiary = NightOnGold,
    tertiaryContainer = NightGoldContainer,
    onTertiaryContainer = NightOnGoldContainer,
    background = NightBackground,
    onBackground = NightOnBackground,
    surface = NightSurface,
    onSurface = NightOnBackground,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightMuted,
    surfaceContainerLowest = SurfaceContainerLowestNight,
    surfaceContainerLow = SurfaceContainerLowNight,
    surfaceContainer = SurfaceContainerNight,
    surfaceContainerHigh = SurfaceContainerHighNight,
    surfaceContainerHighest = SurfaceContainerHighestNight,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    outline = NightOutline,
    outlineVariant = NightOutlineSoft,
    error = ErrorRedDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    scrim = ScrimColor
)

val AdhkarShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

object AdhkarThemeDefaults {
    val colors: AdhkarExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAdhkarColors.current
}

@Composable
fun AdhkarTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    nightModeEnabled: Boolean = false,
    themeStyle: AppThemeStyle = AppThemeStyle.ANDALUSIAN_EMERALD,
    appLanguage: AppLanguage = AppLanguage.ARABIC,
    content: @Composable () -> Unit
) {
    val isDark = nightModeEnabled || when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        nightModeEnabled -> HighContrastNightColorScheme
        isDark -> when (themeStyle) {
            AppThemeStyle.ANDALUSIAN_EMERALD -> DarkColorScheme
            AppThemeStyle.MECCAN_GOLD -> MeccanGoldDarkColorScheme
            AppThemeStyle.MADINAH_TEAL -> MadinahTealDarkColorScheme
            AppThemeStyle.FAJR_SAPPHIRE -> FajrSapphireDarkColorScheme
            AppThemeStyle.DAMASCUS_ROSE -> DamascusRoseDarkColorScheme
        }
        else -> when (themeStyle) {
            AppThemeStyle.ANDALUSIAN_EMERALD -> LightColorScheme
            AppThemeStyle.MECCAN_GOLD -> MeccanGoldLightColorScheme
            AppThemeStyle.MADINAH_TEAL -> MadinahTealLightColorScheme
            AppThemeStyle.FAJR_SAPPHIRE -> FajrSapphireLightColorScheme
            AppThemeStyle.DAMASCUS_ROSE -> DamascusRoseLightColorScheme
        }
    }
    val extendedColors = if (isDark) {
        AdhkarExtendedColors(
            completed = CompletedGreenDark,
            goldHalo = SoftGoldHaloDark,
            isDark = true
        )
    } else {
        AdhkarExtendedColors(
            completed = CompletedGreenLight,
            goldHalo = SoftGoldHaloLight,
            isDark = false
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    val baseContext = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current
    val backPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current

    val localizedContext = remember(baseContext, baseConfig, appLanguage) {
        baseContext.withAppLanguage(appLanguage)
    }

    val providedLocals = buildList<ProvidedValue<*>> {
        add(LocalContext provides localizedContext)
        add(LocalConfiguration provides localizedContext.resources.configuration)
        add(LocalLayoutDirection provides appLanguage.layoutDirection)
        add(LocalAdhkarColors provides extendedColors)
        add(LocalAppLanguage provides appLanguage)
        if (activityResultRegistryOwner != null) {
            add(LocalActivityResultRegistryOwner provides activityResultRegistryOwner)
        }
        if (backPressedDispatcherOwner != null) {
            add(LocalOnBackPressedDispatcherOwner provides backPressedDispatcherOwner)
        }
    }

    CompositionLocalProvider(*providedLocals.toTypedArray()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AdhkarTypography,
            shapes = AdhkarShapes,
            content = content
        )
    }
}
