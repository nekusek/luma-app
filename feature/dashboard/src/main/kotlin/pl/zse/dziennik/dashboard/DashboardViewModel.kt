package pl.zse.dziennik.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.zse.dziennik.model.*

data class DashboardUiState(
    val snapshot: SchoolSnapshot? = null,
    val now: Instant = Instant.EPOCH,
    val refreshing: Boolean = false,
    val error: String? = null,
)

class DashboardViewModel(private val repository: SchoolRepository, private val clock: Clock, private val studentId: String) : ViewModel() {
    private val refreshing = MutableStateFlow(false)
    private val refreshError = MutableStateFlow<String?>(null)
    private val readError = MutableStateFlow<String?>(null)
    val now = flow {
        while (currentCoroutineContext().isActive) { emit(clock.instant()); delay(1_000) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), clock.instant())
    private val snapshot = repository.observe(studentId)
        .onEach { readError.value = null }
        .retryWhen { cause, attempt ->
            if (cause is CancellationException) throw cause
            readError.value = "Nie można odczytać danych lokalnych. Ponawiam próbę."
            delay((1_000L shl attempt.coerceAtMost(5).toInt()).coerceAtMost(30_000L))
            true
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val state: StateFlow<DashboardUiState> = combine(snapshot, refreshing, refreshError, readError) { data, loading, syncMessage, localMessage ->
        DashboardUiState(data, clock.instant(), loading, localMessage ?: syncMessage)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState(now = clock.instant()))

    init { refresh() }

    fun refresh() {
        if (!refreshing.compareAndSet(false, true)) return
        viewModelScope.launch {
            refreshError.value = null
            try { repository.refresh(studentId) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { refreshError.value = "Odświeżanie nie powiodło się. Wyświetlam zapisane dane." }
            finally { refreshing.value = false }
        }
    }
}

class DashboardViewModelFactory @Inject constructor(private val repository: SchoolRepository, private val clock: Clock) {
    fun create(studentId: String) = DashboardViewModel(repository, clock, studentId)
}
