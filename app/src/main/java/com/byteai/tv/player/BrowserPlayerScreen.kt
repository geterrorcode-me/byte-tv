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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
 * RCTI+ is not a plain stream: the embed page is a full JWPlayer 8 + hls.js app
 * (poster, Play button, ads, Firebase). Inside Byte TV's WebView that page renders
 * black on many Android TV boxes, while the very same URL works in the TV's real
 * browser (Chrome / TV Bro).
 *
 * So instead of re-implementing the page, hand the URL to the device's real browser
 * via an Intent - exactly like opening the link in a browser.
 */
@Composable
fun BrowserPlayerScreen(embedUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    fun openInBrowser() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(embedUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // No browser installed: fall back to a chooser-less web intent.
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
                "RCTI+ dibuka di browser",
                color = Color(0xFF02DEEF),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Text(
                "Channel ini memakai halaman RCTI+ (JWPlayer). Buka di browser TV/HP agar player aslinya jalan.",
                color = Color(0xFF8A93A6),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            Button(
                onClick = { openInBrowser() },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Text("🌐 Buka di Browser", fontSize = 16.sp)
            }
            OutlinedButton(
                onClick = { openInBrowser() },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Text("Buka Browser Lain", fontSize = 15.sp)
            }
        }
    }
}