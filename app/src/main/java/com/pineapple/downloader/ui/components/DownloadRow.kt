package com.pineapple.downloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pineapple.downloader.R
import com.pineapple.downloader.model.DownloadItem
import com.pineapple.downloader.model.DownloadKind
import com.pineapple.downloader.model.DownloadStatus
import com.pineapple.downloader.ui.theme.PineappleError
import com.pineapple.downloader.ui.theme.PineappleGreen
import com.pineapple.downloader.ui.theme.PineappleYellow

/**
 * صف تحميل في LazyColumn مع Swipe-to-dismiss مخصص
 * (خلفية حمراء + أيقونة حذف) وشارة حالة ملوّنة.
 */
@Composable
fun DownloadRow(item: DownloadItem, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            if (v == SwipeToDismissBoxValue.EndToStart || v == SwipeToDismissBoxValue.StartToEnd) {
                onDismiss()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PineappleError)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "حذف",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (item.kind == DownloadKind.VIDEO) {
                                PineappleYellow.copy(alpha = 0.2f)
                            } else {
                                PineappleGreen.copy(alpha = 0.12f)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(
                            if (item.kind == DownloadKind.VIDEO) R.drawable.ic_video else R.drawable.ic_audio,
                        ),
                        contentDescription = null,
                        tint = if (item.kind == DownloadKind.VIDEO) PineappleYellow else PineappleGreen,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.fileName.ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                    )
                    if (item.status == DownloadStatus.LOADING) {
                        Spacer(Modifier.height(6.dp))
                        PineappleProgressBar(item.progress, 0.0, 0)
                    }
                    if (item.status == DownloadStatus.ERROR && !item.errorMessage.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            item.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = PineappleError,
                            maxLines = 2,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                when (item.status) {
                    DownloadStatus.LOADING -> StatusBadge("قيد التحميل", PineappleYellow)
                    DownloadStatus.DONE -> StatusBadge("تم", PineappleGreen)
                    DownloadStatus.ERROR -> StatusBadge("خطأ", PineappleError)
                }
            }
        },
    )
}
