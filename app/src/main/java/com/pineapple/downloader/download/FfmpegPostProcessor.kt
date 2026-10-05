package com.pineapple.downloader.download

import android.os.Build
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * معالجة ما بعد التحميل عبر FFmpeg.
 *
 * - دمج فيديو+صوت منفصلين (bestvideo+bestaudio) إلى mp4 واحد.
 * - تحويل الصوت إلى mp3 (libmp3lame — متوفر في حزمة full فقط).
 * - على الأجهزة غير-arm64 (لا توجد .so للـ fork) تُعاد الملفات كما هي
 *   (mp4 / m4a مباشرة) — انظر [isSupported].
 */
object FfmpegPostProcessor {

    /** false على الأجهزة غير المدعومة — عندها نتخطى التحويل ونحفظ الملف الأصلي. */
    val isSupported: Boolean by lazy {
        try {
            Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }
        } catch (_: Exception) {
            false
        }
    }

    /** يدمج/يحوّل [input] إلى [output]. يعيد true عند النجاح. */
    suspend fun convert(input: File, output: File, vararg ffmpegArgs: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!isSupported) return@withContext false
            val cmd = listOf("-y", "-i", input.absolutePath) + ffmpegArgs + output.absolutePath
            val session = FFmpegKit.execute(cmd.joinToString(" ") { escape(it) })
            ReturnCode.isSuccess(session.returnCode)
        }

    suspend fun toMp3(input: File, outputMp3: File): Boolean =
        convert(input, outputMp3, "-vn", "-codec:a", "libmp3lame", "-q:a", "2")

    suspend fun mergeToMp4(video: File, audio: File, outputMp4: File): Boolean =
        withContext(Dispatchers.IO) {
            if (!isSupported) return@withContext false
            val cmd = "-y -i ${escape(video.absolutePath)} -i ${escape(audio.absolutePath)}" +
                " -c:v copy -c:a aac ${escape(outputMp4.absolutePath)}"
            ReturnCode.isSuccess(FFmpegKit.execute(cmd).returnCode)
        }

    private fun escape(arg: String): String = "'" + arg.replace("'", "'\\''") + "'"
}
