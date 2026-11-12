package pl.zse.dziennik.model

import kotlinx.coroutines.flow.Flow

enum class AccountRole { STUDENT, PARENT, TEACHER }

data class DiaryAccount(
    val id: String,
    val studentId: String,
    val displayName: String,
    val className: String,
    val schoolName: String,
    val providerId: String,
    val role: AccountRole = AccountRole.STUDENT,
) {
    init {
        require(id.isNotBlank() && studentId.isNotBlank() && providerId.isNotBlank())
    }
}

data class AccountSession(val accounts: List<DiaryAccount> = emptyList(), val activeId: String? = null) {
    val activeAccount: DiaryAccount? get() = accounts.firstOrNull { it.id == activeId }
    init {
        require(accounts.map { it.id }.distinct().size == accounts.size)
        require(accounts.isEmpty() == (activeId == null))
        require(activeId == null || accounts.any { it.id == activeId })
    }
}

sealed interface AuthenticationRequest {
    val providerId: String
    data class Demo(val profileId: String) : AuthenticationRequest { override val providerId = "demo" }
    data class ExternalCallback(override val providerId: String, val callbackUri: String) : AuthenticationRequest
}

interface AuthenticationProvider {
    val id: String
    suspend fun authenticate(request: AuthenticationRequest): List<DiaryAccount>
    suspend fun signOut(account: DiaryAccount)
}

interface AccountRepository {
    val session: Flow<AccountSession>
    suspend fun signIn(request: AuthenticationRequest): AccountSession
    suspend fun activate(accountId: String)
    suspend fun remove(accountId: String)
}
