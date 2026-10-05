package com.pineapple.downloader.download

import com.pineapple.downloader.model.DownloadKind
import com.pineapple.downloader.model.FormatOption
import com.pineapple.downloader.model.VideoInfo
import com.pineapple.downloader.python.YtDlpBridge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/** حدث تقدم يُبث للواجهة والإشعار. */
data class ProgressEvent(
    val percent: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Double,
    val etaSeconds: Int,
    val tempFile: String,
)

/**
 * منسّق التحميل: يختار selector المناسب، يشغّل yt-dlp، يبث التقدم،
 * ثم معالجة FFmpeg (دمج/تحويل) حسب النوع.
 */
object DownloadManager {

    /** selector يُمرَّر لخيار format في yt-dlp. */
    fun selectorFor(info: VideoInfo, kind: DownloadKind, format: FormatOption?): String {
        if (kind == DownloadKind.AUDIO) return "bestaudio/best"
        val id = format?.formatId
        // bestvideo+bestaudio يُنتج ملفين يدمجهما FFmpeg؛ fallback تلقائي best عند غياب FFmpeg.
        return if (!id.isNullOrBlank()) "$id+bestaudio/best[ext=mp4]/best" else "best[ext=mp4]/best"
    }

    fun downloadFlow(
        url: String,
        title: String,
        kind: DownloadKind,
        format: FormatOption?,
        workDir: File,
        info: VideoInfo,
    ): Flow<ProgressEvent> = flow {
        workDir.mkdirs()
        val safe = title.sanitizeFileName().ifBlank { "pineapple_${System.currentTimeMillis()}" }
        val outtmpl = File(workDir, safe).absolutePath
        val selector = selectorFor(info, kind, format)

        // التحميل حاجب (yt-dlp) يعمل على IO، والمراقبة polling هنا.
        val finalName = coroutineScope {
            val deferred = async(Dispatchers.IO) {
                YtDlpBridge.download(url, outtmpl, selector, kind == DownloadKind.AUDIO)
            }
            try {
                while (!deferred.isCompleted) {
                    val p = runCatching { YtDlpBridge.pollProgress() }.getOrNull()
                    if (p != null && (p.total > 0 || p.downloaded > 0)) {
                        val pct = if (p.total > 0) ((p.downloaded * 100) / p.total).toInt().coerceIn(0, 100) else 0
                        emit(ProgressEvent(pct, p.downloaded, p.total, p.speed, p.eta, p.filename))
                    }
                    delay(400)
                }
                deferred.await()
            } catch (e: CancellationException) {
                deferred.cancel()
                throw e
            }
        }

        // تحديد الملف الناتج الفعلي (yt-dlp يضيف الامتداد)
        val produced = finalName.takeIf { it.isNotBlank() }?.let { File(it) }
            ?.takeIf { it.exists() }
            ?: workDir.listFiles()?.maxByOrNull { it.lastModified() }
            ?: throw IllegalStateException("اكتمل التحميل لكن لم يُعثر على الملف.")

        val finished = postProcess(kind, produced, workDir)
        emit(ProgressEvent(100, finished.length(), finished.length(), 0.0, 0, finished.absolutePath))
    }

    private suspend fun postProcess(kind: DownloadKind, produced: File, workDir: File): File {
        if (kind == DownloadKind.AUDIO && FfmpegPostProcessor.isSupported) {
            if (produced.extension.lowercase() != "mp3") {
                val mp3 = File(workDir, produced.nameWithoutExtension + ".mp3")
                if (FfmpegPostProcessor.toMp3(produced, mp3)) {
                    produced.delete()
                    return mp3
                }
            }
        }
        return produced
    }

    private fun String.sanitizeFileName(): String =
        replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().take(80)
}
