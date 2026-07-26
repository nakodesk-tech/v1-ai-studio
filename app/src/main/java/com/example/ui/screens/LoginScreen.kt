package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.UserRole
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLogin: (email: String, password: String, onError: (String) -> Unit) -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextDarkPrimary,
        unfocusedTextColor = TextDarkPrimary,
        focusedBorderColor = ForestDarkGreen,
        unfocusedBorderColor = SurfaceCardBorder,
        focusedContainerColor = SurfaceWhite,
        unfocusedContainerColor = SurfaceWhite,
        focusedLabelColor = ForestDarkGreen,
        unfocusedLabelColor = TextMuted,
        cursorColor = ForestDarkGreen
    )

    val inputTextStyle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = TextDarkPrimary
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MintBackground)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // HEADER BRANDING CARD
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
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ForestDarkGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = "EduData Sync Logo",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "EduData Sync",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Official Education Portal • Supabase Auth",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ERROR BANNER
        errorMessage?.let { err ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFF0F0),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = "Error Icon",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = err,
                        fontSize = 13.sp,
                        color = Color(0xFFB71C1C),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // LOGIN FORM CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = "PORTAL LOGIN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMuted,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // FIELD 1: EMAIL ID
                Text(
                    text = "Email ID *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    placeholder = { Text("Enter your registered email address", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = "Email Icon",
                            tint = ForestDarkGreen
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_login_email"),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    textStyle = inputTextStyle
                )

                Spacer(modifier = Modifier.height(16.dp))

                // FIELD 2: PASSWORD (WITH VISIBILITY TOGGLE EYE ICON)
                Text(
                    text = "Password *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    placeholder = { Text("Enter your password", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Password Icon",
                            tint = ForestDarkGreen
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                                tint = ForestDarkGreen
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_password"),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    textStyle = inputTextStyle
                )

                Spacer(modifier = Modifier.height(24.dp))

                // LOGIN BUTTON
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (emailInput.isBlank()) {
                            errorMessage = "Please enter your Email ID."
                            return@Button
                        }
                        if (passwordInput.isBlank()) {
                            errorMessage = "Please enter your Password."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        onLogin(emailInput.trim(), passwordInput) { err ->
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
                            contentDescription = "Sign In Button Icon"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Official Education Data Sync Portal • Supabase Secured",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted
        )
    }
}
