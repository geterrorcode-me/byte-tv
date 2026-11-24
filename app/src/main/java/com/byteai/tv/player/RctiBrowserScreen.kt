package com.byteai.tv.player

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.browser.customtabs.CustomTabsIntent

/**
 * RCTI+ "in-app browser" using Chrome Custom Tabs.
 *
 * Why Custom Tabs instead of WebView:
 * - RCTI+ embed pages run JWPlayer 8 + hls.js. Android WebView (even the Google one)
 *   often fails to render them on TV boxes -> permanent black screen.
 * - Chrome / the TV's real browser renders the same page fine (poster + Play button,
 *   remote acts like a mouse).
 * - Custom Tabs launches that real browser *inside* the app, full-screen, with no
 *   address bar and no visible URL. The link is fixed and cannot be edited.
 *
 * The user then presses Play with the remote, exactly like browsing in Chrome.
 */
@Composable
fun RctiBrowserScreen(embedUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val customTabs = remember {
        CustomTabsIntent.Builder()
            .setShowTitle(false)          // no URL / title bar
            .setUrlBarHidingEnabled(true) // hide address bar
            .setStartAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out)
            .setExitAnimations(context, android.R.anim.fade_out, android.R.anim.fade_in)
            .build()
    }

    fun open() {
        try {
            customTabs.launchUrl(context, Uri.parse(embedUrl))
        } catch (_: Exception) {
            // No Custom Tabs provider (no Chrome): fall back to a plain browser intent.
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(embedUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (_: Exception) {
            }
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = Color(0xFF121326)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "RCTI+",
                color = Color(0xFF02DEEF),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Text(
                "Channel RCTI+ dibuka di browser dalam aplikasi (Chrome). Tekan Play dengan remote.",
                color = Color(0xFF8A93A6),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            Button(
                onClick = { open() },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Text("▶ Buka Channel", fontSize = 16.sp)
            }
        }
    }
}