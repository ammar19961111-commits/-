package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AdhkarBackupData
import com.example.data.model.AppThemeStyle
import com.example.data.model.DhikrCategory
import com.example.data.model.DhikrReminder
import com.example.data.model.RosaryBeadDesign
import com.example.notifications.DhikrNotificationHelper
import com.example.ui.components.ReminderTimePickerDialog
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.AdhkarUiState
import com.example.util.BackupJsonManager
import com.example.util.BackupParseResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: AdhkarUiState,
    onFontSizeChange: (Float) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit = {},
    onThemeModeChange: (ThemeMode) -> Unit,
    onSelectThemeStyle: (AppThemeStyle) -> Unit = {},
    onSelectBeadDesign: (RosaryBeadDesign) -> Unit = {},
    onNightModeToggle: (Boolean) -> Unit = {},
    onDailyTotalGoalChange: (Int) -> Unit = {},
    onDailyTasbihTargetChange: (Int) -> Unit,
    onHapticToggle: (Boolean) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onAutoAdvanceToggle: (Boolean) -> Unit,
    onUpdateReminder: (DhikrCategory, Boolean, Int, Int) -> Unit,
    onResetAllToday: () -> Unit,
    onCreateBackupJson: suspend () -> String = { "" },
    onRestoreBackup: (AdhkarBackupData, (Boolean) -> Unit) -> Unit = { _, cb -> cb(false) },
    isRewardedAdLoading: Boolean = false,
    onWatchRewardedAd: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = uiState.preferences
    var showResetConfirm by remember { mutableStateOf(false) }
    var showDailyGoalDialog by remember { mutableStateOf(false) }
    var pendingRestoreBackup by remember { mutableStateOf<AdhkarBackupData?>(null) }
    var editingReminder by remember { mutableStateOf<DhikrReminder?>(null) }
    var pendingEnableReminders by remember { mutableStateOf<List<DhikrReminder>>(emptyList()) }
    var pendingTestNotification by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember {
        mutableStateOf(DhikrNotificationHelper.hasNotificationPermission(context))
    }

    val backupSavedMsg = stringResource(R.string.backup_saved_toast)
    val backupSaveErrMsg = stringResource(R.string.backup_save_error_toast)
    val backupShareSubject = stringResource(R.string.backup_share_subject)
    val restoreInvalidMsg = stringResource(R.string.backup_restore_invalid_toast)
    val restoreUnsupportedMsg = stringResource(R.string.backup_restore_unsupported_toast)
    val restoreSuccessMsg = stringResource(R.string.backup_restore_success_toast)
    val restoreErrorMsg = stringResource(R.string.backup_restore_error_toast)

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = onCreateBackupJson()
                val ok = BackupJsonManager.writeToUri(context, uri, json)
                onShowMessage(if (ok) backupSavedMsg else backupSaveErrMsg)
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val raw = BackupJsonManager.readFromUri(context, uri)
            when (val result = BackupJsonManager.parse(raw.orEmpty())) {
                is BackupParseResult.Success -> pendingRestoreBackup = result.data
                is BackupParseResult.UnsupportedVersion -> onShowMessage(restoreUnsupportedMsg)
                BackupParseResult.InvalidFormat -> onShowMessage(restoreInvalidMsg)
            }
        }
    }

    val notifyReminderScheduled: (DhikrReminder) -> Unit = { target ->
        val formattedTime = if (prefs.appLanguage == AppLanguage.ENGLISH) {
            target.formattedTime12En
        } else {
            target.formattedTime12Ar
        }
        onShowMessage(
            context.getString(
                R.string.reminder_scheduled_toast,
                context.getString(target.category.titleRes),
                formattedTime
            )
        )
    }

    val notifyMorningAndEveningScheduled: (DhikrReminder, DhikrReminder) -> Unit = { morning, evening ->
        val formatTime: (DhikrReminder) -> String = { item ->
            if (prefs.appLanguage == AppLanguage.ENGLISH) item.formattedTime12En else item.formattedTime12Ar
        }
        onShowMessage(
            context.getString(
                R.string.reminder_morning_evening_scheduled_toast,
                formatTime(morning),
                formatTime(evening)
            )
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted || DhikrNotificationHelper.hasNotificationPermission(context)
        val pendingList = pendingEnableReminders
        val shouldTest = pendingTestNotification
        pendingEnableReminders = emptyList()
        pendingTestNotification = false
        if (granted) {
            pendingList.forEach { target ->
                onUpdateReminder(target.category, true, target.hour, target.minute)
            }
            if (pendingList.size == 2 &&
                pendingList.any { it.category == DhikrCategory.MORNING } &&
                pendingList.any { it.category == DhikrCategory.EVENING }
            ) {
                val m = pendingList.first { it.category == DhikrCategory.MORNING }
                val e = pendingList.first { it.category == DhikrCategory.EVENING }
                notifyMorningAndEveningScheduled(m, e)
            } else {
                pendingList.lastOrNull()?.let(notifyReminderScheduled)
            }
            if (shouldTest) {
                DhikrNotificationHelper.showCategoryReminder(
                    context = context,
                    category = DhikrCategory.MORNING,
                    appLanguage = prefs.appLanguage
                )
            }
        }
    }

    val enableWithPermissionCheck: (DhikrReminder) -> Unit = { target ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            pendingEnableReminders = listOf(target)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onUpdateReminder(target.category, true, target.hour, target.minute)
            notifyReminderScheduled(target)
        }
    }

    val enableMorningAndEvening: () -> Unit = {
        val morning = prefs.reminders.find { it.category == DhikrCategory.MORNING }
            ?: DhikrReminder(DhikrCategory.MORNING, false, 6, 0)
        val evening = prefs.reminders.find { it.category == DhikrCategory.EVENING }
            ?: DhikrReminder(DhikrCategory.EVENING, false, 17, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            pendingEnableReminders = listOf(morning, evening)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onUpdateReminder(morning.category, true, morning.hour, morning.minute)
            onUpdateReminder(evening.category, true, evening.hour, evening.minute)
            notifyMorningAndEveningScheduled(morning, evening)
        }
    }

    val sendTestNotification: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            pendingTestNotification = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            DhikrNotificationHelper.showCategoryReminder(
                context = context,
                category = DhikrCategory.MORNING,
                appLanguage = prefs.appLanguage
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // App Language Switcher (Arabic / English with Adhkar translations)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_language_card"),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.settings_language_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.settings_language_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val languageOptions = listOf(
                            AppLanguage.ARABIC to R.string.language_arabic,
                            AppLanguage.ENGLISH to R.string.language_english
                        )
                        languageOptions.forEach { (lang, labelRes) ->
                            val tag = if (lang == AppLanguage.ARABIC) {
                                "language_chip_arabic"
                            } else {
                                "language_chip_english"
                            }
                            FilterChip(
                                selected = prefs.appLanguage == lang,
                                onClick = { onLanguageChange(lang) },
                                label = {
                                    Text(
                                        text = stringResource(labelRes),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag(tag)
                            )
                        }
                    }
                }
            }

            // Font Size Preview & Slider Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FormatSize,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.settings_font_size),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "${prefs.fontSizeSp.roundToInt()} sp",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("settings_font_size_value")
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_font_preview),
                            fontFamily = AmiriFontFamily,
                            fontSize = prefs.fontSizeSp.sp,
                            lineHeight = (prefs.fontSizeSp * 1.7f).sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(16.dp)
                                .testTag("settings_font_preview_text")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onFontSizeChange((prefs.fontSizeSp - 2f).coerceIn(16f, 36f)) },
                            enabled = prefs.fontSizeSp > 16f,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("settings_font_decrease")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TextDecrease,
                                contentDescription = stringResource(R.string.font_size_decrease),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = prefs.fontSizeSp,
                            onValueChange = onFontSizeChange,
                            valueRange = 16f..36f,
                            steps = 9,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_font_slider")
                        )
                        IconButton(
                            onClick = { onFontSizeChange((prefs.fontSizeSp + 2f).coerceIn(16f, 36f)) },
                            enabled = prefs.fontSizeSp < 36f,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("settings_font_increase")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TextIncrease,
                                contentDescription = stringResource(R.string.font_size_increase),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            18f to R.string.font_size_small,
                            22f to R.string.font_size_medium,
                            26f to R.string.font_size_large,
                            32f to R.string.font_size_xlarge
                        )
                        presets.forEach { (sizeSp, labelRes) ->
                            val selected = prefs.fontSizeSp.roundToInt() == sizeSp.roundToInt()
                            FilterChip(
                                selected = selected,
                                onClick = { onFontSizeChange(sizeSp) },
                                label = {
                                    Text(
                                        text = stringResource(labelRes),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_font_chip_${sizeSp.roundToInt()}")
                            )
                        }
                    }
                }
            }

            // Appearance Theme Selector
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.settings_appearance),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(
                            ThemeMode.SYSTEM to R.string.theme_system,
                            ThemeMode.LIGHT to R.string.theme_light,
                            ThemeMode.DARK to R.string.theme_dark
                        )
                        options.forEach { (mode, labelRes) ->
                            FilterChip(
                                selected = !prefs.nightModeEnabled && prefs.themeMode == mode,
                                enabled = !prefs.nightModeEnabled,
                                onClick = { onThemeModeChange(mode) },
                                label = {
                                    Text(
                                        text = stringResource(labelRes),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_chip_${mode.name.lowercase()}")
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )

                    // Dedicated High-Contrast Night Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NightsStay,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_night_mode),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = stringResource(R.string.settings_night_mode_sub),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = prefs.nightModeEnabled,
                            onCheckedChange = onNightModeToggle,
                            modifier = Modifier.testTag("settings_night_mode_switch")
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )

                    // Exclusive App Themes (Rewarded Ad Interstitial Unlocks)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("settings_exclusive_themes_section")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_exclusive_themes_title),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = stringResource(R.string.settings_exclusive_themes_sub),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppThemeStyle.entries.forEach { style ->
                                val isSelected = prefs.appThemeStyle == style
                                val isUnlocked = prefs.isThemeUnlocked(style)
                                Surface(
                                    onClick = { onSelectThemeStyle(style) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    modifier = Modifier
                                        .widthIn(min = 136.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("theme_style_${style.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Canvas(modifier = Modifier.size(28.dp)) {
                                            drawCircle(color = style.previewSurface)
                                            drawCircle(
                                                color = style.previewPrimary,
                                                radius = size.minDimension * 0.36f,
                                                center = Offset(size.width * 0.42f, size.height * 0.44f)
                                            )
                                            drawCircle(
                                                color = style.previewSecondary,
                                                radius = size.minDimension * 0.2f,
                                                center = Offset(size.width * 0.72f, size.height * 0.72f)
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = stringResource(style.titleRes),
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isUnlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = if (isUnlocked) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.tertiary
                                                    }
                                                )
                                                Text(
                                                    text = if (isUnlocked) {
                                                        stringResource(R.string.reward_unlock_badge_unlocked)
                                                    } else {
                                                        stringResource(R.string.reward_unlock_badge_locked)
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isUnlocked) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.tertiary
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )

                    // Quick Custom Rosary Bead Design Picker inside Settings as well
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("settings_bead_designs_section")
                    ) {
                        Text(
                            text = stringResource(R.string.tasbih_bead_design_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RosaryBeadDesign.entries.forEach { design ->
                                val isSelected = prefs.rosaryBeadDesign == design
                                val isUnlocked = prefs.isBeadDesignUnlocked(design)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectBeadDesign(design) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isUnlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(design.titleRes),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("settings_bead_chip_${design.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Overall Daily Goal Selector (Total Adhkar + Tasbeeh counts)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_overall_daily_goal_card"),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TrackChanges,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_daily_goal_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(R.string.settings_daily_goal_sub),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "${prefs.dailyTotalGoal}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("settings_overall_goal_value")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(50, 150, 300, 500).forEach { goal ->
                            FilterChip(
                                selected = prefs.dailyTotalGoal == goal,
                                onClick = { onDailyTotalGoalChange(goal) },
                                label = {
                                    Text(
                                        text = "$goal",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("settings_overall_goal_chip_$goal")
                            )
                        }
                        FilterChip(
                            selected = prefs.dailyTotalGoal !in listOf(50, 150, 300, 500),
                            onClick = { showDailyGoalDialog = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.settings_daily_goal_custom),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("settings_overall_goal_custom_button")
                        )
                    }
                }
            }

            // Daily Tasbih Goal Selector
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.tasbih_daily_goal_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "${prefs.dailyTasbihTarget}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("settings_daily_goal_value")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(100, 300, 500, 1000).forEach { goal ->
                            FilterChip(
                                selected = prefs.dailyTasbihTarget == goal,
                                onClick = { onDailyTasbihTargetChange(goal) },
                                label = {
                                    Text(
                                        text = "$goal",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_daily_goal_chip_$goal")
                            )
                        }
                    }
                }
            }

            // Interactive Toggles
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Vibration,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_haptic),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = stringResource(R.string.settings_haptic_sub),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = prefs.hapticEnabled,
                            onCheckedChange = onHapticToggle,
                            modifier = Modifier.testTag("settings_haptic_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_sound),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = stringResource(R.string.settings_sound_sub),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = prefs.soundEnabled,
                            onCheckedChange = onSoundToggle,
                            modifier = Modifier.testTag("settings_sound_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_auto_advance),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = stringResource(R.string.settings_auto_advance_sub),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = prefs.autoAdvance,
                            onCheckedChange = onAutoAdvanceToggle,
                            modifier = Modifier.testTag("settings_auto_advance_switch")
                        )
                    }
                }
            }

            // Daily Dhikr Reminders Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.settings_reminders_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.settings_reminders_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.reminder_permission_needed),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    modifier = Modifier.testTag("grant_notification_permission")
                                ) {
                                    Text(stringResource(R.string.reminder_grant_permission))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val morningOn = prefs.reminders.any { it.category == DhikrCategory.MORNING && it.enabled }
                        val eveningOn = prefs.reminders.any { it.category == DhikrCategory.EVENING && it.enabled }
                        FilterChip(
                            selected = morningOn && eveningOn,
                            onClick = enableMorningAndEvening,
                            label = {
                                Text(
                                    text = stringResource(R.string.reminder_enable_morning_evening),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reminder_enable_morning_evening_button")
                        )
                        FilterChip(
                            selected = false,
                            onClick = sendTestNotification,
                            label = {
                                Text(
                                    text = stringResource(R.string.reminder_test_notification),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reminder_test_notification_button")
                        )
                    }

                    val activeDailyReminders = prefs.reminders.filter { it.enabled }
                    if (activeDailyReminders.isNotEmpty()) {
                        val summaryParts = activeDailyReminders.joinToString(" • ") { item ->
                            val label = context.getString(item.category.titleRes)
                            val time = if (prefs.appLanguage == AppLanguage.ENGLISH) {
                                item.formattedTime12En
                            } else {
                                item.formattedTime12Ar
                            }
                            "$label $time"
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reminder_active_schedule_status")
                        ) {
                            Text(
                                text = stringResource(R.string.reminder_active_schedule_summary, summaryParts),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    prefs.reminders.forEachIndexed { index, reminder ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = reminder.category.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(reminder.category.titleRes),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Surface(
                                        onClick = { editingReminder = reminder },
                                        shape = CircleShape,
                                        color = if (reminder.enabled) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                        },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("reminder_time_${reminder.category.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Schedule,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (reminder.enabled) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                            Text(
                                                text = if (prefs.appLanguage == AppLanguage.ENGLISH) {
                                                    reminder.formattedTime12En
                                                } else {
                                                    reminder.formattedTime12Ar
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (reminder.enabled) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Switch(
                                checked = reminder.enabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        enableWithPermissionCheck(reminder)
                                    } else {
                                        onUpdateReminder(
                                            reminder.category,
                                            false,
                                            reminder.hour,
                                            reminder.minute
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("reminder_switch_${reminder.category.id}")
                            )
                        }
                    }
                }
            }

            // Ads & App Support Card (Rewarded Ad for 24h Ad-Free Focus)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_ad_support_card"),
                shape = RoundedCornerShape(22.dp),
                color = if (uiState.isAdFreeActive) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (uiState.isAdFreeActive) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isAdFreeActive) {
                                Icons.Rounded.Verified
                            } else {
                                Icons.Rounded.VolunteerActivism
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.settings_ads_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.settings_ads_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val totalRemainingMins = uiState.adFreeRemainingMinutes.coerceAtLeast(0)
                    val remainingHours = totalRemainingMins / 60
                    val remainingMinutesPart = totalRemainingMins % 60
                    val remainingLabel = when {
                        remainingHours > 0 && remainingMinutesPart > 0 -> stringResource(
                            R.string.ads_remaining_hours_minutes,
                            remainingHours,
                            remainingMinutesPart
                        )
                        remainingHours > 0 -> stringResource(R.string.ads_remaining_hours, remainingHours)
                        else -> stringResource(
                            R.string.ads_remaining_minutes,
                            totalRemainingMins.coerceAtLeast(1)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (uiState.isAdFreeActive) {
                                stringResource(R.string.ads_status_ad_free_active, remainingLabel)
                            } else {
                                stringResource(R.string.ads_status_active)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("settings_ad_status_text")
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.testTag("settings_ad_breakdown")
                    ) {
                        listOf(
                            Icons.Rounded.ViewStream to R.string.ads_feature_banner,
                            Icons.Rounded.Timer to R.string.ads_feature_interstitial,
                            Icons.Rounded.CardGiftcard to R.string.ads_feature_rewarded
                        ).forEach { (icon, textRes) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(textRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onWatchRewardedAd,
                        enabled = !isRewardedAdLoading,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("settings_watch_rewarded_ad_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OndemandVideo,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = when {
                                isRewardedAdLoading -> stringResource(R.string.ads_watch_rewarded_loading)
                                uiState.isAdFreeActive -> stringResource(R.string.ads_watch_rewarded_extend_cta)
                                else -> stringResource(R.string.ads_watch_rewarded_cta)
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // JSON Backup & Restore Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_backup_restore_card"),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.settings_backup_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.settings_backup_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(
                                R.string.settings_backup_summary,
                                uiState.dailyHistory.size,
                                uiState.favoriteAdhkar.size,
                                prefs.totalCompletedAllTime
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("settings_backup_summary_text")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val todayDate = remember {
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        }
                        FilledTonalButton(
                            onClick = {
                                createBackupLauncher.launch("adhkar_backup_$todayDate.json")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("backup_save_json_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.backup_save_json))
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val json = onCreateBackupJson()
                                    val shareIntent = BackupJsonManager.createShareIntent(
                                        context = context,
                                        fileName = "adhkar_backup_$todayDate.json",
                                        jsonContent = json,
                                        subject = backupShareSubject
                                    )
                                    val chooser = Intent.createChooser(shareIntent, backupShareSubject)
                                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(chooser)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("backup_share_json_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.backup_share_json))
                        }
                    }

                    FilterChip(
                        selected = false,
                        onClick = {
                            openBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.backup_restore_json),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("backup_restore_json_button")
                    )
                }
            }

            // Reset Daily Progress
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { showResetConfirm = true }
                    .testTag("settings_reset_today_button"),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.settings_reset_today),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.settings_reset_today_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    editingReminder?.let { target ->
        ReminderTimePickerDialog(
            reminder = target,
            onDismiss = { editingReminder = null },
            onConfirm = { hour, minute ->
                editingReminder = null
                enableWithPermissionCheck(target.copy(hour = hour, minute = minute))
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.settings_reset_today)) },
            text = { Text(stringResource(R.string.reset_category_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        onResetAllToday()
                    },
                    modifier = Modifier.testTag("confirm_reset_all_today")
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDailyGoalDialog) {
        var goalInput by remember(prefs.dailyTotalGoal) {
            mutableStateOf(prefs.dailyTotalGoal.toString())
        }
        val parsed = goalInput.toIntOrNull()
        val isValid = parsed != null && parsed in 10..10000
        AlertDialog(
            onDismissRequest = { showDailyGoalDialog = false },
            title = { Text(stringResource(R.string.settings_daily_goal_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { input -> goalInput = input.filter { it.isDigit() }.take(5) },
                    label = { Text(stringResource(R.string.settings_daily_goal_input_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = goalInput.isNotEmpty() && !isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_overall_goal_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (parsed != null && isValid) {
                            onDailyTotalGoalChange(parsed)
                            showDailyGoalDialog = false
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier.testTag("confirm_overall_daily_goal")
                ) {
                    Text(stringResource(R.string.tasbih_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDailyGoalDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    pendingRestoreBackup?.let { backup ->
        val historyCount = backup.dailyHistory.size
        val favCount = backup.dhikrProgress.count { it.isFavorite }
        val countersCount = backup.counters.size
        AlertDialog(
            onDismissRequest = { pendingRestoreBackup = null },
            title = { Text(stringResource(R.string.backup_restore_dialog_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.backup_restore_dialog_body,
                        historyCount,
                        favCount,
                        countersCount
                    ),
                    modifier = Modifier.testTag("restore_confirm_dialog_body")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRestoreBackup = null
                        onRestoreBackup(backup) { success ->
                            onShowMessage(if (success) restoreSuccessMsg else restoreErrorMsg)
                        }
                    },
                    modifier = Modifier.testTag("confirm_restore_backup")
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreBackup = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            modifier = Modifier.testTag("restore_confirm_dialog")
        )
    }
}
