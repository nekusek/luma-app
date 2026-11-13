package pl.zse.dziennik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.zse.dziennik.data.DiaryPreferences
import pl.zse.dziennik.model.*

data class FeaturesUiState(val data: DiaryFeatures = DiaryFeatures(), val pending: List<AbsenceJustification> = emptyList(), val options: DiaryOptions = DiaryOptions(), val busy: Boolean = false, val error: String? = null, val notice: String? = null)
class DiaryFeaturesViewModel(private val repository: DiaryFeaturesRepository, private val school: SchoolRepository, private val preferences: DiaryPreferences, val account: DiaryAccount, clock: Clock) : ViewModel() {
    val today: LocalDate = LocalDate.now(clock.withZone(SchoolZone))
    private val retry = MutableStateFlow(0)
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val notice = MutableStateFlow<String?>(null)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val content = retry.flatMapLatest {
        (if (account.role == AccountRole.TEACHER) repository.pendingJustifications(account.id).map { FeaturesUiState(pending = it) } else repository.observe(account.studentId).map { FeaturesUiState(data = it) })
            .catch { cause -> if (cause is CancellationException) throw cause; error.value = "Nie udało się odczytać danych. Spróbuj ponownie."; emit(FeaturesUiState()) }
    }
    val state = combine(content, preferences.options, busy, error, notice) { content, options, busy, error, notice -> content.copy(options = options, busy = busy, error = error, notice = notice) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FeaturesUiState())
    init { if (account.role != AccountRole.TEACHER) refresh() }
    fun refresh() = operation(null) { if (account.role != AccountRole.TEACHER) repository.refresh(account.studentId); retry.value++ }
    fun refreshAll() = operation(null) { if (account.role != AccountRole.TEACHER) { school.refresh(account.studentId); repository.refresh(account.studentId) }; retry.value++ }
    fun dismissNotice() { notice.value = null }
    fun homework(id: String, done: Boolean) = operation(null) { repository.setHomeworkDone(account.studentId, id, done) }
    fun justify(ids: List<String>, reason: String, onSuccess: () -> Unit) = operation("Wniosek złożony lokalnie. Czeka na decyzję nauczyciela demo.") { repository.requestJustification(account.id, ids, reason); onSuccess() }
    fun decide(id: String, accept: Boolean) = operation("Zapisano decyzję w demo.") { repository.decideJustification(account.id, id, accept) }
    fun gradePolicy(policy: GradePolicy) = operation(null) { preferences.setGradePolicy(policy) }
    fun readReceipts(value: Boolean) = operation(null) { preferences.setReadReceipts(value) }
    fun sync(minutes: Int, wifi: Boolean) = operation(null) { preferences.setSync(minutes, wifi) }
    private fun operation(message: String?, block: suspend () -> Unit) {
        if (!busy.compareAndSet(false, true)) return
        viewModelScope.launch {
            error.value = null
            try { block(); notice.value = message }
            catch (cause: CancellationException) { throw cause }
            catch (_: Exception) { error.value = "Nie udało się zapisać lub odświeżyć danych. Sprawdź formularz i spróbuj ponownie." }
            finally { busy.value = false }
        }
    }
}
class DiaryFeaturesViewModelFactory @Inject constructor(private val repository: DiaryFeaturesRepository, private val school: SchoolRepository, private val preferences: DiaryPreferences, private val clock: Clock) { fun create(account: DiaryAccount) = DiaryFeaturesViewModel(repository, school, preferences, account, clock) }
