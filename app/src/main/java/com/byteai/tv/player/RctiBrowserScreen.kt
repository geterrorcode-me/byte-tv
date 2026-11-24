package com.byteai.tv.player

import android.content.ActivityNotFoundException
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * RCTI+ "browser mode".
 *
 * RCTI+ embed pages run JWPlayer 8 + hls.js. Byte TV's own WebView renders them as a
 * permanent black screen on many Android TV boxes, while the TV's *own* browser
 * (the one the user browses with, remote acting like a mouse) renders the page fine:
 * poster + Play button appear and the remote can press Play.
 *
 * So hand the URL to the device's default browser. The page opens in the browser the
 * user already uses on this TV - no Chrome required, no visible URL editing.
 */
@Composable
fun RctiBrowserScreen(embedUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    fun openInBrowser() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(embedUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Last resort: plain view intent without categories.
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(embedUrl)))
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
                "Channel ini dibuka di browser TV. Setelah terbuka, tekan Play dengan remote.",
                color = Color(0xFF8A93A6),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            Button(
                onClick = { openInBrowser() },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Text("▶ Buka di Browser TV", fontSize = 16.sp)
            }
        }
    }
}