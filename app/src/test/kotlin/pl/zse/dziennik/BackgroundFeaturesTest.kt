package pl.zse.dziennik

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.TextView
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import pl.zse.dziennik.model.AuthenticationRequest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = ZseApplication::class)
class BackgroundFeaturesTest {
    @Test fun syncHasOnePeriodicJobHonorsNetworkConstraintAndCanBeDisabled() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val manager = WorkManager.getInstance(context)
        DemoSyncScheduler.apply(context, 60, true)
        suspend fun work(): List<WorkInfo> = manager.getWorkInfosForUniqueWork("luma-demo-sync").get(10, TimeUnit.SECONDS)
        var jobs = work()
        repeat(100) { if (jobs.isEmpty()) { delay(10); jobs = work() } }
        val job = jobs.single { !it.state.isFinished }
        assertEquals(NetworkType.UNMETERED, job.constraints.requiredNetworkType)
        DemoSyncScheduler.apply(context, 240, true)
        repeat(100) { if (work().count { !it.state.isFinished } != 1) delay(10) }
        assertEquals(job.id, work().single { !it.state.isFinished }.id)
        DemoSyncScheduler.apply(context, 0, true)
        repeat(100) { if (work().any { !it.state.isFinished }) delay(10) }
        assertTrue(work().all { it.state.isFinished })
    }
    @Test fun homeWidgetShowsCachedLessonAndClearsStudentAfterLogout() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val dependencies = EntryPointAccessors.fromApplication(context, DiaryDependencies::class.java)
        dependencies.accounts().session.first().accounts.forEach { dependencies.accounts().remove(it.id) }
        dependencies.accounts().signIn(AuthenticationRequest.Demo("demo:anna"))
        dependencies.school().refresh("demo-zse-3ti")
        dependencies.features().refresh("demo-zse-3ti")
        val shadow = Shadows.shadowOf(AppWidgetManager.getInstance(context))
        val id = shadow.createWidget(LumaWidgetProvider::class.java, R.layout.luma_widget)
        repeat(200) { if (!shadow.getViewFor(id).findViewById<TextView>(R.id.widget_lesson).text.contains("sala")) delay(10) }
        assertTrue(shadow.getViewFor(id).findViewById<TextView>(R.id.widget_lesson).text.contains("sala"))
        dependencies.accounts().remove("demo:anna")
        LumaWidgetProvider().onUpdate(context, AppWidgetManager.getInstance(context), intArrayOf(id))
        repeat(200) { if (!shadow.getViewFor(id).findViewById<TextView>(R.id.widget_lesson).text.contains("Dodaj profil")) delay(10) }
        assertTrue(shadow.getViewFor(id).findViewById<TextView>(R.id.widget_lesson).text.contains("Dodaj profil"))
        assertEquals(1, AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, LumaWidgetProvider::class.java)).size)
    }
}
