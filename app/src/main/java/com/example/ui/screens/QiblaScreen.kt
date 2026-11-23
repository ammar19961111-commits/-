package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.SensorsOff
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.PrayerCityCatalog
import com.example.data.model.PrayerCityPreset
import com.example.qibla.CompassReading
import com.example.qibla.QiblaCalculator
import com.example.qibla.QiblaSensorSource
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.LocalAppLanguage
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QiblaScreen(
    latitude: Double?,
    longitude: Double?,
    locationLabel: String,
    hasLocationPermission: Boolean,
    hapticEnabled: Boolean,
    sensorSource: QiblaSensorSource,
    onBack: () -> Unit,
    onDetectLocation: () -> Unit,
    onLocationPermissionDenied: () -> Unit,
    onSelectCity: (PrayerCityPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val hapticFeedback = LocalHapticFeedback.current

    val activeLat = latitude ?: PrayerCityCatalog.cities.first().latitude
    val activeLon = longitude ?: PrayerCityCatalog.cities.first().longitude
    val activeLocationLabel = locationLabel.ifBlank { PrayerCityCatalog.cities.first().arabicName }
    val hasUserCoordinates = latitude != null && longitude != null

    val compassFlow = remember(sensorSource, activeLat, activeLon) {
        sensorSource.observeCompass(activeLat, activeLon)
    }
    val compassReading by compassFlow.collectAsStateWithLifecycle(
        initialValue = CompassReading()
    )

    val qiblaBearing = remember(activeLat, activeLon) {
        QiblaCalculator.calculateQiblaBearingDegrees(activeLat, activeLon)
    }
    val distanceKm = remember(activeLat, activeLon) {
        QiblaCalculator.calculateDistanceToKaabaKm(activeLat, activeLon)
    }
    val relativeTurn = QiblaCalculator.shortestSignedAngleDelta(
        fromDegrees = compassReading.azimuthDegrees,
        toDegrees = qiblaBearing
    )
    val isAligned = compassReading.hasSensors &&
        abs(relativeTurn) <= QiblaCalculator.ALIGNMENT_TOLERANCE_DEGREES

    // Trigger subtle haptic confirmation once upon entering alignment window
    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned && hapticEnabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasAligned = isAligned
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onDetectLocation()
        } else {
            onLocationPermissionDenied()
        }
    }

    val requestLocationFlow = {
        if (hasLocationPermission) {
            onDetectLocation()
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
                .testTag("qibla_lazy_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
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
                                .testTag("qibla_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back_cd)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.qibla_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.qibla_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = requestLocationFlow,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("qibla_detect_location_button")
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
            }

            // Hero Bearing & Distance Summary Card
            item {
                val badgeColor by animateColorAsState(
                    targetValue = if (isAligned) {
                        Color(0xFF1F9D68)
                    } else {
                        Color(0x33F6E7C4)
                    },
                    label = "qibla_badge_color"
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qibla_hero_card"),
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
                                            text = PrayerCityCatalog.localizeLabel(activeLocationLabel, language),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            modifier = Modifier.testTag("qibla_location_label")
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = badgeColor,
                                    modifier = Modifier.testTag("qibla_alignment_badge")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        if (isAligned) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.Explore,
                                                contentDescription = null,
                                                tint = Color(0xFFF6E7C4),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        val guidanceText = when {
                                            !compassReading.hasSensors ->
                                                stringResource(R.string.qibla_static_bearing_badge)
                                            isAligned ->
                                                stringResource(R.string.qibla_aligned_status)
                                            relativeTurn > 0f ->
                                                stringResource(R.string.qibla_turn_right, abs(relativeTurn).roundToInt())
                                            else ->
                                                stringResource(R.string.qibla_turn_left, abs(relativeTurn).roundToInt())
                                        }
                                        Text(
                                            text = guidanceText,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color.White,
                                            modifier = Modifier.testTag("qibla_alignment_text")
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = stringResource(R.string.qibla_bearing_label),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFD8EDE6)
                                    )
                                    Text(
                                        text = "${qiblaBearing.roundToInt()}° ${localizedCardinal(qiblaBearing, language)}",
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFF6E7C4),
                                        modifier = Modifier.testTag("qibla_bearing_text")
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = stringResource(R.string.qibla_distance_label),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFD8EDE6)
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.qibla_distance_km,
                                            distanceKm.roundToInt()
                                        ),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.testTag("qibla_distance_text")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Location permission helper when using default fallback city
            if (!hasUserCoordinates && !hasLocationPermission) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qibla_location_prompt_card"),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.qibla_permission_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.size(10.dp))
                            Button(
                                onClick = requestLocationFlow,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("qibla_grant_location_cta")
                            ) {
                                Text(stringResource(R.string.prayer_location_grant_cta))
                            }
                        }
                    }
                }
            }

            // Interactive Radial Compass Dial
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(
                        width = if (isAligned) 2.dp else 1.dp,
                        color = if (isAligned) Color(0xFF1F9D68) else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        QiblaCompassDial(
                            azimuthDegrees = compassReading.azimuthDegrees,
                            qiblaBearingDegrees = qiblaBearing,
                            isAligned = isAligned,
                            hasSensors = compassReading.hasSensors,
                            language = language
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.qibla_heading_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (compassReading.hasSensors) {
                                        "${compassReading.azimuthDegrees.roundToInt()}°"
                                    } else {
                                        "--"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("qibla_azimuth_text")
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.qibla_kaaba_angle_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${qiblaBearing.roundToInt()}°",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }

            // Missing Sensors or Calibration Status Banners
            if (!compassReading.hasSensors) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qibla_no_sensor_banner"),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SensorsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(R.string.qibla_no_sensors_message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            } else if (compassReading.isCalibrating) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qibla_calibration_hint"),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = stringResource(R.string.qibla_calibration_message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            // Quick City Selector
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
                            val isSelected = activeLocationLabel == city.arabicName
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
                                modifier = Modifier.testTag("qibla_city_${city.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QiblaCompassDial(
    azimuthDegrees: Float,
    qiblaBearingDegrees: Float,
    isAligned: Boolean,
    hasSensors: Boolean,
    language: AppLanguage
) {
    // Track continuous unwrapped dial rotation so crossing 0/360 animates shortest path
    var continuousTargetDialAngle by remember { mutableFloatStateOf(-azimuthDegrees) }
    val desiredDialAngle = if (hasSensors) -azimuthDegrees else 0f
    LaunchedEffect(desiredDialAngle) {
        val currentMod = QiblaCalculator.normalizeAngle360(-continuousTargetDialAngle)
        val targetMod = QiblaCalculator.normalizeAngle360(-desiredDialAngle)
        val delta = QiblaCalculator.shortestSignedAngleDelta(currentMod, targetMod)
        continuousTargetDialAngle -= delta
    }

    val animatedDialRotation by animateFloatAsState(
        targetValue = continuousTargetDialAngle,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 240f),
        label = "qibla_dial_rotation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val alignedColor = Color(0xFF1F9D68)
    val textMeasurer: TextMeasurer = rememberTextMeasurer()

    val northLabel = if (language == AppLanguage.ARABIC) "ش" else "N"
    val eastLabel = if (language == AppLanguage.ARABIC) "شر" else "E"
    val southLabel = if (language == AppLanguage.ARABIC) "ج" else "S"
    val westLabel = if (language == AppLanguage.ARABIC) "غ" else "W"

    Box(
        modifier = Modifier
            .size(280.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("qibla_compass_canvas")
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension / 2f - 12.dp.toPx()

            // Outer ambient glow when aligned
            if (isAligned) {
                drawCircle(
                    color = alignedColor.copy(alpha = 0.14f),
                    radius = radius + 8.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }

            // Dial outer ring
            drawCircle(
                color = surfaceVariantColor.copy(alpha = 0.45f),
                radius = radius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = if (isAligned) alignedColor else outlineColor,
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 3.dp.toPx())
            )

            // Rotating compass rose + Qibla pointer
            rotate(degrees = animatedDialRotation, pivot = Offset(cx, cy)) {
                // Degree tick marks
                for (deg in 0 until 360 step 5) {
                    val isMajor = deg % 30 == 0
                    val isMedium = deg % 15 == 0
                    val tickLength = when {
                        isMajor -> 12.dp.toPx()
                        isMedium -> 8.dp.toPx()
                        else -> 5.dp.toPx()
                    }
                    val angleRad = (deg - 90.0) * PI / 180.0
                    val startR = radius - tickLength
                    val endR = radius - 2.dp.toPx()
                    drawLine(
                        color = if (deg == 0) tertiaryColor else onSurfaceColor.copy(alpha = if (isMajor) 0.65f else 0.28f),
                        start = Offset(
                            x = cx + (startR * cos(angleRad)).toFloat(),
                            y = cy + (startR * sin(angleRad)).toFloat()
                        ),
                        end = Offset(
                            x = cx + (endR * cos(angleRad)).toFloat(),
                            y = cy + (endR * sin(angleRad)).toFloat()
                        ),
                        strokeWidth = if (isMajor) 2.2.dp.toPx() else 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Cardinal markers
                val cardinals = listOf(
                    0f to northLabel,
                    90f to eastLabel,
                    180f to southLabel,
                    270f to westLabel
                )
                val labelRadius = radius - 26.dp.toPx()
                cardinals.forEach { (angleDeg, label) ->
                    val rad = (angleDeg - 90.0) * PI / 180.0
                    val lx = cx + (labelRadius * cos(rad)).toFloat()
                    val ly = cy + (labelRadius * sin(rad)).toFloat()
                    val style = TextStyle(
                        color = if (angleDeg == 0f) tertiaryColor else onSurfaceColor,
                        fontSize = 14.sp,
                        fontWeight = if (angleDeg == 0f) FontWeight.Bold else FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    val measured = textMeasurer.measure(label, style = style)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = lx - measured.size.width / 2f,
                            y = ly - measured.size.height / 2f
                        )
                    )
                }

                // Qibla needle rotated to qiblaBearingDegrees relative to North
                rotate(degrees = qiblaBearingDegrees, pivot = Offset(cx, cy)) {
                    val needleTipY = cy - radius + 34.dp.toPx()
                    val needleTailY = cy + radius * 0.42f
                    val needleColor = if (isAligned) alignedColor else primaryColor

                    val arrowPath = Path().apply {
                        moveTo(cx, needleTipY)
                        lineTo(cx + 12.dp.toPx(), cy)
                        lineTo(cx, cy - 8.dp.toPx())
                        lineTo(cx - 12.dp.toPx(), cy)
                        close()
                    }
                    drawPath(path = arrowPath, color = needleColor)

                    // Opposite tail pointer
                    val tailPath = Path().apply {
                        moveTo(cx, needleTailY)
                        lineTo(cx + 8.dp.toPx(), cy)
                        lineTo(cx - 8.dp.toPx(), cy)
                        close()
                    }
                    drawPath(path = tailPath, color = outlineColor.copy(alpha = 0.6f))

                    // Stylized Kaaba icon marker near tip
                    val kaabaBoxCenterY = needleTipY - 14.dp.toPx()
                    val boxHalf = 8.dp.toPx()
                    drawRect(
                        color = Color(0xFF1F2421),
                        topLeft = Offset(cx - boxHalf, kaabaBoxCenterY - boxHalf),
                        size = androidx.compose.ui.geometry.Size(boxHalf * 2f, boxHalf * 2f)
                    )
                    drawLine(
                        color = Color(0xFFE7C26B),
                        start = Offset(cx - boxHalf, kaabaBoxCenterY - boxHalf * 0.35f),
                        end = Offset(cx + boxHalf, kaabaBoxCenterY - boxHalf * 0.35f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // Center pivot disc
            drawCircle(
                color = if (isAligned) alignedColor else tertiaryColor,
                radius = 9.dp.toPx(),
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(cx, cy)
            )

            // Fixed top lubber triangle indicating device forward heading
            val topTipY = cy - radius + 2.dp.toPx()
            val lubber = Path().apply {
                moveTo(cx, topTipY + 8.dp.toPx())
                lineTo(cx - 6.dp.toPx(), topTipY - 4.dp.toPx())
                lineTo(cx + 6.dp.toPx(), topTipY - 4.dp.toPx())
                close()
            }
            drawPath(path = lubber, color = tertiaryColor)
        }
    }
}

private fun localizedCardinal(degrees: Float, language: AppLanguage): String {
    val code = QiblaCalculator.cardinalDirectionCode(degrees)
    if (language != AppLanguage.ARABIC) return code
    return when (code) {
        "N" -> "شمال"
        "NE" -> "شمال شرق"
        "E" -> "شرق"
        "SE" -> "جنوب شرق"
        "S" -> "جنوب"
        "SW" -> "جنوب غرب"
        "W" -> "غرب"
        else -> "شمال غرب"
    }
}
