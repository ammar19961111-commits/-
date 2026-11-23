package com.example.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

enum class DhikrCategory(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
    @DrawableRes val bannerRes: Int
) {
    MORNING(
        id = "morning",
        titleRes = R.string.cat_morning,
        subtitleRes = R.string.cat_morning_sub,
        icon = Icons.Rounded.WbSunny,
        bannerRes = R.drawable.img_hero_morning
    ),
    EVENING(
        id = "evening",
        titleRes = R.string.cat_evening,
        subtitleRes = R.string.cat_evening_sub,
        icon = Icons.Rounded.NightsStay,
        bannerRes = R.drawable.img_hero_evening
    ),
    AFTER_PRAYER(
        id = "after_prayer",
        titleRes = R.string.cat_after_prayer,
        subtitleRes = R.string.cat_after_prayer_sub,
        icon = Icons.Rounded.Mosque,
        bannerRes = R.drawable.img_hero_morning
    ),
    SLEEP(
        id = "sleep",
        titleRes = R.string.cat_sleep,
        subtitleRes = R.string.cat_sleep_sub,
        icon = Icons.Rounded.Bedtime,
        bannerRes = R.drawable.img_hero_night
    ),
    WAKING(
        id = "waking",
        titleRes = R.string.cat_waking,
        subtitleRes = R.string.cat_waking_sub,
        icon = Icons.Rounded.AutoAwesome,
        bannerRes = R.drawable.img_hero_morning
    ),
    DUAS(
        id = "duas",
        titleRes = R.string.cat_duas,
        subtitleRes = R.string.cat_duas_sub,
        icon = Icons.Rounded.MenuBook,
        bannerRes = R.drawable.img_hero_evening
    );

    companion object {
        fun fromId(id: String): DhikrCategory =
            entries.find { it.id == id } ?: MORNING
    }
}
