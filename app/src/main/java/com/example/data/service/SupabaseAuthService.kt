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
            if (schoolName != null) jsonParts.add("\"school_name\": \"${escapeJson(schoolName)}\"")
            if (udiseNumber != null) jsonParts.add("\"udise_number\": \"${escapeJson(udiseNumber)}\"")

            if (jsonParts.isEmpty()) {
                return@withContext Result.success(true)
            }

            val jsonBody = "{ " + jsonParts.joinToString(", ") + " }"
            val requestBody = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .patch(requestBody)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build()

            Log.d("SupabaseAuth", "Updating profile for $userId at $url with body: $jsonBody")

            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                Log.d("SupabaseAuth", "updateUserProfile Response Code: ${response.code}, Body: $responseBodyStr")

                if (response.isSuccessful) {
                    val trimmedBody = responseBodyStr.trim()
                    if (trimmedBody == "[]") {
                        Log.e("SupabaseAuth", "0 rows updated in public.profiles for ID $userId. Check RLS policies or user ID.")
                        Result.failure(Exception("Supabase returned 0 updated rows. Check RLS policy on public.profiles or user ID."))
                    } else {
                        Result.success(true)
                    }
                } else {
                    val errorDetail = if (responseBodyStr.isNotBlank()) responseBodyStr else "HTTP Code ${response.code}"
                    Log.e("SupabaseAuth", "updateUserProfile Failed: Code ${response.code}, Detail: $errorDetail")
                    Result.failure(Exception("Supabase update error (Code ${response.code}): $errorDetail"))
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "updateUserProfile Exception", e)
            Result.failure(e)
        }
    }

    suspend fun createAccount(
        email: String,
        password: String,
        fullName: String,
        role: String,
        schoolName: String?,
        udiseNumber: String?,
        accessToken: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val authHeader = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer $anonKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()

            // 1. Attempt secure account creation via Edge Function: /functions/v1/create-user
            val edgeUrl = "$baseUrl/functions/v1/create-user"
            val edgeBody = """
                {
                  "email": "${escapeJson(email)}",
                  "password": "${escapeJson(password)}",
                  "full_name": "${escapeJson(fullName)}",
                  "role": "${escapeJson(role)}",
                  "school_name": ${if (schoolName != null) "\"${escapeJson(schoolName)}\"" else "null"},
                  "udise_number": ${if (udiseNumber != null) "\"${escapeJson(udiseNumber)}\"" else "null"}
                }
            """.trimIndent()

            val edgeReq = Request.Builder()
                .url(edgeUrl)
                .post(edgeBody.toRequestBody(mediaType))
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .build()

            Log.d("SupabaseAuth", "Attempting createAccount via Edge Function at $edgeUrl")

            try {
                client.newCall(edgeReq).execute().use { resp ->
                    val respStr = resp.body?.string() ?: ""
                    Log.d("SupabaseAuth", "Edge Function response code: ${resp.code}, body: $respStr")
                    if (resp.isSuccessful) {
                        return@withContext Result.success(true)
                    } else if (resp.code != 404 && resp.code != 405) {
                        val parsedErr = parseErrorMessage(respStr) ?: "Edge Function error (${resp.code})"
                        return@withContext Result.failure(Exception(parsedErr))
                    }
                }
            } catch (e: Exception) {
                Log.w("SupabaseAuth", "Edge Function call failed, falling back to Auth SignUp API", e)
            }

            // 2. Fallback: Standard Supabase Auth SignUp API
            val fallbackUdise = udiseNumber ?: email.substringBefore("@")
            val signUpRes = signUp(
                email = email,
                password = password,
                udiseCode = fallbackUdise,
                schoolName = schoolName ?: "",
                hmName = fullName,
                phone = "",
                role = role
            )

            if (signUpRes.isFailure) {
                val err = signUpRes.exceptionOrNull()?.message ?: "Supabase registration failed."
                return@withContext Result.failure(Exception(err))
            }

            val authResp = signUpRes.getOrNull()
            val createdId = authResp?.user?.id

            if (!createdId.isNullOrBlank()) {
                updateUserProfile(
                    userId = createdId,
                    fullName = fullName,
                    schoolName = schoolName,
                    udiseNumber = udiseNumber,
                    accessToken = accessToken
                )
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "createAccount Exception", e)
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(jsonStr: String): String? {
        return try {
            val parsed = jsonAdapter.fromJson(jsonStr)
            parsed?.message ?: parsed?.msg ?: parsed?.error_description ?: parsed?.error
        } catch (e: Exception) {
            if (jsonStr.isNotBlank() && !jsonStr.startsWith("<")) jsonStr else null
        }
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
