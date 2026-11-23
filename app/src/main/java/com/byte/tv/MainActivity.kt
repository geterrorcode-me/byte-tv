package com.byte.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.common.util.UnstableApi
import com.byte.tv.data.Channel
import com.byte.tv.data.PlaylistRepository
import com.byte.tv.player.PlayerScreen
import com.byte.tv.ui.ChannelList

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ByteTVApp()
                }
            }
        }
    }
}

private val DEFAULT_PLAYLIST =
    "https://raw.githubusercontent.com/dhasap/dhanytv/main/dhanytv.m3u"

@UnstableApi
@Composable
fun ByteTVApp() {
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var active by remember { mutableStateOf<Channel?>(null) }

    LaunchedEffect(Unit) {
        PlaylistRepository.load(DEFAULT_PLAYLIST, object : PlaylistRepository.Callback {
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
        // Fullscreen player
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            PlayerScreen(videoUrl = current.url, modifier = Modifier.fillMaxSize())
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