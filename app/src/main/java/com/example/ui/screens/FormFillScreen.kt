package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FormWithFields
import com.example.data.model.SchoolEntity
import com.example.data.model.UserEntity
import com.example.ui.UserRole
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormFillScreen(
    formWithFields: FormWithFields?,
    schools: List<SchoolEntity>,
    selectedSchool: SchoolEntity?,
    role: UserRole = UserRole.HEADMASTER,
    currentUser: UserEntity? = null,
    onSelectSchool: (SchoolEntity) -> Unit,
    onSubmit: (Map<String, Pair<String, String>>) -> Unit,
    onBack: () -> Unit
) {
    if (formWithFields == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ForestDarkGreen)
        }
        return
    }

    val form = formWithFields.form
    val fields = formWithFields.fields

    // State map: fieldId -> String value
    val valuesMap = remember { mutableStateMapOf<String, String>() }
    var showSchoolDropdown by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(form.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ForestDarkGreen)
            )
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
            // FORM DESCRIPTION CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SoftGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Description, contentDescription = "Form", tint = ForestDarkGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Official Data Collection Form", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForestDarkGreen)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(form.description, fontSize = 12.sp, color = TextSecondary)
                }
            }

            // SCHOOL INFO CARD (Dropdown for Officer, Static for HM)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Filling Data For School:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (role == UserRole.HEADMASTER) {
                        val displaySchoolName = currentUser?.schoolName?.ifBlank { null } ?: selectedSchool?.name ?: "Registered School"
                        val displayUdise = currentUser?.udiseCode?.ifBlank { null } ?: selectedSchool?.id ?: "N/A"
                        val displayHm = currentUser?.headmasterName?.ifBlank { null } ?: selectedSchool?.headmasterName ?: "Headmaster"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SoftGreenBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.School, contentDescription = "School", tint = ForestDarkGreen, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = displaySchoolName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )
                                Text(
                                    text = "UDISE: $displayUdise | HM: $displayHm",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MintBackground)
                                .clickable { showSchoolDropdown = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = selectedSchool?.name ?: "Select School",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDarkPrimary
                                    )
                                    Text(
                                        text = "UDISE: ${selectedSchool?.id} | HM: ${selectedSchool?.headmasterName}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown", tint = ForestDarkGreen)
                            }

                            DropdownMenu(
                                expanded = showSchoolDropdown,
                                onDismissRequest = { showSchoolDropdown = false }
                            ) {
                                schools.forEach { school ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(school.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("UDISE: ${school.id}", fontSize = 11.sp, color = TextMuted)
                                            }
                                        },
                                        onClick = {
                                            onSelectSchool(school)
                                            showSchoolDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // DYNAMIC FORM FIELDS
            Text("Form Questions & Data Fields", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)

            fields.forEach { field ->
                val currentValue = valuesMap[field.id] ?: ""

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = field.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (field.isRequired) {
                                Text("*Required", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        when (field.fieldType) {
                            "NUMBER" -> {
                                OutlinedTextField(
                                    value = currentValue,
                                    onValueChange = { valuesMap[field.id] = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("field_${field.id}"),
                                    placeholder = { Text("Enter number...") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            }
                            "DROPDOWN" -> {
                                val options = field.options.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                var expanded by remember { mutableStateOf(false) }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MintBackground)
                                        .clickable { expanded = true }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (currentValue.isNotBlank()) currentValue else "Select option...",
                                            fontSize = 14.sp,
                                            color = if (currentValue.isNotBlank()) TextDarkPrimary else TextMuted
                                        )
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select")
                                    }

                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        options.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt) },
                                                onClick = {
                                                    valuesMap[field.id] = opt
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            "CHECKBOX" -> {
                                val checked = currentValue == "Yes"
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            valuesMap[field.id] = if (checked) "No" else "Yes"
                                        }
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { isChecked ->
                                            valuesMap[field.id] = if (isChecked) "Yes" else "No"
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = ForestDarkGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Yes, verified & completed", fontSize = 14.sp)
                                }
                            }
                            else -> { // TEXT / DATE
                                OutlinedTextField(
                                    value = currentValue,
                                    onValueChange = { valuesMap[field.id] = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("field_${field.id}"),
                                    placeholder = { Text("Enter details...") },
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
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SUBMIT BUTTON
            Button(
                onClick = {
                    isSubmitting = true
                    val resultMap = mutableMapOf<String, Pair<String, String>>()
                    fields.forEach { f ->
                        val valStr = valuesMap[f.id] ?: if (f.fieldType == "CHECKBOX") "No" else "N/A"
                        resultMap[f.id] = Pair(f.label, valStr)
                    }
                    onSubmit(resultMap)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_submit_form_data"),
                colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen),
                shape = RoundedCornerShape(14.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Submitting...")
                } else {
                    Icon(Icons.Filled.Check, contentDescription = "Submit")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
