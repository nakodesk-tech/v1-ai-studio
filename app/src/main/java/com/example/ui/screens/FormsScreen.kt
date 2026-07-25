package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FormEntity
import com.example.data.model.FormFieldEntity
import com.example.data.model.SubmissionEntity
import com.example.data.model.UserEntity
import com.example.ui.UserRole
import com.example.ui.theme.*

@Composable
fun FormsScreen(
    role: UserRole,
    forms: List<FormEntity>,
    submissions: List<SubmissionEntity> = emptyList(),
    currentUser: UserEntity? = null,
    selectedSubTab: Int = 0,
    onSubTabSelected: (Int) -> Unit = {},
    onSelectFormToFill: (String) -> Unit,
    onExportFormPdf: (String) -> Unit,
    onExportFormExcel: (String) -> Unit,
    onCreateNewForm: (String, String, String, List<FormFieldEntity>) -> Unit,
    onImportFromExcel: (String, String, String, String) -> Unit,
    onDeleteSubmission: (formId: String, schoolId: String) -> Unit = { _, _ -> },
    onDeletePublishedForm: (formId: String) -> Unit = {}
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showExcelImportDialog by remember { mutableStateOf(false) }
    var showGoogleSheetsDialog by remember { mutableStateOf(false) }
    var submissionToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var publishedFormToDelete by remember { mutableStateOf<FormEntity?>(null) }

    val subTabTitles = listOf("Published Forms", "Filled Forms", "Pending Forms", "Returned for Corrections")

    val userUdise = currentUser?.udiseCode?.trim() ?: ""
    val userSubmissions = remember(submissions, userUdise, role) {
        if (role == UserRole.HEADMASTER && userUdise.isNotBlank()) {
            submissions.filter { it.schoolId.equals(userUdise, ignoreCase = true) }
        } else {
            submissions
        }
    }
    val filledFormIds = remember(userSubmissions) {
        userSubmissions.map { it.formId }.toSet()
    }

    val displayForms = remember(forms, filledFormIds, selectedSubTab, role) {
        when (selectedSubTab) {
            0 -> {
                // Published Forms: For HM, forms not yet filled by HM. For Officer, all forms.
                if (role == UserRole.HEADMASTER) {
                    forms.filter { !filledFormIds.contains(it.id) }
                } else {
                    forms
                }
            }
            1 -> {
                // Filled Forms: Forms that have been filled by HM
                forms.filter { filledFormIds.contains(it.id) }
            }
            2 -> {
                // Pending Forms: Forms not yet filled
                forms.filter { !filledFormIds.contains(it.id) }
            }
            3 -> {
                // Returned for Corrections: Forms with status RETURNED / CORRECTION / REVISION
                forms.filter {
                    it.status.contains("RETURN", ignoreCase = true) ||
                    it.status.contains("REVISION", ignoreCase = true) ||
                    it.status.contains("CORRECTION", ignoreCase = true)
                }
            }
            else -> forms
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DATA COLLECTION FORMS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLightSubtle,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (role == UserRole.HEADMASTER) "Published Officer Forms" else "Officer Form Manager",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        val officerName = forms.firstOrNull()?.createdBy?.ifBlank { null } ?: "Officer Main"
                        Text(
                            text = "Published by $officerName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    if (role == UserRole.OFFICER) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // GOOGLE SHEETS IN-APP WINDOW BUTTON
                            IconButton(
                                onClick = { showGoogleSheetsDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .testTag("btn_open_google_sheets")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInBrowser,
                                    contentDescription = "Google Sheets Window",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { showExcelImportDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .testTag("btn_import_excel")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TableChart,
                                    contentDescription = "Excel Import",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .testTag("btn_create_form")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Create Form",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = MintBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedSubTab,
                edgePadding = 12.dp,
                containerColor = SurfaceWhite,
                contentColor = ForestDarkGreen,
                indicator = { tabPositions ->
                    if (selectedSubTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                            color = ForestDarkGreen
                        )
                    }
                }
            ) {
                subTabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSubTab == index,
                        onClick = { onSubTabSelected(index) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (selectedSubTab == index) ForestDarkGreen else TextSecondary
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Excel Import Prompt Card for Officers in Published tab
                if (role == UserRole.OFFICER && selectedSubTab == 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = BentoHeroBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForestDarkGreen.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.8f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CloudUpload,
                                        contentDescription = "Upload Excel",
                                        tint = ForestDarkGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Upload Excel Sheet to Auto-Create Form",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestDarkGreen
                                    )
                                    Text(
                                        text = "Import headers from Excel/CSV to build forms instantly for HMs.",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                Button(
                                    onClick = { showExcelImportDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Import", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (displayForms.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(BentoHeroBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (selectedSubTab) {
                                            0 -> Icons.Filled.AssignmentTurnedIn
                                            1 -> Icons.Filled.Inbox
                                            2 -> Icons.Filled.CheckCircle
                                            else -> Icons.Filled.Verified
                                        },
                                        contentDescription = "Empty",
                                        tint = ForestDarkGreen,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = when (selectedSubTab) {
                                        0 -> "No Published Forms Available"
                                        1 -> "No Filled Forms Yet"
                                        2 -> "All Forms Completed! No Pending Forms"
                                        else -> "No Forms Returned for Corrections"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (selectedSubTab) {
                                        0 -> "Check back when officers publish new data collection forms."
                                        1 -> "Forms filled by your school will move here and can be edited anytime."
                                        2 -> "Your school has submitted all published officer forms."
                                        else -> "There are currently no forms requiring corrections."
                                    },
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(displayForms) { form ->
                        val isFilled = filledFormIds.contains(form.id)
                        val submission = userSubmissions.firstOrNull { it.formId == form.id }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = form.title,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDarkPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = form.description,
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            maxLines = 2
                                        )
                                    }

                                    Surface(
                                        color = if (isFilled) BentoHeroBg else BentoContainerPurple,
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            text = if (isFilled) "FILLED & SYNCED" else form.status,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isFilled) ForestDarkGreen else SoftPurpleIcon,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Author",
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Created by: ${form.createdBy}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                    if (submission != null) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Schedule,
                                            contentDescription = "Date",
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(submission.submittedAt))
                                        Text(
                                            text = "Submitted: $dateStr",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Divider(color = SurfaceCardBorder)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { onExportFormPdf(form.id) },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PictureAsPdf,
                                                contentDescription = "PDF",
                                                modifier = Modifier.size(14.dp),
                                                tint = ForestDarkGreen
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("PDF", fontSize = 11.sp, color = ForestDarkGreen)
                                        }

                                        OutlinedButton(
                                            onClick = { onExportFormExcel(form.id) },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.TableChart,
                                                contentDescription = "Excel",
                                                modifier = Modifier.size(14.dp),
                                                tint = ForestDarkGreen
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Excel", fontSize = 11.sp, color = ForestDarkGreen)
                                        }
                                    }

                                    Button(
                                        onClick = { onSelectFormToFill(form.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFilled) Icons.Filled.EditNote else Icons.Filled.Edit,
                                            contentDescription = "Fill",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (selectedSubTab == 1 || isFilled) "Edit Submission" else "Fill Form",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                 if (isFilled || submission != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = {
                                            val schoolId = submission?.schoolId ?: currentUser?.udiseCode ?: ""
                                            submissionToDelete = Pair(form.id, schoolId)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete Submission",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Delete and Move to Pending Forms",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }

                                // OFFICER ONLY: DELETE PUBLISHED FORM ENTIRELY
                                if (role == UserRole.OFFICER) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { publishedFormToDelete = form },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("btn_delete_published_form_${form.id}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteForever,
                                            contentDescription = "Delete Form Everywhere",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Delete Published Form Everywhere",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // WARNING CONFIRMATION DIALOG FOR DELETING SUBMISSION
    submissionToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { submissionToDelete = null },
            icon = {
                Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = Color(0xFFDC2626), modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Delete Form Submission Warning", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDarkPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to delete this form submission?\n\nThis will remove the recorded form data and move the form back to Pending Forms. This action cannot be undone.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSubmission(target.first, target.second)
                        onSubTabSelected(2)
                        submissionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Confirm Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { submissionToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // WARNING CONFIRMATION DIALOG FOR DELETING PUBLISHED FORM EVERYWHERE
    publishedFormToDelete?.let { targetForm ->
        AlertDialog(
            onDismissRequest = { publishedFormToDelete = null },
            icon = {
                Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = Color(0xFFB91C1C), modifier = Modifier.size(38.dp))
            },
            title = {
                Text("Delete Published Form Warning", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDarkPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to delete '${targetForm.title}'?\n\nThis action will PERMANENTLY REMOVE this form and all school submissions from:\n1. System App & Room Database\n2. All School Headmaster Portal Lists\n3. Google Drive & Google Sheets Storage\n\nThis action CANNOT be undone.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePublishedForm(targetForm.id)
                        publishedFormToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))
                ) {
                    Text("Confirm Delete Everywhere", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { publishedFormToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showCreateDialog) {
        CreateFormDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, desc, createdBy, fields ->
                onCreateNewForm(title, desc, createdBy, fields)
                showCreateDialog = false
            }
        )
    }

    if (showExcelImportDialog) {
        ExcelImportDialog(
            onDismiss = { showExcelImportDialog = false },
            onImport = { title, desc, createdBy, headersCsv ->
                onImportFromExcel(title, desc, createdBy, headersCsv)
                showExcelImportDialog = false
            },
            onOpenGoogleSheetsWindow = {
                showExcelImportDialog = false
                showGoogleSheetsDialog = true
            }
        )
    }

    if (showGoogleSheetsDialog) {
        GoogleSheetsWebDialog(
            onDismiss = { showGoogleSheetsDialog = false }
        )
    }
}

@Composable
fun CreateFormDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, List<FormFieldEntity>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var createdBy by remember { mutableStateOf("Education Officer") }

    var fieldsList by remember {
        mutableStateOf(
            listOf(
                FormFieldEntity("1", "", "School UDISE & Code", "TEXT", "", true, 0),
                FormFieldEntity("2", "", "Total Students Present", "NUMBER", "", true, 1)
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New School Form", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Form Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary
                    )
                )

                Text("Form Fields (${fieldsList.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                fieldsList.forEachIndexed { index, field ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = field.label,
                            onValueChange = { newLabel ->
                                fieldsList = fieldsList.toMutableList().apply {
                                    this[index] = field.copy(label = newLabel)
                                }
                            },
                            label = { Text("Field #${index + 1} Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextDarkPrimary,
                                unfocusedTextColor = TextDarkPrimary
                            )
                        )

                        IconButton(
                            onClick = {
                                if (fieldsList.size > 1) {
                                    fieldsList = fieldsList.toMutableList().apply { removeAt(index) }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }

                TextButton(
                    onClick = {
                        fieldsList = fieldsList + FormFieldEntity(
                            id = (fieldsList.size + 1).toString(),
                            formId = "",
                            label = "New Field ${fieldsList.size + 1}",
                            fieldType = "TEXT",
                            isRequired = true,
                            orderIndex = fieldsList.size
                        )
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Add Field")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(title, description, createdBy, fieldsList)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen)
            ) {
                Text("Publish Form")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExcelImportDialog(
    onDismiss: () -> Unit,
    onImport: (String, String, String, String) -> Unit,
    onOpenGoogleSheetsWindow: () -> Unit = {}
) {
    var title by remember { mutableStateOf("Excel Sheet Form Import") }
    var description by remember { mutableStateOf("Dynamic form created from imported Excel column headers") }
    var headersCsv by remember { mutableStateOf("School Code, Student Enrollment, Teacher Attendance, Drinking Water Facility, Smart Classrooms, Remarks") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Form from Excel or Google Drive", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Import Excel column headers from device or Google Drive. Each header becomes a form field.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                // ACTION ROW: OPEN GOOGLE SHEETS WINDOW IN APP
                Button(
                    onClick = onOpenGoogleSheetsWindow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = "Sheets")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Google Sheets Window in App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Divider(color = SurfaceCardBorder)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Form Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary
                    )
                )

                OutlinedTextField(
                    value = headersCsv,
                    onValueChange = { headersCsv = it },
                    label = { Text("Excel / Google Drive Column Headers (Comma Separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && headersCsv.isNotBlank()) {
                        onImport(title, description, "Chief Officer", headersCsv)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen)
            ) {
                Text("Generate Form")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GoogleSheetsWebDialog(
    url: String = "https://docs.google.com/spreadsheets/u/0/create",
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TableChart, contentDescription = "Sheets", tint = Color(0xFF0F9D58))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Sheets Window", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            webViewClient = WebViewClient()
                            loadUrl(url)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
