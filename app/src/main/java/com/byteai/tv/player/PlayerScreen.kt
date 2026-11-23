package com.byteai.tv.player

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView

@UnstableApi
@Composable
fun PlayerScreen(
    videoUrl: String,
    userAgent: String? = null,
    referer: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(videoUrl, userAgent, referer) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            val source = HlsMediaSource.Factory(dataSourceFactory(userAgent, referer))
                .createMediaSource(mediaItem)
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

private fun dataSourceFactory(userAgent: String?, referer: String?): DefaultHttpDataSource.Factory {
    val f = DefaultHttpDataSource.Factory()
    if (!userAgent.isNullOrBlank()) f.setUserAgent(userAgent)
    if (!referer.isNullOrBlank()) f.setDefaultRequestProperties(mapOf("Referer" to referer))
    f.setAllowCrossProtocolRedirects(true)
    return f
}