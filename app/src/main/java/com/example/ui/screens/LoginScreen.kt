package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.UserRole
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLogin: (udiseCode: String, password: String, role: UserRole, onError: (String) -> Unit) -> Unit,
    onRegister: (
        udiseCode: String,
        schoolName: String,
        hmName: String,
        phone: String,
        email: String,
        password: String,
        role: UserRole,
        onError: (String) -> Unit
    ) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.HEADMASTER) }

    // Login Fields
    var udiseInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loginRoleDropdownExpanded by remember { mutableStateOf(false) }
    var regRoleDropdownExpanded by remember { mutableStateOf(false) }

    // Register Fields
    var regUdiseInput by remember { mutableStateOf("") }
    var regSchoolNameInput by remember { mutableStateOf("") }
    var regHmNameInput by remember { mutableStateOf("") }
    var regPhoneInput by remember { mutableStateOf("") }
    var regEmailInput by remember { mutableStateOf("") }
    var regPasswordInput by remember { mutableStateOf("") }
    var regConfirmPasswordInput by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MintBackground)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // TOP BENTO BRANDING HERO CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ForestDarkGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = "EduCollect Logo",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "EduCollect Sync",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDarkPrimary
                )

                Text(
                    text = "School UDISE Data Portal",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(18.dp))

                // TAB SWITCHER: SIGN IN vs REGISTER UDISE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(NavBackground)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (!isRegisterMode) SurfaceWhite else Color.Transparent)
                            .clickable {
                                isRegisterMode = false
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp)
                            .testTag("tab_sign_in"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign In",
                            fontSize = 13.sp,
                            fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isRegisterMode) ForestDarkGreen else TextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isRegisterMode) SurfaceWhite else Color.Transparent)
                            .clickable {
                                isRegisterMode = true
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp)
                            .testTag("tab_register_udise"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Register UDISE",
                            fontSize = 13.sp,
                            fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (isRegisterMode) ForestDarkGreen else TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ERROR ALERT BANNER
        errorMessage?.let { err ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFEBEE),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = "Error",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = err,
                        fontSize = 12.sp,
                        color = Color(0xFFB71C1C),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (!isRegisterMode) {
            // SIGN IN FORM CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "ACCOUNT LOGIN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // ROLE DROPDOWN SELECTOR
                    Text(
                        text = "Select Login Role",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = loginRoleDropdownExpanded,
                        onExpandedChange = { loginRoleDropdownExpanded = !loginRoleDropdownExpanded },
                        modifier = Modifier.fillMaxWidth().testTag("dropdown_login_role")
                    ) {
                        OutlinedTextField(
                            value = if (selectedRole == UserRole.HEADMASTER) "1. Headmaster" else "2. Officer",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Login Role", color = TextDarkPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (selectedRole == UserRole.HEADMASTER) Icons.Filled.School else Icons.Filled.AdminPanelSettings,
                                    contentDescription = "Role Icon",
                                    tint = ForestDarkGreen
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = loginRoleDropdownExpanded) },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextDarkPrimary,
                                unfocusedTextColor = TextDarkPrimary,
                                focusedBorderColor = ForestDarkGreen,
                                unfocusedBorderColor = SurfaceCardBorder,
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = loginRoleDropdownExpanded,
                            onDismissRequest = { loginRoleDropdownExpanded = false },
                            modifier = Modifier.background(SurfaceWhite)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.School, contentDescription = "Headmaster", tint = ForestDarkGreen)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("1. Headmaster", fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                                    }
                                },
                                onClick = {
                                    selectedRole = UserRole.HEADMASTER
                                    loginRoleDropdownExpanded = false
                                },
                                modifier = Modifier.background(SurfaceWhite).testTag("role_option_hm")
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Officer", tint = ForestDarkGreen)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("2. Officer", fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                                    }
                                },
                                onClick = {
                                    selectedRole = UserRole.OFFICER
                                    loginRoleDropdownExpanded = false
                                },
                                modifier = Modifier.background(SurfaceWhite).testTag("role_option_officer")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // UDISE CODE INPUT
                    OutlinedTextField(
                        value = udiseInput,
                        onValueChange = { udiseInput = it.uppercase() },
                        label = { Text("School UDISE Code / Number") },
                        placeholder = { Text("e.g. SCH001 or 27010100101") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Badge,
                                contentDescription = "UDISE",
                                tint = ForestDarkGreen
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_udise_code"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextDarkPrimary,
                            unfocusedTextColor = TextDarkPrimary,
                            focusedBorderColor = ForestDarkGreen,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // PASSWORD INPUT
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Password",
                                tint = ForestDarkGreen
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_password"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextDarkPrimary,
                            unfocusedTextColor = TextDarkPrimary,
                            focusedBorderColor = ForestDarkGreen,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // SIGN IN BUTTON
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            isLoading = true
                            errorMessage = null
                            onLogin(udiseInput, passwordInput, selectedRole) { err ->
                                isLoading = false
                                errorMessage = err
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_submit_login"),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen),
                        shape = RoundedCornerShape(20.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Login,
                                contentDescription = "Sign In"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign In to School Portal",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))


                }
            }
        } else {
            // REGISTER FORM CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    var regRole by remember { mutableStateOf(UserRole.HEADMASTER) }

                    Text(
                        text = "REGISTER NEW USER ACCOUNT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // REGISTRATION ROLE DROPDOWN
                    Text(
                        text = "Select Registration Role",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = regRoleDropdownExpanded,
                        onExpandedChange = { regRoleDropdownExpanded = !regRoleDropdownExpanded },
                        modifier = Modifier.fillMaxWidth().testTag("dropdown_reg_role")
                    ) {
                        OutlinedTextField(
                            value = if (regRole == UserRole.HEADMASTER) "1. Headmaster (School)" else "2. Officer (District)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Registration Role", color = TextDarkPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (regRole == UserRole.HEADMASTER) Icons.Filled.School else Icons.Filled.AdminPanelSettings,
                                    contentDescription = "Role Icon",
                                    tint = ForestDarkGreen
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regRoleDropdownExpanded) },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextDarkPrimary,
                                unfocusedTextColor = TextDarkPrimary,
                                focusedBorderColor = ForestDarkGreen,
                                unfocusedBorderColor = SurfaceCardBorder,
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = regRoleDropdownExpanded,
                            onDismissRequest = { regRoleDropdownExpanded = false },
                            modifier = Modifier.background(SurfaceWhite)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.School, contentDescription = "Headmaster", tint = ForestDarkGreen)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("1. Headmaster (School)", fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                                    }
                                },
                                onClick = {
                                    regRole = UserRole.HEADMASTER
                                    regRoleDropdownExpanded = false
                                },
                                modifier = Modifier.background(SurfaceWhite)
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Officer", tint = ForestDarkGreen)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("2. Officer (District)", fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                                    }
                                },
                                onClick = {
                                    regRole = UserRole.OFFICER
                                    regRoleDropdownExpanded = false
                                },
                                modifier = Modifier.background(SurfaceWhite)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // SCHOOL UDISE CODE
                    OutlinedTextField(
                        value = regUdiseInput,
                        onValueChange = {
                            regUdiseInput = it.uppercase()
                            if (regSchoolNameInput.isBlank() && regUdiseInput.length >= 4) {
                                regSchoolNameInput = "School UDISE " + regUdiseInput
                            }
                        },
                        label = { Text("School UDISE Code *") },
                        placeholder = { Text("e.g. 27010100101") },
                        leadingIcon = {
                            Icon(Icons.Filled.Badge, contentDescription = "UDISE", tint = ForestDarkGreen)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reg_udise"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
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

                    // SCHOOL NAME
                    OutlinedTextField(
                        value = regSchoolNameInput,
                        onValueChange = { regSchoolNameInput = it },
                        label = { Text("Full School Name *") },
                        placeholder = { Text("e.g. Govt. Model Secondary School") },
                        leadingIcon = {
                            Icon(Icons.Filled.School, contentDescription = "School", tint = ForestDarkGreen)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reg_school_name"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
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

                    // HEADMASTER / REPRESENTATIVE NAME
                    OutlinedTextField(
                        value = regHmNameInput,
                        onValueChange = { regHmNameInput = it },
                        label = { Text("Headmaster / Coordinator Name *") },
                        placeholder = { Text("e.g. Dr. Ramesh Kumar") },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = "Headmaster", tint = ForestDarkGreen)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reg_hm_name"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
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

                    // PHONE NUMBER
                    OutlinedTextField(
                        value = regPhoneInput,
                        onValueChange = { regPhoneInput = it },
                        label = { Text("Contact Mobile Number") },
                        placeholder = { Text("e.g. 9876543210") },
                        leadingIcon = {
                            Icon(Icons.Filled.Phone, contentDescription = "Phone", tint = ForestDarkGreen)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reg_phone"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
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

                    // GMAIL / EMAIL ADDRESS
                    OutlinedTextField(
                        value = regEmailInput,
                        onValueChange = { regEmailInput = it },
                        label = { Text("School Gmail / Email Address *") },
                        placeholder = { Text("e.g. school.sch001@gmail.com") },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = "Email", tint = ForestDarkGreen)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reg_email"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
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

                    // PASSWORD
                    OutlinedTextField(
                        value = regPasswordInput,
                        onValueChange = { regPasswordInput = it },
                        label = { Text("Create Password *") },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = "Password", tint = ForestDarkGreen)
                        },
                        trailingIcon = {
                            IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                Icon(
                                    imageVector = if (regPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Toggle password"
                                )
                            }
                        },
                        visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reg_password"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
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

                    // CONFIRM PASSWORD
                    OutlinedTextField(
                        value = regConfirmPasswordInput,
                        onValueChange = { regConfirmPasswordInput = it },
                        label = { Text("Confirm Password *") },
                        leadingIcon = {
                            Icon(Icons.Filled.LockReset, contentDescription = "Confirm", tint = ForestDarkGreen)
                        },
                        visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reg_confirm_password"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextDarkPrimary,
                            unfocusedTextColor = TextDarkPrimary,
                            focusedBorderColor = ForestDarkGreen,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // REGISTER BUTTON
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (regPasswordInput != regConfirmPasswordInput) {
                                errorMessage = "Passwords do not match."
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            onRegister(
                                regUdiseInput,
                                regSchoolNameInput,
                                regHmNameInput,
                                regPhoneInput,
                                regEmailInput,
                                regPasswordInput,
                                regRole
                            ) { err ->
                                isLoading = false
                                errorMessage = err
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_submit_register"),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen),
                        shape = RoundedCornerShape(20.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AppRegistration,
                                contentDescription = "Register"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Register School UDISE & Enter",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // FOOTER INFO
        Text(
            text = "Official Education Data Sync Portal v1.0",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted
        )
    }
}
