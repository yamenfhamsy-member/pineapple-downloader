package com.pineapple.downloader.python

import com.chaquo.python.Python
import com.pineapple.downloader.model.FormatOption
import com.pineapple.downloader.model.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** لقطة تقدم يقرأها Kotlin عبر polling من Python. */
data class YtProgress(
    val status: String,
    val downloaded: Long,
    val total: Long,
    val speed: Double,
    val eta: Int,
    val filename: String,
    val error: String,
)

/**
 * جسر Chaquopy نحو yt-dlp.
 *
 * - كل الاستدعاءات على Dispatchers.IO (شبكة + عمليات ثقيلة).
 * - طبقة Python ترجع JSON string يُفك بـ org.json — لا عبور لكائنات Python.
 * - أي استثناء من Python يحمل رسالة عربية من طبقة yt_bridge نفسها.
 */
object YtDlpBridge {
    private fun module() = Python.getInstance().getModule("yt_bridge")

    suspend fun getInfo(url: String): VideoInfo = withContext(Dispatchers.IO) {
        val json = module().callAttr("get_info", url).toString()
        val root = JSONObject(json)
        val formatsJson = root.optJSONArray("formats")
        val formats = mutableListOf<FormatOption>()
        if (formatsJson != null) {
            for (i in 0 until formatsJson.length()) {
                val m = formatsJson.optJSONObject(i) ?: continue
                formats += FormatOption(
                    formatId = m.optString("format_id"),
                    ext = m.optString("ext"),
                    resolution = m.optString("resolution"),
                    vcodec = m.optString("vcodec"),
                    acodec = m.optString("acodec"),
                    filesize = m.optLong("filesize"),
                    tbr = m.optDouble("tbr"),
                    isAudioOnly = m.optBoolean("is_audio_only"),
                )
            }
        }
        VideoInfo(
            title = root.optString("title", "بدون عنوان"),
            duration = root.optInt("duration"),
            thumbnail = root.optString("thumbnail"),
            uploader = root.optString("uploader"),
            webpageUrl = root.optString("webpage_url", url),
            formats = formats,
        )
    }

    /** يبدأ التحميل. outtmpl بدون امتداد. يعيد اسم الملف النهائي. */
    suspend fun download(
        url: String,
        outtmpl: String,
        formatSelector: String,
        audioOnly: Boolean,
    ): String = withContext(Dispatchers.IO) {
        val json = module().callAttr("download", url, outtmpl, formatSelector, audioOnly).toString()
        JSONObject(json).optString("filename")
    }

    suspend fun pollProgress(): YtProgress = withContext(Dispatchers.IO) {
        val m = JSONObject(module().callAttr("poll_progress").toString())
        YtProgress(
            status = m.optString("status"),
            downloaded = m.optLong("downloaded"),
            total = m.optLong("total"),
            speed = m.optDouble("speed"),
            eta = m.optInt("eta"),
            filename = m.optString("filename"),
            error = m.optString("error"),
        )
    }
}
