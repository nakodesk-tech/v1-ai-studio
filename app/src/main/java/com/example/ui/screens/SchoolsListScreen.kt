package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SchoolEntity
import com.example.ui.theme.*

@Composable
fun SchoolsListScreen(
    schools: List<SchoolEntity>,
    onUpdateSchoolContact: (schoolId: String, phone: String, email: String) -> Unit,
    onResetUserPassword: (schoolId: String) -> Unit = {},
    onDeleteSchool: (schoolId: String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingSchool by remember { mutableStateOf<SchoolEntity?>(null) }
    var schoolToDelete by remember { mutableStateOf<SchoolEntity?>(null) }

    val context = LocalContext.current

    val filtered = remember(schools, searchQuery) {
        if (searchQuery.isBlank()) schools
        else schools.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.id.contains(searchQuery, ignoreCase = true) ||
            it.district.contains(searchQuery, ignoreCase = true) ||
            it.headmasterName.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true)
        }
    }

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
                        text = "OFFICER SCHOOL DIRECTORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLightSubtle,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Registered Schools Info (${schools.size})",
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_schools"),
                placeholder = { Text("Search school name, UDISE, HM, email...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForestDarkGreen,
                    unfocusedBorderColor = SurfaceCardBorder,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filtered) { school ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // HEADING FOR SCHOOL NAME
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = school.name,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDarkPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "UDISE Code: ${school.id}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ForestDarkGreen
                                    )
                                }

                                Surface(
                                    color = BentoHeroBg,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = school.category.ifBlank { "Secondary" },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestDarkGreen,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // DECENT SAME COLOR BACKGROUND FOR SCHOOL INFO (STRUCTURED INFORMATION CARD)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BentoHeroBg)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "HM",
                                        tint = ForestDarkGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Headmaster / HM: ${school.headmasterName.ifBlank { "Not Assigned" }}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
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
                                        text = "Mobile: ${school.headmasterPhone.ifBlank { "N/A" }}",
                                        fontSize = 12.sp,
                                        color = TextDarkPrimary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = "Email",
                                        tint = ForestDarkGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Gmail / Email: ${school.email.ifBlank { "Not provided" }}",
                                        fontSize = 12.sp,
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
                                        text = "District: ${school.district.ifBlank { "Central District" }}",
                                        fontSize = 12.sp,
                                        color = TextDarkPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // ROW 1: COMMUNICATION SHORTCUTS (WHATSAPP & GMAIL)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. WHATSAPP SHORTCUT
                                Button(
                                    onClick = {
                                        val cleanPhone = school.headmasterPhone.filter { it.isDigit() }
                                        val phoneToUse = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$phoneToUse"))
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phoneToUse"))
                                            context.startActivity(webIntent)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_whatsapp_${school.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Chat,
                                        contentDescription = "WhatsApp",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                // 2. GMAIL SHORTCUT
                                Button(
                                    onClick = {
                                        val mailTo = school.email.ifBlank { "school.${school.id.lowercase()}@gmail.com" }
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$mailTo"))
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Ignore if no mail app available
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_gmail_${school.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = "Gmail",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gmail", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // ROW 2: MANAGEMENT SHORTCUTS (EDIT & RESET PASSWORD)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 3. EDIT OPTION FOR CONTACT SHORTCUTS
                                OutlinedButton(
                                    onClick = { editingSchool = school },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_edit_contact_${school.id}"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestDarkGreen),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ForestDarkGreen),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit Contact",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Contact", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // 4. RESET PASSWORD BUTTON
                                Button(
                                    onClick = {
                                        onResetUserPassword(school.id)
                                        android.widget.Toast.makeText(context, "Password for ${school.id} reset to Pass@123", android.widget.Toast.LENGTH_LONG).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_reset_pass_${school.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LockReset,
                                        contentDescription = "Reset Password",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset Pass@123", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 5. DELETE SCHOOL BUTTON
                            Button(
                                onClick = { schoolToDelete = school },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("btn_delete_school_${school.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete School",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete School Record", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // WARNING CONFIRMATION DIALOG FOR DELETING A SCHOOL
    schoolToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { schoolToDelete = null },
            icon = {
                Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = Color(0xFFDC2626), modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Delete School Warning", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDarkPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to delete school '${target.name}' (UDISE Code: ${target.id})?\n\nThis will permanently remove the school profile and its registered login account. This action cannot be undone.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSchool(target.id)
                        schoolToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Confirm Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { schoolToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // EDIT CONTACT SHORTCUTS DIALOG FOR OFFICER
    editingSchool?.let { sch ->
        var editPhone by remember(sch) { mutableStateOf(sch.headmasterPhone) }
        var editEmail by remember(sch) { mutableStateOf(sch.email) }

        AlertDialog(
            onDismissRequest = { editingSchool = null },
            title = {
                Text(
                    text = "Edit Social Shortcuts Contact",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestDarkGreen
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Update mobile number and Gmail address for ${sch.name} (${sch.id}).",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("WhatsApp Mobile Number") },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = "Phone") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Gmail / School Email") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateSchoolContact(sch.id, editPhone.trim(), editEmail.trim())
                        editingSchool = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSchool = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}
