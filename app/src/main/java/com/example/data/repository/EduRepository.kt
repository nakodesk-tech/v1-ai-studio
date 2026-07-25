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

    suspend fun updateUserInfo(udiseCode: String, name: String, phone: String, email: String, schoolName: String) {
        userDao.updateUserInfo(udiseCode, name, phone, email, schoolName)
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
        return userDao.login(udiseCode.trim(), password)
    }

    suspend fun getUserByUdise(udiseCode: String): UserEntity? {
        return userDao.getUserByUdise(udiseCode.trim())
    }

    suspend fun registerUser(user: UserEntity) {
        userDao.insertUser(user)
        // Also register school if not already present
        val existingSchool = schoolDao.getSchoolById(user.udiseCode)
        if (existingSchool == null && user.role != "OFFICER") {
            schoolDao.insertSchool(
                SchoolEntity(
                    id = user.udiseCode,
                    name = user.schoolName,
                    category = "Secondary",
                    district = "Central District",
                    headmasterName = user.headmasterName,
                    headmasterPhone = user.phone,
                    email = user.email
                )
            )
        }
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
