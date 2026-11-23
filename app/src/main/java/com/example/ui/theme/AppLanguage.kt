package com.example.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

enum class AppLanguage(val code: String) {
    ARABIC("ar"),
    ENGLISH("en");

    val layoutDirection: LayoutDirection
        get() = if (this == ENGLISH) LayoutDirection.Ltr else LayoutDirection.Rtl

    val locale: Locale
        get() = if (this == ARABIC) {
            Locale.forLanguageTag("ar-u-nu-latn")
        } else {
            Locale.forLanguageTag(code)
        }

    companion object {
        fun fromCode(value: String?): AppLanguage =
            entries.find {
                it.name.equals(value, ignoreCase = true) ||
                    it.code.equals(value, ignoreCase = true)
            } ?: ARABIC
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ARABIC }

class LocalizedContextWrapper(
    base: Context,
    private val localizedResources: Resources,
    private val localizedAssets: AssetManager
) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedResources
    override fun getAssets(): AssetManager = localizedAssets
}

fun Context.withAppLanguage(appLanguage: AppLanguage): Context {
    var unwrapped = this
    while (unwrapped is LocalizedContextWrapper) {
        unwrapped = unwrapped.baseContext
    }
    val config = Configuration(unwrapped.resources.configuration)
    val locale = appLanguage.locale
    config.setLocale(locale)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.setLocales(LocaleList(locale))
    }
    config.setLayoutDirection(locale)
    val configContext = unwrapped.createConfigurationContext(config)
    return LocalizedContextWrapper(
        base = unwrapped,
        localizedResources = configContext.resources,
        localizedAssets = configContext.assets
    )
}
