package pl.zse.dziennik.data

import java.time.Clock
import java.time.LocalDate
import pl.zse.dziennik.model.*


class MockDataRepository(private val clock: Clock) : SnapshotSource {
    override suspend fun fetch(studentId: String): SchoolSnapshot {
        val account = requireNotNull(DemoProfiles.accounts.firstOrNull { it.studentId == studentId })
        require(account.role == AccountRole.STUDENT)
        val today = LocalDate.now(clock.withZone(SchoolZone))
        fun lesson(id: String, number: Int, subject: String, hour: Int, minute: Int, room: String?, status: LessonStatus = LessonStatus.SCHEDULED, group: String? = null): TimetableEntry {
            val start = today.atTime(hour, minute).atZone(SchoolZone).toInstant()
            return TimetableEntry(id, studentId, number, subject, start, start.plusSeconds(45 * 60), room, "Nauczyciel demo", if (studentId == DEMO_STUDENT_ID) group else group?.replace("1/2", "2/2"), status,
                if (status == LessonStatus.SUBSTITUTION) "Zastępstwo · zmiana sali" else null)
        }
        return SchoolSnapshot(
            student = Student(studentId, account.displayName, account.className, account.schoolName),
            grades = listOf(
                Grade("g1", studentId, "programming", "Programowanie", if (studentId == DEMO_STUDENT_ID) "5" else "3", if (studentId == DEMO_STUDENT_ID) 5.0 else 3.0, 3, "Projekt", "Aplikacja w Kotlinie", today),
                Grade("g2", studentId, "math", "Matematyka", "4", 4.0, 2, "Kartkówka", "Funkcje", today.minusDays(1)),
                Grade("g3", studentId, "electronics", "Elektronika", "6", 6.0, 2, "Laboratorium", "Pomiary układów", today.minusDays(2)),
                Grade("g4", studentId, "programming", "Programowanie", "4", 4.0, 1, "Odpowiedź", "Kolekcje", today.minusDays(3)),
                Grade("g5", studentId, "math", "Matematyka", "np", null, 0, "Nieprzygotowanie", "", today.minusDays(4), false),
            ),
            timetable = listOf(
                lesson("t1", 1, "Matematyka", 7, 10, "204"),
                lesson("t2", 2, "Programowanie", 8, 0, "L12", group = "1/2"),
                lesson("t3", 3, "Programowanie", 8, 55, "L12", group = "1/2"),
                lesson("t4", 4, "Okienko", 9, 50, null, LessonStatus.WINDOW),
                lesson("t5", 5, "Elektronika", 10, 45, "L08", LessonStatus.SUBSTITUTION, "1/2"),
                lesson("t6", 6, "Język angielski", 11, 40, "103", LessonStatus.CANCELLED),
                lesson("t7", 7, "Systemy operacyjne", 12, 35, "L14", group = "1/2"),
            ),
            attendance = (1..30).map { Attendance("a$it", studentId, "programming", today.minusDays(it.toLong()), when (it) { 5 -> AttendanceStatus.LATE; 10 -> AttendanceStatus.ABSENT; 20 -> AttendanceStatus.EXCUSED_ABSENCE; else -> AttendanceStatus.PRESENT }) },
            assessments = listOf(Assessment("e1", "Systemy operacyjne", "Procesy i wątki", today.plusDays(2))),
            syncedAt = clock.instant(),
        )
    }
    companion object { const val DEMO_STUDENT_ID = "demo-zse-3ti" }
}
