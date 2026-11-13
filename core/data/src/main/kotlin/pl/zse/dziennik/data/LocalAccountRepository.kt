package pl.zse.dziennik.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.zse.dziennik.model.*

private val Context.accountsStore by preferencesDataStore(name = "diary_accounts")

class LocalAccountRepository internal constructor(private val store: DataStore<Preferences>, providers: List<AuthenticationProvider>, private val clearStudent: suspend (String) -> Unit) : AccountRepository {
    constructor(context: Context, providers: List<AuthenticationProvider>, clearStudent: suspend (String) -> Unit) : this(context.applicationContext.accountsStore, providers, clearStudent)
    private val registry = providers.associateBy { it.id }
    private val key = stringPreferencesKey("session_v1")
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    override val session = store.data.map { decode(it[key]) }

    override suspend fun signIn(request: AuthenticationRequest) = mutex.withLock {
        val provider = requireNotNull(registry[request.providerId])
        val accounts = provider.authenticate(request)
        require(accounts.isNotEmpty() && accounts.all { it.providerId == provider.id })
        require(accounts.map { it.id }.distinct().size == accounts.size)
        val updated = store.edit { prefs ->
            val current = decode(prefs[key])
            prefs[key] = encode(AccountSession(current.accounts.filterNot { old -> accounts.any { it.id == old.id } } + accounts, accounts.first().id))
        }
        decode(updated[key])
    }

    override suspend fun activate(accountId: String) = mutex.withLock {
        store.edit { prefs ->
            val current = decode(prefs[key])
            require(current.accounts.any { it.id == accountId })
            prefs[key] = encode(current.copy(activeId = accountId))
        }
        Unit
    }

    override suspend fun remove(accountId: String) = mutex.withLock {
        var removed: DiaryAccount? = null
        var clearCache = false
        store.edit { prefs ->
            val current = decode(prefs[key])
            removed = current.accounts.firstOrNull { it.id == accountId }
            val remaining = current.accounts.filterNot { it.id == accountId }
            clearCache = removed?.let { removed -> remaining.none { it.studentId == removed.studentId } } ?: false
            val activeId = if (current.activeId == accountId) remaining.firstOrNull()?.id else current.activeId
            prefs[key] = encode(AccountSession(remaining, activeId))
        }
        removed?.let { account ->
            try { registry[account.providerId]?.signOut(account) }
            finally { if (clearCache) clearStudent(account.studentId) }
        }
        Unit
    }

    private fun encode(session: AccountSession) = json.encodeToString(StoredSession(session.accounts.map { account ->
        StoredAccount(account.id, account.studentId, account.displayName, account.className, account.schoolName, account.providerId, account.role.name)
    }, session.activeId))

    private fun decode(value: String?): AccountSession {
        if (value == null) return AccountSession()
        try {
            val stored = json.decodeFromString<StoredSession>(value)
            return AccountSession(stored.accounts.map { DiaryAccount(it.id, it.studentId, it.displayName, it.className, it.schoolName, it.providerId, AccountRole.valueOf(it.role)) }, stored.activeId)
        } catch (error: SerializationException) {
            throw IOException("Invalid account metadata", error)
        } catch (error: IllegalArgumentException) {
            throw IOException("Invalid account metadata", error)
        }
    }
}

@Serializable
private data class StoredSession(val accounts: List<StoredAccount> = emptyList(), val activeId: String? = null)

@Serializable
private data class StoredAccount(val id: String, val studentId: String, val displayName: String, val className: String, val schoolName: String, val providerId: String, val role: String)
