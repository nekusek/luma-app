package pl.zse.dziennik.data

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
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
class DiaryFeaturesRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private val clock = Clock.fixed(Instant.parse("2026-11-13T07:15:00Z"), SchoolZone)
    private val anna = DemoProfiles.accounts[0]
    private val mateusz = DemoProfiles.accounts[1]
    private val parent get() = DemoProfiles.accounts.first { it.role == AccountRole.PARENT }
    private val teacher get() = DemoProfiles.accounts.first { it.role == AccountRole.TEACHER }
    private lateinit var db: WorkspaceDatabase
    private lateinit var schoolDb: SchoolDatabase
    private lateinit var school: SchoolRepository
    private lateinit var workspace: LocalWorkspaceRepository
    private lateinit var features: LocalDiaryFeaturesRepository
    @Before fun setup() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.databaseBuilder(context, WorkspaceDatabase::class.java, folder.root.resolve("workspace.db").absolutePath).allowMainThreadQueries().build()
        schoolDb = Room.inMemoryDatabaseBuilder(context, SchoolDatabase::class.java).allowMainThreadQueries().build()
        workspace = LocalWorkspaceRepository(db, clock)
        school = DemoSchoolRepository(CachedSchoolRepository(schoolDb.schoolDao(), MockDataRepository(clock)), workspace, db.featuresDao())
        features = LocalDiaryFeaturesRepository(db, MockDiaryFeaturesSource(clock), school, workspace, clock)
    }
    @After fun cleanup() { db.close(); schoolDb.close() }

    @Test fun homeworkCompletionSurvivesRefreshAndIsIsolatedByStudent() = runBlocking {
        features.refresh(anna.studentId); features.refresh(mateusz.studentId)
        val task = features.observe(anna.studentId).first().tasks.first { it.kind == SchoolTaskKind.HOMEWORK }
        features.setHomeworkDone(anna.studentId, task.id, true)
        features.refresh(anna.studentId)
        assertTrue(task.id in features.observe(anna.studentId).first().completedHomework)
        assertFalse(task.id in features.observe(mateusz.studentId).first().completedHomework)
        assertTrue(runCatching { features.setHomeworkDone(anna.studentId, "e1", true) }.isFailure)
        features.setHomeworkDone(anna.studentId, task.id, false)
        assertFalse(task.id in features.observe(anna.studentId).first().completedHomework)
    }
    @Test fun justificationNeedsParentAndTeacherApprovalBeforeAttendanceChanges() = runBlocking {
        school.refresh(anna.studentId)
        val absence = school.observe(anna.studentId).first()!!.attendance.first { it.status == AttendanceStatus.ABSENT }
        assertTrue(runCatching { features.requestJustification(anna.id, listOf(absence.id), "Wizyta lekarska") }.isFailure)
        features.requestJustification(parent.id, listOf(absence.id), "Wizyta lekarska")
        val request = features.pendingJustifications(teacher.id).first().single()
        assertEquals(AttendanceStatus.ABSENT, school.observe(anna.studentId).first()!!.attendance.first { it.id == absence.id }.status)
        assertTrue(runCatching { features.requestJustification(parent.id, listOf(absence.id), "Powtórzony wniosek") }.isFailure)
        assertTrue(runCatching { features.decideJustification(anna.id, request.id, true) }.isFailure)
        features.decideJustification(teacher.id, request.id, true)
        school.refresh(anna.studentId)
        assertEquals(AttendanceStatus.EXCUSED_ABSENCE, school.observe(anna.studentId).first()!!.attendance.first { it.id == absence.id }.status)
        assertEquals(JustificationStatus.ACCEPTED, features.observe(anna.studentId).first().justifications.single().status)
        assertTrue(runCatching { features.decideJustification(teacher.id, request.id, false) }.isFailure)
    }
    @Test fun crossStudentAndPresentLessonRequestsAreRejectedAndRejectedRequestsCanBeResubmitted() = runBlocking {
        school.refresh(anna.studentId); school.refresh(mateusz.studentId)
        val present = school.observe(anna.studentId).first()!!.attendance.first { it.status == AttendanceStatus.PRESENT }
        assertTrue(runCatching { features.requestJustification(parent.id, listOf(present.id), "Błędny wniosek") }.isFailure)
        val absence = school.observe(anna.studentId).first()!!.attendance.first { it.status == AttendanceStatus.ABSENT }
        features.requestJustification(parent.id, listOf(absence.id), "Nieobecność rodzinna")
        features.decideJustification(teacher.id, features.pendingJustifications(teacher.id).first().single().id, false)
        features.requestJustification(parent.id, listOf(absence.id), "Dodatkowe wyjaśnienie")
        assertEquals(2, features.observe(anna.studentId).first().justifications.size)
        assertTrue(features.observe(mateusz.studentId).first().justifications.isEmpty())
    }
    @Test fun teacherNotesAndHomeworkReachOnlyTheMatchingStudent() = runBlocking {
        workspace.saveEntry(teacher.id, anna.studentId, anna.className, TeachingKind.POSITIVE_NOTE, "Świetny projekt", "", 1, java.time.LocalDate.now(clock))
        assertTrue(features.observe(anna.studentId).first().notes.any { it.body == "Świetny projekt" && it.positive == true })
        assertFalse(features.observe(mateusz.studentId).first().notes.any { it.body == "Świetny projekt" })
        workspace.saveEntry(teacher.id, null, anna.className, TeachingKind.HOMEWORK, "Zadanie nauczyciela", "", 1, java.time.LocalDate.now(clock).plusDays(1))
        val task = features.observe(anna.studentId).first().tasks.single { it.title == "Zadanie nauczyciela" }
        features.setHomeworkDone(anna.studentId, task.id, true)
        features.refresh(anna.studentId)
        assertTrue(task.id in features.observe(anna.studentId).first().completedHomework)
        assertFalse(features.observe(mateusz.studentId).first().tasks.any { it.title == "Zadanie nauczyciela" })
    }
    @Test fun versionOneWorkspaceMigratesWithoutLosingMessages() = runBlocking {
        val path = folder.root.resolve("old-workspace.db")
        val json = javaClass.classLoader!!.getResourceAsStream("pl.zse.dziennik.database.WorkspaceDatabase/1.json")!!.bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject["database"]!!.jsonObject }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            json["entities"]!!.jsonArray.forEach { entity ->
                val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
                old.execSQL(entity.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\u0024{TABLE_NAME}", table))
                (entity.jsonObject["indices"]?.jsonArray ?: JsonArray(emptyList())).forEach { index -> old.execSQL(index.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\u0024{TABLE_NAME}", table)) }
            }
            json["setupQueries"]!!.jsonArray.forEach { old.execSQL(it.jsonPrimitive.content) }
            old.execSQL("INSERT INTO messages VALUES ('saved','demo:teacher','demo:anna','Nie usuń','Treść',1,0)")
            old.version = 1
        }
        val migrated = Room.databaseBuilder(RuntimeEnvironment.getApplication(), WorkspaceDatabase::class.java, path.absolutePath).allowMainThreadQueries().build()
        try { assertEquals("Nie usuń", migrated.workspaceDao().messages(anna.id).first().single().subject); migrated.featuresDao().upsertInfo(PortalInfoEntity(anna.studentId, 17, null, 17)) }
        finally { migrated.close() }
    }
}
