package com.byteai.tv.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
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
 * WebView player for embed pages (RCTI+, etc).
 * - Starts MUTED; mute/unmute button is D-pad-reachable and auto-hides.
 * - Forces Referer header to embed.rctiplus.com so RCTI+ HLS ACL allows playback.
 * - Autoplay enabled; no manual Play button (D-pad unreachable on the source page).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbedPlayerScreen(embedUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? Activity

    var muted by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    val muteFocus = remember { FocusRequester() }

    LaunchedEffect(controlsVisible, muted) {
        if (controlsVisible) {
            delay(3500)
            controlsVisible = false
        }
    }

    val webView = remember(embedUrl) {
        WebView(context).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            // Allow autoplay of HLS/media without user gesture
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.setSupportZoom(false)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            // Force Referer so RCTI+ HLS ACL allows playback
            // (otherwise returns 403 Forbidden from proxy-c.rctiplus.id)
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    // Inject Referer via header for subsequent requests
                    view?.settings?.setUserAgentString(
                        settings.userAgentString + " Referer: https://embed.rctiplus.com/"
                    )
                }
            }
            webChromeClient = FullscreenWebChromeClient(this)
            loadUrl(embedUrl, mapOf("Referer" to "https://embed.rctiplus.com/"))
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
            .clickable { controlsVisible = true }
    ) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    setBackgroundColor(Color.BLACK)
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    addView(webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // D-pad-reachable mute/unmute (auto-hides)
        androidx.compose.animation.AnimatedVisibility(
            visible = controlsVisible,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
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
        Text(
            text = if (muted) "🔇" else "🔊",
            fontSize = 24.sp,
            color = ComposeColor.White
        )
    }
}

private fun setWebViewMuted(webView: WebView, muted: Boolean) {
    val js = if (muted) {
        "document.querySelectorAll('video,audio').forEach(function(e){e.muted=true;});"
    } else {
        "document.querySelectorAll('video,audio').forEach(function(e){e.muted=false;e.volume=1.0;});"
    }
    webView.evaluateJavascript(js, null)
}

private class FullscreenWebChromeClient(private val webView: WebView) : WebChromeClient() {
    private var customView: View? = null
    private var callback: CustomViewCallback? = null
    override fun onShowCustomView(view: View?, customCallback: CustomViewCallback?) {
        if (customView != null) { customCallback?.onCustomViewHidden(); return }
        customView = view; callback = customCallback
        view?.let {
            it.setBackgroundColor(Color.BLACK)
            (webView.parent as? ViewGroup)?.addView(
                it,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )
        }
        (webView.context as? Activity)?.let { goImmersive(it) }
    }
    override fun onHideCustomView() {
        customView?.let { (webView.parent as? ViewGroup)?.removeView(it) }
        customView = null; callback?.onCustomViewHidden(); callback = null
    }
}


private fun goImmersive(activity: Activity) {
    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

private fun exitImmersive(activity: Activity) {
    WindowCompat.setDecorFitsSystemWindows(activity.window, true)
    WindowInsetsControllerCompat(activity.window, activity.window.decorView).show(WindowInsetsCompat.Type.systemBars())
}