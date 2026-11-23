package com.byteai.tv.data

data class Channel(
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    val userAgent: String? = null,
    val referer: String? = null,
    val youtubeId: String? = null
)