package pl.zse.dziennik.data

import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.zse.dziennik.database.*
import pl.zse.dziennik.model.*

class LocalDiaryFeaturesRepository(private val database: WorkspaceDatabase, private val source: DiaryFeaturesSource, private val cached: SchoolRepository, private val workspace: WorkspaceRepository, private val clock: Clock) : DiaryFeaturesRepository {
    private val dao = database.featuresDao()
    private fun pupil(studentId: String) = requireNotNull(DemoProfiles.accounts.firstOrNull { it.studentId == studentId && it.role == AccountRole.STUDENT })
    private fun teacher(id: String) { require(DemoProfiles.accounts.any { it.id == id && it.role == AccountRole.TEACHER }) }
    override fun observe(studentId: String): Flow<DiaryFeatures> {
        pupil(studentId)
        return combine(dao.observe(studentId), dao.completed(studentId), dao.justifications(studentId), workspace.studentEntries(studentId)) { stored, done, requests, entries ->
            val base = stored?.toModel() ?: DiaryFeatures()
            base.copy(
                tasks = base.tasks + entries.filter { it.kind == TeachingKind.HOMEWORK }.map { SchoolTask("teacher:${it.id}", SchoolTaskKind.HOMEWORK, "Programowanie", it.value, it.description, it.date, "Piotr Wiśniewski") },
                notes = base.notes + entries.filter { it.kind == TeachingKind.POSITIVE_NOTE || it.kind == TeachingKind.NEGATIVE_NOTE }.map { SchoolNote("teacher:${it.id}", it.kind == TeachingKind.POSITIVE_NOTE, "Wpis nauczyciela demo", it.value, null, it.date, "Piotr Wiśniewski") },
                completedHomework = done.map { it.taskId }.toSet(), justifications = requests.map { it.toModel() },
            )
        }
    }
    override fun pendingJustifications(teacherId: String): Flow<List<AbsenceJustification>> {
        teacher(teacherId)
        return dao.pending().map { it.map { entry -> entry.toModel() } }
    }
    override suspend fun refresh(studentId: String) {
        pupil(studentId)
        val data = source.fetch(studentId)
        database.withTransaction {
            dao.upsertInfo(PortalInfoEntity(studentId, data.luckyNumber, data.luckyDate?.toEpochDay(), data.pupilNumber))
            dao.clearPeriods(studentId); dao.clearTasks(studentId); dao.clearNotes(studentId); dao.clearClassification(studentId)
            dao.insertPeriods(data.periods.map { PeriodEntity(studentId, it.id, it.schoolYear, it.number, it.startsOn.toEpochDay(), it.endsOn.toEpochDay()) })
            dao.insertTasks(data.tasks.map { SchoolTaskEntity(studentId, it.id, it.kind.name, it.subject, it.title, it.description, it.dueOn.toEpochDay(), it.teacher, it.attachmentName, it.attachmentContent) })
            dao.insertNotes(data.notes.map { SchoolNoteEntity(studentId, it.id, it.positive, it.category, it.body, it.points, it.date.toEpochDay(), it.teacher) })
            dao.insertClassification(data.classification.map { ClassificationEntity(studentId, it.id, it.subject, it.schoolYear, it.predicted, it.midyear, it.annual) })
        }
    }
    override suspend fun setHomeworkDone(studentId: String, taskId: String, done: Boolean) {
        require(observe(studentId).first().tasks.any { it.id == taskId && it.kind == SchoolTaskKind.HOMEWORK })
        if (done) dao.complete(HomeworkDoneEntity(studentId, taskId)) else dao.uncomplete(studentId, taskId)
    }
    override suspend fun requestJustification(accountId: String, attendanceIds: List<String>, reason: String) {
        val parent = requireNotNull(DemoProfiles.accounts.firstOrNull { it.id == accountId && it.role == AccountRole.PARENT })
        require(reason.trim().length in 5..1000 && attendanceIds.isNotEmpty() && attendanceIds.size <= 50 && attendanceIds.distinct().size == attendanceIds.size)
        val today = LocalDate.now(clock.withZone(SchoolZone))
        val attendance = cached.observe(parent.studentId).first()?.attendance.orEmpty()
        require(attendanceIds.all { id -> attendance.any { it.id == id && it.studentId == parent.studentId && it.status == AttendanceStatus.ABSENT && it.date <= today } })
        database.withTransaction {
            val existing = dao.requests(parent.studentId).filter { it.status != JustificationStatus.REJECTED.name }.flatMap { Json.decodeFromString<List<String>>(it.attendanceIds) }
            require(attendanceIds.none { it in existing })
            dao.insertJustification(JustificationEntity(UUID.randomUUID().toString(), parent.studentId, accountId, Json.encodeToString(attendanceIds), reason.trim(), JustificationStatus.PENDING.name, clock.instant().toEpochMilli(), null))
        }
    }
    override suspend fun decideJustification(teacherId: String, requestId: String, accept: Boolean) {
        teacher(teacherId)
        database.withTransaction {
            val request = requireNotNull(dao.justification(requestId))
            pupil(request.studentId)
            check(dao.decide(requestId, if (accept) JustificationStatus.ACCEPTED.name else JustificationStatus.REJECTED.name, teacherId) == 1)
        }
    }
}

private fun StoredDiaryFeatures.toModel() = DiaryFeatures(
    periods = periods.map { SchoolPeriod(it.id, it.schoolYear, it.number, LocalDate.ofEpochDay(it.startsOn), LocalDate.ofEpochDay(it.endsOn)) },
    tasks = tasks.map { SchoolTask(it.id, SchoolTaskKind.valueOf(it.kind), it.subject, it.title, it.description, LocalDate.ofEpochDay(it.dueOn), it.teacher, it.attachmentName, it.attachmentContent) },
    notes = notes.map { SchoolNote(it.id, it.positive, it.category, it.body, it.points, LocalDate.ofEpochDay(it.date), it.teacher) },
    classification = classification.map { ClassificationGrade(it.id, it.subject, it.schoolYear, it.predicted, it.midyear, it.annual) },
    luckyNumber = info.luckyNumber, luckyDate = info.luckyDate?.let(LocalDate::ofEpochDay), pupilNumber = info.pupilNumber,
)
private fun JustificationEntity.toModel() = AbsenceJustification(id, studentId, requesterId, Json.decodeFromString(attendanceIds), reason, JustificationStatus.valueOf(status), Instant.ofEpochMilli(createdAt), decidedBy)
