package com.example.zhuki_game

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class GoldWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            fetchAndUpdate(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        fetchAndUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun fetchAndUpdate(
        context: Context,
        manager: AppWidgetManager? = null,
        widgetIds: IntArray? = null
    ) {
        val pendingResult = goAsync() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rate = GoldRateRepository.getInstance(context).fetchGoldRate()
                val text = rate?.let { formatRate(it) }
                    ?: context.getString(R.string.widget_gold_none)
                updateWidgets(context, manager, widgetIds, text)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun updateWidgets(
        context: Context,
        manager: AppWidgetManager?,
        widgetIds: IntArray?,
        rateText: String
    ) {
        val appWidgetManager = manager ?: AppWidgetManager.getInstance(context)
        val ids = widgetIds ?: appWidgetManager.getAppWidgetIds(
            ComponentName(context, GoldWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return
        val views = RemoteViews(context.packageName, R.layout.widget_gold).apply {
            setTextViewText(R.id.tvWidgetRate, rateText)
            setOnClickPendingIntent(R.id.widgetRoot, refreshIntent(context))
        }
        appWidgetManager.updateAppWidget(ids, views)
    }

    private fun refreshIntent(context: Context): android.app.PendingIntent {
        val intent = Intent(context, GoldWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        return android.app.PendingIntent.getBroadcast(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatRate(rate: Double): String =
        String.format(Locale.US, "%,.0f", rate).replace(',', ' ')

    companion object {
        const val ACTION_REFRESH = "com.example.zhuki_game.ACTION_REFRESH_GOLD"
    }
}
