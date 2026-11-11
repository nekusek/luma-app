package pl.zse.dziennik.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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

@HiltViewModel
class DashboardViewModel @Inject constructor(private val repository: SchoolRepository, private val clock: Clock) : ViewModel() {
    private val studentId = "demo-zse-3ti" // Replaced by selected account after production onboarding.
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val ticker = flow {
        while (currentCoroutineContext().isActive) { emit(clock.instant()); delay(1_000) }
    }
    val state: StateFlow<DashboardUiState> = combine(
        repository.observe(studentId).catch { e ->
            if (e is CancellationException) throw e
            error.value = "Nie można odczytać danych lokalnych."
            emit(null)
        }, ticker, refreshing, error,
    ) { snapshot, now, loading, message -> DashboardUiState(snapshot, now, loading, message) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState(now = clock.instant()))

    init { refresh() }

    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            error.value = null
            try { repository.refresh(studentId) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error.value = "Odświeżanie nie powiodło się. Wyświetlam zapisane dane." }
            finally { refreshing.value = false }
        }
    }
}
