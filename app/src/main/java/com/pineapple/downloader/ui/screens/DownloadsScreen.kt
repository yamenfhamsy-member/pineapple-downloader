package com.pineapple.downloader.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pineapple.downloader.download.HistoryStore
import com.pineapple.downloader.ui.components.DownloadRow
import com.pineapple.downloader.ui.components.PineappleEmptyState
import com.pineapple.downloader.ui.components.PineappleTopBar
import com.pineapple.downloader.ui.theme.PineappleOutline
import kotlinx.coroutines.launch

/** شاشة التحميلات: LazyColumn مع Swipe-to-dismiss وشارات حالة ملوّنة. */
@Composable
fun DownloadsScreen(onOpenSettings: () -> Unit, onBackToHome: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { HistoryStore(context) }
    val items by store.history.collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize()) {
        PineappleTopBar(onSettingsClick = onOpenSettings)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("التحميلات", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(4.dp))
            Text(
                if (items.isEmpty()) "لا يوجد شيء بعد" else "${items.size} عناصر — اسحب لحذف أي عنصر",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(12.dp))
            if (items.isEmpty()) {
                PineappleEmptyState(
                    title = "سجلّك فارغ",
                    hint = "كل تحميل ناجح سيظهر هنا مع اسمه وحالته.",
                    actionLabel = "ابدأ تحميلًا جديدًا",
                    onAction = onBackToHome,
                )
            } else {
                LazyColumn {
                    items(items, key = { it.id }) { item ->
                        DownloadRow(
                            item = item,
                            onDismiss = { scope.launch { store.remove(item.id) } },
                        )
                        Divider(color = PineappleOutline.copy(alpha = 0.6f), thickness = 0.5.dp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
