package com.byteai.tv.player

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resolves the live HLS URL for RCTI+ embed pages.
 *
 * The RCTI+ embed page (embed.rctiplus.com/live/<channel>/inewsid) embeds the
 * stream URL as a base64 string inside `atob('...')`. The URL carries a short-lived
 * `auth_key` (roughly 30 minutes), so it must be fetched at runtime - never hardcoded.
 *
 * The HLS CDN only accepts requests whose Referer is the RCTI+ embed host, so the
 * resolved URL must be played with `Referer: https://embed.rctiplus.com/`.
 */
object RctiPlusResolver {

    private const val TAG = "RctiPlusResolver"
    private const val EMBED_ORIGIN = "https://embed.rctiplus.com/"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 9; SmartTV) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"

    /** Referer required by the RCTI+ HLS CDN. */
    const val STREAM_REFERER = EMBED_ORIGIN

    /**
     * Fetch the embed page and extract the base64-encoded HLS URL.
     * Returns null when the page has no playable stream.
     */
    suspend fun resolveStreamUrl(embedUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val html = httpGet(embedUrl)
            val b64 = extractAtobString(html)
            if (b64.isNullOrBlank()) {
                Log.w(TAG, "No atob() stream url found in embed page")
                return@withContext null
            }
            val decoded = String(Base64.decode(b64, Base64.DEFAULT))
            if (!decoded.startsWith("http")) {
                Log.w(TAG, "Decoded stream url is not http(s)")
                return@withContext null
            }
            Log.i(TAG, "Resolved RCTI+ stream: ${decoded.substringBefore('?')}")
            decoded
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve RCTI+ stream url", e)
            null
        }
    }

    /** True when this embed URL belongs to the RCTI+ embed host. */
    fun isRctiPlus(embedUrl: String): Boolean =
        embedUrl.contains("embed.rctiplus.com", ignoreCase = true) ||
            embedUrl.contains("sindikasi.inews.id", ignoreCase = true)

    private fun extractAtobString(html: String): String? {
        // The page contains several atob() calls (jwplayer key, stream url, ...).
        // The stream url is the one that decodes to an http(s) m3u8 URL.
        val pattern = Regex("atob\\(\\s*['\"]([A-Za-z0-9+/=]{40,})['\"]\\s*\\)")
        for (match in pattern.findAll(html)) {
            val candidate = match.groupValues[1]
            val decoded = runCatching {
                String(Base64.decode(candidate, Base64.DEFAULT))
            }.getOrNull() ?: continue
            if (decoded.startsWith("http") && decoded.contains(".m3u8")) {
                return candidate
            }
        }
        return null
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 15000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Referer", EMBED_ORIGIN)
            setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code for $url")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
