package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

import com.example.ui.UserRole

enum class NavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    FORMS("Forms", Icons.Filled.Description, Icons.Outlined.Description, "nav_forms"),
    RECORDS("Records", Icons.Filled.Layers, Icons.Outlined.Layers, "nav_records"),
    SCHOOLS("Schools", Icons.Filled.School, Icons.Outlined.School, "nav_schools"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
}

@Composable
fun EduBottomBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    role: UserRole = UserRole.OFFICER,
    modifier: Modifier = Modifier
) {
    val visibleTabs = if (role == UserRole.HEADMASTER) {
        NavTab.values().filter { it != NavTab.SCHOOLS }
    } else {
        NavTab.values().toList()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NavBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleTabs.forEach { tab ->
            val isSelected = tab == selectedTab

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTabSelected(tab) }
                    .testTag(tab.testTag)
                    .padding(vertical = 4.dp, horizontal = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) NavSelectedPill else Color.Transparent)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title,
                        tint = if (isSelected) ForestDarkGreen else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = tab.title,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) ForestDarkGreen else TextMuted
                )
            }
        }
    }
}
