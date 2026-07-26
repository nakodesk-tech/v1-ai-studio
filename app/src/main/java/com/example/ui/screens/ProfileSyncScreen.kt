package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncConfigEntity
import com.example.data.model.UserEntity
import com.example.ui.UserRole
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileSyncScreen(
    currentRole: UserRole,
    syncConfig: SyncConfigEntity,
    currentUser: UserEntity? = null,
    allUsers: List<UserEntity> = emptyList(),
    isSyncingUsers: Boolean = false,
    onSyncUsers: () -> Unit = {},
    onSaveSyncConfig: (String, String, String, Boolean) -> Unit,
    onRegisterOfficer: (officerId: String, fullName: String, designation: String, phone: String, email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onRegisterSchool: (udiseCode: String, schoolName: String, hmName: String, phone: String, email: String, password: String, role: UserRole, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onCreateAccount: (role: UserRole, fullName: String, email: String, password: String, confirmPassword: String, schoolName: String, udiseNumber: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onChangePassword: (oldPass: String, newPass: String, confirmPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onResetUserPassword: (targetUserId: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _ -> },
    onDeleteUser: (targetUserId: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _ -> },
    onUpdateUserInfo: (udiseCode: String, name: String, phone: String, email: String, schoolName: String, udiseNumber: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onLogout: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        onSyncUsers()
    }

    // Config states for Officer
    var driveEmail by remember(syncConfig) { mutableStateOf(syncConfig.officerDriveEmail) }
    var sheetId by remember(syncConfig) { mutableStateOf(syncConfig.googleSheetId) }
    var webhookUrl by remember(syncConfig) { mutableStateOf(syncConfig.appsScriptWebhookUrl) }
    var autoSync by remember(syncConfig) { mutableStateOf(syncConfig.autoSync) }
    var isGoogleConnected by remember { mutableStateOf(true) }

    // Dialog flags
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showManageUsersDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }
    var userToResetPassword by remember { mutableStateOf<UserEntity?>(null) }

    // Section state for Officer User & Role Management (0: Officers, 1: School Users, 2: Add New Account)
    var selectedSectionTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ForestDarkGreen)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column {
                    Text(
                        text = "ACCOUNT & CLOUD CONFIGURATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLightSubtle,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "User Profile & Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        },
        containerColor = MintBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. USER PROFILE INFO CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header Row: Avatar, Name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(BentoHeroBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = "User Avatar",
                                    tint = ForestDarkGreen,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = currentUser?.headmasterName?.ifBlank { "Registered User" } ?: "Chief Education Officer",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )
                                Text(
                                    text = "UDISE Number: ${if (currentUser?.udiseNumber.isNullOrBlank()) "Not assigned" else currentUser!!.udiseNumber}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ForestDarkGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // HIGHLIGHT LOGIN ROLE BADGE
                    Surface(
                        color = BentoHeroBg,
                        border = BorderStroke(1.dp, ForestDarkGreen.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (currentRole == UserRole.OFFICER) Icons.Filled.VerifiedUser else Icons.Filled.School,
                                    contentDescription = "Role",
                                    tint = ForestDarkGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Active Login Role:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDarkPrimary
                                )
                            }

                            Text(
                                text = if (currentRole == UserRole.OFFICER) "OFFICER LOGIN" else "HEADMASTER LOGIN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ForestDarkGreen,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = SurfaceCardBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // DETAILED REGISTRATION INFORMATION GRID
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("School / Office Name", fontSize = 11.sp, color = ForestDarkGreen, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (currentUser?.schoolName.isNullOrBlank()) "Not assigned" else currentUser!!.schoolName,
                                    fontSize = 13.sp,
                                    color = TextDarkPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Registered Contact Phone", fontSize = 11.sp, color = ForestDarkGreen, fontWeight = FontWeight.Bold)
                                Text(
                                    text = currentUser?.phone?.ifBlank { "9876500000" } ?: "9876500000",
                                    fontSize = 13.sp,
                                    color = TextDarkPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Registered Gmail / Email", fontSize = 11.sp, color = ForestDarkGreen, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (currentUser?.email.isNullOrBlank()) "Not assigned" else currentUser!!.email,
                                    fontSize = 13.sp,
                                    color = TextDarkPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Registration Date", fontSize = 11.sp, color = ForestDarkGreen, fontWeight = FontWeight.Bold)
                                val regTime = currentUser?.registeredAt ?: System.currentTimeMillis()
                                val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(regTime))
                                Text(
                                    text = formattedDate,
                                    fontSize = 12.sp,
                                    color = TextDarkPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ACTION BUTTONS ROW BELOW PROFILE: CHANGE PASSWORD & SIGN OUT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // CHANGE PASSWORD POPUP BUTTON
                        Button(
                            onClick = { showChangePasswordDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("btn_change_password_popup"),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.LockReset, contentDescription = "Change Password", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Password", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // SIGN OUT BUTTON
                        onLogout?.let { logoutAction ->
                            OutlinedButton(
                                onClick = logoutAction,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("btn_profile_logout"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                                border = BorderStroke(1.dp, Color(0xFFFFCDD2))
                            ) {
                                Icon(Icons.Filled.Logout, contentDescription = "Log Out", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. OFFICER PORTAL / USER & ROLE MANAGEMENT SECTION
            if (currentRole == UserRole.OFFICER) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BentoHeroBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.AdminPanelSettings,
                                        contentDescription = "User & Role Management",
                                        tint = ForestDarkGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "User & Role Management",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDarkPrimary
                                    )
                                    Text(
                                        text = "Administer officers & school accounts",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            // Sync / Refresh Button
                            IconButton(
                                onClick = { onSyncUsers() },
                                enabled = !isSyncingUsers,
                                modifier = Modifier.size(36.dp).testTag("btn_refresh_role_management")
                            ) {
                                if (isSyncingUsers) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = ForestDarkGreen,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Sync from Supabase",
                                        tint = ForestDarkGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // DYNAMIC SUMMARY CARDS FROM SUPABASE public.profiles
                        val officersCount = remember(allUsers) { allUsers.count { it.role == "OFFICER" } }
                        val schoolUsersCount = remember(allUsers) { allUsers.count { it.role != "OFFICER" } }
                        val totalAccountsCount = remember(allUsers) { allUsers.size }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Summary Card 1: Officers
                            Surface(
                                onClick = { selectedSectionTab = 0 },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = if (selectedSectionTab == 0) BentoHeroBg else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    if (selectedSectionTab == 0) 1.5.dp else 1.dp,
                                    if (selectedSectionTab == 0) ForestDarkGreen else SurfaceCardBorder
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.VerifiedUser, contentDescription = "Officers", tint = ForestDarkGreen, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$officersCount", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ForestDarkGreen)
                                    Text("Officers", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextDarkPrimary)
                                }
                            }

                            // Summary Card 2: School Users
                            Surface(
                                onClick = { selectedSectionTab = 1 },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = if (selectedSectionTab == 1) BentoHeroBg else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    if (selectedSectionTab == 1) 1.5.dp else 1.dp,
                                    if (selectedSectionTab == 1) ForestDarkGreen else SurfaceCardBorder
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.School, contentDescription = "School Users", tint = Color(0xFF1E88E5), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$schoolUsersCount", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E88E5))
                                    Text("School Users", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextDarkPrimary)
                                }
                            }

                            // Summary Card 3: Total Accounts
                            Surface(
                                onClick = { selectedSectionTab = 0 },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, SurfaceCardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.Group, contentDescription = "Total Accounts", tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$totalAccountsCount", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF475569))
                                    Text("Total Accounts", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextDarkPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // SECTION CONTROL TABS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = selectedSectionTab == 0,
                                onClick = { selectedSectionTab = 0 },
                                label = { Text("Officers", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(13.dp)) },
                                modifier = Modifier.weight(1f).testTag("tab_manage_officers")
                            )
                            FilterChip(
                                selected = selectedSectionTab == 1,
                                onClick = { selectedSectionTab = 1 },
                                label = { Text("School Users", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(13.dp)) },
                                modifier = Modifier.weight(1f).testTag("tab_manage_school_users")
                            )
                            FilterChip(
                                selected = selectedSectionTab == 2,
                                onClick = { selectedSectionTab = 2 },
                                label = { Text("Add Account", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(13.dp)) },
                                modifier = Modifier.weight(1f).testTag("tab_add_new_account")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        when (selectedSectionTab) {
                            0 -> {
                                // MANAGE OFFICERS TAB
                                val officersList = remember(allUsers) { allUsers.filter { it.role == "OFFICER" } }
                                if (officersList.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No registered Officers found in Supabase profiles.", fontSize = 12.sp, color = TextMuted)
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        officersList.forEach { usr ->
                                            OfficerUserCardItem(
                                                user = usr,
                                                onEdit = { editingUser = usr },
                                                onResetPassword = { userToResetPassword = usr },
                                                onDelete = { userToDelete = usr }
                                            )
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // MANAGE SCHOOL USERS TAB
                                var searchQuery by remember { mutableStateOf("") }
                                val schoolUsersList = remember(allUsers, searchQuery) {
                                    allUsers.filter { usr ->
                                        usr.role != "OFFICER" && (
                                            searchQuery.isBlank() ||
                                            usr.schoolName.contains(searchQuery, ignoreCase = true) ||
                                            usr.headmasterName.contains(searchQuery, ignoreCase = true) ||
                                            usr.udiseNumber.contains(searchQuery, ignoreCase = true) ||
                                            usr.email.contains(searchQuery, ignoreCase = true)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search school, HM, UDISE, email...", fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextDarkPrimary,
                                            unfocusedTextColor = TextDarkPrimary,
                                            focusedBorderColor = ForestDarkGreen,
                                            unfocusedBorderColor = SurfaceCardBorder,
                                            focusedContainerColor = SurfaceWhite,
                                            unfocusedContainerColor = SurfaceWhite
                                        )
                                    )

                                    if (schoolUsersList.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No registered School Users found.", fontSize = 12.sp, color = TextMuted)
                                        }
                                    } else {
                                        schoolUsersList.forEach { usr ->
                                            SchoolUserCardItem(
                                                user = usr,
                                                onEdit = { editingUser = usr },
                                                onResetPassword = { userToResetPassword = usr },
                                                onDelete = { userToDelete = usr }
                                            )
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // ADD NEW ACCOUNT TAB
                                AddNewAccountForm(
                                    onCreateAccount = onCreateAccount,
                                    onAccountCreated = { createdRole ->
                                        selectedSectionTab = if (createdRole == UserRole.OFFICER) 0 else 1
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 4. GOOGLE DRIVE & SHEETS API SYNC CARD (READ-ONLY FOR HM, CONNECTABLE FOR OFFICER)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BentoHeroBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.CloudSync, contentDescription = "Cloud", tint = ForestDarkGreen, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (currentRole == UserRole.OFFICER) "Connect Google Account & Cloud Sync" else "Google Drive & Sheets API Sync",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                        }

                        if (currentRole == UserRole.HEADMASTER) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "READ-ONLY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentRole == UserRole.HEADMASTER) {
                        // READ-ONLY INFO VIEW FOR HEADMASTER
                        Text(
                            text = "This cloud configuration is set by Officer Main. Submissions from your school automatically sync to this Google Drive & Sheets API storage.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(BentoHeroBg)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Email, contentDescription = "Email", tint = ForestDarkGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Officer Drive Email: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                                Text(driveEmail.ifBlank { "officer.main@education.gov.in" }, fontSize = 12.sp, color = TextDarkPrimary)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.TableChart, contentDescription = "Sheet ID", tint = ForestDarkGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Google Sheet ID: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                                Text(sheetId.take(16) + "...", fontSize = 12.sp, color = TextDarkPrimary)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Sync, contentDescription = "Status", tint = ForestDarkGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Real-Time Auto Sync: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                                Text(if (autoSync) "ACTIVE (AUTOMATIC)" else "MANUAL", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = ForestDarkGreen)
                            }
                        }
                    } else {
                        // OFFICER CONNECT GOOGLE ACCOUNT & EDITABLE SETTINGS
                        Text(
                            text = "Connect Officer Google Drive account to auto-create folders and sync published forms and submissions.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // CONNECT GOOGLE ACCOUNT BUTTON FOR OFFICER
                        Button(
                            onClick = {
                                isGoogleConnected = true
                                if (driveEmail.isBlank()) {
                                    driveEmail = currentUser?.email?.ifBlank { "officer.main@education.gov.in" } ?: "officer.main@education.gov.in"
                                }
                                if (sheetId.isBlank()) {
                                    sheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms"
                                }
                                if (webhookUrl.isBlank()) {
                                    webhookUrl = "https://script.google.com/macros/s/AKfycbz_EduCollect_OfficerSync_Webhook/exec"
                                }
                                onSaveSyncConfig(driveEmail, sheetId, webhookUrl, autoSync)
                                Toast.makeText(
                                    context,
                                    "Google Drive Sign-In Successful!\nAuto-fetched Officer Drive Email, Sheets ID & Webhook Endpoint.",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_connect_google_account"),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isGoogleConnected) Color(0xFF1E88E5) else ForestDarkGreen),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.AccountBox, contentDescription = "Google Account")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGoogleConnected) "Google Drive Connected ($driveEmail)" else "Sign In with Google Drive Account",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val standardFieldColors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextDarkPrimary,
                            unfocusedTextColor = TextDarkPrimary,
                            focusedBorderColor = ForestDarkGreen,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedLabelColor = ForestDarkGreen,
                            unfocusedLabelColor = TextDarkPrimary,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        )

                        OutlinedTextField(
                            value = driveEmail,
                            onValueChange = { driveEmail = it },
                            label = { Text("1. Officer Main Google Drive Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email", tint = TextMuted) },
                            shape = RoundedCornerShape(16.dp),
                            colors = standardFieldColors
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = sheetId,
                            onValueChange = { sheetId = it },
                            label = { Text("2. Google Sheets Spreadsheet ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.TableChart, contentDescription = "Sheet ID", tint = TextMuted) },
                            shape = RoundedCornerShape(16.dp),
                            colors = standardFieldColors
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = webhookUrl,
                            onValueChange = { webhookUrl = it },
                            label = { Text("3. Apps Script Webhook Endpoint") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Link, contentDescription = "Webhook", tint = TextMuted) },
                            shape = RoundedCornerShape(16.dp),
                            colors = standardFieldColors
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Automatic Real-Time Sync", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Sync HM submissions automatically", fontSize = 11.sp, color = TextMuted)
                            }

                            Switch(
                                checked = autoSync,
                                onCheckedChange = { autoSync = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = ForestDarkGreen, checkedTrackColor = BentoHeroBg)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                onSaveSyncConfig(driveEmail, sheetId, webhookUrl, autoSync)
                                Toast.makeText(context, "Google Services & Cloud Config Saved!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_save_sync_settings"),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Google Sync Settings", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // FOOTER: "Made in Love with Teachers by Sachin Nakode"
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = BentoHeroBg,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Love",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Made in Love with Teachers by Sachin Nakode",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestDarkGreen,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- DIALOG 1: CHANGE PASSWORD POPUP WINDOW ---
    if (showChangePasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmNewPassword by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LockReset, contentDescription = "Password", tint = ForestDarkGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Account Password", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your current password and set a new password.", fontSize = 12.sp, color = TextSecondary)

                    val dialogFieldColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary,
                        focusedBorderColor = ForestDarkGreen,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedLabelColor = ForestDarkGreen,
                        unfocusedLabelColor = TextDarkPrimary,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    )

                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Old Password *") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = dialogFieldColors
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password *") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = dialogFieldColors
                    )

                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = { Text("Confirm New Password *") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = dialogFieldColors
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onChangePassword(
                            oldPassword,
                            newPassword,
                            confirmNewPassword,
                            {
                                showChangePasswordDialog = false
                                Toast.makeText(context, "Password changed successfully!", Toast.LENGTH_SHORT).show()
                            },
                            { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_submit_change_password")
                ) {
                    Text("Change Password", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // --- DIALOG 4: MANAGE ALL REGISTERED USERS (SHOW ALL SCHOOLS & OFFICERS) ---
    if (showManageUsersDialog) {
        var userSearch by remember { mutableStateOf("") }
        var selectedRoleFilter by remember { mutableStateOf("ALL") } // "ALL", "OFFICER", "HEADMASTER"
        var editingUser by remember { mutableStateOf<UserEntity?>(null) }
        var userToDelete by remember { mutableStateOf<UserEntity?>(null) }

        val filteredUsers = remember(allUsers, userSearch, selectedRoleFilter) {
            allUsers.filter { usr ->
                val matchesRole = when (selectedRoleFilter) {
                    "OFFICER" -> usr.role == "OFFICER"
                    "HEADMASTER" -> usr.role == "HEADMASTER"
                    else -> true
                }
                val matchesQuery = if (userSearch.isBlank()) true else {
                    usr.headmasterName.contains(userSearch, ignoreCase = true) ||
                    usr.udiseCode.contains(userSearch, ignoreCase = true) ||
                    usr.schoolName.contains(userSearch, ignoreCase = true) ||
                    usr.email.contains(userSearch, ignoreCase = true)
                }
                matchesRole && matchesQuery
            }
        }

        AlertDialog(
            onDismissRequest = { showManageUsersDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val registeredUserCountInDialog = allUsers.count { it.role != "OFFICER" }
                        Text("Registered Users ($registeredUserCountInDialog)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Source: Supabase Profiles", fontSize = 11.sp, color = TextSecondary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onSyncUsers() },
                            enabled = !isSyncingUsers,
                            modifier = Modifier.testTag("btn_dialog_refresh_users")
                        ) {
                            if (isSyncingUsers) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = ForestDarkGreen,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Refresh Users from Supabase",
                                    tint = ForestDarkGreen
                                )
                            }
                        }

                        IconButton(onClick = { showManageUsersDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                    // ROLE FILTER CHIPS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedRoleFilter == "ALL",
                            onClick = { selectedRoleFilter = "ALL" },
                            label = { Text("All (${allUsers.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedRoleFilter == "OFFICER",
                            onClick = { selectedRoleFilter = "OFFICER" },
                            label = { Text("Officers (${allUsers.count { it.role == "OFFICER" }})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedRoleFilter == "HEADMASTER",
                            onClick = { selectedRoleFilter = "HEADMASTER" },
                            label = { Text("Schools/HM (${allUsers.count { it.role == "HEADMASTER" }})", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = userSearch,
                        onValueChange = { userSearch = it },
                        placeholder = { Text("Search name, UDISE/ID, email...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextDarkPrimary,
                            unfocusedTextColor = TextDarkPrimary,
                            focusedBorderColor = ForestDarkGreen,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (filteredUsers.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No registered users found.", fontSize = 13.sp, color = TextMuted)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filteredUsers) { usr ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = BentoHeroBg,
                                    border = BorderStroke(1.dp, SurfaceCardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                val displaySchoolName = if (usr.schoolName.isBlank() || usr.schoolName == "School Portal" || usr.schoolName == "District Office") "Not assigned" else usr.schoolName
                                                val displayUdise = if (usr.udiseNumber.isBlank()) "Not assigned" else usr.udiseNumber
                                                val displayEmail = if (usr.email.isBlank()) "Not assigned" else usr.email
                                                val displayName = usr.headmasterName.ifBlank { "Not assigned" }

                                                Text("School Name: $displaySchoolName", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                                                Text("HM / User Name: $displayName", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextDarkPrimary)
                                                Text("UDISE Number: $displayUdise", fontSize = 11.sp, color = ForestDarkGreen, fontWeight = FontWeight.Bold)
                                                Text("Email: $displayEmail", fontSize = 11.sp, color = TextMuted)
                                            }

                                            Surface(
                                                color = if (usr.role == "OFFICER") Color(0xFF1E88E5) else ForestDarkGreen,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = usr.role,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // ACTION BUTTONS FOR USER: RESET PASSWORD, EDIT, DELETE
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // RESET PASSWORD
                                            Button(
                                                onClick = { userToResetPassword = usr },
                                                modifier = Modifier.weight(1f).height(32.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                                contentPadding = PaddingValues(0.dp),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Reset Password", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // EDIT USER INFO
                                            OutlinedButton(
                                                onClick = { editingUser = usr },
                                                modifier = Modifier.weight(0.8f).height(32.dp),
                                                contentPadding = PaddingValues(0.dp),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Edit", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // DELETE USER (WITH CONFIRMATION)
                                            IconButton(
                                                onClick = { userToDelete = usr },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            shape = RoundedCornerShape(24.dp)
        )

        // CONFIRMATION DIALOG FOR DELETING USER
        userToDelete?.let { targetUsr ->
            var isDeleting by remember { mutableStateOf(false) }
            var deleteError by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { if (!isDeleting) userToDelete = null },
                icon = {
                    Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = Color(0xFFDC2626), modifier = Modifier.size(36.dp))
                },
                title = {
                    Text("Permanently delete this account?", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDarkPrimary)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "User: ${targetUsr.headmasterName.ifBlank { "User" }}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                        Text(
                            text = "Email: ${if (targetUsr.email.isBlank()) targetUsr.udiseCode else targetUsr.email}",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Role: ${targetUsr.role} | School/Office: ${targetUsr.schoolName}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This action will permanently remove this user account from Supabase Auth and profiles. This cannot be undone.",
                            fontSize = 12.sp,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium
                        )

                        deleteError?.let { err ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = err,
                                    fontSize = 12.sp,
                                    color = Color(0xFFC62828),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isDeleting = true
                            deleteError = null
                            onDeleteUser(
                                targetUsr.udiseCode,
                                {
                                    isDeleting = false
                                    userToDelete = null
                                    Toast.makeText(context, "Account permanently deleted", Toast.LENGTH_SHORT).show()
                                },
                                { err ->
                                    isDeleting = false
                                    deleteError = err
                                }
                            )
                        },
                        enabled = !isDeleting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.testTag("btn_confirm_delete_user")
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Confirm Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { userToDelete = null },
                        enabled = !isDeleting
                    ) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        // RESET PASSWORD DIALOG FOR USER
        userToResetPassword?.let { targetUsr ->
            var newPassword by remember { mutableStateOf("") }
            var confirmPassword by remember { mutableStateOf("") }
            var passwordVisible by remember { mutableStateOf(false) }
            var confirmPasswordVisible by remember { mutableStateOf(false) }
            var isResetting by remember { mutableStateOf(false) }
            var resetError by remember { mutableStateOf<String?>(null) }

            val dialogFieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextDarkPrimary,
                unfocusedTextColor = TextDarkPrimary,
                focusedBorderColor = ForestDarkGreen,
                unfocusedBorderColor = SurfaceCardBorder,
                focusedLabelColor = ForestDarkGreen,
                unfocusedLabelColor = TextDarkPrimary,
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite
            )

            AlertDialog(
                onDismissRequest = { if (!isResetting) userToResetPassword = null },
                title = { Text("Reset User Password", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "User: ${targetUsr.headmasterName.ifBlank { "User" }}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                        Text(
                            text = "Email: ${if (targetUsr.email.isBlank()) targetUsr.udiseCode else targetUsr.email}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password (min 6 chars) *") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Toggle Password Visibility"
                                    )
                                }
                            },
                            colors = dialogFieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("input_reset_new_password")
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm New Password *") },
                            singleLine = true,
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Toggle Confirm Password Visibility"
                                    )
                                }
                            },
                            colors = dialogFieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("input_reset_confirm_password")
                        )

                        resetError?.let { err ->
                            Surface(
                                color = Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = err,
                                    fontSize = 12.sp,
                                    color = Color(0xFFC62828),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPassword.length < 6) {
                                resetError = "Password must be at least 6 characters long."
                                return@Button
                            }
                            if (newPassword != confirmPassword) {
                                resetError = "Passwords do not match."
                                return@Button
                            }
                            isResetting = true
                            resetError = null
                            onResetUserPassword(
                                targetUsr.udiseCode,
                                newPassword,
                                {
                                    isResetting = false
                                    userToResetPassword = null
                                    Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                                },
                                { err ->
                                    isResetting = false
                                    resetError = err
                                }
                            )
                        },
                        enabled = !isResetting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        modifier = Modifier.testTag("btn_submit_reset_password")
                    ) {
                        if (isResetting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Reset Password", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { userToResetPassword = null },
                        enabled = !isResetting
                    ) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // EDIT USER SUB-DIALOG
        editingUser?.let { usrToEdit ->
            val isOfficerToEdit = usrToEdit.role == "OFFICER"
            var editName by remember(usrToEdit) { mutableStateOf(usrToEdit.headmasterName) }
            var editPhone by remember(usrToEdit) { mutableStateOf(usrToEdit.phone) }
            var editSchoolName by remember(usrToEdit) {
                mutableStateOf(if (usrToEdit.schoolName == "School Portal" || usrToEdit.schoolName == "District Office") "" else usrToEdit.schoolName)
            }
            var editUdiseNumber by remember(usrToEdit) { mutableStateOf(usrToEdit.udiseNumber) }
            var isSaving by remember { mutableStateOf(false) }
            var editError by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { if (!isSaving) editingUser = null },
                title = { Text(if (isOfficerToEdit) "Edit Officer Profile" else "Edit School User Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    val dialogFieldColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary,
                        focusedBorderColor = ForestDarkGreen,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedLabelColor = ForestDarkGreen,
                        unfocusedLabelColor = TextDarkPrimary,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Full Name *") },
                            singleLine = true,
                            enabled = !isSaving,
                            colors = dialogFieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("input_edit_fullname")
                        )
                        if (!isOfficerToEdit) {
                            OutlinedTextField(
                                value = editSchoolName,
                                onValueChange = { editSchoolName = it },
                                label = { Text("School Name *") },
                                singleLine = true,
                                enabled = !isSaving,
                                colors = dialogFieldColors,
                                modifier = Modifier.fillMaxWidth().testTag("input_edit_school_name")
                            )
                            OutlinedTextField(
                                value = editUdiseNumber,
                                onValueChange = { editUdiseNumber = it },
                                label = { Text("UDISE Number *") },
                                singleLine = true,
                                enabled = !isSaving,
                                colors = dialogFieldColors,
                                modifier = Modifier.fillMaxWidth().testTag("input_edit_udise")
                            )
                        }
                        OutlinedTextField(
                            value = usrToEdit.email,
                            onValueChange = {},
                            enabled = false,
                            label = { Text("Email (Auth - Read Only)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = TextSecondary,
                                disabledBorderColor = SurfaceCardBorder,
                                disabledLabelColor = TextMuted,
                                disabledContainerColor = SurfaceWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        editError?.let { err ->
                            Surface(
                                color = Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = err,
                                    fontSize = 12.sp,
                                    color = Color(0xFFC62828),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editName.isBlank()) {
                                editError = "Full Name cannot be empty."
                                return@Button
                            }
                            if (!isOfficerToEdit && editSchoolName.isBlank()) {
                                editError = "School Name cannot be empty."
                                return@Button
                            }
                            isSaving = true
                            editError = null
                            val finalSchoolName = if (isOfficerToEdit) usrToEdit.schoolName else editSchoolName
                            val finalUdiseNumber = if (isOfficerToEdit) usrToEdit.udiseNumber else editUdiseNumber
                            onUpdateUserInfo(
                                usrToEdit.udiseCode,
                                editName,
                                editPhone,
                                usrToEdit.email,
                                finalSchoolName,
                                finalUdiseNumber,
                                {
                                    isSaving = false
                                    editingUser = null
                                },
                                { err ->
                                    isSaving = false
                                    editError = err
                                }
                            )
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen),
                        modifier = Modifier.testTag("btn_save_user_edit")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { editingUser = null },
                        enabled = !isSaving
                    ) { Text("Cancel") }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun AddNewAccountForm(
    onCreateAccount: (role: UserRole, fullName: String, email: String, password: String, confirmPassword: String, schoolName: String, udiseNumber: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onAccountCreated: (UserRole) -> Unit
) {
    val context = LocalContext.current
    var selectedRole by remember { mutableStateOf(UserRole.OFFICER) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Required for School User / HM
    var schoolName by remember { mutableStateOf("") }
    var udiseNumber by remember { mutableStateOf("") }

    var formErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Select User Role *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)

        // TWO LARGE SELECTABLE ROLE CARDS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ROLE 1: OFFICER
            Surface(
                onClick = { selectedRole = UserRole.OFFICER },
                modifier = Modifier.weight(1f).testTag("role_card_officer"),
                shape = RoundedCornerShape(18.dp),
                color = if (selectedRole == UserRole.OFFICER) BentoHeroBg else SurfaceWhite,
                border = BorderStroke(
                    if (selectedRole == UserRole.OFFICER) 2.dp else 1.dp,
                    if (selectedRole == UserRole.OFFICER) ForestDarkGreen else SurfaceCardBorder
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = "Officer Role",
                            tint = ForestDarkGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        RadioButton(
                            selected = selectedRole == UserRole.OFFICER,
                            onClick = { selectedRole = UserRole.OFFICER },
                            colors = RadioButtonDefaults.colors(selectedColor = ForestDarkGreen)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("OFFICER", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ForestDarkGreen)
                    Text("Administrative access", fontSize = 10.sp, color = TextSecondary)
                }
            }

            // ROLE 2: SCHOOL USER / HM
            Surface(
                onClick = { selectedRole = UserRole.HEADMASTER },
                modifier = Modifier.weight(1f).testTag("role_card_school_user"),
                shape = RoundedCornerShape(18.dp),
                color = if (selectedRole == UserRole.HEADMASTER) BentoHeroBg else SurfaceWhite,
                border = BorderStroke(
                    if (selectedRole == UserRole.HEADMASTER) 2.dp else 1.dp,
                    if (selectedRole == UserRole.HEADMASTER) ForestDarkGreen else SurfaceCardBorder
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = "School User Role",
                            tint = ForestDarkGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        RadioButton(
                            selected = selectedRole == UserRole.HEADMASTER,
                            onClick = { selectedRole = UserRole.HEADMASTER },
                            colors = RadioButtonDefaults.colors(selectedColor = ForestDarkGreen)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("SCHOOL USER / HM", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = ForestDarkGreen)
                    Text("School-level access", fontSize = 10.sp, color = TextSecondary)
                }
            }
        }

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextDarkPrimary,
            unfocusedTextColor = TextDarkPrimary,
            focusedBorderColor = ForestDarkGreen,
            unfocusedBorderColor = SurfaceCardBorder,
            focusedLabelColor = ForestDarkGreen,
            unfocusedLabelColor = TextDarkPrimary,
            focusedContainerColor = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite
        )

        // COMMON REQUIRED FIELDS
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it; formErrorMessage = null },
            label = { Text("Full Name *") },
            placeholder = { Text("e.g. Ramesh Kumar") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_reg_fullname"),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; formErrorMessage = null },
            label = { Text("Email Address *") },
            placeholder = { Text("e.g. officer@education.gov.in") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().testTag("input_reg_email"),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; formErrorMessage = null },
            label = { Text("Password * (Min 6 chars)") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = "Toggle password"
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_reg_password"),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; formErrorMessage = null },
            label = { Text("Confirm Password *") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_reg_confirm_password"),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors
        )

        // IF ROLE = SCHOOL USER / HM: SHOW SCHOOL NAME & UDISE NUMBER
        if (selectedRole == UserRole.HEADMASTER) {
            OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it; formErrorMessage = null },
                label = { Text("School Name *") },
                placeholder = { Text("e.g. Govt Higher Secondary School") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_reg_school_name"),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors
            )

            OutlinedTextField(
                value = udiseNumber,
                onValueChange = { udiseNumber = it; formErrorMessage = null },
                label = { Text("UDISE Number *") },
                placeholder = { Text("e.g. 27010100101") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_reg_udise"),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors
            )
        }

        // ERROR DISPLAY
        formErrorMessage?.let { err ->
            Surface(
                color = Color(0xFFFFEBEE),
                border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Error, contentDescription = "Error", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = err, fontSize = 12.sp, color = Color(0xFFB71C1C), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // SUBMIT BUTTON
        Button(
            onClick = {
                val trimmedName = fullName.trim()
                val trimmedEmail = email.trim()
                val trimmedSchool = schoolName.trim()
                val trimmedUdise = udiseNumber.trim()

                if (trimmedName.isBlank()) {
                    formErrorMessage = "Full Name cannot be empty."
                    return@Button
                }
                if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                    formErrorMessage = "Please enter a valid email address."
                    return@Button
                }
                if (password.length < 6) {
                    formErrorMessage = "Password must be at least 6 characters long."
                    return@Button
                }
                if (password != confirmPassword) {
                    formErrorMessage = "Password and Confirm Password do not match."
                    return@Button
                }
                if (selectedRole == UserRole.HEADMASTER) {
                    if (trimmedSchool.isBlank()) {
                        formErrorMessage = "School Name is required for School User / HM registration."
                        return@Button
                    }
                    if (trimmedUdise.isBlank()) {
                        formErrorMessage = "UDISE Number is required for School User / HM registration."
                        return@Button
                    }
                }

                isSubmitting = true
                formErrorMessage = null

                onCreateAccount(
                    selectedRole,
                    trimmedName,
                    trimmedEmail,
                    password,
                    confirmPassword,
                    trimmedSchool,
                    trimmedUdise,
                    {
                        isSubmitting = false
                        fullName = ""
                        email = ""
                        password = ""
                        confirmPassword = ""
                        schoolName = ""
                        udiseNumber = ""
                        Toast.makeText(context, "Account created successfully!", Toast.LENGTH_LONG).show()
                        onAccountCreated(selectedRole)
                    },
                    { err ->
                        isSubmitting = false
                        formErrorMessage = err
                    }
                )
            },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_create_account"),
            colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Creating Account in Supabase...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Filled.PersonAdd, contentDescription = "Create Account", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Account", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OfficerUserCardItem(
    user: UserEntity,
    onEdit: () -> Unit,
    onResetPassword: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BentoHeroBg,
        border = BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.headmasterName.ifBlank { "Officer Account" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = if (user.email.isBlank()) "Email: Not assigned" else user.email,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                Surface(
                    color = Color(0xFF1E88E5),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "OFFICER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onResetPassword,
                    modifier = Modifier.weight(1f).height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reset Password", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(0.8f).height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Edit Profile", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun SchoolUserCardItem(
    user: UserEntity,
    onEdit: () -> Unit,
    onResetPassword: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BentoHeroBg,
        border = BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val displaySchool = if (user.schoolName.isBlank() || user.schoolName == "School Portal" || user.schoolName == "District Office") "School Not Set" else user.schoolName
                    Text(
                        text = displaySchool,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "HM / User: ${user.headmasterName.ifBlank { "Not set" }}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "UDISE: ${if (user.udiseNumber.isBlank()) "Not set" else user.udiseNumber}",
                        fontSize = 11.sp,
                        color = ForestDarkGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Email: ${if (user.email.isBlank()) "Not set" else user.email}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Surface(
                    color = ForestDarkGreen,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "SCHOOL USER / HM",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onResetPassword,
                    modifier = Modifier.weight(1f).height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reset Password", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(0.8f).height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Edit Profile", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
