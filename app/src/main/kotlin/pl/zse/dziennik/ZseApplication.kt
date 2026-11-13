package pl.zse.dziennik

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pl.zse.dziennik.data.DiaryPreferences

@HiltAndroidApp
class ZseApplication : Application(), androidx.work.Configuration.Provider {
    override val workManagerConfiguration get() = androidx.work.Configuration.Builder().build()
    @Inject lateinit var preferences: DiaryPreferences
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override fun onCreate() {
        super.onCreate()
        scope.launch { preferences.options.map { it.syncMinutes to it.wifiOnly }.distinctUntilChanged().collect { (minutes, wifi) -> DemoSyncScheduler.apply(this@ZseApplication, minutes, wifi) } }
    }
}
