package com.byteai.tv.player

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/**
 * Native HLS player for RCTI+ channels.
 *
 * Why native instead of WebView:
 * - The RCTI+ embed page relies on JWPlayer 8 + hls.js. On older Android TV WebViews
 *   that script fails to run, leaving a completely black screen (no poster, no button).
 * - The HLS stream itself is plain HLS (no DRM). It only requires the correct
 *   `Referer` header, which ExoPlayer can send natively.
 *
 * The stream URL contains a short-lived `auth_key`, so it is resolved at runtime and
 * refreshed when playback fails.
 */
@UnstableApi
@Composable
fun RctiPlusPlayerScreen(embedUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var streamUrl by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("Memuat channel...") }
    var generation by remember { mutableStateOf(0) }

    // Resolve the (time-limited) HLS url from the embed page.
    LaunchedEffect(embedUrl, generation) {
        statusText = "Memuat channel..."
        val resolved = RctiPlusResolver.resolveStreamUrl(embedUrl)
        if (resolved == null) {
            statusText = "Gagal memuat sumber channel"
        } else {
            streamUrl = resolved
            statusText = ""
        }
    }

    val currentUrl = streamUrl
    val exoPlayer = remember(currentUrl) {
        if (currentUrl == null) {
            null
        } else {
            ExoPlayer.Builder(context).build().apply {
                val dataSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent("Mozilla/5.0 (Linux; Android 9; SmartTV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setDefaultRequestProperties(
                        mapOf("Referer" to RctiPlusResolver.STREAM_REFERER)
                    )
                    .setAllowCrossProtocolRedirects(true)

                val mediaItem = MediaItem.fromUri(currentUrl)
                val source: MediaSource =
                    HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
                setMediaSource(source)
                prepare()
                playWhenReady = true
                volume = 0f // start muted, like the rest of the app
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        // The auth_key expires; re-resolve a fresh url and retry.
                        generation += 1
                    }
                })
            }
        }
    }

    LaunchedEffect(exoPlayer) {
        if (exoPlayer != null) {
            // Refresh the stream url periodically so long sessions do not expire.
            while (true) {
                delay(10 * 60 * 1000L)
                generation += 1
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(exoPlayer) {
        onDispose { exoPlayer?.release() }
    }

    Box(modifier = modifier.fillMaxSize().background(ComposeColor.Black)) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowNextButton(false)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        if (statusText.isNotBlank()) {
            Box(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(statusText, color = ComposeColor.White, fontSize = 15.sp)
            }
        }
    }
}
