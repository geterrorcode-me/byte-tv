package com.byteai.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byteai.tv.data.Channel

private val Accent = Color(0xFF02DEEF)
private val Bg = Color(0xFF121326)
private val CardBg = Color(0xFF1C2029)
private val FocusBorder = Color(0xFF02DEEF)   // cyan border = focused/current channel

@Composable
fun ChannelList(
    channels: List<Channel>,
    loading: Boolean,
    error: String?,
    focusedUrl: String?,                 // URL of the last opened channel (cursor/focus)
    listState: LazyListState,             // hoisted so scroll position survives back navigation
    onChannelClick: (Channel) -> Unit
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }

    val filtered = remember(channels, query.text) {
        val q = query.text.trim().lowercase()
        if (q.isEmpty()) channels
        else channels.filter {
            it.name.lowercase().contains(q) ||
                (it.group?.lowercase()?.contains(q) ?: false)
        }
    }

    val grouped = remember(filtered) {
        val seen = HashSet<String>()
        filtered.filter { seen.add(it.url) }
            .groupBy { it.group?.ifBlank { "Lainnya" } ?: "Lainnya" }
            .toSortedMap()
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Byte TV", color = Accent, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }

        // Search
        Surface(color = CardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🔎", color = Color(0xFF8A93A6), fontSize = 16.sp)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp)
                ) { inner ->
                    if (query.text.isEmpty()) {
                        Text("Cari channel…", color = Color(0xFF8A93A6), fontSize = 15.sp)
                    }
                    inner()
                }
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Gagal memuat: $error", color = Color(0xFFFF6B6B), modifier = Modifier.padding(24.dp))
            }
            filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada channel cocok", color = Color.White)
            }
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (group, items) ->
                    item(key = "hdr_$group") {
                        Text(
                            "$group (${items.size})",
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(items, key = { "${it.name}_${it.url.hashCode()}_${it.url.take(24)}" }) { ch ->
                        ChannelRow(
                            channel = ch,
                            isFocused = ch.url == focusedUrl,
                            onClick = { onChannelClick(ch) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(channel: Channel, isFocused: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isFocused) Color(0xFF16303A) else CardBg,  // slightly cyan-tinted when focused
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isFocused) Modifier.border(2.dp, FocusBorder, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(
                    if (isFocused) FocusBorder else Accent,
                    RoundedCornerShape(8.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channel.name.take(1).uppercase(),
                    color = Bg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = channel.name,
                    color = if (isFocused) FocusBorder else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}