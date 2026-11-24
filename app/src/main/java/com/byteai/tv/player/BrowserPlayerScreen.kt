package com.byteai.tv.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * "Browser mode" for RCTI+ channels.
 *
 * RCTI+ is NOT a plain HLS stream: its embed page is a full JWPlayer 8 + hls.js app
 * (poster, Play button, ads, Firebase). Flattening it into a native ExoPlayer source
 * fails ("gagal membuat sumber konten"). So this screen simply loads the original
 * page like a browser and lets the page's own player run.
 *
 * Notes:
 * - Desktop User-Agent: the embed page's JWPlayer expects a desktop-class browser;
 *   phone UAs can take a different code path.
 * - Autoplay is allowed, but the page itself uses autostart:false, so the user may
 *   still need to press Play once (on TV, focus the page area and press OK).
 * - No Referer/UA mutation hacks (those caused nginx 400 header-too-large loops).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserPlayerScreen(embedUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? Activity

    var loadError by remember { mutableStateOf<String?>(null) }

    val webView = remember(embedUrl) {
        WebView(context).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.setSupportZoom(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // Desktop UA: the RCTI+ embed page (JWPlayer) is built for a desktop browser.
            settings.userAgentString =
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36"
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    loadError = null
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    super.onPageFinished(view, url)
                    // Nudge autoplay where the page allows it. autostart:false means this
                    // is best-effort; the page's Play button remains the fallback.
                    view.evaluateJavascript(
                        "(function(){try{if(window.jwplayer&&jwplayer().ready&&!jwplayer().playing){" +
                            "var p=jwplayer().play();if(p&&p.catch){p.catch(function(){});}}}catch(e){}" +
                            "var v=document.querySelector('video');if(v&&v.paused){v.play().catch(function(){});}})();",
                        null
                    )
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    super.onReceivedError(view, request, error)
                    if (request.isForMainFrame) {
                        loadError = error.description?.toString()
                            ?: "Gagal memuat halaman channel"
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    if (request.isForMainFrame) {
                        loadError = "HTTP ${errorResponse.statusCode}: ${errorResponse.reasonPhrase}"
                    }
                }
            }
            webChromeClient = FullscreenWebChromeClient(this)
            loadUrl(embedUrl)
        }
    }

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.let { goImmersive(it) }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.let { exitImmersive(it) }
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(ComposeColor.Black)) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    setBackgroundColor(Color.BLACK)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    addView(
                        webView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (loadError != null) {
            Box(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tidak bisa memuat channel\n$loadError",
                    color = ComposeColor.White,
                    fontSize = 15.sp
                )
            }
        }
    }
}

private class FullscreenWebChromeClient(private val webView: WebView) : WebChromeClient() {
    private var customView: View? = null
    private var callback: CustomViewCallback? = null

    override fun onShowCustomView(view: View?, customCallback: CustomViewCallback?) {
        if (customView != null) {
            customCallback?.onCustomViewHidden()
            return
        }
        customView = view
        callback = customCallback
        view?.let {
            it.setBackgroundColor(Color.BLACK)
            (webView.parent as? ViewGroup)?.addView(
                it,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        (webView.context as? Activity)?.let { goImmersive(it) }
    }

    override fun onHideCustomView() {
        customView?.let { (webView.parent as? ViewGroup)?.removeView(it) }
        customView = null
        callback?.onCustomViewHidden()
        callback = null
    }
}

private fun goImmersive(activity: Activity) {
    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

private fun exitImmersive(activity: Activity) {
    WindowCompat.setDecorFitsSystemWindows(activity.window, true)
    WindowInsetsControllerCompat(activity.window, activity.window.decorView).show(
        WindowInsetsCompat.Type.systemBars()
    )
}