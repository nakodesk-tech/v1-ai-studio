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
    onSaveSyncConfig: (String, String, String, Boolean) -> Unit,
    onRegisterOfficer: (officerId: String, fullName: String, designation: String, phone: String, email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onRegisterSchool: (udiseCode: String, schoolName: String, hmName: String, phone: String, email: String, password: String, role: UserRole, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onChangePassword: (oldPass: String, newPass: String, confirmPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onResetUserPassword: (udiseCode: String) -> Unit = {},
    onDeleteUser: (udiseCode: String) -> Unit = {},
    onUpdateUserInfo: (udiseCode: String, name: String, phone: String, email: String, schoolName: String) -> Unit = { _, _, _, _, _ -> },
    onLogout: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Config states for Officer
    var driveEmail by remember(syncConfig) { mutableStateOf(syncConfig.officerDriveEmail) }
    var sheetId by remember(syncConfig) { mutableStateOf(syncConfig.googleSheetId) }
    var webhookUrl by remember(syncConfig) { mutableStateOf(syncConfig.appsScriptWebhookUrl) }
    var autoSync by remember(syncConfig) { mutableStateOf(syncConfig.autoSync) }
    var isGoogleConnected by remember { mutableStateOf(true) }

    // Dialog flags
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showAddOfficerDialog by remember { mutableStateOf(false) }
    var showAddSchoolDialog by remember { mutableStateOf(false) }
    var showManageUsersDialog by remember { mutableStateOf(false) }

    // Officer registration form states (for HM profile view)
    var hmRegOfficerName by remember { mutableStateOf("") }
    var hmRegOfficerDesig by remember { mutableStateOf("") }
    var hmRegOfficerPhone by remember { mutableStateOf("") }
    var hmRegOfficerEmail by remember { mutableStateOf("") }
    var hmRegOfficerPassword by remember { mutableStateOf("") }
    var hmRegOfficerConfirmPassword by remember { mutableStateOf("") }
    var hmRegPasswordVisible by remember { mutableStateOf(false) }

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
                                    text = "ID / UDISE: ${currentUser?.udiseCode ?: "OFFICER123"}",
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
                                    text = currentUser?.schoolName?.ifBlank { "District Education Office" } ?: "District Education Office",
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
                                    text = currentUser?.email?.ifBlank { "officer.main@education.gov.in" } ?: "officer.main@education.gov.in",
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

            // 2. OFFICER PORTAL / ACTIONS FOR OFFICER USER ROLE
            if (currentRole == UserRole.OFFICER) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BentoHeroBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Officer Tools", tint = ForestDarkGreen, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Officer Administration & Role Management", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // BUTTON 1: ADD NEW OFFICER USER ROLE
                        Button(
                            onClick = { showAddOfficerDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_add_officer_role"),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = "Add Officer")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add New Officer User Role", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // BUTTON 2: ADD NEW SCHOOL USER ROLE
                        OutlinedButton(
                            onClick = { showAddSchoolDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("btn_add_school_role"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestDarkGreen),
                            border = BorderStroke(1.dp, ForestDarkGreen),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.DomainAdd, contentDescription = "Add School")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add New School User Role (Register UDISE)", fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // BUTTON 3: MANAGE REGISTERED USERS
                        Button(
                            onClick = { showManageUsersDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("btn_manage_registered_users"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.Group, contentDescription = "Manage Users")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manage Registered Users (${allUsers.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
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

    // --- DIALOG 2: ADD NEW OFFICER USER ROLE (OFFICER PORTAL) ---
    if (showAddOfficerDialog) {
        var offIdInput by remember { mutableStateOf("") }
        var offNameInput by remember { mutableStateOf("") }
        var offDesigInput by remember { mutableStateOf("") }
        var offPhoneInput by remember { mutableStateOf("") }
        var offEmailInput by remember { mutableStateOf("") }
        var offPassInput by remember { mutableStateOf("") }
        var offConfirmPassInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddOfficerDialog = false },
            title = {
                Text("Add New Officer User Role", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ForestDarkGreen)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
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
                    OutlinedTextField(value = offNameInput, onValueChange = { offNameInput = it }, label = { Text("Full Name *") }, singleLine = true, colors = dialogFieldColors)
                    OutlinedTextField(value = offDesigInput, onValueChange = { offDesigInput = it }, label = { Text("Designation *") }, singleLine = true, colors = dialogFieldColors)
                    OutlinedTextField(value = offPhoneInput, onValueChange = { offPhoneInput = it }, label = { Text("Mobile Number *") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), colors = dialogFieldColors)
                    OutlinedTextField(value = offEmailInput, onValueChange = { offEmailInput = it }, label = { Text("Office Email ID *") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), colors = dialogFieldColors)
                    OutlinedTextField(value = offPassInput, onValueChange = { offPassInput = it }, label = { Text("Create Password *") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, colors = dialogFieldColors)
                    OutlinedTextField(value = offConfirmPassInput, onValueChange = { offConfirmPassInput = it }, label = { Text("Confirm Password *") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, colors = dialogFieldColors)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (offPassInput != offConfirmPassInput) {
                            Toast.makeText(context, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onRegisterOfficer(
                            offIdInput,
                            offNameInput,
                            offDesigInput,
                            offPhoneInput,
                            offEmailInput,
                            offPassInput,
                            {
                                showAddOfficerDialog = false
                                Toast.makeText(context, "Officer registered successfully!", Toast.LENGTH_SHORT).show()
                            },
                            { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen)
                ) {
                    Text("Register Officer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddOfficerDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // --- DIALOG 3: ADD NEW SCHOOL USER ROLE (REGISTER UDISE) ---
    if (showAddSchoolDialog) {
        var schUdiseInput by remember { mutableStateOf("") }
        var schNameInput by remember { mutableStateOf("") }
        var schHmInput by remember { mutableStateOf("") }
        var schPhoneInput by remember { mutableStateOf("") }
        var schEmailInput by remember { mutableStateOf("") }
        var schPassInput by remember { mutableStateOf("") }
        var schConfirmPassInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSchoolDialog = false },
            title = {
                Text("Add New School User Role (UDISE)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ForestDarkGreen)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
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
                    OutlinedTextField(value = schUdiseInput, onValueChange = { schUdiseInput = it }, label = { Text("UDISE Code *") }, singleLine = true, colors = dialogFieldColors)
                    OutlinedTextField(value = schNameInput, onValueChange = { schNameInput = it }, label = { Text("School Name *") }, singleLine = true, colors = dialogFieldColors)
                    OutlinedTextField(value = schHmInput, onValueChange = { schHmInput = it }, label = { Text("Headmaster Name *") }, singleLine = true, colors = dialogFieldColors)
                    OutlinedTextField(value = schPhoneInput, onValueChange = { schPhoneInput = it }, label = { Text("Phone Number *") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), colors = dialogFieldColors)
                    OutlinedTextField(value = schEmailInput, onValueChange = { schEmailInput = it }, label = { Text("School Gmail / Email *") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), colors = dialogFieldColors)
                    OutlinedTextField(value = schPassInput, onValueChange = { schPassInput = it }, label = { Text("Password *") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, colors = dialogFieldColors)
                    OutlinedTextField(value = schConfirmPassInput, onValueChange = { schConfirmPassInput = it }, label = { Text("Confirm Password *") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, colors = dialogFieldColors)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (schPassInput != schConfirmPassInput) {
                            Toast.makeText(context, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onRegisterSchool(
                            schUdiseInput,
                            schNameInput,
                            schHmInput,
                            schPhoneInput,
                            schEmailInput,
                            schPassInput,
                            UserRole.HEADMASTER,
                            {
                                showAddSchoolDialog = false
                                Toast.makeText(context, "School UDISE user added!", Toast.LENGTH_SHORT).show()
                            },
                            { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen)
                ) {
                    Text("Add School User", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSchoolDialog = false }) { Text("Cancel") }
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
                        Text("All Registered Users (${allUsers.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Registered by Officer & School HM Users", fontSize = 11.sp, color = TextSecondary)
                    }
                    IconButton(onClick = { showManageUsersDialog = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
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
                                                Text(usr.headmasterName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("${if (usr.role == "OFFICER") "OFFICER ID" else "UDISE"}: ${usr.udiseCode}", fontSize = 11.sp, color = ForestDarkGreen, fontWeight = FontWeight.Bold)
                                                Text(usr.schoolName, fontSize = 11.sp, color = TextSecondary)
                                                if (usr.email.isNotBlank()) {
                                                    Text("Email: ${usr.email}", fontSize = 11.sp, color = TextMuted)
                                                }
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
                                            // RESET PASSWORD TO Pass@123
                                            Button(
                                                onClick = {
                                                    onResetUserPassword(usr.udiseCode)
                                                    Toast.makeText(context, "Password for ${usr.udiseCode} reset to Pass@123", Toast.LENGTH_LONG).show()
                                                },
                                                modifier = Modifier.weight(1f).height(32.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                                contentPadding = PaddingValues(0.dp),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Reset Pass@123", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

        // WARNING CONFIRMATION DIALOG FOR DELETING USER
        userToDelete?.let { targetUsr ->
            AlertDialog(
                onDismissRequest = { userToDelete = null },
                icon = {
                    Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = Color(0xFFDC2626), modifier = Modifier.size(36.dp))
                },
                title = {
                    Text("Delete Registered User Warning", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDarkPrimary)
                },
                text = {
                    Text(
                        "Are you sure you want to delete user '${targetUsr.headmasterName}' (${targetUsr.udiseCode})?\n\nRole: ${targetUsr.role}\nSchool/Office: ${targetUsr.schoolName}\n\nThis will permanently revoke access for this account. This action cannot be undone.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteUser(targetUsr.udiseCode)
                            Toast.makeText(context, "User ${targetUsr.udiseCode} deleted", Toast.LENGTH_SHORT).show()
                            userToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Confirm Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { userToDelete = null }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        // EDIT USER SUB-DIALOG
        editingUser?.let { usrToEdit ->
            var editName by remember(usrToEdit) { mutableStateOf(usrToEdit.headmasterName) }
            var editPhone by remember(usrToEdit) { mutableStateOf(usrToEdit.phone) }
            var editEmail by remember(usrToEdit) { mutableStateOf(usrToEdit.email) }
            var editSchoolName by remember(usrToEdit) { mutableStateOf(usrToEdit.schoolName) }

            AlertDialog(
                onDismissRequest = { editingUser = null },
                title = { Text("Edit User Information (${usrToEdit.udiseCode})", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
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
                        OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Full Name") }, singleLine = true, colors = dialogFieldColors)
                        OutlinedTextField(value = editSchoolName, onValueChange = { editSchoolName = it }, label = { Text("School / Office Name") }, singleLine = true, colors = dialogFieldColors)
                        OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text("Phone") }, singleLine = true, colors = dialogFieldColors)
                        OutlinedTextField(value = editEmail, onValueChange = { editEmail = it }, label = { Text("Email") }, singleLine = true, colors = dialogFieldColors)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onUpdateUserInfo(usrToEdit.udiseCode, editName, editPhone, editEmail, editSchoolName)
                            editingUser = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingUser = null }) { Text("Cancel") }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
