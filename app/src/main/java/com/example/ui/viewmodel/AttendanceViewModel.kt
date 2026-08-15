package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AttendanceRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val attendanceDao = database.attendanceDao()

    val attendanceRecords: StateFlow<List<AttendanceRecord>> = attendanceDao.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val primaryUrl = "https://smpn1banjarmasin.sch.id/absen/"

    private val _currentUrl = MutableStateFlow(primaryUrl)
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _pageTitle = MutableStateFlow("Absensi Online SMPN 1 Banjarmasin")
    val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0)
    val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _showSchoolInfo = MutableStateFlow(false)
    val showSchoolInfo: StateFlow<Boolean> = _showSchoolInfo.asStateFlow()

    private val _showAttendanceSheet = MutableStateFlow(false)
    val showAttendanceSheet: StateFlow<Boolean> = _showAttendanceSheet.asStateFlow()

    private val _showPermissionGuide = MutableStateFlow(false)
    val showPermissionGuide: StateFlow<Boolean> = _showPermissionGuide.asStateFlow()

    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    init {
        monitorNetwork()
    }

    private fun monitorNetwork() {
        val manager = connectivityManager ?: return
        val isCurrentlyConnected = manager.activeNetwork?.let { network ->
            val capabilities = manager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } ?: true
        _isOnline.value = isCurrentlyConnected

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            manager.registerNetworkCallback(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isOnline.value = true
                    }

                    override fun onLost(network: Network) {
                        _isOnline.value = false
                    }
                }
            )
        } catch (_: Exception) {
            // Fallback gracefully
        }
    }

    fun updateProgress(progress: Int) {
        _loadingProgress.value = progress
        _isLoading.value = progress in 1..99
    }

    fun updatePageTitle(title: String?) {
        if (!title.isNullOrBlank() && !title.startsWith("http")) {
            _pageTitle.value = title
        } else {
            _pageTitle.value = "Presensi SMPN 1 Banjarmasin"
        }
    }

    fun updateNavigationState(canBack: Boolean, canForward: Boolean, url: String?) {
        _canGoBack.value = canBack
        _canGoForward.value = canForward
        if (!url.isNullOrBlank()) {
            _currentUrl.value = url
        }
    }

    fun setError(error: Boolean, message: String = "") {
        _hasError.value = error
        _errorMessage.value = message
    }

    fun setSchoolInfoVisible(show: Boolean) {
        _showSchoolInfo.value = show
    }

    fun setAttendanceSheetVisible(show: Boolean) {
        _showAttendanceSheet.value = show
    }

    fun setPermissionGuideVisible(show: Boolean) {
        _showPermissionGuide.value = show
    }

    fun logAttendance(type: String, note: String = "") {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            val timeFormat = SimpleDateFormat("HH:mm 'WITA'", Locale("id", "ID"))
            val now = Date()
            val record = AttendanceRecord(
                date = dateFormat.format(now),
                time = timeFormat.format(now),
                type = type,
                note = note,
                timestamp = System.currentTimeMillis()
            )
            attendanceDao.insertRecord(record)
        }
    }

    fun deleteAttendanceRecord(record: AttendanceRecord) {
        viewModelScope.launch {
            attendanceDao.deleteRecord(record)
        }
    }

    fun clearAllAttendanceRecords() {
        viewModelScope.launch {
            attendanceDao.clearAllRecords()
        }
    }
}
