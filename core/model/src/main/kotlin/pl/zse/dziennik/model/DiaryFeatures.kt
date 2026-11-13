package pl.zse.dziennik.model

import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class SchoolPeriod(val id: String, val schoolYear: Int, val number: Int, val startsOn: LocalDate, val endsOn: LocalDate)
enum class SchoolTaskKind { EXAM, HOMEWORK }
data class SchoolTask(val id: String, val kind: SchoolTaskKind, val subject: String, val title: String, val description: String, val dueOn: LocalDate, val teacher: String, val attachmentName: String? = null, val attachmentContent: String? = null)
data class SchoolNote(val id: String, val positive: Boolean?, val category: String, val body: String, val points: Int?, val date: LocalDate, val teacher: String)
data class ClassificationGrade(val id: String, val subject: String, val schoolYear: Int, val predicted: String?, val midyear: String?, val annual: String?)
enum class JustificationStatus { PENDING, ACCEPTED, REJECTED }
data class AbsenceJustification(val id: String, val studentId: String, val requesterId: String, val attendanceIds: List<String>, val reason: String, val status: JustificationStatus, val createdAt: Instant, val decidedBy: String?)
data class DiaryFeatures(
    val periods: List<SchoolPeriod> = emptyList(),
    val tasks: List<SchoolTask> = emptyList(),
    val notes: List<SchoolNote> = emptyList(),
    val classification: List<ClassificationGrade> = emptyList(),
    val luckyNumber: Int? = null,
    val luckyDate: LocalDate? = null,
    val pupilNumber: Int? = null,
    val completedHomework: Set<String> = emptySet(),
    val justifications: List<AbsenceJustification> = emptyList(),
)

interface DiaryFeaturesSource { suspend fun fetch(studentId: String): DiaryFeatures }
interface DiaryFeaturesRepository {
    fun observe(studentId: String): Flow<DiaryFeatures>
    fun pendingJustifications(teacherId: String): Flow<List<AbsenceJustification>>
    suspend fun refresh(studentId: String)
    suspend fun setHomeworkDone(studentId: String, taskId: String, done: Boolean)
    suspend fun requestJustification(accountId: String, attendanceIds: List<String>, reason: String)
    suspend fun decideJustification(teacherId: String, requestId: String, accept: Boolean)
}

data class GradePolicy(val useModifiers: Boolean = false, val plus: Double = 0.5, val minus: Double = 0.25) {
    init { require(plus in 0.0..1.0 && minus in 0.0..1.0) }
}
fun Iterable<Grade>.weightedAverage(policy: GradePolicy): Double? {
    val values = filter { it.countsTowardsAverage && it.weight > 0 }.mapNotNull { grade ->
        val raw = grade.displayValue.trim()
        val value = if (policy.useModifiers && raw.matches(Regex("[1-6][+-]"))) raw.take(1).toDouble() + if (raw.endsWith('+')) policy.plus else -policy.minus else grade.numericValue
        value?.takeIf { it.isFinite() && it in 1.0..6.0 }?.let { it to grade.weight }
    }
    val weight = values.sumOf { it.second.toDouble() }
    return if (weight == 0.0) null else values.sumOf { it.first * it.second } / weight
}

data class DiaryOptions(val gradePolicy: GradePolicy = GradePolicy(), val readReceipts: Boolean = true, val syncMinutes: Int = 0, val wifiOnly: Boolean = true)
