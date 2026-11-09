package com.flowapps.countdown

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class CountdownWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    companion object {
        fun refresh(c: Context) {
            val m = AppWidgetManager.getInstance(c)
            m.getAppWidgetIds(ComponentName(c, CountdownWidget::class.java)).forEach { update(c, m, it) }
        }

        fun update(c: Context, m: AppWidgetManager, id: Int) {
            val now = System.currentTimeMillis()
            val future = Store.load(c).filter { it.targetMillis > now }
            val pick = future.filter { it.favorite }.minByOrNull { it.targetMillis }
                ?: future.minByOrNull { it.targetMillis }
            val v = RemoteViews(c.packageName, R.layout.widget_countdown)
            if (pick == null) {
                v.setTextViewText(R.id.w_title, "CountDown")
                v.setTextViewText(R.id.w_num, "—")
                v.setTextViewText(R.id.w_unit, "Crea una cuenta regresiva")
            } else {
                val diff = pick.targetMillis - now
                val days = diff / 86_400_000L
                val num = if (days >= 1) days else diff / 3_600_000L
                val unit = if (days >= 1) (if (days == 1L) "día" else "días") else "horas"
                v.setTextViewText(R.id.w_title, pick.title)
                v.setTextViewText(R.id.w_num, num.toString())
                v.setTextViewText(R.id.w_unit, unit)
            }
            val open = PendingIntent.getActivity(
                c, 0, Intent(c, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            v.setOnClickPendingIntent(R.id.root, open)
            m.updateAppWidget(id, v)
        }
    }
}
