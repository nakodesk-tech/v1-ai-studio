package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "schools")
data class SchoolEntity(
    @PrimaryKey val id: String, // UDISE Code or unique ID
    val name: String,
    val category: String,
    val district: String,
    val headmasterName: String,
    val headmasterPhone: String,
    val email: String = ""
)

@Entity(tableName = "forms")
data class FormEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val createdBy: String,
    val createdAt: Long,
    val status: String = "ACTIVE", // ACTIVE, ARCHIVED
    val linkedGoogleSheetId: String = ""
)

@Entity(
    tableName = "form_fields",
    foreignKeys = [
        ForeignKey(
            entity = FormEntity::class,
            parentColumns = ["id"],
            childColumns = ["formId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["formId"])]
)
data class FormFieldEntity(
    @PrimaryKey val id: String,
    val formId: String,
    val label: String,
    val fieldType: String, // TEXT, NUMBER, DROPDOWN, DATE, CHECKBOX
    val options: String = "", // Comma-separated options for dropdown
    val isRequired: Boolean = true,
    val orderIndex: Int = 0
)

@Entity(tableName = "submissions")
data class SubmissionEntity(
    @PrimaryKey val id: String,
    val formId: String,
    val formTitle: String,
    val schoolId: String,
    val schoolName: String,
    val submittedBy: String,
    val submittedAt: Long,
    val syncStatus: String = "SYNCED", // SYNCED, PENDING, FAILED
    val googleDriveFileUrl: String = ""
)

@Entity(
    tableName = "submission_values",
    foreignKeys = [
        ForeignKey(
            entity = SubmissionEntity::class,
            parentColumns = ["id"],
            childColumns = ["submissionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["submissionId"])]
)
data class SubmissionValueEntity(
    @PrimaryKey val id: String,
    val submissionId: String,
    val fieldId: String,
    val fieldLabel: String,
    val value: String
)

@Entity(tableName = "sync_config")
data class SyncConfigEntity(
    @PrimaryKey val id: Int = 1,
    val officerDriveEmail: String = "officer.main@education.gov.in",
    val googleSheetId: String = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms",
    val appsScriptWebhookUrl: String = "https://script.google.com/macros/s/AKfycbx_edudata_sample/exec",
    val autoSync: Boolean = true,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val udiseCode: String,
    val schoolName: String,
    val headmasterName: String,
    val phone: String,
    val passwordHash: String,
    val role: String = "HEADMASTER", // HEADMASTER or OFFICER
    val registeredAt: Long = System.currentTimeMillis(),
    val email: String = "",
    val udiseNumber: String = ""
)

// Data Transfer Objects for Full Form View & Submissions
data class FormWithFields(
    val form: FormEntity,
    val fields: List<FormFieldEntity>
)

data class SubmissionWithValues(
    val submission: SubmissionEntity,
    val values: List<SubmissionValueEntity>
)
