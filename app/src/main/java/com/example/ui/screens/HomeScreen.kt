package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FormEntity
import com.example.data.model.SchoolEntity
import com.example.data.model.SubmissionEntity
import com.example.data.model.UserEntity
import com.example.ui.UserRole
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    role: UserRole,
    currentUser: UserEntity? = null,
    selectedSchool: SchoolEntity?,
    formCount: Int,
    recordCount: Int,
    schoolCount: Int,
    recentSubmissions: List<SubmissionEntity>,
    activeForms: List<FormEntity>,
    onRoleToggle: () -> Unit,
    onNavigateToNewForm: () -> Unit,
    onNavigateToFillForm: (String) -> Unit,
    onNavigateToFormsTab: ((Int) -> Unit)? = null,
    onNavigateToRecords: () -> Unit,
    onNavigateToSchools: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    // Calculate Form Status Metrics according to requirement #6
    val userUdise = currentUser?.udiseCode?.trim() ?: ""
    
    val userSubmissions = remember(recentSubmissions, userUdise, role) {
        if (role == UserRole.HEADMASTER && userUdise.isNotBlank()) {
            recentSubmissions.filter { it.schoolId.equals(userUdise, ignoreCase = true) }
        } else {
            recentSubmissions
        }
    }

    val publishedFormsCount = activeForms.size
    val filledFormsCount = userSubmissions.map { sub -> sub.formId }.distinct().size
    val pendingFormsCount = (publishedFormsCount - filledFormsCount).coerceAtLeast(0)
    val returnedFormsCount = activeForms.count { 
        it.status.equals("RETURNED", ignoreCase = true) || 
        it.status.equals("NEEDS_REVISION", ignoreCase = true) || 
        it.status.equals("CORRECTION", ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MintBackground)
            .verticalScroll(scrollState)
    ) {
        // TOP BENTO HEADER BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ForestDarkGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = "App Icon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "EduCollect Sync",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                        Text(
                            text = if (role == UserRole.OFFICER) "OFFICER DASHBOARD" else "HEADMASTER MODE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextMuted,
                            letterSpacing = 1.2.sp
                        )
                        val headerSchoolName = currentUser?.schoolName?.ifBlank { null } ?: selectedSchool?.name ?: ""
                        if (headerSchoolName.isNotBlank()) {
                            Text(
                                text = headerSchoolName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestDarkGreen
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Role Switcher Chip
                    Surface(
                        onClick = onRoleToggle,
                        shape = RoundedCornerShape(20.dp),
                        color = NavBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier.testTag("role_switcher_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (role == UserRole.OFFICER) Icons.Filled.AdminPanelSettings else Icons.Filled.School,
                                contentDescription = "Switch Role",
                                tint = ForestDarkGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (role == UserRole.OFFICER) "Officer" else "HM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestDarkGreen
                            )
                        }
                    }

                    // Logout Button
                    onLogout?.let { logoutAction ->
                        Surface(
                            onClick = logoutAction,
                            shape = CircleShape,
                            color = BentoContainerPurple,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                            modifier = Modifier.testTag("btn_logout")
                        ) {
                            Box(
                                modifier = Modifier.padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Logout,
                                    contentDescription = "Log Out",
                                    tint = SoftPurpleIcon,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Divider(color = SurfaceCardBorder)

        Spacer(modifier = Modifier.height(16.dp))

        // REGISTERED SCHOOL DETAILS BENTO CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = BentoHeroBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForestDarkGreen.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = "School Info",
                            tint = ForestDarkGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "REGISTERED SCHOOL INFO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoHeroText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val infoSchoolName = currentUser?.schoolName?.ifBlank { null } ?: selectedSchool?.name ?: "District High School"
                val infoHmName = currentUser?.headmasterName?.ifBlank { null } ?: selectedSchool?.headmasterName ?: "Headmaster"
                val infoUdise = currentUser?.udiseCode?.ifBlank { null } ?: selectedSchool?.id ?: "UDISE Code"
                val infoPhone = currentUser?.phone?.ifBlank { null } ?: selectedSchool?.headmasterPhone ?: "N/A"
                val infoDistrict = selectedSchool?.district ?: "Assigned District"
                val infoCategory = selectedSchool?.category ?: "Primary & Secondary"

                Text(
                    text = infoSchoolName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoHeroText
                )

                Spacer(modifier = Modifier.height(10.dp))

                Divider(color = ForestDarkGreen.copy(alpha = 0.15f))

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "HM Name",
                            tint = ForestDarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Headmaster: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = infoHmName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Badge,
                            contentDescription = "UDISE",
                            tint = ForestDarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UDISE Code: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = infoUdise,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = "Phone",
                            tint = ForestDarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Contact Phone: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = infoPhone,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "District",
                            tint = ForestDarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "District / Category: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = "$infoDistrict ($infoCategory)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // BENTO FORM STATUS GRID (4 Cards: Published, Filled, Pending, Returned for Corrections)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Form Overview & Status",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    icon = Icons.Filled.Assignment,
                    iconBg = BentoContainerNeutral,
                    iconTint = ForestDarkGreen,
                    count = publishedFormsCount.toString(),
                    label = "Published Forms",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToFormsTab?.invoke(0) },
                    testTag = "metric_published_forms"
                )

                MetricCard(
                    icon = Icons.Filled.CheckCircle,
                    iconBg = BentoHeroBg,
                    iconTint = ForestDarkGreen,
                    count = filledFormsCount.toString(),
                    label = "Filled Forms",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToFormsTab?.invoke(1) },
                    testTag = "metric_filled_forms"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    icon = Icons.Filled.HourglassEmpty,
                    iconBg = BentoContainerPurple,
                    iconTint = SoftPurpleIcon,
                    count = pendingFormsCount.toString(),
                    label = "Pending Forms",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToFormsTab?.invoke(2) },
                    testTag = "metric_pending_forms"
                )

                MetricCard(
                    icon = Icons.Filled.AssignmentReturn,
                    iconBg = Color(0xFFFFF3E0),
                    iconTint = Color(0xFFE65100),
                    count = returnedFormsCount.toString(),
                    label = "Returned for Corrections",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToFormsTab?.invoke(3) },
                    testTag = "metric_returned_forms"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // QUICK ACTIONS BENTO GRID
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Quick Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // If OFFICER: Show New Form + Fill Form
                // If HEADMASTER: Show Fill Form + View Records (HM cannot create forms)
                if (role == UserRole.OFFICER) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(28.dp))
                            .clickable { onNavigateToNewForm() }
                            .testTag("action_new_form"),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = BentoContainerNeutral),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp, horizontal = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(ForestDarkGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "New Form",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "New Form",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .clickable {
                            val firstFormId = activeForms.firstOrNull()?.id ?: "FORM_INFRA_2026"
                            onNavigateToFillForm(firstFormId)
                        }
                        .testTag("action_fill_form"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoContainerPurple),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SurfaceWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Fill Form",
                                tint = SoftPurpleIcon,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Fill Form",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                    }
                }

                if (role == UserRole.HEADMASTER) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(28.dp))
                            .clickable { onNavigateToRecords() }
                            .testTag("action_view_records"),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = BentoHeroBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ForestDarkGreen.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp, horizontal = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(ForestDarkGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.List,
                                    contentDescription = "View Records",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "View Records",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                        }
                    }
                }
            }

            if (role == UserRole.OFFICER) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .clickable { onNavigateToRecords() }
                        .testTag("action_view_records"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoHeroBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForestDarkGreen.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ForestDarkGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.List,
                                contentDescription = "View Records",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "View Records",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun MetricCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    count: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    testTag: String
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .testTag(testTag),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = count,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextDarkPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
        }
    }
}

@Composable
fun TextTextButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = ForestDarkGreen,
        modifier = Modifier.clickable { onClick() }
    )
}
