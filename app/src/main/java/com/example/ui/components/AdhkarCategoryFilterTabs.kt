package com.example.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Dhikr
import com.example.data.model.DhikrCategory

enum class AdhkarTabFilter(
    val id: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val linkedCategory: DhikrCategory?
) {
    MORNING(
        id = "morning",
        labelRes = R.string.tab_filter_morning,
        icon = Icons.Rounded.WbSunny,
        linkedCategory = DhikrCategory.MORNING
    ),
    EVENING(
        id = "evening",
        labelRes = R.string.tab_filter_evening,
        icon = Icons.Rounded.NightsStay,
        linkedCategory = DhikrCategory.EVENING
    ),
    GENERAL(
        id = "general",
        labelRes = R.string.tab_filter_general,
        icon = Icons.Rounded.AutoAwesome,
        linkedCategory = null
    );

    fun matches(dhikr: Dhikr): Boolean = when (this) {
        MORNING -> dhikr.category == DhikrCategory.MORNING.id
        EVENING -> dhikr.category == DhikrCategory.EVENING.id
        GENERAL -> dhikr.category != DhikrCategory.MORNING.id &&
            dhikr.category != DhikrCategory.EVENING.id
    }

    fun filterAdhkar(allAdhkar: List<Dhikr>): List<Dhikr> = allAdhkar.filter(::matches)
}

@Composable
fun AdhkarCategoryFilterBar(
    selectedTab: AdhkarTabFilter,
    onSelectTab: (AdhkarTabFilter) -> Unit,
    filteredCount: Int,
    onOpenLinkedCategory: (DhikrCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = AdhkarTabFilter.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.tab_filter_section_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.testTag("filtered_dhikr_count_badge")
                ) {
                    Text(
                        text = stringResource(R.string.tab_filter_count_badge, filteredCount),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                selectedTab.linkedCategory?.let { linked ->
                    TextButton(
                        onClick = { onOpenLinkedCategory(linked) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("tab_filter_open_section_button")
                    ) {
                        Text(
                            text = stringResource(R.string.tab_filter_open_section),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            TabRow(
                selectedTabIndex = selectedIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedIndex])
                                .padding(horizontal = 14.dp)
                                .clip(CircleShape),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dhikr_category_tab_row")
            ) {
                tabs.forEach { tab ->
                    val selected = tab == selectedTab
                    Tab(
                        selected = selected,
                        onClick = { onSelectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("category_tab_${tab.id}")
                    )
                }
            }
        }
    }
}
