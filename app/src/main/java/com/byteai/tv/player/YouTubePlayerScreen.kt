package com.byteai.tv.player

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * YouTube Live player via WebView. Plays the live stream embedded from youtube-nocookie.
 * Reliable for national channels that have an official 24/7 YouTube live stream.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerScreen(videoId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val webView = remember(videoId) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            val html = buildYouTubeEmbedHtml(videoId)
            loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)
        }
    }

    DisposableEffect(webView) {
        onDispose { webView.destroy() }
    }

    AndroidView(factory = { webView }, modifier = modifier.fillMaxSize())
}

private fun buildYouTubeEmbedHtml(videoId: String): String {
    return """
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
      <style>
        html,body { margin:0; padding:0; background:#000; height:100%; overflow:hidden; }
        .wrap { position:relative; width:100%; height:100%; }
        iframe { position:absolute; top:0; left:0; width:100%; height:100%; border:0; }
      </style>
    </head>
    <body>
      <div class="wrap">
        <iframe
          src="https://www.youtube-nocookie.com/embed/${videoId}?autoplay=1&mute=0&playsinline=1&rel=0&controls=1&fs=1&iv_load_policy=3"
          allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
          allowfullscreen
          frameborder="0">
        </iframe>
      </div>
    </body>
    </html>
    """.trimIndent()
}