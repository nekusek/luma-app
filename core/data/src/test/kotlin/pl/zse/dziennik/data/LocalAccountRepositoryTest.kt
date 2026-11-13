package pl.zse.dziennik.data

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.core.DataStore
import androidx.room.Room
import java.io.IOException
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import pl.zse.dziennik.database.SchoolDatabase
import pl.zse.dziennik.model.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class LocalAccountRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private lateinit var scope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private lateinit var database: SchoolDatabase
    private lateinit var cache: CachedSchoolRepository
    private lateinit var repository: LocalAccountRepository
    private val clock = Clock.fixed(Instant.parse("2026-11-12T07:15:00Z"), SchoolZone)
    private val anna = DemoProfiles.accounts[0]
    private val mateusz = DemoProfiles.accounts[1]

    private fun createStore(): DataStore<Preferences> {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { folder.root.resolve("accounts.preferences_pb") })
    }

    @Before fun setup() {
        store = createStore()
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), SchoolDatabase::class.java).allowMainThreadQueries().build()
        cache = CachedSchoolRepository(database.schoolDao(), MockDataRepository(clock))
        repository = LocalAccountRepository(store, listOf(DemoAuthenticationProvider()), cache::clearStudent)
    }

    @After fun cleanup() = runBlocking {
        scope.cancel()
        scope.coroutineContext.job.join()
        database.close()
    }

    @Test fun switchingAccountsKeepsSeparateStudentData() = runBlocking {
        repository.signIn(AuthenticationRequest.Demo(anna.id))
        cache.refresh(anna.studentId)
        repository.signIn(AuthenticationRequest.Demo(mateusz.id))
        cache.refresh(mateusz.studentId)
        assertEquals(mateusz.id, repository.session.first().activeId)
        assertEquals("5", cache.observe(anna.studentId).first()!!.grades.first { it.id == "g1" }.displayValue)
        assertEquals("3", cache.observe(mateusz.studentId).first()!!.grades.first { it.id == "g1" }.displayValue)
        repository.activate(anna.id)
        assertEquals(anna, repository.session.first().activeAccount)
        assertTrue(cache.observe(anna.studentId).first()!!.grades.all { it.studentId == anna.studentId })
        repository.remove(anna.id)
        assertEquals(mateusz.id, repository.session.first().activeId)
        assertNull(cache.observe(anna.studentId).first())
        assertNotNull(cache.observe(mateusz.studentId).first())
        repository.remove(mateusz.id)
        assertEquals(AccountSession(), repository.session.first())
    }

    @Test fun selectedAccountSurvivesStoreRecreation() = runBlocking {
        repository.signIn(AuthenticationRequest.Demo(anna.id))
        repository.signIn(AuthenticationRequest.Demo(mateusz.id))
        repository.activate(anna.id)
        scope.cancel()
        scope.coroutineContext.job.join()
        store = createStore()
        val restored = LocalAccountRepository(store, listOf(DemoAuthenticationProvider()), cache::clearStudent)
        assertEquals(anna, restored.session.first().activeAccount)
        assertEquals(2, restored.session.first().accounts.size)
    }

    @Test fun removingOneProfileDoesNotDeleteCacheSharedWithAParent() = runBlocking {
        val parent = DemoProfiles.accounts.first { it.role == AccountRole.PARENT }
        repository.signIn(AuthenticationRequest.Demo(anna.id))
        cache.refresh(anna.studentId)
        repository.signIn(AuthenticationRequest.Demo(parent.id))
        repository.remove(parent.id)
        assertNotNull(cache.observe(anna.studentId).first())
        repository.remove(anna.id)
        assertNull(cache.observe(anna.studentId).first())
    }

    @Test fun unknownAccountCannotReplaceTheActiveAccount() = runBlocking {
        repository.signIn(AuthenticationRequest.Demo(anna.id))
        assertTrue(runCatching { repository.activate("missing") }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(anna.id, repository.session.first().activeId)
        repository.signIn(AuthenticationRequest.Demo(anna.id))
        assertEquals(1, repository.session.first().accounts.size)
    }

    @Test fun unsupportedProviderCannotCreateAnAuthenticatedSession() = runBlocking {
        assertTrue(runCatching { repository.signIn(AuthenticationRequest.ExternalCallback("edu-vulcan", "luma://callback")) }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(AccountSession(), repository.session.first())
    }

    @Test fun corruptMetadataFailsClosed() = runBlocking {
        store.edit { it[stringPreferencesKey("session_v1")] = "invalid-json" }
        assertTrue(runCatching { repository.session.first() }.exceptionOrNull() is IOException)
    }

    @Test fun logoutWaitsForAnInflightRefreshBeforeRemovingCache() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val clearing = CompletableDeferred<Unit>()
        val delayedCache = CachedSchoolRepository(database.schoolDao(), object : SnapshotSource {
            override suspend fun fetch(studentId: String): SchoolSnapshot {
                started.complete(Unit)
                finish.await()
                return MockDataRepository(clock).fetch(studentId)
            }
        })
        val accounts = LocalAccountRepository(store, listOf(DemoAuthenticationProvider())) { id ->
            clearing.complete(Unit)
            delayedCache.clearStudent(id)
        }
        accounts.signIn(AuthenticationRequest.Demo(anna.id))
        val refresh = async(Dispatchers.IO) { delayedCache.refresh(anna.studentId) }
        started.await()
        val logout = async(Dispatchers.IO) { accounts.remove(anna.id) }
        clearing.await()
        finish.complete(Unit)
        refresh.await()
        logout.await()
        assertNull(delayedCache.observe(anna.studentId).first())
        assertEquals(AccountSession(), accounts.session.first())
    }
}
