package com.example.data.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.R

enum class AppThemeStyle(
    val isExclusive: Boolean,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val previewPrimary: Color,
    val previewSecondary: Color,
    val previewSurface: Color
) {
    ANDALUSIAN_EMERALD(
        isExclusive = false,
        titleRes = R.string.theme_style_emerald,
        subtitleRes = R.string.theme_style_emerald_sub,
        previewPrimary = Color(0xFF165B4B),
        previewSecondary = Color(0xFFB58932),
        previewSurface = Color(0xFFFAF6EE)
    ),
    MECCAN_GOLD(
        isExclusive = true,
        titleRes = R.string.theme_style_meccan_gold,
        subtitleRes = R.string.theme_style_meccan_gold_sub,
        previewPrimary = Color(0xFF8C6214),
        previewSecondary = Color(0xFFD4A64A),
        previewSurface = Color(0xFFFDF8EC)
    ),
    MADINAH_TEAL(
        isExclusive = true,
        titleRes = R.string.theme_style_madinah_teal,
        subtitleRes = R.string.theme_style_madinah_teal_sub,
        previewPrimary = Color(0xFF0E6E74),
        previewSecondary = Color(0xFF53A39E),
        previewSurface = Color(0xFFF3FAFA)
    ),
    FAJR_SAPPHIRE(
        isExclusive = true,
        titleRes = R.string.theme_style_fajr_sapphire,
        subtitleRes = R.string.theme_style_fajr_sapphire_sub,
        previewPrimary = Color(0xFF284785),
        previewSecondary = Color(0xFFC59B43),
        previewSurface = Color(0xFFF4F7FC)
    ),
    DAMASCUS_ROSE(
        isExclusive = true,
        titleRes = R.string.theme_style_damascus_rose,
        subtitleRes = R.string.theme_style_damascus_rose_sub,
        previewPrimary = Color(0xFF8B3B56),
        previewSecondary = Color(0xFFC49257),
        previewSurface = Color(0xFFFDF5F7)
    );

    companion object {
        fun fromName(value: String?): AppThemeStyle {
            return entries.firstOrNull { it.name == value } ?: ANDALUSIAN_EMERALD
        }
    }
}

enum class RosaryBeadDesign(
    val isExclusive: Boolean,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val primaryBeadColor: Color,
    val dividerBeadColor: Color,
    val trackBeadColor: Color,
    val sheenColor: Color
) {
    CLASSIC_RING(
        isExclusive = false,
        titleRes = R.string.bead_design_classic,
        subtitleRes = R.string.bead_design_classic_sub,
        primaryBeadColor = Color(0xFF165B4B),
        dividerBeadColor = Color(0xFFB58932),
        trackBeadColor = Color(0xFFE5DEC9),
        sheenColor = Color(0xFFFFF8E6)
    ),
    PEARL_BEADS(
        isExclusive = true,
        titleRes = R.string.bead_design_pearl,
        subtitleRes = R.string.bead_design_pearl_sub,
        primaryBeadColor = Color(0xFFF3EFE6),
        dividerBeadColor = Color(0xFFD0A145),
        trackBeadColor = Color(0xFFD8D2C5),
        sheenColor = Color(0xFFFFFFFF)
    ),
    AMBER_TASBEEH(
        isExclusive = true,
        titleRes = R.string.bead_design_amber,
        subtitleRes = R.string.bead_design_amber_sub,
        primaryBeadColor = Color(0xFFD27D1E),
        dividerBeadColor = Color(0xFF8B4C0B),
        trackBeadColor = Color(0xFFEBD2B0),
        sheenColor = Color(0xFFFFE09E)
    ),
    LAPIS_LAZULI(
        isExclusive = true,
        titleRes = R.string.bead_design_lapis,
        subtitleRes = R.string.bead_design_lapis_sub,
        primaryBeadColor = Color(0xFF214A8C),
        dividerBeadColor = Color(0xFFE0B44C),
        trackBeadColor = Color(0xFFC3D0E6),
        sheenColor = Color(0xFF8AB4F8)
    ),
    OLIVE_WOOD(
        isExclusive = true,
        titleRes = R.string.bead_design_olive_wood,
        subtitleRes = R.string.bead_design_olive_wood_sub,
        primaryBeadColor = Color(0xFF7B5333),
        dividerBeadColor = Color(0xFFB68346),
        trackBeadColor = Color(0xFFD9C7B4),
        sheenColor = Color(0xFFC99E74)
    );

    companion object {
        fun fromName(value: String?): RosaryBeadDesign {
            return entries.firstOrNull { it.name == value } ?: CLASSIC_RING
        }
    }
}

sealed interface RewardedUnlockTarget {
    data class ThemeStyle(val style: AppThemeStyle) : RewardedUnlockTarget
    data class BeadDesign(val design: RosaryBeadDesign) : RewardedUnlockTarget
}
