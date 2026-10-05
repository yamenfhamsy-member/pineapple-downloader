package com.pineapple.downloader.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pineapple.downloader.download.DownloadManager
import com.pineapple.downloader.download.HistoryStore
import com.pineapple.downloader.model.DownloadItem
import com.pineapple.downloader.model.DownloadKind
import com.pineapple.downloader.model.DownloadStatus
import com.pineapple.downloader.model.FormatOption
import com.pineapple.downloader.model.VideoInfo
import com.pineapple.downloader.python.YtDlpBridge
import com.pineapple.downloader.service.DownloadService
import com.pineapple.downloader.util.Haptics
import com.pineapple.downloader.util.MediaStoreSaver
import com.pineapple.downloader.util.Permissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class HomeUiState(
    val url: String = "",
    val isFetching: Boolean = false,
    val info: VideoInfo? = null,
    val error: String? = null,
    val kind: DownloadKind = DownloadKind.VIDEO,
    val selectedFormat: FormatOption? = null,
    val isDownloading: Boolean = false,
    val downloadPercent: Int = 0,
    val downloadSpeed: Double = 0.0,
    val downloadEta: Int = 0,
    val downloadDone: Boolean = false,
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onUrlChange(v: String) {
        _uiState.update {
            it.copy(
                url = v,
                error = null,
                downloadDone = false,
                // رابط جديد = معلومات قديمة لاغية
                info = if (v.trim() != it.url.trim()) null else it.info,
            )
        }
    }

    fun onKindChange(k: DownloadKind) {
        _uiState.update {
            it.copy(
                kind = k,
                selectedFormat = if (k == DownloadKind.VIDEO) {
                    it.selectedFormat ?: it.info?.videoFormats?.firstOrNull()
                } else {
                    null
                },
            )
        }
    }

    fun onFormatSelect(f: FormatOption) {
        _uiState.update { it.copy(selectedFormat = f) }
    }

    suspend fun fetchInfo() {
        val url = _uiState.value.url.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(error = "الصق رابطًا أولًا.") }
            return
        }
        _uiState.update { it.copy(isFetching = true, error = null, info = null, downloadDone = false) }
        try {
            val info = YtDlpBridge.getInfo(url)
            if (info.formats.isEmpty() && info.title == "بدون عنوان") {
                _uiState.update { it.copy(isFetching = false, error = "تعذر جلب المعلومات.") }
                return
            }
            _uiState.update {
                it.copy(
                    isFetching = false,
                    info = info,
                    selectedFormat = if (it.kind == DownloadKind.VIDEO) info.videoFormats.firstOrNull() else null,
                )
            }
        } catch (e: Exception) {
            val msg = (e.message ?: "").ifBlank { "حدث خطأ غير معروف." }
            _uiState.update { it.copy(isFetching = false, error = msg) }
        }
    }

    /**
     * التحميل داخل التطبيق (مباشر، مع شريط تقدم) + نسخ إلى التنزيلات العامة.
     * ملاحظة: خدمة DownloadService الأمامية موجودة للتحميل في الخلفية وتُستخدم
     * عبر [startBackgroundDownload]؛ الزر الرئيسي يستخدم المسار المباشر الأبسط.
     */
    suspend fun startDownload(context: Context) {
        val s = _uiState.value
        val info = s.info ?: return
        val app = context.applicationContext
        _uiState.update { it.copy(isDownloading = true, downloadPercent = 0, downloadDone = false, error = null) }
        Haptics.confirm(app)
        val id = UUID.randomUUID().toString()
        val store = HistoryStore(app)
        try {
            val workDir = File(app.cacheDir, "pineapple_tmp").apply { mkdirs() }
            DownloadManager.downloadFlow(s.url.trim(), info.title, s.kind, s.selectedFormat, workDir, info)
                .catch { e ->
                    val msg = (e.message ?: "").ifBlank { "حدث خطأ أثناء التحميل." }
                    store.upsert(
                        DownloadItem(id, info.title, s.url, "", s.kind, DownloadStatus.ERROR, errorMessage = msg),
                    )
                    _uiState.update { it.copy(isDownloading = false, error = msg) }
                    Haptics.error(app)
                }
                .collect { ev ->
                    _uiState.update {
                        it.copy(
                            downloadPercent = ev.percent,
                            downloadSpeed = ev.speedBytesPerSec,
                            downloadEta = ev.etaSeconds,
                        )
                    }
                    if (ev.percent >= 100) {
                        val tmp = File(ev.tempFile)
                        val ext = tmp.extension.ifBlank { if (s.kind == DownloadKind.AUDIO) "mp3" else "mp4" }
                        val displayName = tmp.nameWithoutExtension + "." + ext
                        runCatching {
                            MediaStoreSaver.save(app, tmp, displayName, MediaStoreSaver.mimeFor(displayName))
                        }
                        tmp.delete()
                        store.upsert(
                            DownloadItem(id, info.title, s.url, displayName, s.kind, DownloadStatus.DONE, progress = 100),
                        )
                        _uiState.update { it.copy(isDownloading = false, downloadDone = true) }
                        Haptics.confirm(app)
                    }
                }
        } catch (e: Exception) {
            val msg = (e.message ?: "").ifBlank { "حدث خطأ أثناء التحميل." }
            _uiState.update { it.copy(isDownloading = false, error = msg) }
            Haptics.error(app)
        }
    }

    /** تحميل خلفية عبر Foreground Service (إشعار + إلغاء). */
    fun startBackgroundDownload(context: Context) {
        val s = _uiState.value
        val info = s.info ?: return
        if (!Permissions.hasNotificationPermission(context)) return
        DownloadService.start(context, s.url.trim(), info.title, s.kind, s.selectedFormat?.formatId.orEmpty())
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
