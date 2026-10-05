package com.pineapple.downloader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pineapple.downloader.MainActivity
import com.pineapple.downloader.R
import com.pineapple.downloader.download.DownloadManager
import com.pineapple.downloader.download.HistoryStore
import com.pineapple.downloader.model.DownloadItem
import com.pineapple.downloader.model.DownloadKind
import com.pineapple.downloader.model.DownloadStatus
import com.pineapple.downloader.model.VideoInfo
import com.pineapple.downloader.model.formatBytes
import com.pineapple.downloader.python.YtDlpBridge
import com.pineapple.downloader.util.Haptics
import com.pineapple.downloader.util.MediaStoreSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * خدمة أمامية للتحميل: إشعار أخضر بأيقونة الأناناس + زر إلغاء.
 * تُشغَّل من الواجهة عبر [start] وتُنهي نفسها عند الاكتمال/الفشل/الإلغاء.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                currentJob?.cancel()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val url = intent.getStringExtra(EXTRA_URL).orEmpty()
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                val kind = DownloadKind.valueOf(intent.getStringExtra(EXTRA_KIND) ?: "VIDEO")
                val formatId = intent.getStringExtra(EXTRA_FORMAT_ID).orEmpty()
                if (url.isBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                launchDownload(url, title, kind, formatId)
            }
        }
        return START_NOT_STICKY
    }

    private fun launchDownload(url: String, title: String, kind: DownloadKind, formatId: String) {
        startForegroundCompat(buildNotification(title, 0, true))
        currentJob?.cancel()
        currentJob = scope.launch {
            val id = UUID.randomUUID().toString()
            val store = HistoryStore(applicationContext)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            try {
                // نحتاج قائمة الصيغ لاختيار الـ selector — نجلب المعلومات سريعًا إذا لزم
                val info: VideoInfo = runCatching { YtDlpBridge.getInfo(url) }.getOrNull()
                val format = info?.formats?.firstOrNull { it.formatId == formatId }
                val workDir = File(cacheDir, "pineapple_tmp")
                DownloadManager.downloadFlow(url, title.ifBlank { "pineapple" }, kind, format, workDir, info ?: emptyInfo(url))
                    .catch { e ->
                        val msg = e.message ?: "حدث خطأ أثناء التحميل."
                        nm.notify(NOTIF_ID, buildNotification(title, 0, false, error = msg))
                        store.upsert(
                            DownloadItem(id, title, url, "", kind, DownloadStatus.ERROR, errorMessage = msg),
                        )
                        Haptics.error(applicationContext)
                        stopSelf()
                    }
                    .collect { ev ->
                        nm.notify(NOTIF_ID, buildNotification(title, ev.percent, true))
                        if (ev.percent >= 100) {
                            val tmp = File(ev.tempFile)
                            val ext = tmp.extension.ifBlank { if (kind == DownloadKind.AUDIO) "mp3" else "mp4" }
                            val displayName = tmp.nameWithoutExtension + "." + ext
                            runCatching {
                                MediaStoreSaver.save(applicationContext, tmp, displayName, MediaStoreSaver.mimeFor(displayName))
                            }
                            tmp.delete()
                            workDir.deleteRecursively()
                            store.upsert(
                                DownloadItem(id, title, url, displayName, kind, DownloadStatus.DONE, progress = 100),
                            )
                            nm.notify(NOTIF_ID, buildNotification(title, 100, false))
                            Haptics.confirm(applicationContext)
                            stopSelf()
                        }
                    }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val msg = e.message ?: "حدث خطأ أثناء التحميل."
                nm.notify(NOTIF_ID, buildNotification(title, 0, false, error = msg))
                store.upsert(
                    DownloadItem(id, title, url, "", kind, DownloadStatus.ERROR, errorMessage = msg),
                )
                Haptics.error(applicationContext)
                stopSelf()
            }
        }
    }

    private fun emptyInfo(url: String) = VideoInfo("بدون عنوان", 0, "", "", url, emptyList())

    private fun buildNotification(title: String, percent: Int, ongoing: Boolean, error: String? = null): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelIntent = PendingIntent.getService(
            this, 1, Intent(this, DownloadService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = when {
            error != null -> error
            !ongoing && percent >= 100 -> "اكتمل التحميل — $title"
            else -> "جارٍ التحميل $percent٪ — $title"
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.pineapple_logo)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setColor(0xFF1B8A3E.toInt())
            .setColorized(true)
        if (ongoing) {
            builder.setProgress(100, percent, percent == 0)
                .addAction(R.drawable.ic_close, "إلغاء", cancelIntent)
        }
        return builder.build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "التحميلات",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "تقدم تحميل الملفات" },
            )
        }
    }

    override fun onDestroy() {
        currentJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "pineapple_downloads"
        const val NOTIF_ID = 1001
        const val ACTION_START = "com.pineapple.downloader.START"
        const val ACTION_CANCEL = "com.pineapple.downloader.CANCEL"
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_KIND = "kind"
        const val EXTRA_FORMAT_ID = "format_id"

        fun start(context: Context, url: String, title: String, kind: DownloadKind, formatId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_KIND, kind.name)
                putExtra(EXTRA_FORMAT_ID, formatId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        @Suppress("unused")
        fun bytesLabel(v: Long) = formatBytes(v)
    }
}
