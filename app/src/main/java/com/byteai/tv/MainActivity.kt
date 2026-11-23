package com.byteai.tv

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import com.byteai.tv.data.Channel
import com.byteai.tv.data.PlaylistRepository
import com.byteai.tv.player.EmbedPlayerScreen
import com.byteai.tv.player.PlayerScreen
import com.byteai.tv.ui.ChannelList

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make app draw edge-to-edge (needed for immersive player)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ByteTVApp()
                }
            }
        }
    }

    @Deprecated("Deprecated by Android")
    override fun onBackPressed() {
        // handled by BackHandler in Compose
        super.onBackPressed()
    }
}

private fun goFullscreen(activity: Activity) {
    WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

private fun exitFullscreen(activity: Activity) {
    WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
        show(WindowInsetsCompat.Type.systemBars())
    }
}

@UnstableApi
@Composable
fun ByteTVApp() {
    val context = LocalContext.current
    val activity = context as? Activity

    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var active by remember { mutableStateOf<Channel?>(null) }

    // Enter immersive fullscreen + landscape while a video is playing; restore on exit.
    DisposableEffect(active != null) {
        if (active != null) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity?.let { goFullscreen(it) }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.let { exitFullscreen(it) }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.let { exitFullscreen(it) }
        }
    }

    // Back button: if a video is playing, go back to channel list instead of exiting the app.
    BackHandler(enabled = active != null) {
        active = null
    }

    LaunchedEffect(Unit) {
        PlaylistRepository.loadLocal(context, object : PlaylistRepository.Callback {
            override fun onResult(result: List<Channel>) {
                channels = result
                loading = false
            }
            override fun onError(message: String) {
                error = message
                loading = false
            }
        })
    }

    val current = active
    if (current != null) {
        // Fullscreen player (immersive + landscape)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            when {
                current.youtubeId != null -> {
                    com.byteai.tv.player.YouTubePlayerScreen(
                        videoId = current.youtubeId!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                current.embedUrl != null -> {
                    EmbedPlayerScreen(
                        embedUrl = current.embedUrl!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    PlayerScreen(
                        videoUrl = current.url,
                        userAgent = current.userAgent,
                        referer = current.referer,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    } else {
        ChannelList(
            channels = channels,
            loading = loading,
            error = error,
            onChannelClick = { active = it }
        )
    }
}