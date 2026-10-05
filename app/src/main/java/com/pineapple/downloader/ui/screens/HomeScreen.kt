package com.pineapple.downloader.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pineapple.downloader.R
import com.pineapple.downloader.model.DownloadKind
import com.pineapple.downloader.model.FormatOption
import com.pineapple.downloader.model.formatDuration
import com.pineapple.downloader.ui.components.KindSegmentedControl
import com.pineapple.downloader.ui.components.PineappleEmptyState
import com.pineapple.downloader.ui.components.PineappleErrorState
import com.pineapple.downloader.ui.components.PineappleLoadingSkeleton
import com.pineapple.downloader.ui.components.PineappleTopBar
import com.pineapple.downloader.ui.components.QualityDropdown
import com.pineapple.downloader.ui.components.UrlInputCard
import com.pineapple.downloader.ui.theme.PineappleGreen
import com.pineapple.downloader.ui.theme.PineappleOutline
import com.pineapple.downloader.ui.theme.PineappleWhite
import kotlinx.coroutines.launch

/**
 * الشاشة الرئيسية — تخطيط غير متمركز عمدًا:
 * عنوان أعلى اليمين + معلومات صغيرة أسفله + زر التحميل في شريط سفلي ثابت.
 */
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by vm.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            if (state.info != null && state.error == null) {
                Button(
                    onClick = {
                        scope.launch {
                            vm.startDownload(context)
                        }
                    },
                    enabled = !state.isDownloading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PineappleGreen,
                        contentColor = PineappleWhite,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.isDownloading) "جارٍ التحميل…" else "تحميل الآن",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            PineappleTopBar(onSettingsClick = onOpenSettings)

            Column(Modifier.padding(horizontal = 20.dp)) {
                // عنوان الزاوية — لا تمركز
                Text("حمّل أي فيديو", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "الصق الرابط، اختر الصيغة، والباقي علينا.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(16.dp))

                UrlInputCard(
                    value = state.url,
                    onValueChange = vm::onUrlChange,
                    onPaste = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                        if (text.isNotBlank()) vm.onUrlChange(text.trim())
                    },
                    onFetch = { scope.launch { vm.fetchInfo() } },
                    fetchEnabled = state.url.isNotBlank() && !state.isFetching,
                )
                Spacer(Modifier.height(16.dp))

                when {
                    state.isFetching -> PineappleLoadingSkeleton()
                    state.error != null -> PineappleErrorState(
                        message = "تعذر جلب المعلومات",
                        reason = state.error,
                        onRetry = { scope.launch { vm.fetchInfo() } },
                    )
                    state.info == null -> PineappleEmptyState(
                        title = "لا يوجد فيديو بعد",
                        hint = "الصق رابطًا من يوتيوب أو أي موقع مدعوم لعرض الصيغ المتاحة.",
                        actionLabel = "لصق من الحافظة",
                        onAction = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val text = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                            if (text.isNotBlank()) {
                                vm.onUrlChange(text.trim())
                                scope.launch { vm.fetchInfo() }
                            }
                        },
                    )
                    else -> {
                        val info = state.info!!
                        VideoResultCard(
                            title = info.title,
                            uploader = info.uploader,
                            duration = info.duration,
                            thumbnail = info.thumbnail,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("النوع", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        KindSegmentedControl(state.kind, vm::onKindChange)
                        Spacer(Modifier.height(16.dp))
                        if (state.kind == DownloadKind.VIDEO) {
                            Text("الجودة", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            QualityDropdown(
                                options = info.videoFormats,
                                selected = state.selectedFormat,
                                onSelect = vm::onFormatSelect,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            AudioNoteCard()
                        }
                        if (state.isDownloading) {
                            Spacer(Modifier.height(16.dp))
                            com.pineapple.downloader.ui.components.PineappleProgressBar(
                                state.downloadPercent,
                                state.downloadSpeed,
                                state.downloadEta,
                            )
                        }
                        AnimatedVisibility(
                            visible = state.downloadDone,
                            enter = fadeIn(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = PineappleGreen,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "تم الحفظ في التنزيلات — تحقق من شاشة التحميلات",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PineappleGreen,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onOpenDownloads,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("عرض سجل التحميلات", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(96.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun VideoResultCard(title: String, uploader: String, duration: Int, thumbnail: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, PineappleOutline, RoundedCornerShape(12.dp)),
    ) {
        if (thumbnail.isNotBlank()) {
            AsyncImage(
                model = thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            )
        }
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Text(
                listOf(uploader, formatDuration(duration)).filter { it != "—" && it.isNotBlank() }
                    .joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun AudioNoteCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, PineappleGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_audio),
            contentDescription = null,
            tint = PineappleGreen,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "سيُحوَّل الصوت إلى MP3 تلقائيًا (أو M4A على الأجهزة غير المدعومة).",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
