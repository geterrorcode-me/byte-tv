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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

/**
 * RCTI+ in-app browser - full-screen WebView inside Byte TV.
 *
 * No external browser, no Custom Tabs, no URL bar. The page fills the app on both
 * phone and TV. The user can press Play with the remote (D-pad OK acts as a user
 * gesture, which is what the page's autostart:false player needs).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RctiBrowserScreen(embedUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? Activity

    var muted by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val muteFocus = remember { FocusRequester() }

    LaunchedEffect(controlsVisible, muted) {
        if (controlsVisible) {
            delay(3500)
            controlsVisible = false
        }
    }

    fun userGesture(view: WebView) {
        view.evaluateJavascript(
            "(function(){var v=document.querySelector('video');if(v){v.muted=false;v.play().catch(function(){});}" +
                "var el=document.getElementById('videoplayer');if(el){el.click();}})();",
            null
        )
    }

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
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFocusable = true
            isFocusableInTouchMode = true

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    loadError = null
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    super.onPageFinished(view, url)
                    view.evaluateJavascript(
                        "(function(){var v=document.querySelector('video');if(v&&v.paused){v.muted=true;v.play().catch(function(){});}" +
                            "try{if(window.jwplayer&&jwplayer().ready&&!jwplayer().playing){var p=jwplayer().play();if(p&&p.catch){p.catch(function(){});}}}catch(e){}})();",
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
                        loadError = error.description?.toString() ?: "Gagal memuat channel"
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
            webChromeClient = RctiFullscreenWebChromeClient(this)
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ComposeColor.Black)
            .clickable { userGesture(webView); controlsVisible = true }
    ) {
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
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp &&
                        (event.key == Key.Enter || event.key == Key.DirectionCenter)
                    ) {
                        userGesture(webView)
                        controlsVisible = true
                        true
                    } else false
                }
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

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            MuteButton(
                muted = muted,
                focusRequester = muteFocus,
                onInteraction = { controlsVisible = true },
                onClick = {
                    muted = !muted
                    setWebViewMuted(webView, muted)
                }
            )
        }
    }
}

@Composable
private fun MuteButton(
    muted: Boolean,
    focusRequester: FocusRequester,
    onInteraction: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (muted) ComposeColor(0xCC000000) else ComposeColor(0xCC1E3A1E))
            .border(2.dp, ComposeColor(0xFF02DEEF), CircleShape)
            .focusRequester(focusRequester)
            .focusable()
            .clickable { onInteraction(); onClick() }
            .onKeyEvent { event ->
                onInteraction()
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onClick()
                    true
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        Text(if (muted) "🔇" else "🔊", fontSize = 24.sp, color = ComposeColor.White)
    }
}

private fun setWebViewMuted(webView: WebView, muted: Boolean) {
    val js = if (muted) {
        "(function(){try{if(window.jwplayer&&jwplayer().ready){jwplayer().setMute(true);}}catch(e){}" +
            "document.querySelectorAll('video,audio').forEach(function(e){e.muted=true;});})();"
    } else {
        "(function(){try{if(window.jwplayer&&jwplayer().ready){jwplayer().setMute(false);jwplayer().setVolume(100);}}catch(e){}" +
            "document.querySelectorAll('video,audio').forEach(function(e){e.muted=false;e.volume=1.0;});})();"
    }
    webView.evaluateJavascript(js, null)
}

private class RctiFullscreenWebChromeClient(private val webView: WebView) : WebChromeClient() {
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