package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.ui.components.AbsenWebView
import com.example.ui.components.AppTopBar
import com.example.ui.components.AttendanceNotesSheet
import com.example.ui.components.OfflineStateView
import com.example.ui.components.PermissionGuideDialog
import com.example.ui.components.SchoolInfoDialog
import com.example.ui.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUrl by viewModel.currentUrl.collectAsState()
    val pageTitle by viewModel.pageTitle.collectAsState()
    val loadingProgress by viewModel.loadingProgress.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasError by viewModel.hasError.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val canGoBack by viewModel.canGoBack.collectAsState()
    val canGoForward by viewModel.canGoForward.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    val showSchoolInfo by viewModel.showSchoolInfo.collectAsState()
    val showAttendanceSheet by viewModel.showAttendanceSheet.collectAsState()
    val showPermissionGuide by viewModel.showPermissionGuide.collectAsState()
    val records by viewModel.attendanceRecords.collectAsState()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pendingGeoCallback by remember { mutableStateOf<Pair<String, GeolocationPermissions.Callback>?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // File Chooser Launcher
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        val clipData = result.data?.clipData
        val results: Array<Uri>? = when {
            uri != null -> arrayOf(uri)
            clipData != null -> Array(clipData.itemCount) { index -> clipData.getItemAt(index).uri }
            else -> null
        }
        fileChooserCallback?.onReceiveValue(results)
        fileChooserCallback = null
    }

    // Permission Launcher for GPS and Camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true

        if (fineLocation || coarseLocation) {
            pendingGeoCallback?.let { (origin, callback) ->
                callback.invoke(origin, true, false)
            }
            pendingGeoCallback = null
            scope.launch {
                snackbarHostState.showSnackbar("Izin lokasi GPS berhasil diaktifkan untuk presensi.")
            }
        }

        if (cameraGranted) {
            scope.launch {
                snackbarHostState.showSnackbar("Izin kamera siap digunakan.")
            }
        }
    }

    val requestEssentialPermissions = {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
            )
        )
    }

    // Check permissions on first launch
    LaunchedEffect(Unit) {
        val hasFineLoc = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFineLoc) {
            // Suggest permission guide
            requestEssentialPermissions()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = pageTitle,
                isLoading = isLoading,
                progress = loadingProgress,
                isOnline = isOnline,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                onBackClick = {
                    if (webViewInstance?.canGoBack() == true) {
                        webViewInstance?.goBack()
                    }
                },
                onForwardClick = {
                    if (webViewInstance?.canGoForward() == true) {
                        webViewInstance?.goForward()
                    }
                },
                onRefreshClick = {
                    viewModel.setError(false)
                    webViewInstance?.reload()
                },
                onHomeClick = {
                    viewModel.setError(false)
                    webViewInstance?.loadUrl(viewModel.primaryUrl)
                },
                onOpenAttendanceSheet = {
                    viewModel.setAttendanceSheetVisible(true)
                },
                onCopyUrlClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("Link Absensi SMPN 1", currentUrl)
                    clipboard?.setPrimaryClip(clip)
                    scope.launch {
                        snackbarHostState.showSnackbar("Tautan berhasil disalin ke papan klip.")
                    }
                },
                onOpenExternalBrowserClick = {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                        context.startActivity(browserIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Tidak dapat membuka browser", Toast.LENGTH_SHORT).show()
                    }
                },
                onClearCacheClick = {
                    webViewInstance?.clearCache(true)
                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().flush()
                    viewModel.setError(false)
                    webViewInstance?.loadUrl(viewModel.primaryUrl)
                    scope.launch {
                        snackbarHostState.showSnackbar("Cache dan cookie berhasil dibersihkan.")
                    }
                },
                onShowPermissionsGuide = {
                    viewModel.setPermissionGuideVisible(true)
                },
                onShowSchoolInfo = {
                    viewModel.setSchoolInfoVisible(true)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (hasError && !isOnline) {
                OfflineStateView(
                    errorMessage = errorMessage,
                    onRetry = {
                        viewModel.setError(false)
                        webViewInstance?.loadUrl(currentUrl)
                    }
                )
            } else {
                AbsenWebView(
                    url = viewModel.primaryUrl,
                    onWebViewCreated = { webView ->
                        webViewInstance = webView
                    },
                    onProgressChanged = { progress ->
                        viewModel.updateProgress(progress)
                    },
                    onTitleReceived = { title ->
                        viewModel.updatePageTitle(title)
                    },
                    onNavigationStateChanged = { canBack, canFwd, url ->
                        viewModel.updateNavigationState(canBack, canFwd, url)
                    },
                    onErrorOccurred = { error ->
                        viewModel.setError(true, error)
                    },
                    onPageFinishedLoading = {
                        viewModel.setError(false)
                    },
                    onFileChooserRequested = { callback, params ->
                        fileChooserCallback?.onReceiveValue(null)
                        fileChooserCallback = callback

                        val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }

                        try {
                            fileChooserLauncher.launch(intent)
                            true
                        } catch (e: Exception) {
                            fileChooserCallback = null
                            false
                        }
                    },
                    onGeolocationRequested = { origin, callback ->
                        if (origin != null && callback != null) {
                            val fineLocGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (fineLocGranted) {
                                callback.invoke(origin, true, false)
                            } else {
                                pendingGeoCallback = Pair(origin, callback)
                                requestEssentialPermissions()
                            }
                        }
                    }
                )
            }
        }
    }

    if (showSchoolInfo) {
        SchoolInfoDialog(
            onDismiss = { viewModel.setSchoolInfoVisible(false) }
        )
    }

    if (showPermissionGuide) {
        PermissionGuideDialog(
            onDismiss = { viewModel.setPermissionGuideVisible(false) },
            onRequestPermissions = {
                requestEssentialPermissions()
            }
        )
    }

    if (showAttendanceSheet) {
        AttendanceNotesSheet(
            sheetState = sheetState,
            records = records,
            onDismiss = { viewModel.setAttendanceSheetVisible(false) },
            onLogAttendance = { type, note ->
                viewModel.logAttendance(type, note)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Catatan $type berhasil disimpan!",
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onDeleteRecord = { record ->
                viewModel.deleteAttendanceRecord(record)
            },
            onClearAll = {
                viewModel.clearAllAttendanceRecords()
                scope.launch {
                    snackbarHostState.showSnackbar("Semua riwayat catatan telah dihapus.")
                }
            }
        )
    }
}
