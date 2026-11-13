package pl.zse.dziennik

import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource
import org.robolectric.RuntimeEnvironment

class AccountsIsolationRule : ExternalResource() {
    override fun before() = runBlocking {
        val repository = EntryPointAccessors.fromApplication(RuntimeEnvironment.getApplication(), DiaryDependencies::class.java).accounts()
        repository.session.first().accounts.forEach { repository.remove(it.id) }
    }
}
