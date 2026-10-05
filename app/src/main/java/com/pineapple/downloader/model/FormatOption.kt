package com.pineapple.downloader.model

/** صيغة واحدة من قائمة الصيغ التي يرجعها yt-dlp. */
data class FormatOption(
    val formatId: String,
    val ext: String,
    val resolution: String,
    val vcodec: String,
    val acodec: String,
    val filesize: Long,
    val tbr: Double,
    val isAudioOnly: Boolean,
) {
    val isVideo: Boolean get() = !isAudioOnly

    /** حجم تقريبي للعرض، أو null إذا مجهول. */
    val displaySize: String? get() = filesize.takeIf { it > 0 }?.let { formatBytes(it) }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "—"
    val units = arrayOf("ب", "ك.ب", "م.ب", "ج.ب")
    var v = bytes.toDouble()
    var i = 0
    while (v >= 1024 && i < units.lastIndex) {
        v /= 1024
        i++
    }
    return if (i == 0) "$bytes ${units[i]}" else "%.1f %s".format(v, units[i])
}

fun formatDuration(totalSeconds: Int): String {
    if (totalSeconds <= 0) return "—"
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun formatSpeed(bytesPerSec: Double): String {
    if (bytesPerSec <= 0) return "—"
    return formatBytes(bytesPerSec.toLong()) + "/ث"
}

fun formatEta(totalSeconds: Int): String {
    if (totalSeconds <= 0) return "—"
    return "متبقٍ " + formatDuration(totalSeconds)
}
