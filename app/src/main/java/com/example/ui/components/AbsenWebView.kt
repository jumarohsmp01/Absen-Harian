package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AbsenWebView(
    url: String,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {},
    onProgressChanged: (Int) -> Unit = {},
    onTitleReceived: (String?) -> Unit = {},
    onNavigationStateChanged: (canGoBack: Boolean, canGoForward: Boolean, url: String?) -> Unit = { _, _, _ -> },
    onErrorOccurred: (String) -> Unit = {},
    onPageFinishedLoading: () -> Unit = {},
    onFileChooserRequested: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean = { _, _ -> false },
    onGeolocationRequested: (String?, GeolocationPermissions.Callback?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
                allowFileAccess = true
                allowContentAccess = true
                setGeolocationEnabled(true)
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = "$userAgentString SMPN1BanjarmasinApp/1.0"
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = false

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    onProgressChanged(newProgress)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    onTitleReceived(title)
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback?
                ) {
                    onGeolocationRequested(origin, callback)
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    return onFileChooserRequested(filePathCallback, fileChooserParams)
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    onNavigationStateChanged(canGoBack(), canGoForward(), url)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    onNavigationStateChanged(canGoBack(), canGoForward(), url)
                    onPageFinishedLoading()
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        val description = error?.description?.toString() ?: "Gagal memuat halaman"
                        onErrorOccurred(description)
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val targetUri = request?.url ?: return false
                    val scheme = targetUri.scheme?.lowercase()

                    return when {
                        scheme == "tel" || scheme == "mailto" || scheme == "sms" || scheme == "geo" -> {
                            handleExternalIntent(context, Intent(Intent.ACTION_VIEW, targetUri))
                            true
                        }
                        scheme == "whatsapp" || targetUri.host?.contains("wa.me") == true -> {
                            handleExternalIntent(context, Intent(Intent.ACTION_VIEW, targetUri))
                            true
                        }
                        targetUri.host?.contains("smpn1banjarmasin.sch.id") == true -> {
                            false // Load in WebView
                        }
                        else -> {
                            false // Load in WebView for school portals or sub-services
                        }
                    }
                }
            }

            setDownloadListener { downloadUrl, _, contentDisposition, mimeType, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(downloadUrl)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // Fallback
                }
            }
        }
    }

    DisposableEffect(webView) {
        onWebViewCreated(webView)
        webView.loadUrl(url)
        onDispose {
            webView.stopLoading()
        }
    }

    BackHandler(enabled = webView.canGoBack()) {
        webView.goBack()
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}

private fun handleExternalIntent(context: Context, intent: Intent) {
    try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback gracefully
    }
}
