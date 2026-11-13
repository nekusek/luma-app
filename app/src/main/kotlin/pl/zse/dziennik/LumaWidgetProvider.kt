package pl.zse.dziennik

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dagger.hilt.android.EntryPointAccessors
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import pl.zse.dziennik.model.*

class LumaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dependencies = EntryPointAccessors.fromApplication(context.applicationContext, DiaryDependencies::class.java)
                val account = dependencies.accounts().session.first().activeAccount
                val snapshot = account?.takeIf { it.role != AccountRole.TEACHER }?.let { dependencies.school().observe(it.studentId).first() }
                val lesson = snapshot?.timetable?.currentOrNextLesson(Instant.now())
                val extras = account?.takeIf { it.role != AccountRole.TEACHER }?.let { dependencies.features().observe(it.studentId).first() }
                val text = if (account == null) "Dodaj profil w aplikacji." else if (account.role == AccountRole.TEACHER) "Otwórz dziennik nauczyciela demo." else if (lesson == null) "Brak kolejnych lekcji w cache." else "${lesson.subjectName}\n${lesson.startsAt.atZone(SchoolZone).format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))} · sala ${lesson.room ?: "—"}"
                ids.forEach { id ->
                    val views = RemoteViews(context.packageName, R.layout.luma_widget)
                    views.setTextViewText(R.id.widget_title, "Luma · ${account?.displayName ?: "demo"}")
                    views.setTextViewText(R.id.widget_lesson, text)
                    views.setTextViewText(R.id.widget_info, extras?.luckyNumber?.let { "Numerek: $it · ${extras.luckyDate}\nDEMO · ostatnio zapisane dane" } ?: "DEMO · dane lokalne")
                    views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                    manager.updateAppWidget(id, views)
                }
            } catch (cause: CancellationException) { throw cause } catch (_: Exception) {
                ids.forEach { id -> val views = RemoteViews(context.packageName, R.layout.luma_widget); views.setTextViewText(R.id.widget_lesson, "Otwórz Lumę, aby odświeżyć dane demo."); manager.updateAppWidget(id, views) }
            } finally { pending?.finish() }
        }
    }
    companion object {
        fun refresh(context: Context) {
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, LumaWidgetProvider::class.java))
            if (ids.isNotEmpty()) context.sendBroadcast(Intent(context, LumaWidgetProvider::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
        }
    }
}
