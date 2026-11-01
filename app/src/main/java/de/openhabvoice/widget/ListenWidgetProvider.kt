package de.openhabvoice.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import de.openhabvoice.MainActivity
import de.openhabvoice.R

/**
 * 1x1-Home-Screen-Widget: einzelner Mikrofon-Button.
 *
 * Tap startet [MainActivity] mit Extra
 * [MainActivity.EXTRA_AUTO_START_LISTENING] – die App beginnt dann
 * unmittelbar mit der Spracherkennung (inkl. Berechtigungs-Handling).
 */
class ListenWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val views = buildRemoteViews(context)
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun buildRemoteViews(context: Context): RemoteViews {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_AUTO_START_LISTENING, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteViews(context.packageName, R.layout.listen_widget).apply {
            setOnClickPendingIntent(R.id.widget_mic_button, pendingIntent)
        }
    }
}