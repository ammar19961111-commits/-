package com.example.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

object AdhkarRoutes {
    const val HOME = "home"
    const val TASBIH = "tasbih"
    const val STATS = "stats"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val PRAYER_TIMES = "prayer_times"
    const val QIBLA = "qibla"
    const val CHAT = "chat"
    const val DHIKR_LIST = "dhikr_list/{categoryId}"

    fun dhikrListRoute(categoryId: String): String = "dhikr_list/$categoryId"
}

enum class BottomNavTab(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME(
        route = AdhkarRoutes.HOME,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_tab_home"
    ),
    TASBIH(
        route = AdhkarRoutes.TASBIH,
        labelRes = R.string.nav_tasbih,
        selectedIcon = Icons.Rounded.RadioButtonChecked,
        unselectedIcon = Icons.Outlined.RadioButtonChecked,
        testTag = "nav_tab_tasbih"
    ),
    STATS(
        route = AdhkarRoutes.STATS,
        labelRes = R.string.nav_stats,
        selectedIcon = Icons.Rounded.Insights,
        unselectedIcon = Icons.Outlined.Insights,
        testTag = "nav_tab_stats"
    ),
    FAVORITES(
        route = AdhkarRoutes.FAVORITES,
        labelRes = R.string.nav_favorites,
        selectedIcon = Icons.Rounded.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        testTag = "nav_tab_favorites"
    ),
    SETTINGS(
        route = AdhkarRoutes.SETTINGS,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
}
