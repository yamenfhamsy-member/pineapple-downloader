package com.pineapple.downloader.model

/** عنصر واحد في سجل التحميلات. */
data class DownloadItem(
    val id: String,
    val title: String,
    val url: String,
    val fileName: String,
    val kind: DownloadKind,
    val status: DownloadStatus,
    val progress: Int = 0,
    val speed: String = "—",
    val eta: String = "—",
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class DownloadKind { VIDEO, AUDIO }

enum class DownloadStatus { LOADING, DONE, ERROR }
