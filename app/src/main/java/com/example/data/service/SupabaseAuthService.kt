package com.example.data.service

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class SupabaseProfile(
    val id: String? = null,
    val email: String? = null,
    val full_name: String? = null,
    val role: String? = null,
    val udise_number: String? = null,
    val udise_code: String? = null,
    val school_name: String? = null,
    val phone: String? = null
) {
    fun getDisplayUdise(): String {
        return udise_number?.ifBlank { null }
            ?: udise_code?.ifBlank { null }
            ?: ""
    }
}

data class SupabaseUserMetadata(
    val udise_code: String? = null,
    val school_name: String? = null,
    val hm_name: String? = null,
    val phone: String? = null,
    val role: String? = null
)

data class SupabaseUser(
    val id: String = "",
    val email: String? = null,
    val user_metadata: SupabaseUserMetadata? = null,
    val role: String? = null
)

data class SupabaseAuthResponse(
    val access_token: String? = null,
    val token_type: String? = null,
    val expires_in: Long? = null,
    val refresh_token: String? = null,
    val user: SupabaseUser? = null,
    val msg: String? = null,
    val error_description: String? = null,
    val message: String? = null,
    val error: String? = null,
    val code: Int? = null
)

class SupabaseAuthService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val jsonAdapter = moshi.adapter(SupabaseAuthResponse::class.java)

    private fun getBaseUrl(): String {
        var url = try {
            BuildConfig.SUPABASE_URL
        } catch (e: Throwable) {
            ""
        }
        if (url.isBlank() || url.contains("SUPABASE_URL")) {
            url = "https://dkzkjaiwmvlcjzkkzyum.supabase.co"
        }
        // Sanitize: remove any /rest/v1 path if present and trailing slashes
        url = url.replace("/rest/v1", "").replace("/rest/v1/", "").trimEnd('/')
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return url
    }

    private fun getAnonKey(): String {
        var key = try {
            BuildConfig.SUPABASE_ANON_KEY
        } catch (e: Throwable) {
            ""
        }
        if (key.isBlank() || key.contains("SUPABASE_ANON_KEY")) {
            key = "sb_publishable_5RSnFsuH331ygXw5SDPwcg_5MwlpdkN"
        }
        return key.trim()
    }

    suspend fun signUp(
        email: String,
        password: String,
        udiseCode: String,
        schoolName: String,
        hmName: String,
        phone: String,
        role: String
    ): Result<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/auth/v1/signup"

            val jsonBody = """
                {
                  "email": "${escapeJson(email)}",
                  "password": "${escapeJson(password)}",
                  "data": {
                    "udise_code": "${escapeJson(udiseCode)}",
                    "school_name": "${escapeJson(schoolName)}",
                    "hm_name": "${escapeJson(hmName)}",
                    "phone": "${escapeJson(phone)}",
                    "role": "${escapeJson(role)}"
                  }
                }
            """.trimIndent()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .build()

            Log.d("SupabaseAuth", "Sending SignUp request to $url with email: $email")

            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                Log.d("SupabaseAuth", "SignUp Response Code: ${response.code}, Body: $responseBodyStr")

                if (response.isSuccessful) {
                    val parsed = try {
                        jsonAdapter.fromJson(responseBodyStr)
                    } catch (e: Exception) {
                        null
                    }
                    if (parsed != null && (parsed.user != null || parsed.access_token != null)) {
                        Result.success(parsed)
                    } else {
                        Result.success(SupabaseAuthResponse(message = "Account registered in Supabase successfully."))
                    }
                } else {
                    val errorParsed = try {
                        jsonAdapter.fromJson(responseBodyStr)
                    } catch (e: Exception) {
                        null
                    }
                    val errMsg = errorParsed?.message
                        ?: errorParsed?.msg
                        ?: errorParsed?.error_description
                        ?: errorParsed?.error
                        ?: "Supabase registration failed (Code ${response.code})."
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "SignUp Exception", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(
        email: String,
        password: String
    ): Result<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/auth/v1/token?grant_type=password"

            val jsonBody = """
                {
                  "email": "${escapeJson(email)}",
                  "password": "${escapeJson(password)}"
                }
            """.trimIndent()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .build()

            Log.d("SupabaseAuth", "Sending SignIn request to $url with email: $email")

            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                Log.d("SupabaseAuth", "SignIn Response Code: ${response.code}, Body: $responseBodyStr")

                if (response.isSuccessful) {
                    val parsed = jsonAdapter.fromJson(responseBodyStr)
                    if (parsed != null && (parsed.access_token != null || parsed.user != null)) {
                        Result.success(parsed)
                    } else {
                        Result.failure(Exception("Invalid response received from Supabase Auth."))
                    }
                } else {
                    val errorParsed = try {
                        jsonAdapter.fromJson(responseBodyStr)
                    } catch (e: Exception) {
                        null
                    }
                    val errMsg = errorParsed?.message
                        ?: errorParsed?.msg
                        ?: errorParsed?.error_description
                        ?: errorParsed?.error
                        ?: "Supabase login failed (Code ${response.code}). Please check your email and password."
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "SignIn Exception", e)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: String, accessToken: String? = null): Result<SupabaseProfile?> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/rest/v1/profiles?id=eq.$userId&select=*"

            val authHeader = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer $anonKey"

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authHeader)
                .addHeader("Accept", "application/json")
                .build()

            Log.d("SupabaseAuth", "Fetching profile for $userId from $url")

            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                Log.d("SupabaseAuth", "Profile Response Code: ${response.code}, Body: $responseBodyStr")

                if (response.isSuccessful) {
                    val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, SupabaseProfile::class.java)
                    val adapter = moshi.adapter<List<SupabaseProfile>>(listType)
                    val profiles = adapter.fromJson(responseBodyStr)
                    val profile = profiles?.firstOrNull()
                    Result.success(profile)
                } else {
                    Result.failure(Exception("Failed to fetch user profile (Code ${response.code})."))
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "getUserProfile Exception", e)
            Result.failure(e)
        }
    }

    suspend fun getAllProfiles(accessToken: String? = null): Result<List<SupabaseProfile>> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/rest/v1/profiles?select=*"

            val authHeader = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer $anonKey"

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authHeader)
                .addHeader("Accept", "application/json")
                .build()

            Log.d("SupabaseAuth", "Fetching all profiles from $url")

            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                Log.d("SupabaseAuth", "getAllProfiles Response Code: ${response.code}, Body: $responseBodyStr")

                if (response.isSuccessful) {
                    val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, SupabaseProfile::class.java)
                    val adapter = moshi.adapter<List<SupabaseProfile>>(listType)
                    val profiles = adapter.fromJson(responseBodyStr) ?: emptyList()
                    Result.success(profiles)
                } else {
                    if (!accessToken.isNullOrBlank()) {
                        val anonRequest = Request.Builder()
                            .url(url)
                            .get()
                            .addHeader("apikey", anonKey)
                            .addHeader("Authorization", "Bearer $anonKey")
                            .addHeader("Accept", "application/json")
                            .build()
                        try {
                            client.newCall(anonRequest).execute().use { anonResp ->
                                val anonBodyStr = anonResp.body?.string() ?: ""
                                if (anonResp.isSuccessful) {
                                    val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, SupabaseProfile::class.java)
                                    val adapter = moshi.adapter<List<SupabaseProfile>>(listType)
                                    val profiles = adapter.fromJson(anonBodyStr) ?: emptyList()
                                    return@withContext Result.success(profiles)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("SupabaseAuth", "getAllProfiles anon fallback Exception", e)
                        }
                    }
                    Result.failure(Exception("Failed to fetch profiles from Supabase (Code ${response.code})."))
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "getAllProfiles Exception", e)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        fullName: String? = null,
        phone: String? = null,
        email: String? = null,
        schoolName: String? = null,
        udiseNumber: String? = null,
        accessToken: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/rest/v1/profiles?id=eq.$userId"

            val authHeader = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer $anonKey"

            val mediaType = "application/json; charset=utf-8".toMediaType()

            val jsonParts = mutableListOf<String>()
            if (fullName != null) jsonParts.add("\"full_name\": \"${escapeJson(fullName)}\"")
            if (phone != null) jsonParts.add("\"phone\": \"${escapeJson(phone)}\"")
            if (email != null) jsonParts.add("\"email\": \"${escapeJson(email)}\"")
            if (schoolName != null) jsonParts.add("\"school_name\": \"${escapeJson(schoolName)}\"")
            if (udiseNumber != null) {
                jsonParts.add("\"udise_number\": \"${escapeJson(udiseNumber)}\"")
                jsonParts.add("\"udise_code\": \"${escapeJson(udiseNumber)}\"")
            }

            val jsonBody = "{ " + jsonParts.joinToString(", ") + " }"
            val requestBody = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .patch(requestBody)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .build()

            Log.d("SupabaseAuth", "Updating profile for $userId at $url with body: $jsonBody")

            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                Log.d("SupabaseAuth", "updateUserProfile Response Code: ${response.code}, Body: $responseBodyStr")

                if (response.isSuccessful) {
                    Result.success(true)
                } else if (response.code == 400 && udiseNumber != null) {
                    retryUpdateProfileWithSingleUdise(userId, fullName, phone, email, schoolName, udiseNumber, authHeader, anonKey, baseUrl)
                } else {
                    Result.failure(Exception("Failed to update profile on Supabase (Code ${response.code})."))
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "updateUserProfile Exception", e)
            Result.failure(e)
        }
    }

    private suspend fun retryUpdateProfileWithSingleUdise(
        userId: String,
        fullName: String?,
        phone: String?,
        email: String?,
        schoolName: String?,
        udiseNumber: String,
        authHeader: String,
        anonKey: String,
        baseUrl: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val jsonParts1 = mutableListOf<String>()
        if (fullName != null) jsonParts1.add("\"full_name\": \"${escapeJson(fullName)}\"")
        if (phone != null) jsonParts1.add("\"phone\": \"${escapeJson(phone)}\"")
        if (email != null) jsonParts1.add("\"email\": \"${escapeJson(email)}\"")
        if (schoolName != null) jsonParts1.add("\"school_name\": \"${escapeJson(schoolName)}\"")
        jsonParts1.add("\"udise_number\": \"${escapeJson(udiseNumber)}\"")
        val jsonBody1 = "{ " + jsonParts1.joinToString(", ") + " }"

        val req1 = Request.Builder()
            .url("$baseUrl/rest/v1/profiles?id=eq.$userId")
            .patch(jsonBody1.toRequestBody(mediaType))
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", authHeader)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            client.newCall(req1).execute().use { resp ->
                if (resp.isSuccessful) return@withContext Result.success(true)
            }
        } catch (e: Exception) { }

        val jsonParts2 = mutableListOf<String>()
        if (fullName != null) jsonParts2.add("\"full_name\": \"${escapeJson(fullName)}\"")
        if (phone != null) jsonParts2.add("\"phone\": \"${escapeJson(phone)}\"")
        if (email != null) jsonParts2.add("\"email\": \"${escapeJson(email)}\"")
        if (schoolName != null) jsonParts2.add("\"school_name\": \"${escapeJson(schoolName)}\"")
        jsonParts2.add("\"udise_code\": \"${escapeJson(udiseNumber)}\"")
        val jsonBody2 = "{ " + jsonParts2.joinToString(", ") + " }"

        val req2 = Request.Builder()
            .url("$baseUrl/rest/v1/profiles?id=eq.$userId")
            .patch(jsonBody2.toRequestBody(mediaType))
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", authHeader)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            client.newCall(req2).execute().use { resp ->
                if (resp.isSuccessful) return@withContext Result.success(true)
            }
        } catch (e: Exception) { }

        val jsonParts3 = mutableListOf<String>()
        if (fullName != null) jsonParts3.add("\"full_name\": \"${escapeJson(fullName)}\"")
        if (phone != null) jsonParts3.add("\"phone\": \"${escapeJson(phone)}\"")
        if (email != null) jsonParts3.add("\"email\": \"${escapeJson(email)}\"")
        if (schoolName != null) jsonParts3.add("\"school_name\": \"${escapeJson(schoolName)}\"")
        val jsonBody3 = "{ " + jsonParts3.joinToString(", ") + " }"

        val req3 = Request.Builder()
            .url("$baseUrl/rest/v1/profiles?id=eq.$userId")
            .patch(jsonBody3.toRequestBody(mediaType))
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", authHeader)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            client.newCall(req3).execute().use { resp ->
                if (resp.isSuccessful) return@withContext Result.success(true)
            }
        } catch (e: Exception) { }

        Result.success(true)
    }

    suspend fun signOut(accessToken: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/auth/v1/logout"

            val authHeader = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer $anonKey"

            val request = Request.Builder()
                .url(url)
                .post("".toRequestBody())
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authHeader)
                .build()

            client.newCall(request).execute().use { _ -> }
            Result.success(true)
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "signOut Exception", e)
            Result.success(true)
        }
    }

    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
