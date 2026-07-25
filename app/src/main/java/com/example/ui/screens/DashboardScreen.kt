package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FormEntity
import com.example.data.model.SchoolEntity
import com.example.data.model.SubmissionEntity
import com.example.data.model.SubmissionValueEntity
import com.example.data.model.UserEntity
import com.example.ui.UserRole
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    forms: List<FormEntity>,
    schools: List<SchoolEntity>,
    submissions: List<SubmissionEntity>,
    currentUser: UserEntity? = null,
    role: UserRole = UserRole.OFFICER,
    onExportSelectedPdf: (formId: String, selectedSchoolIds: Set<String>) -> Unit = { _, _ -> },
    onExportSelectedExcel: (formId: String, selectedSchoolIds: Set<String>) -> Unit = { _, _ -> },
    onLoadSubmissionValues: (submissionId: String, onResult: (List<SubmissionValueEntity>) -> Unit) -> Unit = { _, _ -> },
    onMarkSubmissionStatus: (submissionId: String, status: String) -> Unit = { _, _ -> }
) {
    // Selected Tab Category: 0 = Filled Forms, 1 = Pending Forms, 2 = Returned for Corrections
    var selectedCategory by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state for Officer Formal Review of a specific submission
    var reviewingSubmission by remember { mutableStateOf<SubmissionEntity?>(null) }
    var reviewingValues by remember { mutableStateOf<List<SubmissionValueEntity>>(emptyList()) }
    var isLoadingValues by remember { mutableStateOf(false) }

    // Track selected school IDs per form for batch export: formId -> Set<schoolId>
    val selectedSchoolIdsMap = remember { mutableStateMapOf<String, Set<String>>() }

    // Filter schools for Headmaster
    val userUdise = currentUser?.udiseCode?.trim() ?: ""
    val displaySchools = remember(schools, currentUser, role) {
        if (role == UserRole.HEADMASTER && userUdise.isNotBlank()) {
            val filtered = schools.filter { it.id.equals(userUdise, ignoreCase = true) }
            if (filtered.isNotEmpty()) filtered
            else listOf(
                SchoolEntity(
                    id = userUdise,
                    name = currentUser?.schoolName ?: "Registered School",
                    category = "Secondary",
                    district = "District HQ",
                    headmasterName = currentUser?.headmasterName ?: "HM",
                    headmasterPhone = currentUser?.phone ?: ""
                )
            )
        } else {
            schools
        }
    }

    val displaySubmissions = remember(submissions, userUdise, role) {
        if (role == UserRole.HEADMASTER && userUdise.isNotBlank()) {
            submissions.filter { it.schoolId.equals(userUdise, ignoreCase = true) }
        } else {
            submissions
        }
    }

    // Categorized Submissions & Forms
    val filledSubmissions = remember(displaySubmissions) {
        displaySubmissions.filter { !it.syncStatus.equals("RETURNED", ignoreCase = true) }
    }

    val returnedSubmissions = remember(displaySubmissions) {
        displaySubmissions.filter { it.syncStatus.equals("RETURNED", ignoreCase = true) }
    }

    val filledForms = remember(forms, filledSubmissions, searchQuery) {
        val matchingForms = forms.filter { form ->
            filledSubmissions.any { it.formId == form.id }
        }
        if (searchQuery.isBlank()) matchingForms
        else matchingForms.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val pendingForms = remember(forms, filledSubmissions, displaySchools, searchQuery) {
        val matchingForms = forms.filter { form ->
            val submittedSchoolIds = filledSubmissions.filter { it.formId == form.id }.map { it.schoolId }.toSet()
            displaySchools.any { !submittedSchoolIds.contains(it.id) }
        }
        if (searchQuery.isBlank()) matchingForms
        else matchingForms.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val returnedForms = remember(forms, returnedSubmissions, searchQuery) {
        val matchingForms = forms.filter { form ->
            returnedSubmissions.any { it.formId == form.id }
        }
        if (searchQuery.isBlank()) matchingForms
        else matchingForms.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MintBackground)
    ) {
        // TOP HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ForestDarkGreen)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column {
                Text(
                    text = "OFFICIAL RECORDS & SUBMISSION ANALYTICS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLightSubtle,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (role == UserRole.HEADMASTER) "${currentUser?.schoolName ?: "School"} Submissions" else "District School Data Repository",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select schools to review data or export to PDF & Excel format",
                    fontSize = 12.sp,
                    color = TextLightSubtle
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3 CARDS FOR CATEGORY SELECTOR (Filled, Pending, Returned)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Filled Forms Card
            CategoryCard(
                title = "Filled Forms",
                count = filledForms.size.toString(),
                isSelected = selectedCategory == 0,
                icon = Icons.Filled.AssignmentTurnedIn,
                badgeColor = SoftGreenBg,
                textColor = ForestDarkGreen,
                onClick = { selectedCategory = 0 },
                modifier = Modifier.weight(1f)
            )

            // Pending Forms Card
            CategoryCard(
                title = "Pending Forms",
                count = pendingForms.size.toString(),
                isSelected = selectedCategory == 1,
                icon = Icons.Filled.HourglassEmpty,
                badgeColor = BentoContainerPurple,
                textColor = SoftPurpleIcon,
                onClick = { selectedCategory = 1 },
                modifier = Modifier.weight(1f)
            )

            // Returned for Corrections Card
            CategoryCard(
                title = "Returned",
                subtitle = "Corrections",
                count = returnedForms.size.toString(),
                isSelected = selectedCategory == 2,
                icon = Icons.Filled.EditNote,
                badgeColor = Color(0xFFFEE2E2),
                textColor = Color(0xFFDC2626),
                onClick = { selectedCategory = 2 },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SEARCH BAR
        PaddingValues(horizontal = 16.dp).let {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                placeholder = { Text("Search form title or school...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = TextMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
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

        Spacer(modifier = Modifier.height(12.dp))

        // LIST CONTENT BASED ON SELECTED CATEGORY
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            when (selectedCategory) {
                // ---------------- 1. FILLED FORMS TAB ----------------
                0 -> {
                    if (filledForms.isEmpty()) {
                        item { EmptyStateCard("No filled forms found for selected filter.") }
                    } else {
                        items(filledForms) { form ->
                            val formSubmissions = filledSubmissions.filter { it.formId == form.id }
                            val formSchoolIds = formSubmissions.map { it.schoolId }.toSet()

                            // Initialize selected set for this form if not present
                            if (!selectedSchoolIdsMap.containsKey(form.id)) {
                                selectedSchoolIdsMap[form.id] = formSchoolIds
                            }
                            val currentSelected = selectedSchoolIdsMap[form.id] ?: formSchoolIds
                            val isAllSelected = currentSelected.containsAll(formSchoolIds) && formSchoolIds.isNotEmpty()

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    // Form Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = form.title,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDarkPrimary
                                            )
                                            Text(
                                                text = "${formSubmissions.size} School(s) Submitted Data",
                                                fontSize = 12.sp,
                                                color = ForestDarkGreen,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        if (role == UserRole.OFFICER) {
                                            Surface(
                                                color = SoftGreenBg,
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text(
                                                    text = "${currentSelected.size}/${formSubmissions.size} Selected",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ForestDarkGreen,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider(color = SurfaceCardBorder)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // SELECT ALL ROW (Officer only)
                                    if (role == UserRole.OFFICER) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(BentoHeroBg)
                                                .clickable {
                                                    if (isAllSelected) {
                                                        selectedSchoolIdsMap[form.id] = emptySet()
                                                    } else {
                                                        selectedSchoolIdsMap[form.id] = formSchoolIds
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isAllSelected,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        selectedSchoolIdsMap[form.id] = formSchoolIds
                                                    } else {
                                                        selectedSchoolIdsMap[form.id] = emptySet()
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = ForestDarkGreen)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Select All Schools to this form for Export",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ForestDarkGreen
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    // SCHOOLS LIST FOR THIS FILLED FORM
                                    formSubmissions.forEach { sub ->
                                        val isChecked = currentSelected.contains(sub.schoolId)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isChecked && role == UserRole.OFFICER) SoftGreenBg.copy(alpha = 0.5f) else Color.Transparent)
                                                .clickable {
                                                    // Open Formal Review Dialog
                                                    reviewingSubmission = sub
                                                    isLoadingValues = true
                                                    onLoadSubmissionValues(sub.id) { vals ->
                                                        reviewingValues = vals
                                                        isLoadingValues = false
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Checkbox for Export (Officer only)
                                            if (role == UserRole.OFFICER) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        if (checked) {
                                                            selectedSchoolIdsMap[form.id] = currentSelected + sub.schoolId
                                                        } else {
                                                            selectedSchoolIdsMap[form.id] = currentSelected - sub.schoolId
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = ForestDarkGreen)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = sub.schoolName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextDarkPrimary
                                                )
                                                Text(
                                                    text = "UDISE: ${sub.schoolId} • HM: ${sub.submittedBy}",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(sub.submittedAt))
                                                Text(
                                                    text = "Submitted: $dateStr",
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Filled.ChevronRight,
                                                contentDescription = "Review Data",
                                                tint = ForestDarkGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Divider(color = SurfaceCardBorder)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 2 BUTTONS AT BOTTOM: EXPORT TO PDF & EXPORT TO EXCEL
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val selected = if (role == UserRole.OFFICER) (selectedSchoolIdsMap[form.id] ?: emptySet()) else formSchoolIds
                                                onExportSelectedPdf(form.id, selected)
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = ForestDarkGreen),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PictureAsPdf,
                                                contentDescription = "PDF",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Export to PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                val selected = if (role == UserRole.OFFICER) (selectedSchoolIdsMap[form.id] ?: emptySet()) else formSchoolIds
                                                onExportSelectedExcel(form.id, selected)
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.TableChart,
                                                contentDescription = "Excel",
                                                tint = TextDarkPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Export to Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                                        }
                                    }

                                    // RETURN SELECTED BUTTON FOR OFFICER ONLY
                                    if (role == UserRole.OFFICER) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = {
                                                val selected = selectedSchoolIdsMap[form.id] ?: emptySet()
                                                if (selected.isNotEmpty()) {
                                                    val subsToReturn = formSubmissions.filter { selected.contains(it.schoolId) }
                                                    subsToReturn.forEach { sub ->
                                                        onMarkSubmissionStatus(sub.id, "RETURNED")
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("btn_return_selected_${form.id}"),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706)),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Undo,
                                                contentDescription = "Return Selected",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Return Selected Submissions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ---------------- 2. PENDING FORMS TAB ----------------
                1 -> {
                    if (pendingForms.isEmpty()) {
                        item { EmptyStateCard("All schools have completed submissions for all forms!") }
                    } else {
                        items(pendingForms) { form ->
                            val submittedSchoolIds = filledSubmissions.filter { it.formId == form.id }.map { it.schoolId }.toSet()
                            val pendingSchools = displaySchools.filter { !submittedSchoolIds.contains(it.id) }

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
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            if (role == UserRole.OFFICER) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(BentoHeroBg)
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = form.title,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ForestDarkGreen
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = form.title,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextDarkPrimary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${pendingSchools.size} School(s) Pending",
                                                fontSize = 12.sp,
                                                color = SoftPurpleIcon,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Surface(
                                            color = BentoContainerPurple,
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text(
                                                text = "PENDING",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = SoftPurpleIcon,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider(color = SurfaceCardBorder)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    pendingSchools.forEach { school ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MintBackground)
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(BentoContainerPurple),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.School,
                                                    contentDescription = "Pending School",
                                                    tint = SoftPurpleIcon,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = school.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextDarkPrimary
                                                )
                                                Text(
                                                    text = "UDISE: ${school.id} | ${school.district}",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = "HM: ${school.headmasterName} (${school.headmasterPhone})",
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                            }

                                            Surface(
                                                color = BentoContainerPurple,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    text = "Pending",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SoftPurpleIcon,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ---------------- 3. RETURNED FOR CORRECTIONS TAB ----------------
                2 -> {
                    if (returnedForms.isEmpty()) {
                        item { EmptyStateCard("No forms currently returned for corrections.") }
                    } else {
                        items(returnedForms) { form ->
                            val formReturnedSubs = returnedSubmissions.filter { it.formId == form.id }

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
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = form.title,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDarkPrimary
                                            )
                                            Text(
                                                text = "${formReturnedSubs.size} School(s) Returned for Corrections",
                                                fontSize = 12.sp,
                                                color = Color(0xFFDC2626),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Surface(
                                            color = Color(0xFFFEE2E2),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text(
                                                text = "CORRECTION",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFDC2626),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider(color = SurfaceCardBorder)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    formReturnedSubs.forEach { sub ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFFEF2F2))
                                                .clickable {
                                                    // Open Formal Review Dialog
                                                    reviewingSubmission = sub
                                                    isLoadingValues = true
                                                    onLoadSubmissionValues(sub.id) { vals ->
                                                        reviewingValues = vals
                                                        isLoadingValues = false
                                                    }
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFEE2E2)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PriorityHigh,
                                                    contentDescription = "Returned",
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = sub.schoolName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextDarkPrimary
                                                )
                                                Text(
                                                    text = "UDISE: ${sub.schoolId} | HM: ${sub.submittedBy}",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = "Status: Awaiting corrections resubmission from HM",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFDC2626),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Filled.ChevronRight,
                                                contentDescription = "View Details",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(20.dp)
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
    }

    // FORMAL OFFICER REVIEW DIALOG
    if (reviewingSubmission != null) {
        val sub = reviewingSubmission!!
        AlertDialog(
            onDismissRequest = { reviewingSubmission = null },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (role == UserRole.OFFICER && !sub.syncStatus.equals("RETURNED", ignoreCase = true)) {
                        Button(
                            onClick = {
                                onMarkSubmissionStatus(sub.id, "RETURNED")
                                reviewingSubmission = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Undo, contentDescription = "Return", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Return for Correction", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { reviewingSubmission = null },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", fontSize = 12.sp)
                    }
                }
            },
            title = {
                Column {
                    Text(
                        text = "Formal Officer Review",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ForestDarkGreen
                    )
                    Text(
                        text = "${sub.schoolName} (${sub.schoolId})",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Submitted Form: ${sub.formTitle}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "Submitted By: HM ${sub.submittedBy}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = SurfaceCardBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoadingValues) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ForestDarkGreen, modifier = Modifier.size(32.dp))
                        }
                    } else if (reviewingValues.isEmpty()) {
                        Text("No response fields recorded.", fontSize = 12.sp, color = TextMuted)
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            reviewingValues.forEach { valEntity ->
                                Surface(
                                    color = MintBackground,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = valEntity.fieldLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMuted
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = valEntity.value.ifBlank { "N/A" },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDarkPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SurfaceWhite
        )
    }
}

@Composable
private fun CategoryCard(
    title: String,
    subtitle: String? = null,
    count: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("category_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SurfaceWhite else SurfaceWhite.copy(alpha = 0.75f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) ForestDarkGreen else SurfaceCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = count,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) ForestDarkGreen else TextDarkPrimary
            )

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) ForestDarkGreen else TextSecondary,
                textAlign = TextAlign.Center
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
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
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Empty",
                tint = ForestDarkGreen,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}
