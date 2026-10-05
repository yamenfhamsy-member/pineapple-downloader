package com.pineapple.downloader.model

/** معلومات الفيديو بعد "جلب المعلومات" — قبل التحميل. */
data class VideoInfo(
    val title: String,
    val duration: Int,
    val thumbnail: String,
    val uploader: String,
    val webpageUrl: String,
    val formats: List<FormatOption>,
) {
    /** صيغ فيديو (فيها صورة) مرتبة من الأعلى جودة تقريبًا. */
    val videoFormats: List<FormatOption> get() = formats.filter { it.isVideo }.sortedByDescending { it.tbr }

    /** صيغ صوت فقط. */
    val audioFormats: List<FormatOption> get() = formats.filter { it.isAudioOnly }.sortedByDescending { it.tbr }
}
