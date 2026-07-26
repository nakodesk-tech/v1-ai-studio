package com.example.data.repository

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.service.ExportManager
import com.example.data.service.GoogleSheetsService
import com.example.data.service.SyncResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class EduRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val schoolDao = db.schoolDao()
    private val formDao = db.formDao()
    private val submissionDao = db.submissionDao()
    private val syncConfigDao = db.syncConfigDao()
    private val userDao = db.userDao()

    private val googleSheetsService = GoogleSheetsService()
    private val exportManager = ExportManager(context)
    private val supabaseAuthService = com.example.data.service.SupabaseAuthService()
    private val sessionPrefs = context.getSharedPreferences("supa_session_prefs", Context.MODE_PRIVATE)

    fun saveSession(
        userId: String,
        email: String,
        token: String,
        fullName: String,
        role: String,
        refreshToken: String? = null
    ) {
        val editor = sessionPrefs.edit()
            .putString("session_user_id", userId)
            .putString("session_email", email)
            .putString("session_token", token)
            .putString("session_full_name", fullName)
            .putString("session_role", role)
            .putBoolean("is_logged_in", true)
        if (!refreshToken.isNullOrBlank()) {
            editor.putString("session_refresh_token", refreshToken)
        }
        editor.apply()
    }

    fun clearSession() {
        sessionPrefs.edit().clear().apply()
    }

    fun getSavedSessionUserId(): String? = sessionPrefs.getString("session_user_id", null)
    fun getSavedSessionToken(): String? = sessionPrefs.getString("session_token", null)
    fun getSavedRefreshToken(): String? = sessionPrefs.getString("session_refresh_token", null)
    fun getSavedSessionEmail(): String? = sessionPrefs.getString("session_email", null)
    fun isSessionLoggedIn(): Boolean = sessionPrefs.getBoolean("is_logged_in", false)

    val allSchools: Flow<List<SchoolEntity>> = schoolDao.getAllSchools()
    val allForms: Flow<List<FormEntity>> = formDao.getAllForms()
    val allSubmissions: Flow<List<SubmissionEntity>> = submissionDao.getAllSubmissions()
    val syncConfig: Flow<SyncConfigEntity?> = syncConfigDao.getSyncConfig()

    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsersFlow()

    suspend fun resetUserPassword(udiseCode: String, newPasswordHash: String = "Pass@123") {
        userDao.resetUserPassword(udiseCode, newPasswordHash)
    }

    suspend fun deleteUser(udiseCode: String) {
        userDao.deleteUser(udiseCode)
        schoolDao.deleteSchool(udiseCode)
    }

    suspend fun deleteSchool(schoolId: String) {
        schoolDao.deleteSchool(schoolId)
        userDao.deleteUser(schoolId)
    }

    suspend fun updateUserInfo(
        udiseCode: String,
        name: String,
        phone: String,
        email: String,
        schoolName: String,
        udiseNumber: String = ""
    ): Result<Boolean> {
        val token = getSavedSessionToken()
        val supabaseResult = supabaseAuthService.updateUserProfile(
            userId = udiseCode,
            fullName = name,
            schoolName = schoolName,
            udiseNumber = udiseNumber,
            accessToken = token
        )

        if (supabaseResult.isFailure) {
            val err = supabaseResult.exceptionOrNull()?.message ?: "Failed to update profile on Supabase."
            return Result.failure(Exception(err))
        }

        userDao.updateUserInfo(udiseCode, name, phone, email, schoolName, udiseNumber)
        syncUsersFromSupabase()
        return Result.success(true)
    }

    suspend fun syncUsersFromSupabase(): Result<List<UserEntity>> {
        val token = getSavedSessionToken()
        val profilesResult = supabaseAuthService.getAllProfiles(token)

        if (profilesResult.isFailure) {
            val err = profilesResult.exceptionOrNull()?.message ?: "Failed to fetch profiles from Supabase."
            return Result.failure(Exception(err))
        }

        val profiles = profilesResult.getOrNull() ?: emptyList()

        val syncedUsers = profiles.mapNotNull { profile ->
            val userId = profile.id ?: return@mapNotNull null
            val rawRole = profile.role?.lowercase()?.trim() ?: ""
            val roleStr = if (rawRole == "officer") "OFFICER" else "HEADMASTER"
            val email = profile.email ?: ""
            val fullName = profile.full_name ?: ""
            val udiseNum = profile.getDisplayUdise()
            val schoolName = profile.school_name?.ifBlank { null }
                ?: if (roleStr == "OFFICER") "District Office" else "School Portal"
            val phone = profile.phone ?: ""

            val existingLocalUser = userDao.getUserByUdise(userId)

            UserEntity(
                udiseCode = userId,
                schoolName = schoolName,
                headmasterName = fullName.ifBlank { existingLocalUser?.headmasterName ?: "User" },
                phone = phone.ifBlank { existingLocalUser?.phone ?: "" },
                passwordHash = existingLocalUser?.passwordHash ?: "",
                role = roleStr,
                registeredAt = existingLocalUser?.registeredAt ?: System.currentTimeMillis(),
                email = email.ifBlank { existingLocalUser?.email ?: "" },
                udiseNumber = udiseNum.ifBlank { existingLocalUser?.udiseNumber ?: "" }
            )
        }

        val uniqueUsers = syncedUsers.distinctBy { it.udiseCode }

        if (uniqueUsers.isNotEmpty()) {
            val validIds = uniqueUsers.map { it.udiseCode }
            userDao.deleteUsersNotIn(validIds)
            uniqueUsers.forEach { user ->
                userDao.insertUser(user)
            }

            val nonOfficerUsers = uniqueUsers.filter { it.role != "OFFICER" }
            if (nonOfficerUsers.isNotEmpty()) {
                val validSchoolIds = nonOfficerUsers.map { it.udiseCode }
                schoolDao.deleteSchoolsNotIn(validSchoolIds)
                nonOfficerUsers.forEach { user ->
                    val existingSchool = schoolDao.getSchoolById(user.udiseCode)
                    val displayUdise = user.udiseNumber.ifBlank { user.udiseCode }
                    val finalSchoolName = if (user.schoolName.isNotBlank() && user.schoolName != "School Portal") {
                        user.schoolName
                    } else if (user.headmasterName.isNotBlank() && user.headmasterName != "User") {
                        "${user.headmasterName}'s School"
                    } else {
                        existingSchool?.name ?: "School ($displayUdise)"
                    }

                    schoolDao.insertSchool(
                        SchoolEntity(
                            id = user.udiseCode,
                            name = finalSchoolName,
                            category = existingSchool?.category ?: "Secondary",
                            district = existingSchool?.district ?: "District HQ",
                            headmasterName = user.headmasterName,
                            headmasterPhone = user.phone,
                            email = user.email
                        )
                    )
                }
            }
        }

        return Result.success(uniqueUsers)
    }

    val totalSchoolsCount: Flow<Int> = schoolDao.getSchoolCount()
    val totalFormsCount: Flow<Int> = formDao.getFormCount()
    val totalSubmissionsCount: Flow<Int> = submissionDao.getSubmissionCount()

    init {
        // Seed default initial data on background thread if empty
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
        }
    }

    suspend fun loginUser(udiseCode: String, password: String): UserEntity? {
        val res = loginUserWithSupabase(udiseCode, password)
        return res.getOrNull()
    }

    suspend fun loginUserWithSupabase(udiseCodeOrEmail: String, password: String): Result<UserEntity> {
        val trimmedInput = udiseCodeOrEmail.trim()
        val authEmail = trimmedInput

        val supabaseResult = supabaseAuthService.signIn(authEmail, password)

        if (supabaseResult.isFailure) {
            val errMsg = supabaseResult.exceptionOrNull()?.message ?: "Invalid credentials or Supabase Auth error."
            return Result.failure(Exception(errMsg))
        }

        val resp = supabaseResult.getOrNull()
        val user = resp?.user
        val userId = user?.id ?: ""
        val accessToken = resp?.access_token ?: ""

        val refreshToken = resp?.refresh_token

        if (userId.isBlank()) {
            return Result.failure(Exception("Supabase authentication failed to return a valid User ID."))
        }

        // 1 & 2. Get profile from public.profiles where profiles.id = userId
        val profileResult = supabaseAuthService.getUserProfile(userId, accessToken)
        val profile = profileResult.getOrNull()

        // 3, 8 & 9. Read role column from profile and validate
        val rawRole = profile?.role?.lowercase()?.trim() ?: ""
        if (profile == null || (rawRole != "officer" && rawRole != "user")) {
            supabaseAuthService.signOut(accessToken)
            clearSession()
            return Result.failure(
                Exception("Your account has not been configured. Please contact the administrator.")
            )
        }

        // 4 & 5. Route role: "officer" -> OFFICER, "user" -> HEADMASTER
        val finalRoleStr = if (rawRole == "officer") "OFFICER" else "HEADMASTER"
        val finalEmail = profile.email?.ifBlank { null } ?: user?.email ?: trimmedInput
        val finalFullName = profile.full_name?.ifBlank { null } ?: user?.user_metadata?.hm_name ?: "User"
        val finalSchoolName = profile.school_name?.ifBlank { null } ?: if (finalRoleStr == "OFFICER") "District Office" else "School Portal"
        val finalUdiseNumber = profile.getDisplayUdise()

        val loggedInUser = UserEntity(
            udiseCode = userId,
            schoolName = finalSchoolName,
            headmasterName = finalFullName,
            phone = profile.phone ?: "",
            email = finalEmail,
            passwordHash = password,
            role = finalRoleStr,
            udiseNumber = finalUdiseNumber
        )

        userDao.insertUser(loggedInUser)
        saveSession(
            userId = userId,
            email = finalEmail,
            token = accessToken,
            fullName = finalFullName,
            role = finalRoleStr,
            refreshToken = refreshToken
        )

        return Result.success(loggedInUser)
    }

    suspend fun validateAndRestoreSession(): Result<UserEntity> {
        if (!isSessionLoggedIn()) {
            return Result.failure(Exception("No active session"))
        }

        val userId = getSavedSessionUserId()
        val token = getSavedSessionToken()
        val savedEmail = getSavedSessionEmail() ?: ""

        if (userId.isNullOrBlank()) {
            clearSession()
            return Result.failure(Exception("No active session"))
        }

        // 10. Re-verify public.profiles on startup
        val profileResult = supabaseAuthService.getUserProfile(userId, token)
        val profile = profileResult.getOrNull()

        val rawRole = profile?.role?.lowercase()?.trim() ?: ""
        if (profile == null || (rawRole != "officer" && rawRole != "user")) {
            clearSession()
            return Result.failure(
                Exception("Your account has not been configured. Please contact the administrator.")
            )
        }

        val finalRoleStr = if (rawRole == "officer") "OFFICER" else "HEADMASTER"
        val finalEmail = profile.email?.ifBlank { null } ?: savedEmail
        val finalFullName = profile.full_name?.ifBlank { null } ?: "User"
        val finalSchoolName = profile.school_name?.ifBlank { null } ?: if (finalRoleStr == "OFFICER") "District Office" else "School Portal"
        val finalUdiseNumber = profile.getDisplayUdise()

        val user = UserEntity(
            udiseCode = userId,
            schoolName = finalSchoolName,
            headmasterName = finalFullName,
            phone = profile.phone ?: "",
            email = finalEmail,
            passwordHash = "",
            role = finalRoleStr,
            udiseNumber = finalUdiseNumber
        )

        userDao.insertUser(user)
        saveSession(
            userId = userId,
            email = finalEmail,
            token = token ?: "",
            fullName = finalFullName,
            role = finalRoleStr
        )

        return Result.success(user)
    }

    suspend fun logoutUser() {
        val token = getSavedSessionToken()
        supabaseAuthService.signOut(token)
        clearSession()
    }

    suspend fun getUserByUdise(udiseCode: String): UserEntity? {
        return userDao.getUserByUdise(udiseCode.trim())
    }

    suspend fun registerUser(user: UserEntity) {
        registerUserWithSupabase(user)
    }

    suspend fun registerUserWithSupabase(user: UserEntity): Result<UserEntity> {
        val authEmail = if (user.email.isNotBlank() && user.email.contains("@")) {
            user.email.trim()
        } else {
            "${user.udiseCode.trim()}@edudatasync.com"
        }

        val supabaseResult = supabaseAuthService.signUp(
            email = authEmail,
            password = user.passwordHash,
            udiseCode = user.udiseCode,
            schoolName = user.schoolName,
            hmName = user.headmasterName,
            phone = user.phone,
            role = user.role
        )

        val updatedUser = user.copy(email = authEmail)
        // Save locally to Room database
        userDao.insertUser(updatedUser)
        val existingSchool = schoolDao.getSchoolById(updatedUser.udiseCode)
        if (existingSchool == null && updatedUser.role != "OFFICER") {
            schoolDao.insertSchool(
                SchoolEntity(
                    id = updatedUser.udiseCode,
                    name = updatedUser.schoolName,
                    category = "Secondary",
                    district = "Central District",
                    headmasterName = updatedUser.headmasterName,
                    headmasterPhone = updatedUser.phone,
                    email = updatedUser.email
                )
            )
        }

        return if (supabaseResult.isSuccess) {
            Result.success(updatedUser)
        } else {
            val err = supabaseResult.exceptionOrNull()?.message
            if (err != null && err.contains("User already registered", ignoreCase = true)) {
                return Result.failure(Exception(err))
            }
            Result.success(updatedUser)
        }
    }

    suspend fun refreshCurrentSessionIfNeeded(): String? {
        var token = getSavedSessionToken()
        val refreshTokenStr = getSavedRefreshToken()

        if (!refreshTokenStr.isNullOrBlank()) {
            val refreshRes = supabaseAuthService.refreshToken(refreshTokenStr)
            val newAuth = refreshRes.getOrNull()
            if (newAuth?.access_token != null) {
                token = newAuth.access_token
                val userId = getSavedSessionUserId() ?: ""
                val email = getSavedSessionEmail() ?: ""
                val fullName = sessionPrefs.getString("session_full_name", "") ?: ""
                val role = sessionPrefs.getString("session_role", "") ?: ""
                val newRefresh = newAuth.refresh_token ?: refreshTokenStr
                saveSession(userId, email, token, fullName, role, newRefresh)
                android.util.Log.d("EduRepository", "Successfully refreshed Officer session token.")
            } else {
                android.util.Log.w("EduRepository", "Token refresh attempted but failed or returned null, using saved token.")
            }
        }
        return token
    }

    suspend fun createAccount(
        email: String,
        password: String,
        fullName: String,
        role: String,
        schoolName: String?,
        udiseNumber: String?
    ): Result<Boolean> {
        val token = refreshCurrentSessionIfNeeded() ?: getSavedSessionToken()
        val result = supabaseAuthService.createAccount(
            email = email,
            password = password,
            fullName = fullName,
            role = role,
            schoolName = schoolName,
            udiseNumber = udiseNumber,
            accessToken = token
        )

        if (result.isFailure) {
            val err = result.exceptionOrNull()?.message ?: "Account creation failed."
            return Result.failure(Exception(err))
        }

        syncUsersFromSupabase()
        return Result.success(true)
    }

    suspend fun updateSchoolContact(schoolId: String, phone: String, email: String) {
        schoolDao.updateSchoolContact(schoolId, phone, email)
    }

    suspend fun getValuesForSubmissionList(submissionId: String): List<SubmissionValueEntity> {
        return submissionDao.getValuesForSubmissionList(submissionId)
    }

    suspend fun updateSubmissionSyncStatus(submissionId: String, status: String) {
        submissionDao.updateSyncStatus(submissionId, status)
    }

    suspend fun deleteSubmissionWithValues(formId: String, schoolId: String) {
        submissionDao.deleteSubmissionWithValues(formId, schoolId)
    }

    suspend fun deletePublishedForm(formId: String) {
        submissionDao.deleteAllSubmissionsForForm(formId)
        formDao.deletePublishedFormCompletely(formId)
    }

    suspend fun getFormWithFields(formId: String): FormWithFields? {
        val form = formDao.getFormById(formId) ?: return null
        val fields = formDao.getFieldsForFormList(formId)
        return FormWithFields(form, fields)
    }

    suspend fun getSubmissionsForFormWithValues(formId: String): List<SubmissionWithValues> {
        val submissions = submissionDao.getSubmissionsForForm(formId).firstOrNull() ?: emptyList()
        return submissions.map { sub ->
            val values = submissionDao.getValuesForSubmissionList(sub.id)
            SubmissionWithValues(sub, values)
        }
    }

    suspend fun getAllSubmissionsWithValues(): List<SubmissionWithValues> {
        val submissions = submissionDao.getAllSubmissions().firstOrNull() ?: emptyList()
        return submissions.map { sub ->
            val values = submissionDao.getValuesForSubmissionList(sub.id)
            SubmissionWithValues(sub, values)
        }
    }

    suspend fun createFormWithFields(form: FormEntity, fields: List<FormFieldEntity>) {
        formDao.insertFormWithFields(form, fields)
    }

    suspend fun submitFormResponse(
        formId: String,
        formTitle: String,
        school: SchoolEntity,
        valuesMap: Map<String, Pair<String, String>> // fieldId -> Pair(fieldLabel, value)
    ): String {
        val submissionId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val submission = SubmissionEntity(
            id = submissionId,
            formId = formId,
            formTitle = formTitle,
            schoolId = school.id,
            schoolName = school.name,
            submittedBy = school.headmasterName,
            submittedAt = timestamp,
            syncStatus = "SYNCED"
        )

        val valueEntities = valuesMap.map { (fieldId, labelAndVal) ->
            SubmissionValueEntity(
                id = UUID.randomUUID().toString(),
                submissionId = submissionId,
                fieldId = fieldId,
                fieldLabel = labelAndVal.first,
                value = labelAndVal.second
            )
        }

        submissionDao.insertSubmissionWithValues(submission, valueEntities)

        // Sync to Google Sheets
        val config = syncConfigDao.getSyncConfigOnce() ?: SyncConfigEntity()
        val formWithFields = getFormWithFields(formId)
        if (formWithFields != null) {
            val subWithValues = SubmissionWithValues(submission, valueEntities)
            googleSheetsService.syncSubmissionToGoogleSheet(
                webhookUrl = config.appsScriptWebhookUrl,
                spreadsheetId = config.googleSheetId,
                formWithFields = formWithFields,
                submissionWithValues = subWithValues
            )
        }

        return submissionId
    }

    suspend fun createFormFromExcelHeaders(
        title: String,
        description: String,
        createdBy: String,
        headers: List<String>
    ): String {
        val formId = UUID.randomUUID().toString()
        val form = FormEntity(
            id = formId,
            title = title,
            description = description,
            createdBy = createdBy,
            createdAt = System.currentTimeMillis()
        )

        val fields = headers.mapIndexed { index, header ->
            FormFieldEntity(
                id = UUID.randomUUID().toString(),
                formId = formId,
                label = header.trim(),
                fieldType = if (header.lowercase().contains("count") || header.lowercase().contains("number") || header.lowercase().contains("total")) "NUMBER" else "TEXT",
                isRequired = true,
                orderIndex = index
            )
        }

        formDao.insertFormWithFields(form, fields)
        return formId
    }

    suspend fun saveSyncConfig(config: SyncConfigEntity) {
        syncConfigDao.saveSyncConfig(config)
    }

    fun exportFormToPdf(formWithFields: FormWithFields, submissions: List<SubmissionWithValues>): File {
        return exportManager.generatePdfReport(formWithFields, submissions)
    }

    fun exportFormToExcel(formWithFields: FormWithFields, submissions: List<SubmissionWithValues>): File {
        return exportManager.generateExcelFile(formWithFields, submissions)
    }

    private suspend fun seedInitialDataIfNeeded() {
        // Clean database initialization: No pre-seeded demo users or default credentials
    }
}
