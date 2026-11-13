package pl.zse.dziennik.data

import android.app.Application
import androidx.room.Room
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import pl.zse.dziennik.database.*
import pl.zse.dziennik.model.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class WorkspaceRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private val clock = Clock.fixed(Instant.parse("2026-11-13T07:15:00Z"), SchoolZone)
    private val today = LocalDate.of(2026, 11, 13)
    private val anna = DemoProfiles.accounts[0]
    private val mateusz = DemoProfiles.accounts[1]
    private val teacher get() = DemoProfiles.accounts.first { it.role == AccountRole.TEACHER }
    private lateinit var database: WorkspaceDatabase
    private lateinit var repository: LocalWorkspaceRepository
    private fun open() {
        database = Room.databaseBuilder(RuntimeEnvironment.getApplication(), WorkspaceDatabase::class.java, folder.root.resolve("workspace.db").absolutePath).allowMainThreadQueries().build()
        repository = LocalWorkspaceRepository(database, clock)
    }
    @Before fun setup() = open()
    @After fun cleanup() = database.close()

    @Test fun deliveryReadStatusAndRepliesAreScopedToTheRightProfiles() = runBlocking {
        repository.send(anna.id, teacher.id, "Projekt", "Czy mogę oddać projekt jutro?")
        val message = repository.messages(teacher.id).first().first { it.subject == "Projekt" }
        assertFalse(message.read)
        assertTrue(repository.messages(mateusz.id).first().none { it.id == message.id })
        repository.markRead(mateusz.id, message.id)
        assertFalse(repository.messages(teacher.id).first().first { it.id == message.id }.read)
        repository.markRead(teacher.id, message.id)
        assertTrue(repository.messages(teacher.id).first().first { it.id == message.id }.read)
        repository.send(teacher.id, anna.id, "Re: Projekt", "Tak, do jutra.")
        assertEquals("Tak, do jutra.", repository.messages(anna.id).first().first { it.subject == "Re: Projekt" }.body)
    }

    @Test fun invalidMessagesAndStudentJournalWritesAreRejected() = runBlocking {
        suspend fun rejected(block: suspend () -> Unit) { assertTrue(runCatching { block() }.exceptionOrNull() is IllegalArgumentException) }
        rejected { repository.send(anna.id, mateusz.id, "Cześć", "Test") }
        rejected { repository.send(anna.id, anna.id, "Cześć", "Test") }
        rejected { repository.send(anna.id, teacher.id, " ", "Test") }
        rejected { repository.send(anna.id, teacher.id, "Temat", "x".repeat(5001)) }
        rejected { repository.saveEntry(anna.id, anna.studentId, anna.className, TeachingKind.GRADE, "6", "Test", 1, today) }
        rejected { repository.saveEntry(teacher.id, mateusz.studentId, anna.className, TeachingKind.GRADE, "6", "Test", 1, today) }
        rejected { repository.saveEntry(teacher.id, anna.studentId, anna.className, TeachingKind.GRADE, "7", "Test", 1, today) }
        rejected { repository.saveEntry(teacher.id, anna.studentId, anna.className, TeachingKind.GRADE, "5", "Test", 0, today) }
        assertTrue(repository.teachingEntries(teacher.id).first().isEmpty())
    }

    @Test fun teacherEntriesReachTheStudentAndSurviveSnapshotRefresh() = runBlocking {
        val school = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), SchoolDatabase::class.java).allowMainThreadQueries().build()
        try {
            val cache = CachedSchoolRepository(school.schoolDao(), MockDataRepository(clock))
            val combined = DemoSchoolRepository(cache, repository)
            repository.saveEntry(teacher.id, anna.studentId, anna.className, TeachingKind.GRADE, "6", "Projekt Room", 2, today)
            repository.saveEntry(teacher.id, anna.studentId, anna.className, TeachingKind.ATTENDANCE, AttendanceStatus.ABSENT.name, "", 1, today)
            repository.saveEntry(teacher.id, anna.studentId, anna.className, TeachingKind.ATTENDANCE, AttendanceStatus.PRESENT.name, "", 1, today)
            repository.saveEntry(teacher.id, null, anna.className, TeachingKind.HOMEWORK, "Dokończ projekt", "", 1, today.plusDays(2))
            cache.refresh(anna.studentId)
            cache.refresh(mateusz.studentId)
            cache.refresh(anna.studentId)
            val snapshot = combined.observe(anna.studentId).first()!!
            assertEquals(1, snapshot.grades.count { it.description == "Projekt Room" })
            assertEquals(1, snapshot.attendance.count { it.date == today })
            assertEquals(AttendanceStatus.PRESENT, snapshot.attendance.first { it.date == today }.status)
            assertFalse(snapshot.assessments.any { it.title == "Dokończ projekt" })
            val other = combined.observe(mateusz.studentId).first()!!
            assertFalse(other.grades.any { it.description == "Projekt Room" })
            assertFalse(other.assessments.any { it.title == "Dokończ projekt" })
        } finally { school.close() }
    }

    @Test fun messagesAndTeacherNotesSurviveDatabaseReopening() = runBlocking {
        repository.send(teacher.id, anna.id, "Trwała wiadomość", "Zapis do Room")
        repository.saveEntry(teacher.id, null, anna.className, TeachingKind.TOPIC, "Coroutines", "", 1, today)
        database.close()
        open()
        assertTrue(repository.messages(anna.id).first().any { it.subject == "Trwała wiadomość" })
        assertEquals("Coroutines", repository.teachingEntries(teacher.id).first().single().value)
    }

    @Test fun concurrentMailboxInitializationDoesNotDuplicateDemoMessages() = runBlocking {
        coroutineScope { (1..8).map { async(Dispatchers.IO) { repository.messages(anna.id).first() } }.awaitAll() }
        assertEquals(1, repository.messages(anna.id).first().count { it.id == "welcome:${anna.id}" })
    }
}
