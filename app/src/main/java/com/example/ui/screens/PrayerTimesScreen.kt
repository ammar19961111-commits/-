package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PrayerCalculationMethod
import com.example.data.model.PrayerCityCatalog
import com.example.data.model.PrayerCityPreset
import com.example.data.model.PrayerName
import com.example.data.model.PrayerTimeEntry
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.viewmodel.PrayerTimesUiState

@Composable
fun PrayerTimesScreen(
    prayerState: PrayerTimesUiState,
    onBack: () -> Unit,
    onEnsureLoaded: () -> Unit,
    onDetectLocationAndRefresh: () -> Unit,
    onLocationPermissionDenied: () -> Unit,
    onSelectCity: (PrayerCityPreset) -> Unit,
    onSelectMethod: (PrayerCalculationMethod) -> Unit,
    onRetry: () -> Unit,
    onNavigateToQibla: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    LaunchedEffect(Unit) {
        onEnsureLoaded()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onDetectLocationAndRefresh()
        } else {
            onLocationPermissionDenied()
        }
    }

    val requestLocationFlow = {
        if (prayerState.hasLocationPermission) {
            onDetectLocationAndRefresh()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("prayer_lazy_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top App Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("prayer_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back_cd)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.prayer_dashboard_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.prayer_dashboard_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateToQibla,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("prayer_open_qibla_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Explore,
                                contentDescription = stringResource(R.string.qibla_open_cd),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = onRetry,
                            enabled = !prayerState.isLoading,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("prayer_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.prayer_refresh_cd),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Next Prayer Hero Summary Card
            val schedule = prayerState.schedule
            if (schedule != null) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prayer_next_hero_card"),
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 4.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF0E4B3E),
                                            Color(0xFF156151),
                                            Color(0xFF1D7461)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0x2EFFFFFF)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.LocationOn,
                                                contentDescription = null,
                                                tint = Color(0xFFF6E7C4),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = PrayerCityCatalog.localizeLabel(schedule.locationLabel, language),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color.White,
                                                modifier = Modifier.testTag("prayer_location_label")
                                            )
                                        }
                                    }

                                    FilledTonalButton(
                                        onClick = requestLocationFlow,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("prayer_request_location_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MyLocation,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text(
                                            text = stringResource(R.string.prayer_location_detect),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }

                                val nextPrayer = schedule.nextPrayer
                                if (nextPrayer != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = stringResource(R.string.prayer_next_label),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFFD8EDE6)
                                            )
                                            Text(
                                                text = nextPrayer.prayer.displayName(language),
                                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                                modifier = Modifier.testTag("prayer_next_name")
                                            )
                                            val hijriDisplay = schedule.localizedHijriDate(language)
                                            if (hijriDisplay.isNotBlank()) {
                                                Text(
                                                    text = hijriDisplay,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFF6E7C4),
                                                    modifier = Modifier.testTag("prayer_hijri_date")
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = nextPrayer.localizedTime(language),
                                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFF6E7C4),
                                                modifier = Modifier.testTag("prayer_next_time")
                                            )
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0x33F6E7C4)
                                            ) {
                                                Text(
                                                    text = stringResource(
                                                        R.string.prayer_countdown_prefix,
                                                        schedule.localizedCountdown(language)
                                                    ),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White,
                                                    modifier = Modifier
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                        .testTag("prayer_next_countdown")
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.prayer_sunrise_note,
                                            schedule.localizedSunrise(language)
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFD8EDE6)
                                    )
                                    if (schedule.dateReadable.isNotBlank()) {
                                        Text(
                                            text = schedule.dateReadable,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFD8EDE6)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (!prayerState.isLoading) {
                // Location Permission / Detect Prompt Card when no location schedule is loaded yet
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prayer_location_prompt_card"),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val isLocationUnavailable =
                                prayerState.hasLocationPermission && prayerState.locationUnavailable
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MyLocation,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Text(
                                    text = stringResource(
                                        if (isLocationUnavailable) {
                                            R.string.prayer_location_unavailable_title
                                        } else {
                                            R.string.prayer_location_permission_title
                                        }
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = stringResource(
                                    if (isLocationUnavailable) {
                                        R.string.prayer_location_unavailable_body
                                    } else {
                                        R.string.prayer_location_permission_body
                                    }
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = requestLocationFlow,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("prayer_grant_location_cta")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    stringResource(
                                        if (isLocationUnavailable) {
                                            R.string.prayer_location_retry_cta
                                        } else {
                                            R.string.prayer_location_grant_cta
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Loading / Error feedback
            if (prayerState.isLoading) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prayer_loading"),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
                            Text(
                                text = stringResource(R.string.prayer_loading_text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (prayerState.errorMessage != null) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prayer_error_card"),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.prayer_error_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = stringResource(R.string.prayer_error_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            FilledTonalButton(
                                onClick = onRetry,
                                modifier = Modifier.testTag("prayer_retry_button")
                            ) {
                                Text(stringResource(R.string.prayer_retry_cta))
                            }
                        }
                    }
                }
            }

            // Five Daily Prayer Times List
            if (schedule != null) {
                items(schedule.entries, key = { it.prayer.apiKey }) { entry ->
                    PrayerTimeRowCard(entry = entry)
                }
            }

            // Quick Cities Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.prayer_location_cities_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrayerCityCatalog.cities.forEach { city ->
                            val isSelected = schedule?.locationLabel == city.arabicName
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectCity(city) },
                                label = {
                                    Text(
                                        text = city.displayName(language),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("prayer_city_${city.id}")
                            )
                        }
                    }
                }
            }

            // Calculation Method Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.prayer_method_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("prayer_method_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrayerCalculationMethod.entries.forEach { method ->
                            val selected = prayerState.selectedMethod == method
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectMethod(method) },
                                label = {
                                    Text(
                                        text = method.displayLabel(language),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("prayer_method_${method.apiMethodId}")
                            )
                        }
                    }
                }
            }

            if (schedule != null) {
                item {
                    val tzOrCoords = schedule.timezone.ifBlank { schedule.formattedCoordinates }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prayer_api_attribution")
                    ) {
                        Text(
                            text = stringResource(
                                R.string.prayer_api_source_note,
                                schedule.method.displayLabel(language),
                                tzOrCoords
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerTimeRowCard(
    entry: PrayerTimeEntry,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val icon: ImageVector = when (entry.prayer) {
        PrayerName.FAJR -> Icons.Rounded.WbTwilight
        PrayerName.DHUHR -> Icons.Rounded.WbSunny
        PrayerName.ASR -> Icons.Rounded.Schedule
        PrayerName.MAGHRIB -> Icons.Rounded.NightsStay
        PrayerName.ISHA -> Icons.Rounded.Bedtime
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("prayer_row_${entry.prayer.apiKey}"),
        shape = RoundedCornerShape(18.dp),
        color = if (entry.isNext) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (entry.isNext) 3.dp else 1.dp,
        border = BorderStroke(
            width = if (entry.isNext) 1.5.dp else 1.dp,
            color = if (entry.isNext) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (entry.isNext) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (entry.isNext) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = entry.prayer.displayName(language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (entry.isNext) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (entry.isNext) {
                        Text(
                            text = stringResource(R.string.prayer_next_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                text = entry.localizedTime(language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (entry.isNext) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.testTag("prayer_time_${entry.prayer.apiKey}")
            )
        }
    }
}
