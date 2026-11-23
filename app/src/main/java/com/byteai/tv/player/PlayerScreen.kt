package com.byteai.tv.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi

@UnstableApi
@Composable
fun PlayerScreen(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            val source = HlsMediaSource.Factory(dataSourceFactory(context)).createMediaSource(mediaItem)
            setMediaSource(source)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                setShowNextButton(false)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun dataSourceFactory(context: android.content.Context) =
    androidx.media3.datasource.DefaultHttpDataSource.Factory().apply {
        setUserAgent("ByteTV/1.0")
        setAllowCrossProtocolRedirects(true)
    }