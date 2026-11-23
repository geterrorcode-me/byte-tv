package com.byteai.tv.player

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Generic embed player via WebView. Loads a channel's official embed page
 * (rctiplus, detik, dailymotion, vidio, beritasatu, etc.) inside a WebView.
 * These are real embed pages (not YouTube), so no Error 153 / WebView detection.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbedPlayerScreen(embedUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val webView = remember(embedUrl) {
        WebView(context).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.domStorageEnabled = true
            // Allow fullscreen video in WebView
            settings.setSupportZoom(false)
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(
                    view: android.view.View?,
                    callback: CustomViewCallback?
                ) {
                    // Fullscreen video not needed for embed pages; keep default.
                    super.onShowCustomView(view, callback)
                }
            }
            loadUrl(embedUrl)
        }
    }

    DisposableEffect(webView) {
        onDispose { webView.destroy() }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize().background(ComposeColor.Black)
    )
}