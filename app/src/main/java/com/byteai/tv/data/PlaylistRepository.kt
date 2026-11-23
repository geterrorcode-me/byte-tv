package com.byteai.tv.data

import android.content.Context
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Lightweight M3U playlist parser + fetcher.
 *
 * Playlist convention (every channel has exactly one URL line):
 *   - HLS/DASH:  <url>
 *   - Embed page: embed://<url>   -> loaded in a WebView
 *   - YouTube:    yt://<videoId>   -> loaded in the YouTube WebView
 *
 * This keeps the parser a simple, uniform state machine (no special cases that
 * previously dropped channels without a URL line).
 */
object PlaylistRepository {

    private const val TAG = "PlaylistRepo"
    private val executor = Executors.newSingleThreadExecutor()

    interface Callback {
        fun onResult(channels: List<Channel>)
        fun onError(message: String)
    }

    /** Load the bundled (offline-audited) channel list from assets. */
    fun loadLocal(context: Context, callback: Callback) {
        executor.execute {
            try {
                val text = context.assets.open("channels.m3u").bufferedReader().use { it.readText() }
                val channels = parseM3U(text)
                Log.d(TAG, "Parsed ${channels.size} channels")
                callback.onResult(channels)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load local playlist", e)
                callback.onError(e.message ?: "Unknown error")
            }
        }
    }

    /** Load a remote M3U playlist. */
    fun load(url: String, callback: Callback) {
        executor.execute {
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("User-Agent", "ByteTV/1.0")
                }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val channels = parseM3U(text)
                if (channels.isEmpty()) callback.onError("Playlist kosong")
                else callback.onResult(channels)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load playlist", e)
                callback.onError(e.message ?: "Unknown error")
            }
        }
    }

    fun parseM3U(content: String): List<Channel> {
        val result = mutableListOf<Channel>()
        var name: String? = null
        var group: String? = null
        var logo: String? = null
        var ua: String? = null
        var referer: String? = null

        for (raw in content.lineSequence()) {
            val line = raw.trim()
            when {
                line.isEmpty() -> {}
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val attrs = parseAttrs(line)
                    name = extractName(line)
                    group = attrs["group-title"]
                    logo = attrs["tvg-logo"]
                    ua = null
                    referer = null
                }
                line.startsWith("#EXTVLCOPT", ignoreCase = true) -> {
                    val opt = line.substringAfter(":", "").trim()
                    when {
                        opt.startsWith("http-user-agent", ignoreCase = true) ->
                            ua = opt.substringAfter("=", "").trim()
                        opt.startsWith("http-referrer", ignoreCase = true) ->
                            referer = opt.substringAfter("=", "").trim()
                    }
                }
                line.startsWith("#") -> { /* other directives */ }
                else -> {
                    // URL line -> build channel from the pending metadata
                    val ch = Channel(
                        name = name ?: line,
                        url = line,
                        logo = logo,
                        group = group,
                        userAgent = ua,
                        referer = referer
                    )
                    result.add(ch)
                    name = null; group = null; logo = null; ua = null; referer = null
                }
            }
        }
        return result
    }

    private fun extractName(line: String): String {
        val idx = line.lastIndexOf(',')
        return if (idx >= 0) line.substring(idx + 1).trim() else "Channel"
    }

    private fun parseAttrs(line: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val regex = Regex("""([\w-]+)="([^"]*)"""")
        regex.findAll(line).forEach { map[it.groupValues[1]] = it.groupValues[2] }
        return map
    }
}