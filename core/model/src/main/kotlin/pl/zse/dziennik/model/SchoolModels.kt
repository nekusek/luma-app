package pl.zse.dziennik.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

val SchoolZone: ZoneId = ZoneId.of("Europe/Warsaw")
enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

data class Student(val id: String, val name: String, val className: String, val school: String)

/** Numeric value is supplied by a verified mapper; '+' and '-' are not guessed. */
data class Grade(
    val id: String,
    val studentId: String,
    val subjectId: String,
    val subjectName: String,
    val displayValue: String,
    val numericValue: Double?,
    val weight: Int,
    val category: String,
    val description: String,
    val receivedOn: LocalDate,
    val countsTowardsAverage: Boolean = true,
) {
    init {
        require(weight >= 0)
        require(numericValue == null || numericValue.isFinite() && numericValue in 1.0..6.0)
    }
}

fun Iterable<Grade>.weightedAverage(): Double? {
    val eligible = filter { it.countsTowardsAverage && it.numericValue != null && it.weight > 0 }
    val totalWeight = eligible.sumOf { it.weight.toDouble() }
    return if (totalWeight == 0.0) null else eligible.sumOf { it.numericValue!! * it.weight } / totalWeight
}

enum class LessonStatus { SCHEDULED, SUBSTITUTION, CANCELLED, WINDOW }

data class TimetableEntry(
    val id: String,
    val studentId: String,
    val number: Int,
    val subjectName: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val room: String?,
    val teacherName: String?,
    val group: String?,
    val status: LessonStatus,
    val changeDescription: String? = null,
) {
    init { require(endsAt > startsAt); require(number > 0) }
}

fun Iterable<TimetableEntry>.currentOrNextLesson(now: Instant): TimetableEntry? =
    filter { it.status != LessonStatus.CANCELLED && it.status != LessonStatus.WINDOW && it.endsAt > now }
        .minByOrNull { it.startsAt }

enum class AttendanceStatus { PRESENT, LATE, ABSENT, EXCUSED_ABSENCE, RELEASED }
data class Attendance(
    val id: String, val studentId: String, val subjectId: String,
    val date: LocalDate, val status: AttendanceStatus,
)

/** Demo rule: late counts as present, release is excluded; verify school policy for live data. */
fun Iterable<Attendance>.attendancePercent(): Double? {
    val eligible = filter { it.status != AttendanceStatus.RELEASED }
    return eligible.takeIf { it.isNotEmpty() }?.let {
        100.0 * it.count { a -> a.status == AttendanceStatus.PRESENT || a.status == AttendanceStatus.LATE } / it.size
    }
}

data class Message(val id: String, val studentId: String, val sender: String, val subject: String, val sentAt: Instant, val read: Boolean)
data class Assessment(val id: String, val subject: String, val title: String, val date: LocalDate)
data class SchoolSnapshot(
    val student: Student,
    val grades: List<Grade>,
    val timetable: List<TimetableEntry>,
    val attendance: List<Attendance>,
    val assessments: List<Assessment>,
    val syncedAt: Instant,
)

interface SchoolRepository {
    fun observe(studentId: String): Flow<SchoolSnapshot?>
    suspend fun refresh(studentId: String)
}

/** A successful response must be a complete snapshot for one student. Partial responses must not be passed here. */
interface SnapshotSource { suspend fun fetch(studentId: String): SchoolSnapshot }
