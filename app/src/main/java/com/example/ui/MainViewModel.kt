package com.example.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.EduRepository
import com.example.data.service.SyncResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

enum class UserRole {
    OFFICER,
    HEADMASTER
}

sealed class UIEvent {
    data class ShowToast(val message: String) : UIEvent()
    data class OpenPdfFile(val file: File) : UIEvent()
    data class OpenExcelFile(val file: File) : UIEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EduRepository(application)

    val allSchools: StateFlow<List<SchoolEntity>> = repository.allSchools
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allForms: StateFlow<List<FormEntity>> = repository.allForms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubmissions: StateFlow<List<SubmissionEntity>> = repository.allSubmissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncConfig: StateFlow<SyncConfigEntity> = repository.syncConfig
        .map { it ?: SyncConfigEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncConfigEntity())

    val schoolCount: StateFlow<Int> = repository.totalSchoolsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val formCount: StateFlow<Int> = repository.totalFormsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val recordCount: StateFlow<Int> = repository.totalSubmissionsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8)

    // User Authentication State
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Current User Role
    private val _userRole = MutableStateFlow(UserRole.OFFICER)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    // Selected School for HM Mode
    private val _selectedSchool = MutableStateFlow<SchoolEntity?>(null)
    val selectedSchool: StateFlow<SchoolEntity?> = _selectedSchool.asStateFlow()

    // Selected Form for Filling or Viewing
    private val _activeFormWithFields = MutableStateFlow<FormWithFields?>(null)
    val activeFormWithFields: StateFlow<FormWithFields?> = _activeFormWithFields.asStateFlow()

    // Submissions for Active Form
    private val _activeFormSubmissions = MutableStateFlow<List<SubmissionWithValues>>(emptyList())
    val activeFormSubmissions: StateFlow<List<SubmissionWithValues>> = _activeFormSubmissions.asStateFlow()

    private val _events = MutableSharedFlow<UIEvent>()
    val events: SharedFlow<UIEvent> = _events.asSharedFlow()

    private val _isSyncingUsers = MutableStateFlow(false)
    val isSyncingUsers: StateFlow<Boolean> = _isSyncingUsers.asStateFlow()

    fun syncUsers(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isSyncingUsers.value = true
            val result = repository.syncUsersFromSupabase()
            _isSyncingUsers.value = false
            if (result.isSuccess) {
                _events.emit(UIEvent.ShowToast("Users synced successfully"))
                onSuccess()
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Failed to sync users"
                _events.emit(UIEvent.ShowToast("Sync error: $msg"))
                onError(msg)
            }
        }
    }

    init {
        viewModelScope.launch {
            allSchools.collect { schools ->
                if (_selectedSchool.value == null && schools.isNotEmpty()) {
                    _selectedSchool.value = schools.first()
                }
            }
        }
        // Requirement 10: Check existing session and re-verify public.profiles on startup
        viewModelScope.launch {
            val restoreResult = repository.validateAndRestoreSession()
            val user = restoreResult.getOrNull()
            if (user != null) {
                val effectiveRole = if (user.role.equals("OFFICER", ignoreCase = true)) UserRole.OFFICER else UserRole.HEADMASTER
                _currentUser.value = user
                _userRole.value = effectiveRole
                _isLoggedIn.value = true
                if (effectiveRole == UserRole.OFFICER) {
                    syncUsers()
                }
            }
        }
    }

    fun setUserRole(role: UserRole) {
        _userRole.value = role
    }

    fun selectSchool(school: SchoolEntity) {
        _selectedSchool.value = school
    }

    fun loadFormDetails(formId: String) {
        viewModelScope.launch {
            val formDetails = repository.getFormWithFields(formId)
            _activeFormWithFields.value = formDetails

            if (formDetails != null) {
                val subs = repository.getSubmissionsForFormWithValues(formId)
                _activeFormSubmissions.value = subs
            }
        }
    }

    fun submitForm(
        formId: String,
        formTitle: String,
        valuesMap: Map<String, Pair<String, String>>,
        onComplete: () -> Unit
    ) {
        val school = _selectedSchool.value ?: return
        viewModelScope.launch {
            val subId = repository.submitFormResponse(formId, formTitle, school, valuesMap)
            _events.emit(UIEvent.ShowToast("Data submitted & synced to Officer's Google Drive!"))
            loadFormDetails(formId)
            onComplete()
        }
    }

    fun createNewForm(
        title: String,
        description: String,
        createdBy: String,
        fields: List<FormFieldEntity>,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val formId = java.util.UUID.randomUUID().toString()
            val form = FormEntity(
                id = formId,
                title = title,
                description = description,
                createdBy = createdBy,
                createdAt = System.currentTimeMillis()
            )
            val updatedFields = fields.map { it.copy(formId = formId) }
            repository.createFormWithFields(form, updatedFields)
            _events.emit(UIEvent.ShowToast("New form created & published to all school HMs!"))
            onComplete()
        }
    }

    fun importFormFromExcelText(
        title: String,
        description: String,
        createdBy: String,
        commaSeparatedHeaders: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val headers = commaSeparatedHeaders.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (headers.isEmpty()) {
                _events.emit(UIEvent.ShowToast("Please enter or paste at least one column header."))
                return@launch
            }
            repository.createFormFromExcelHeaders(title, description, createdBy, headers)
            _events.emit(UIEvent.ShowToast("Form generated from Excel columns (${headers.size} fields)!"))
            onComplete()
        }
    }

    fun saveGoogleSyncSettings(
        email: String,
        sheetId: String,
        webhookUrl: String,
        autoSync: Boolean
    ) {
        viewModelScope.launch {
            val config = SyncConfigEntity(
                id = 1,
                officerDriveEmail = email,
                googleSheetId = sheetId,
                appsScriptWebhookUrl = webhookUrl,
                autoSync = autoSync,
                lastSyncedAt = System.currentTimeMillis()
            )
            repository.saveSyncConfig(config)
            _events.emit(UIEvent.ShowToast("Google Sheets API connection updated successfully!"))
        }
    }

    fun exportActiveFormPdf() {
        val formWithFields = _activeFormWithFields.value ?: return
        viewModelScope.launch {
            val subs = repository.getSubmissionsForFormWithValues(formWithFields.form.id)
            val file = repository.exportFormToPdf(formWithFields, subs)
            _events.emit(UIEvent.OpenPdfFile(file))
        }
    }

    fun exportActiveFormExcel() {
        val formWithFields = _activeFormWithFields.value ?: return
        viewModelScope.launch {
            val subs = repository.getSubmissionsForFormWithValues(formWithFields.form.id)
            val file = repository.exportFormToExcel(formWithFields, subs)
            _events.emit(UIEvent.OpenExcelFile(file))
        }
    }

    fun exportFormPdfForSelectedSchools(formId: String, selectedSchoolIds: Set<String>) {
        viewModelScope.launch {
            if (selectedSchoolIds.isEmpty()) {
                _events.emit(UIEvent.ShowToast("No schools selected. Please select at least one school to export."))
                return@launch
            }
            val formWithFields = repository.getFormWithFields(formId) ?: return@launch
            val allSubs = repository.getSubmissionsForFormWithValues(formId)
            val filteredSubs = allSubs.filter { selectedSchoolIds.contains(it.submission.schoolId) }
            if (filteredSubs.isEmpty()) {
                _events.emit(UIEvent.ShowToast("No submission data found for selected school(s)."))
                return@launch
            }
            val file = repository.exportFormToPdf(formWithFields, filteredSubs)
            _events.emit(UIEvent.ShowToast("PDF exported for ${filteredSubs.size} selected school(s)!"))
            _events.emit(UIEvent.OpenPdfFile(file))
        }
    }

    fun exportFormExcelForSelectedSchools(formId: String, selectedSchoolIds: Set<String>) {
        viewModelScope.launch {
            if (selectedSchoolIds.isEmpty()) {
                _events.emit(UIEvent.ShowToast("No schools selected. Please select at least one school to export."))
                return@launch
            }
            val formWithFields = repository.getFormWithFields(formId) ?: return@launch
            val allSubs = repository.getSubmissionsForFormWithValues(formId)
            val filteredSubs = allSubs.filter { selectedSchoolIds.contains(it.submission.schoolId) }
            if (filteredSubs.isEmpty()) {
                _events.emit(UIEvent.ShowToast("No submission data found for selected school(s)."))
                return@launch
            }
            val file = repository.exportFormToExcel(formWithFields, filteredSubs)
            _events.emit(UIEvent.ShowToast("Excel exported for ${filteredSubs.size} selected school(s)!"))
            _events.emit(UIEvent.OpenExcelFile(file))
        }
    }

    fun deleteSubmission(formId: String, schoolId: String) {
        viewModelScope.launch {
            repository.deleteSubmissionWithValues(formId, schoolId)
            _events.emit(UIEvent.ShowToast("Submission deleted. Form moved to Pending Forms."))
        }
    }

    fun loadSubmissionValues(submissionId: String, onResult: (List<SubmissionValueEntity>) -> Unit) {
        viewModelScope.launch {
            val values = repository.getValuesForSubmissionList(submissionId)
            onResult(values)
        }
    }

    fun markSubmissionStatus(submissionId: String, status: String) {
        viewModelScope.launch {
            repository.updateSubmissionSyncStatus(submissionId, status)
            val msg = if (status == "RETURNED") "Submission returned to school for corrections." else "Status updated to $status."
            _events.emit(UIEvent.ShowToast(msg))
        }
    }

    fun exportAllDataCombined() {
        viewModelScope.launch {
            val forms = repository.allForms.firstOrNull() ?: emptyList()
            if (forms.isNotEmpty()) {
                val firstForm = repository.getFormWithFields(forms.first().id)
                if (firstForm != null) {
                    val subs = repository.getAllSubmissionsWithValues()
                    val pdfFile = repository.exportFormToPdf(firstForm, subs)
                    val excelFile = repository.exportFormToExcel(firstForm, subs)
                    _events.emit(UIEvent.ShowToast("Generated PDF (${pdfFile.name}) & Excel (${excelFile.name})!"))
                    _events.emit(UIEvent.OpenPdfFile(pdfFile))
                }
            }
        }
    }

    fun login(
        emailOrUdise: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (emailOrUdise.isBlank() || password.isBlank()) {
                onError("Please enter Email ID and Password.")
                return@launch
            }
            val result = repository.loginUserWithSupabase(emailOrUdise, password)
            val user = result.getOrNull()
            if (user != null) {
                // Determine user role strictly from validated public.profiles role
                val effectiveRole = if (user.role.equals("OFFICER", ignoreCase = true)) UserRole.OFFICER else UserRole.HEADMASTER

                _currentUser.value = user
                _isLoggedIn.value = true
                _userRole.value = effectiveRole

                // Find matching school if Headmaster
                val schools = allSchools.value
                val matched = schools.find { it.id.equals(user.udiseCode, ignoreCase = true) }
                if (matched != null) {
                    _selectedSchool.value = matched
                } else if (effectiveRole == UserRole.HEADMASTER) {
                    val newSchool = SchoolEntity(
                        id = user.udiseCode,
                        name = user.schoolName,
                        category = "Secondary",
                        district = "Central District",
                        headmasterName = user.headmasterName,
                        headmasterPhone = user.phone,
                        email = user.email
                    )
                    _selectedSchool.value = newSchool
                }

                val portalTitle = if (effectiveRole == UserRole.OFFICER) "District Officer Portal" else "School Portal"
                if (effectiveRole == UserRole.OFFICER) {
                    syncUsers()
                }
                _events.emit(UIEvent.ShowToast("Welcome, ${user.headmasterName}! Logged in to $portalTitle."))
                onSuccess()
            } else {
                val errMessage = result.exceptionOrNull()?.message ?: "Invalid credentials or Supabase Auth error."
                onError(errMessage)
            }
        }
    }

    fun deletePublishedForm(formId: String) {
        viewModelScope.launch {
            repository.deletePublishedForm(formId)
            _events.emit(UIEvent.ShowToast("Published Form '$formId' permanently deleted from everywhere!"))
        }
    }

    fun registerSchoolUser(
        udiseCode: String,
        schoolName: String,
        hmName: String,
        phone: String,
        email: String,
        password: String,
        role: UserRole,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (udiseCode.isBlank() || password.isBlank() || schoolName.isBlank() || hmName.isBlank()) {
                onError("Please fill in all required fields.")
                return@launch
            }

            val roleString = if (role == UserRole.OFFICER) "OFFICER" else "HEADMASTER"
            val newUser = UserEntity(
                udiseCode = udiseCode.trim().uppercase(),
                schoolName = schoolName.trim(),
                headmasterName = hmName.trim(),
                phone = phone.trim(),
                passwordHash = password,
                role = roleString,
                email = email.trim()
            )

            val regResult = repository.registerUserWithSupabase(newUser)
            if (regResult.isFailure) {
                val err = regResult.exceptionOrNull()?.message ?: "Registration failed."
                onError(err)
                return@launch
            }

            val registeredUser = regResult.getOrDefault(newUser)

            // Auto log in after registration
            _currentUser.value = registeredUser
            _isLoggedIn.value = true
            _userRole.value = role

            val newSchool = SchoolEntity(
                id = registeredUser.udiseCode,
                name = registeredUser.schoolName,
                category = "Secondary",
                district = "Central District",
                headmasterName = registeredUser.headmasterName,
                headmasterPhone = registeredUser.phone,
                email = registeredUser.email
            )
            _selectedSchool.value = newSchool

            _events.emit(UIEvent.ShowToast("Registered in Supabase Auth successfully!"))
            onSuccess()
        }
    }

    fun updateSchoolContact(schoolId: String, phone: String, email: String) {
        viewModelScope.launch {
            repository.updateSchoolContact(schoolId, phone, email)
            _events.emit(UIEvent.ShowToast("School contact information updated!"))
        }
    }

    fun registerOfficerUser(
        officerId: String,
        fullName: String,
        designation: String,
        phone: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (fullName.isBlank() || phone.isBlank() || password.isBlank() || email.isBlank()) {
                onError("Please fill in all required officer registration fields.")
                return@launch
            }
            val idToUse = if (officerId.isNotBlank()) officerId.trim().uppercase() else "OFF_${phone.takeLast(4)}_${System.currentTimeMillis() % 1000}"
            val existing = repository.getUserByUdise(idToUse)
            if (existing != null) {
                onError("An officer with ID '$idToUse' already exists.")
                return@launch
            }

            val newOfficer = UserEntity(
                udiseCode = idToUse,
                schoolName = designation.ifBlank { "District Education Office" },
                headmasterName = fullName.trim(),
                phone = phone.trim(),
                passwordHash = password,
                role = "OFFICER",
                email = email.trim()
            )
            repository.registerUser(newOfficer)
            _events.emit(UIEvent.ShowToast("Officer registered successfully ($idToUse)!"))
            onSuccess()
        }
    }

    fun resetUserPassword(udiseCode: String, newPassword: String = "Pass@123") {
        viewModelScope.launch {
            repository.resetUserPassword(udiseCode, newPassword)
            _events.emit(UIEvent.ShowToast("Password for $udiseCode reset to default: $newPassword"))
        }
    }

    fun deleteUser(udiseCode: String) {
        viewModelScope.launch {
            repository.deleteUser(udiseCode)
            _events.emit(UIEvent.ShowToast("User $udiseCode deleted successfully."))
        }
    }

    fun deleteSchool(schoolId: String) {
        viewModelScope.launch {
            repository.deleteSchool(schoolId)
            _events.emit(UIEvent.ShowToast("School $schoolId deleted successfully."))
        }
    }

    fun updateUserInfo(udiseCode: String, name: String, phone: String, email: String, schoolName: String, udiseNumber: String = "") {
        viewModelScope.launch {
            _isSyncingUsers.value = true
            repository.updateUserInfo(udiseCode, name, phone, email, schoolName, udiseNumber)
            repository.syncUsersFromSupabase()
            _isSyncingUsers.value = false
            _events.emit(UIEvent.ShowToast("User profile updated successfully in Supabase."))
        }
    }

    fun changeCurrentUserPassword(oldPass: String, newPass: String, confirmPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                onError("No user currently logged in.")
                return@launch
            }
            if (user.passwordHash != oldPass) {
                onError("Old password is incorrect.")
                return@launch
            }
            if (newPass.isBlank()) {
                onError("New password cannot be empty.")
                return@launch
            }
            if (newPass != confirmPass) {
                onError("New password and confirm password do not match.")
                return@launch
            }

            repository.resetUserPassword(user.udiseCode, newPass)
            _currentUser.value = user.copy(passwordHash = newPass)
            _events.emit(UIEvent.ShowToast("Password changed successfully!"))
            onSuccess()
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
        viewModelScope.launch {
            repository.logoutUser()
            _events.emit(UIEvent.ShowToast("Logged out successfully."))
        }
    }
}
