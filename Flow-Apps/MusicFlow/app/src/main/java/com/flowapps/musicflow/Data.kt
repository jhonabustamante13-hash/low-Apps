package com.flowapps.musicflow

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri
)

object Repo {
    fun load(c: Context): List<Song> {
        val col = if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val proj = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION
        )
        val sel = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 30000"
        val out = mutableListOf<Song>()
        c.contentResolver.query(col, proj, sel, null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { cur ->
            val iId = cur.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iT = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iAr = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iAl = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val iD = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cur.moveToNext()) {
                val id = cur.getLong(iId)
                val artist = cur.getString(iAr)?.takeIf { it != "<unknown>" } ?: "Artista desconocido"
                out.add(
                    Song(
                        id, cur.getString(iT) ?: "Sin título", artist,
                        cur.getString(iAl) ?: "Álbum desconocido", cur.getLong(iD),
                        android.content.ContentUris.withAppendedId(col, id)
                    )
                )
            }
        }
        return out
    }
}

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("music", Context.MODE_PRIVATE)

    fun favs(c: Context): Set<Long> =
        (sp(c).getStringSet("favs", emptySet()) ?: emptySet()).map { it.toLong() }.toSet()

    fun toggleFav(c: Context, id: Long): Set<Long> {
        val s = favs(c).toMutableSet()
        if (!s.add(id)) s.remove(id)
        sp(c).edit().putStringSet("favs", s.map { it.toString() }.toSet()).apply()
        return s
    }

    private fun lf(c: Context, id: Long): File {
        val dir = File(c.filesDir, "lyrics")
        dir.mkdirs()
        return File(dir, "$id.txt")
    }

    fun lyrics(c: Context, id: Long): String = lf(c, id).takeIf { it.exists() }?.readText() ?: ""

    fun saveLyrics(c: Context, id: Long, text: String) {
        if (text.isBlank()) lf(c, id).delete() else lf(c, id).writeText(text)
    }
}

data class LyricLine(val t: Long?, val text: String)

fun parseLrc(raw: String): List<LyricLine> {
    val re = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
    val out = mutableListOf<LyricLine>()
    raw.lines().forEach { line ->
        val ms = re.findAll(line).toList()
        val text = re.replace(line, "").trim()
        if (ms.isEmpty()) {
            if (line.isNotBlank() && !line.trim().startsWith("[")) out.add(LyricLine(null, line.trim()))
        } else {
            ms.forEach { m ->
                val f = m.groupValues[3]
                val frac = if (f.isEmpty()) 0L else f.padEnd(3, '0').take(3).toLong()
                out.add(LyricLine(m.groupValues[1].toLong() * 60000 + m.groupValues[2].toLong() * 1000 + frac, text))
            }
        }
    }
    return if (out.any { it.t != null }) out.filter { it.t != null }.sortedBy { it.t } else out
}

fun fmtTime(ms: Long): String = "%d:%02d".format(ms / 60000, ms / 1000 % 60)
