package pl.zse.dziennik.data

import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.*
import pl.zse.dziennik.database.*
import pl.zse.dziennik.model.*

class LocalWorkspaceRepository(private val database: WorkspaceDatabase, private val clock: Clock) : WorkspaceRepository {
    private val dao = database.workspaceDao()
    override val contacts get() = DemoProfiles.accounts
    private fun account(id: String) = requireNotNull(contacts.firstOrNull { it.id == id })
    private suspend fun seed() = database.withTransaction {
        if (dao.seeded() == 0) {
            val teacher = contacts.first { it.role == AccountRole.TEACHER }
            contacts.filter { it.role == AccountRole.STUDENT }.forEachIndexed { i, student ->
                dao.insertMessage(MessageEntity("welcome:${student.id}", teacher.id, student.id, "Pracownia programowania", "Cześć! Na następne zajęcia przygotuj projekt aplikacji i krótką listę pytań. To przykładowa wiadomość demo.", clock.instant().minusSeconds(3600L + i).toEpochMilli(), false))
            }
            dao.insertMessage(MessageEntity("question:teacher", contacts.first { it.role == AccountRole.STUDENT }.id, teacher.id, "Pytanie o projekt", "Dzień dobry, czy na kolejnych zajęciach możemy omówić obsługę bazy danych? Wiadomość demo.", clock.instant().minusSeconds(1800).toEpochMilli(), false))
            dao.seedMarker(WorkspaceMeta("seed_v1"))
        }
    }
    override fun messages(accountId: String): Flow<List<DiaryMessage>> = flow {
        account(accountId)
        seed()
        emitAll(dao.messages(accountId).map { rows -> rows.map { DiaryMessage(it.id, it.senderId, it.recipientId, it.subject, it.body, Instant.ofEpochMilli(it.sentAt), it.read) } })
    }
    override fun teachingEntries(teacherId: String): Flow<List<TeachingEntry>> {
        require(account(teacherId).role == AccountRole.TEACHER)
        return dao.teacherEntries(teacherId).map { it.map(TeachingEntity::toModel) }
    }
    override fun studentEntries(studentId: String) = dao.studentEntries(studentId, contacts.firstOrNull { it.studentId == studentId }?.className.orEmpty()).map { it.map(TeachingEntity::toModel) }
    override suspend fun send(senderId: String, recipientId: String, subject: String, body: String) {
        val sender = account(senderId)
        val recipient = account(recipientId)
        require(senderId != recipientId && (sender.role == AccountRole.TEACHER || recipient.role == AccountRole.TEACHER))
        require(subject.trim().length in 1..120 && body.trim().length in 1..5000)
        seed()
        dao.insertMessage(MessageEntity(UUID.randomUUID().toString(), senderId, recipientId, subject.trim(), body.trim(), clock.instant().toEpochMilli(), false))
    }
    override suspend fun markRead(accountId: String, messageId: String) {
        account(accountId)
        dao.markRead(accountId, messageId)
    }
    override suspend fun saveEntry(teacherId: String, studentId: String?, className: String, kind: TeachingKind, value: String, description: String, weight: Int, date: LocalDate) {
        require(account(teacherId).role == AccountRole.TEACHER)
        val students = contacts.filter { it.role == AccountRole.STUDENT && it.className == className }
        require(students.isNotEmpty() && value.trim().length in 1..1000 && description.length <= 1000)
        if (kind == TeachingKind.GRADE || kind == TeachingKind.ATTENDANCE) require(students.any { it.studentId == studentId })
        else require(studentId == null)
        if (kind == TeachingKind.GRADE) require(value.toIntOrNull() in 1..6 && weight in 1..5)
        if (kind == TeachingKind.ATTENDANCE) AttendanceStatus.valueOf(value)
        if (kind == TeachingKind.HOMEWORK) require(date >= LocalDate.now(clock.withZone(SchoolZone)))
        database.withTransaction {
            if (kind == TeachingKind.ATTENDANCE) dao.clearAttendance(teacherId, requireNotNull(studentId), date.toEpochDay())
            dao.insertEntry(TeachingEntity(UUID.randomUUID().toString(), teacherId, studentId, className, kind.name, value.trim(), description.trim(), weight, date.toEpochDay()))
        }
    }
}

private fun TeachingEntity.toModel() = TeachingEntry(id, teacherId, studentId, className, TeachingKind.valueOf(kind), value, description, weight, LocalDate.ofEpochDay(date))

class DemoSchoolRepository(private val cached: SchoolRepository, private val workspace: WorkspaceRepository) : SchoolRepository {
    override fun observe(studentId: String): Flow<SchoolSnapshot?> = combine(cached.observe(studentId), workspace.studentEntries(studentId)) { snapshot, entries ->
        snapshot?.copy(
            grades = snapshot.grades + entries.filter { it.kind == TeachingKind.GRADE }.map { Grade("teacher:${it.id}", studentId, "programming", "Programowanie", it.value, it.value.toDouble(), it.weight, "Ocena nauczyciela demo", it.description, it.date) },
            assessments = snapshot.assessments + entries.filter { it.kind == TeachingKind.HOMEWORK }.map { Assessment("teacher:${it.id}", "Programowanie · zadanie demo", it.value, it.date) },
            attendance = snapshot.attendance + entries.filter { it.kind == TeachingKind.ATTENDANCE }.map { Attendance("teacher:${it.id}", studentId, "programming", it.date, AttendanceStatus.valueOf(it.value)) },
        )
    }
    override suspend fun refresh(studentId: String) = cached.refresh(studentId)
}
