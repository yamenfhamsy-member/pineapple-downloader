package com.pineapple.downloader.download

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pineapple.downloader.model.DownloadItem
import com.pineapple.downloader.model.DownloadKind
import com.pineapple.downloader.model.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.historyStore by preferencesDataStore(name = "download_history")

/**
 * سجل التحميلات محفوظ في DataStore (سطر واحد لكل عنصر، مفصول بـ � record separator).
 * الحذف بالسحب يُنفَّذ من الواجهة عبر [remove].
 */
class HistoryStore(private val context: Context) {

    private val key = stringPreferencesKey("history_v1")

    val history: Flow<List<DownloadItem>> =
        context.historyStore.data.map { prefs ->
            val raw = prefs[key].orEmpty()
            if (raw.isBlank()) emptyList() else raw.split("\u001E").mapNotNull { decode(it) }
        }

    suspend fun upsert(item: DownloadItem) {
        context.historyStore.edit { prefs ->
            val current = prefs[key].orEmpty()
                .takeIf { it.isNotBlank() }?.split("\u001E")?.mapNotNull { decode(it) }
                .orEmpty()
                .toMutableList()
            current.removeAll { it.id == item.id }
            current.add(0, item)
            prefs[key] = current.take(100).joinToString("\u001E") { encode(it) }
        }
    }

    suspend fun remove(id: String) {
        context.historyStore.edit { prefs ->
            val current = prefs[key].orEmpty()
                .takeIf { it.isNotBlank() }?.split("\u001E")?.mapNotNull { decode(it) }
                .orEmpty()
            prefs[key] = current.filterNot { it.id == id }.joinToString("\u001E") { encode(it) }
        }
    }

    suspend fun clear() {
        context.historyStore.edit { prefs -> prefs[key] = "" }
    }

    private fun encode(i: DownloadItem): String = listOf(
        i.id,
        i.title.replace("|", " "),
        i.url,
        i.fileName.replace("|", " "),
        i.kind.name,
        i.status.name,
        i.createdAt.toString(),
    ).joinToString("|")

    private fun decode(s: String): DownloadItem? {
        val p = s.split("|")
        if (p.size < 7) return null
        return DownloadItem(
            id = p[0],
            title = p[1],
            url = p[2],
            fileName = p[3],
            kind = runCatching { DownloadKind.valueOf(p[4]) }.getOrDefault(DownloadKind.VIDEO),
            status = runCatching { DownloadStatus.valueOf(p[5]) }.getOrDefault(DownloadStatus.DONE),
            createdAt = p[6].toLongOrNull() ?: System.currentTimeMillis(),
        )
    }
}
