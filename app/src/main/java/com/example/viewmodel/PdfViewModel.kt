package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AdConfig
import com.example.data.RecentPdf
import com.example.data.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val DEFAULT_SYNC_URL = "https://kvdb.io/C84v7Y8NnZ5KzYmQ9K1J/nirvana_pdf_ad_campaign"

    init {
        // Fetch ad configuration from network on application startup
        viewModelScope.launch {
            fetchAndSyncAdConfig()
        }
        // Register or update installed user metrics dynamically
        registerUserIfNeeded()
    }

    // Recent PDFs Flow
    val recentPdfs: StateFlow<List<RecentPdf>> = repository.allRecent
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Ad Config Flow
    val adConfig: StateFlow<AdConfig?> = repository.adConfigFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AdConfig(
                id = 1,
                bannerUrl = "https://picsum.photos/800/450",
                redirectUrl = "https://ai.studio/build",
                isAdEnabled = true,
                telegramUsername = "tg_admin_support",
                customSyncUrl = ""
            )
        )

    // Current State of PDF Viewer
    var currentPdfUri by mutableStateOf<Uri?>(null)
        private set

    var currentPdfName by mutableStateOf("")
        private set

    // Ad overlay state
    var isAdShowing by mutableStateOf(false)
        private set

    fun openPdf(uri: Uri, context: Context) {
        val (name, sizeStr) = getPdfMetadata(context, uri)
        currentPdfName = name

        // Store in database
        viewModelScope.launch {
            repository.insertRecent(
                RecentPdf(
                    uri = uri.toString(),
                    displayName = name,
                    size = sizeStr,
                    lastOpened = System.currentTimeMillis()
                )
            )

            // Try to fetch latest campaign from online before showing the screen
            try {
                fetchAndSyncAdConfig()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Read latest synced ad configuration
            val config = repository.getAdConfig() ?: AdConfig(
                id = 1,
                bannerUrl = "https://picsum.photos/800/450",
                redirectUrl = "https://ai.studio/build",
                isAdEnabled = true,
                telegramUsername = "tg_admin_support",
                customSyncUrl = ""
            )

            currentPdfUri = uri
            if (config.isAdEnabled) {
                isAdShowing = true
            } else {
                isAdShowing = false
            }
        }
    }

    fun dismissAd() {
        isAdShowing = false
    }

    fun closePdf() {
        currentPdfUri = null
        currentPdfName = ""
        isAdShowing = false
    }

    fun deleteRecent(pdf: RecentPdf) {
        viewModelScope.launch {
            repository.deleteRecent(pdf.uri)
        }
    }

    fun clearAllRecent() {
        viewModelScope.launch {
            repository.clearRecent()
        }
    }

    fun updateAdSettings(bannerUrl: String, redirectUrl: String, isEnabled: Boolean, telegramUsername: String, customSyncUrl: String) {
        viewModelScope.launch {
            val newConfig = AdConfig(
                id = 1,
                bannerUrl = bannerUrl,
                redirectUrl = redirectUrl,
                isAdEnabled = isEnabled,
                telegramUsername = telegramUsername,
                customSyncUrl = customSyncUrl
            )
            repository.saveAdConfig(newConfig)
            // Push changes to the network
            publishAdConfig(newConfig)
        }
    }

    // Fetch latest ad config from remote URL (default or custom)
    suspend fun fetchAndSyncAdConfig() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val currentConfig = repository.getAdConfig()
                val syncUrl = if (currentConfig != null && currentConfig.customSyncUrl.isNotBlank()) {
                    currentConfig.customSyncUrl
                } else {
                    DEFAULT_SYNC_URL
                }

                val jsonStr = httpGet(syncUrl)
                if (!jsonStr.isNullOrBlank()) {
                    val jsonObject = org.json.JSONObject(jsonStr)
                    val isEnabled = jsonObject.optBoolean("isAdEnabled", true)
                    val banner = jsonObject.optString("bannerUrl", "")
                    val redirect = jsonObject.optString("redirectUrl", "")
                    val tgUsername = jsonObject.optString("telegramUsername", "tg_admin_support")
                    
                    // Keep the custom URL if already set locally
                    val customUrl = currentConfig?.customSyncUrl ?: ""

                    repository.saveAdConfig(
                        AdConfig(
                            id = 1,
                            bannerUrl = banner,
                            redirectUrl = redirect,
                            isAdEnabled = isEnabled,
                            telegramUsername = tgUsername,
                            customSyncUrl = customUrl
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Publish current local ad config to network (default or writable custom)
    private fun publishAdConfig(config: AdConfig) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val json = org.json.JSONObject().apply {
                    put("isAdEnabled", config.isAdEnabled)
                    put("bannerUrl", config.bannerUrl)
                    put("redirectUrl", config.redirectUrl)
                    put("telegramUsername", config.telegramUsername)
                }.toString()

                // Always publish to default sync URL so all standard app clients get it
                httpPut(DEFAULT_SYNC_URL, json)

                // Also try to publish to custom sync URL if it supports PUT (e.g. KVDB)
                if (config.customSyncUrl.isNotBlank() && config.customSyncUrl.contains("kvdb.io")) {
                    httpPut(config.customSyncUrl, json)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun httpGet(urlStr: String): String? {
        var connection: java.net.HttpURLConnection? = null
        try {
            val url = java.net.URL(urlStr)
            connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.setRequestProperty("Accept", "application/json")
            
            val responseCode = connection.responseCode
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                return response.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        return null
    }

    private fun httpPut(urlStr: String, jsonBody: String): Boolean {
        var connection: java.net.HttpURLConnection? = null
        try {
            val url = java.net.URL(urlStr)
            connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "PUT"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            
            val writer = java.io.OutputStreamWriter(connection.outputStream)
            writer.write(jsonBody)
            writer.flush()
            writer.close()
            
            val responseCode = connection.responseCode
            return responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        return false
    }

    private fun getPdfMetadata(context: Context, uri: Uri): Pair<String, String> {
        var name = "Unnamed.pdf"
        var sizeStr = "Unknown size"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                    if (sizeIndex != -1) {
                        val sizeBytes = cursor.getLong(sizeIndex)
                        sizeStr = formatFileSize(sizeBytes)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback for file uris or cases where metadata query fails
        if (name == "Unnamed.pdf" && uri.path != null) {
            val lastSegment = uri.lastPathSegment
            if (lastSegment != null) {
                name = if (lastSegment.lowercase().endsWith(".pdf")) {
                    lastSegment
                } else {
                    "$lastSegment.pdf"
                }
            }
        }

        return Pair(name, sizeStr)
    }

    private val _installedUsers = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.data.InstalledUser>>(emptyList())
    val installedUsers: StateFlow<List<com.example.data.InstalledUser>> = _installedUsers

    private val _isLoadingUsers = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isLoadingUsers: StateFlow<Boolean> = _isLoadingUsers

    fun loadInstalledUsers() {
        viewModelScope.launch {
            _isLoadingUsers.value = true
            try {
                val list = fetchInstalledUsers()
                _installedUsers.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingUsers.value = false
            }
        }
    }

    suspend fun fetchInstalledUsers(): List<com.example.data.InstalledUser> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val users = mutableListOf<com.example.data.InstalledUser>()
            try {
                val listUrl = "https://kvdb.io/C84v7Y8NnZ5KzYmQ9K1J/?prefix=nirvana_pdf_user_"
                val response = httpGet(listUrl)
                if (!response.isNullOrBlank()) {
                    val keys = mutableListOf<String>()
                    if (response.trim().startsWith("[")) {
                        // JSON Array
                        val jsonArray = org.json.JSONArray(response)
                        for (i in 0 until jsonArray.length()) {
                            keys.add(jsonArray.getString(i))
                        }
                    } else {
                        // Newline separated list
                        response.split("\n").forEach { line ->
                            val k = line.trim()
                            if (k.isNotBlank()) {
                                keys.add(k)
                            }
                        }
                    }

                    // Sort keys and take up to latest 50
                    val sortedKeys = keys.distinct().sortedDescending().take(50)
                    for (key in sortedKeys) {
                        try {
                            val userJson = httpGet("https://kvdb.io/C84v7Y8NnZ5KzYmQ9K1J/$key")
                            if (!userJson.isNullOrBlank()) {
                                val obj = org.json.JSONObject(userJson)
                                users.add(
                                    com.example.data.InstalledUser(
                                        userId = obj.optString("userId", ""),
                                        deviceModel = obj.optString("deviceModel", "دستگاه نامشخص"),
                                        installTime = obj.optString("installTime", "تاریخ نامشخص"),
                                        lastActive = obj.optString("lastActive", "زمان نامشخص")
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            users
        }
    }

    private fun registerUserIfNeeded() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val sharedPref = getApplication<Application>().getSharedPreferences("nirvana_pdf_prefs", Context.MODE_PRIVATE)
                var userId = sharedPref.getString("user_id", null)
                val currentDate = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())

                if (userId == null) {
                    userId = java.util.UUID.randomUUID().toString().substring(0, 8)
                    sharedPref.edit().putString("user_id", userId).apply()
                }

                val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                var installTime = sharedPref.getString("install_time", null)
                if (installTime == null) {
                    installTime = currentDate
                    sharedPref.edit().putString("install_time", installTime).apply()
                }

                val userObj = org.json.JSONObject().apply {
                    put("userId", userId)
                    put("deviceModel", deviceModel)
                    put("installTime", installTime)
                    put("lastActive", currentDate)
                }

                httpPut("https://kvdb.io/C84v7Y8NnZ5KzYmQ9K1J/nirvana_pdf_user_$userId", userObj.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = if (digitGroups < units.size) digitGroups else units.size - 1
        return String.format("%.1f %s", bytes / Math.pow(1024.0, index.toDouble()), units[index])
    }
}

class PdfViewModelFactory(
    private val application: Application,
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PdfViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
