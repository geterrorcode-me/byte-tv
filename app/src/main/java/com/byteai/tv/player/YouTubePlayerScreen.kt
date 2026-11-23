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
 * YouTube Live player via WebView.
 *
 * YouTube's embedded player (Error 153) requires a valid Referer + Referrer-Policy.
 * We inject a Referer header via loadUrl(extraHeaders) and add the referrerpolicy
 * meta tag inside the HTML so YouTube accepts the embed config.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerScreen(videoId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val webView = remember(videoId) {
        WebView(context).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()

            // Inject a Referer header on the top-level document load. This is what
            // YouTube checks to validate the embed (fixes Error 153 in WebViews).
            val headers = HashMap<String, String>()
            headers["Referer"] = "https://www.youtube.com/"
            loadUrl(
                "https://www.youtube-nocookie.com/embed/${videoId}" +
                    "?autoplay=1&playsinline=1&rel=0&controls=1&fs=1&iv_load_policy=3&mute=0",
                headers
            )
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