package com.flowapps.countdown

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

data class Cd(
    val id: Long,
    val title: String,
    val category: String,
    val targetMillis: Long,
    val color: Int,
    val favorite: Boolean,
    val photo: String?,
    val notify: Boolean
)

object Store {
    private fun sp(c: Context) = c.getSharedPreferences("countdown", Context.MODE_PRIVATE)

    fun load(c: Context): List<Cd> {
        val a = JSONArray(sp(c).getString("items", "[]") ?: "[]")
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Cd(
                o.getLong("id"), o.getString("title"), o.getString("category"),
                o.getLong("t"), o.getInt("color"), o.getBoolean("fav"),
                if (o.isNull("photo")) null else o.getString("photo"),
                o.optBoolean("notify", true)
            )
        }
    }

    fun save(c: Context, list: List<Cd>) {
        val a = JSONArray()
        list.forEach {
            a.put(
                JSONObject().put("id", it.id).put("title", it.title).put("category", it.category)
                    .put("t", it.targetMillis).put("color", it.color).put("fav", it.favorite)
                    .put("photo", it.photo ?: JSONObject.NULL).put("notify", it.notify)
            )
        }
        sp(c).edit().putString("items", a.toString()).apply()
        Notifier.schedule(c, list)
        CountdownWidget.refresh(c)
    }
}

object Notifier {
    fun schedule(c: Context, list: List<Cd>) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val sp = c.getSharedPreferences("countdown", Context.MODE_PRIVATE)
        (sp.getStringSet("alarms", emptySet()) ?: emptySet()).forEach { am.cancel(pi(c, it.toLong(), "")) }
        val ids = mutableSetOf<String>()
        list.filter { it.notify && it.targetMillis > System.currentTimeMillis() }.forEach {
            am.set(AlarmManager.RTC_WAKEUP, it.targetMillis, pi(c, it.id, it.title))
            ids.add(it.id.toString())
        }
        sp.edit().putStringSet("alarms", ids).apply()
    }

    private fun pi(c: Context, id: Long, title: String): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java).putExtra("title", title)
        return PendingIntent.getBroadcast(
            c, id.toInt(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
