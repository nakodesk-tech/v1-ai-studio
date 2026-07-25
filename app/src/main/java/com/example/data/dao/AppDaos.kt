package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {
    @Query("SELECT * FROM schools ORDER BY name ASC")
    fun getAllSchools(): Flow<List<SchoolEntity>>

    @Query("SELECT * FROM schools WHERE id = :id")
    suspend fun getSchoolById(id: String): SchoolEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchool(school: SchoolEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchools(schools: List<SchoolEntity>)

    @Query("UPDATE schools SET headmasterPhone = :phone, email = :email WHERE id = :schoolId")
    suspend fun updateSchoolContact(schoolId: String, phone: String, email: String)

    @Query("SELECT COUNT(*) FROM schools")
    fun getSchoolCount(): Flow<Int>

    @Query("DELETE FROM schools WHERE LOWER(id) = LOWER(:schoolId)")
    suspend fun deleteSchool(schoolId: String)
}

@Dao
interface FormDao {
    @Query("SELECT * FROM forms ORDER BY createdAt DESC")
    fun getAllForms(): Flow<List<FormEntity>>

    @Query("SELECT * FROM forms WHERE id = :id")
    suspend fun getFormById(id: String): FormEntity?

    @Query("SELECT * FROM form_fields WHERE formId = :formId ORDER BY orderIndex ASC")
    fun getFieldsForForm(formId: String): Flow<List<FormFieldEntity>>

    @Query("SELECT * FROM form_fields WHERE formId = :formId ORDER BY orderIndex ASC")
    suspend fun getFieldsForFormList(formId: String): List<FormFieldEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForm(form: FormEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFormFields(fields: List<FormFieldEntity>)

    @Transaction
    suspend fun insertFormWithFields(form: FormEntity, fields: List<FormFieldEntity>) {
        insertForm(form)
        insertFormFields(fields)
    }

    @Query("DELETE FROM form_fields WHERE formId = :formId")
    suspend fun deleteFormFields(formId: String)

    @Query("DELETE FROM forms WHERE id = :formId")
    suspend fun deleteForm(formId: String)

    @Transaction
    suspend fun deletePublishedFormCompletely(formId: String) {
        deleteFormFields(formId)
        deleteForm(formId)
    }

    @Query("SELECT COUNT(*) FROM forms")
    fun getFormCount(): Flow<Int>
}

@Dao
interface SubmissionDao {
    @Query("SELECT * FROM submissions ORDER BY submittedAt DESC")
    fun getAllSubmissions(): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE formId = :formId ORDER BY submittedAt DESC")
    fun getSubmissionsForForm(formId: String): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE schoolId = :schoolId ORDER BY submittedAt DESC")
    fun getSubmissionsForSchool(schoolId: String): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submission_values WHERE submissionId = :submissionId")
    fun getValuesForSubmission(submissionId: String): Flow<List<SubmissionValueEntity>>

    @Query("SELECT * FROM submission_values WHERE submissionId = :submissionId")
    suspend fun getValuesForSubmissionList(submissionId: String): List<SubmissionValueEntity>

    @Query("SELECT * FROM submissions WHERE id = :id")
    suspend fun getSubmissionById(id: String): SubmissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: SubmissionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmissionValues(values: List<SubmissionValueEntity>)

    @Transaction
    suspend fun insertSubmissionWithValues(submission: SubmissionEntity, values: List<SubmissionValueEntity>) {
        insertSubmission(submission)
        insertSubmissionValues(values)
    }

    @Query("UPDATE submissions SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM submissions WHERE formId = :formId AND LOWER(schoolId) = LOWER(:schoolId)")
    suspend fun deleteSubmissionForFormAndSchool(formId: String, schoolId: String)

    @Query("DELETE FROM submission_values WHERE submissionId IN (SELECT id FROM submissions WHERE formId = :formId AND LOWER(schoolId) = LOWER(:schoolId))")
    suspend fun deleteSubmissionValuesForFormAndSchool(formId: String, schoolId: String)

    @Transaction
    suspend fun deleteSubmissionWithValues(formId: String, schoolId: String) {
        deleteSubmissionValuesForFormAndSchool(formId, schoolId)
        deleteSubmissionForFormAndSchool(formId, schoolId)
    }

    @Query("DELETE FROM submission_values WHERE submissionId IN (SELECT id FROM submissions WHERE formId = :formId)")
    suspend fun deleteSubmissionValuesForForm(formId: String)

    @Query("DELETE FROM submissions WHERE formId = :formId")
    suspend fun deleteSubmissionsForForm(formId: String)

    @Transaction
    suspend fun deleteAllSubmissionsForForm(formId: String) {
        deleteSubmissionValuesForForm(formId)
        deleteSubmissionsForForm(formId)
    }

    @Query("SELECT COUNT(*) FROM submissions")
    fun getSubmissionCount(): Flow<Int>
}

@Dao
interface SyncConfigDao {
    @Query("SELECT * FROM sync_config WHERE id = 1")
    fun getSyncConfig(): Flow<SyncConfigEntity?>

    @Query("SELECT * FROM sync_config WHERE id = 1")
    suspend fun getSyncConfigOnce(): SyncConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSyncConfig(config: SyncConfigEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE LOWER(udiseCode) = LOWER(:udiseCode) LIMIT 1")
    suspend fun getUserByUdise(udiseCode: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(udiseCode) = LOWER(:udiseCode) AND passwordHash = :password LIMIT 1")
    suspend fun login(udiseCode: String, password: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("SELECT * FROM users ORDER BY registeredAt DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("UPDATE users SET passwordHash = :newPassword WHERE LOWER(udiseCode) = LOWER(:udiseCode)")
    suspend fun resetUserPassword(udiseCode: String, newPassword: String)

    @Query("DELETE FROM users WHERE LOWER(udiseCode) = LOWER(:udiseCode)")
    suspend fun deleteUser(udiseCode: String)

    @Query("UPDATE users SET headmasterName = :name, phone = :phone, email = :email, schoolName = :schoolName WHERE LOWER(udiseCode) = LOWER(:udiseCode)")
    suspend fun updateUserInfo(udiseCode: String, name: String, phone: String, email: String, schoolName: String)
}
