package com.byteai.tv.data

import android.content.Context
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Lightweight M3U playlist parser + fetcher.
 * Parses #EXTINF lines, #EXTVLCOPT headers (user-agent / referrer) and URL lines
 * into Channel objects so premium channels that require specific UA/Referer still play.
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
                callback.onResult(parseM3U(text))
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
                if (channels.isEmpty()) {
                    callback.onError("Playlist kosong")
                } else {
                    callback.onResult(channels)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load playlist", e)
                callback.onError(e.message ?: "Unknown error")
            }
        }
    }

    fun parseM3U(content: String): List<Channel> {
        val lines = content.lineSequence().map { it.trim() }.iterator()
        val result = mutableListOf<Channel>()
        var pending: Channel? = null
        var pendingUa: String? = null
        var pendingReferer: String? = null
        var pendingIsYt = false
        while (lines.hasNext()) {
            val line = lines.next()
            when {
                line.isEmpty() -> {}
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val name = extractName(line)
                    val attrs = parseAttrs(line)
                    // Finalize any previous YouTube channel (no URL line follows EXTINF)
                    if (pendingIsYt) {
                        pending?.let { result.add(it) }
                    }
                    pending = Channel(
                        name = name,
                        url = "",
                        logo = attrs["tvg-logo"],
                        group = attrs["group-title"],
                        userAgent = null,
                        referer = null,
                        youtubeId = attrs["tvg-youtube"]
                    )
                    pendingIsYt = !attrs["tvg-youtube"].isNullOrBlank()
                    pendingUa = null
                    pendingReferer = null
                }
                // Capture VLC options (UA / referrer) that apply to the next URL line
                line.startsWith("#EXTVLCOPT", ignoreCase = true) -> {
                    val opt = line.substringAfter(":", "").trim()
                    when {
                        opt.startsWith("http-user-agent", ignoreCase = true) ->
                            pendingUa = opt.substringAfter("=", "").trim()
                        opt.startsWith("http-referrer", ignoreCase = true) ->
                            pendingReferer = opt.substringAfter("=", "").trim()
                    }
                }
                line.startsWith("#") -> { /* skip other directives */ }
                else -> {
                    // URL line -> finalize channel
                    val ch = (pending ?: Channel(name = line, url = ""))
                        .copy(url = line, userAgent = pendingUa, referer = pendingReferer)
                    result.add(ch)
                    pending = null
                    pendingIsYt = false
                    pendingUa = null
                    pendingReferer = null
                }
            }
        }
        // finalize any trailing pending (e.g. YouTube channels with no URL line)
        pending?.let { result.add(it) }
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