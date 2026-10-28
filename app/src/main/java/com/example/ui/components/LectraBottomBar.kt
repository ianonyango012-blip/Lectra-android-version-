package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.LectraDestination
import com.example.ui.theme.LectraDarkBorder
import com.example.ui.theme.LectraDarkCardBg
import com.example.ui.theme.LectraDarkPrimaryAccent
import com.example.ui.theme.LectraDarkSecondaryText
import com.example.ui.theme.LectraLightBorder
import com.example.ui.theme.LectraLightPrimaryAccent
import com.example.ui.theme.LectraLightSecondaryText

/**
 * Fixed 5-destination bottom navigation bar matching the visual reference screenshot.
 * Exactly in order: Home, Notes, Timetable, Flashcards, Google Search.
 */
@Composable
fun LectraBottomBar(
    currentDestination: LectraDestination,
    onNavigate: (LectraDestination) -> Unit
) {
    val navDestinations = listOf(
        LectraDestination.HOME,
        LectraDestination.COURSES,
        LectraDestination.PLANNER,
        LectraDestination.REVISION,
        LectraDestination.SEARCH
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.fillMaxWidth()
            ) {
                navDestinations.forEach { destination ->
                    val isSelected = currentDestination == destination

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onNavigate(destination) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.title,
                                modifier = Modifier.size(23.dp)
                            )
                        },
                        label = {
                            Text(
                                text = destination.shortLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                    )
                }
            }
        }
    }
}
