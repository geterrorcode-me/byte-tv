package com.byteai.tv

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import com.byteai.tv.data.Channel
import com.byteai.tv.data.PlaylistRepository
import com.byteai.tv.player.EmbedPlayerScreen
import com.byteai.tv.player.PlayerScreen
import com.byteai.tv.player.RctiPlusPlayerScreen
import com.byteai.tv.player.RctiPlusResolver
import com.byteai.tv.player.YouTubePlayerScreen
import com.byteai.tv.ui.ChannelList

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ByteTVApp()
                }
            }
        }
    }
}

/** How the user wants to watch: normal windowed, landscape, or fullscreen. */
private enum class PlayMode { NORMAL, LANDSCAPE, FULLSCREEN }

@UnstableApi
@Composable
fun ByteTVApp() {
    val context = LocalContext.current
    val activity = context as? Activity

    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Selected channel + whether the player is showing
    var selected by remember { mutableStateOf<Channel?>(null) }   // picked from list -> show chooser
    var playing by remember { mutableStateOf(false) }            // actually playing
    var focusedUrl by remember { mutableStateOf<String?>(null) } // last opened (cursor)
    var playMode by remember { mutableStateOf(PlayMode.NORMAL) }
    // Hoisted scroll state so the list keeps its position when coming back from a stream
    val listState = rememberLazyListState()

    // Back behavior: if playing -> stop (go to chooser); if chooser -> back to list
    BackHandler(enabled = playing || selected != null) {
        when {
            playing -> {
                playing = false
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                activity?.let { exitImmersive(it) }
            }
            selected != null -> selected = null
        }
    }

    // Apply orientation / immersive only when playing, based on chosen PlayMode
    DisposableEffect(playing, playMode) {
        if (playing) {
            when (playMode) {
                PlayMode.NORMAL -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                PlayMode.LANDSCAPE -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                PlayMode.FULLSCREEN -> {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    activity?.let { goImmersive(it) }
                }
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.let { exitImmersive(it) }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.let { exitImmersive(it) }
        }
    }

    LaunchedEffect(Unit) {
        PlaylistRepository.loadLocal(context, object : PlaylistRepository.Callback {
            override fun onResult(result: List<Channel>) { channels = result; loading = false }
            override fun onError(message: String) { error = message; loading = false }
        })
    }

    when {
        // 1) Actually playing
        playing && selected != null -> {
            val ch = selected!!
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                when {
                    ch.youtubeId != null -> YouTubePlayerScreen(ch.youtubeId!!, Modifier.fillMaxSize())
                    ch.embedUrl != null && RctiPlusResolver.isRctiPlus(ch.embedUrl!!) ->
                        RctiPlusPlayerScreen(ch.embedUrl!!, Modifier.fillMaxSize())
                    ch.embedUrl != null -> EmbedPlayerScreen(ch.embedUrl!!, Modifier.fillMaxSize())
                    else -> PlayerScreen(ch.url, ch.userAgent, ch.referer, Modifier.fillMaxSize())
                }
            }
        }
        // 2) Chooser: user picks how to watch (like YouTube quality screen)
        selected != null -> {
            PlayChooser(
                channel = selected!!,
                onBack = { selected = null },
                onPlay = { mode ->
                    playMode = mode
                    playing = true
                    focusedUrl = selected!!.url
                }
            )
        }
        // 3) Channel list
        else -> {
            ChannelList(
                channels = channels,
                loading = loading,
                error = error,
                focusedUrl = focusedUrl,
                listState = listState,
                onChannelClick = { selected = it }
            )
        }
    }
}

/** Overlay screen where the user chooses how to watch (portrait default). */
@Composable
private fun PlayChooser(
    channel: Channel,
    onBack: () -> Unit,
    onPlay: (PlayMode) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121326)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(channel.name, color = Color(0xFF02DEEF), fontWeight = FontWeight.Bold, fontSize = 26.sp)
            Text("Pilih cara menonton", color = Color(0xFF8A93A6), fontSize = 14.sp, modifier = Modifier.padding(bottom = 24.dp))

            Button(
                onClick = { onPlay(PlayMode.NORMAL) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Text("▶ Normal (jendela)", fontSize = 16.sp)
            }
            OutlinedButton(
                onClick = { onPlay(PlayMode.LANDSCAPE) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Text("↻ Landscape (putar layar)", fontSize = 16.sp)
            }
            OutlinedButton(
                onClick = { onPlay(PlayMode.FULLSCREEN) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Text("⛶ Fullscreen (landscape + sembunyikan bar)", fontSize = 15.sp)
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Text("← Kembali", fontSize = 16.sp)
            }
        }
    }
}

private fun goImmersive(activity: Activity) {
    WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

private fun exitImmersive(activity: Activity) {
    WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
        show(WindowInsetsCompat.Type.systemBars())
    }
}