package pl.zse.dziennik

import android.content.Context
import androidx.work.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import pl.zse.dziennik.model.*
import pl.zse.dziennik.data.CachedSchoolRepository

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DiaryDependencies {
    fun cached(): CachedSchoolRepository
    fun accounts(): AccountRepository
    fun school(): SchoolRepository
    fun features(): DiaryFeaturesRepository
}
class DemoSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        return try {
            val dependencies = EntryPointAccessors.fromApplication(applicationContext, DiaryDependencies::class.java)
            dependencies.accounts().session.first().accounts.filter { it.role != AccountRole.TEACHER }.distinctBy { it.studentId }.forEach {
                if (dependencies.accounts().session.first().accounts.any { current -> current.studentId == it.studentId }) {
                    dependencies.school().refresh(it.studentId)
                    dependencies.features().refresh(it.studentId)
                }
                if (dependencies.accounts().session.first().accounts.none { current -> current.studentId == it.studentId }) dependencies.cached().clearStudent(it.studentId)
            }
            LumaWidgetProvider.refresh(applicationContext)
            Result.success()
        } catch (cause: CancellationException) { throw cause }
        catch (_: Exception) { if (runAttemptCount < 3) Result.retry() else Result.failure() }
    }
}
object DemoSyncScheduler {
    fun apply(context: Context, minutes: Int, wifiOnly: Boolean) {
        val manager = WorkManager.getInstance(context)
        if (minutes == 0) { manager.cancelUniqueWork("luma-demo-sync"); return }
        require(minutes >= 15)
        val work = PeriodicWorkRequestBuilder<DemoSyncWorker>(minutes.toLong(), TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        manager.enqueueUniquePeriodicWork("luma-demo-sync", ExistingPeriodicWorkPolicy.UPDATE, work)
    }
}
