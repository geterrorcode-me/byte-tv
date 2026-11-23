package com.byteai.tv.data

data class Channel(
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    val userAgent: String? = null,
    val referer: String? = null
) {
    /** YouTube video id if url is a yt:// scheme, else null */
    val youtubeId: String?
        get() = if (url.startsWith("yt://", ignoreCase = true)) url.removePrefix("yt://").trim() else null

    /** Embed page URL if url is an embed:// scheme, else null */
    val embedUrl: String?
        get() = if (url.startsWith("embed://", ignoreCase = true)) url.removePrefix("embed://").trim() else null

    /** True if this channel plays via a WebView (YouTube or embed page) */
    val isWebView: Boolean
        get() = youtubeId != null || embedUrl != null
}