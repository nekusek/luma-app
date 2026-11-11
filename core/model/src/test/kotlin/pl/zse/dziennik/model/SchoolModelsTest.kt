package pl.zse.dziennik.model

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SchoolModelsTest {
    private fun grade(value: Double?, weight: Int, counts: Boolean = true) = Grade("g", "s", "math", "Matematyka", value?.toString() ?: "np", value, weight, "Test", "", LocalDate.of(2026, 11, 12), counts)

    @Test fun `weighted average excludes non numeric zero weight and excluded grades`() {
        val grades = listOf(grade(5.0, 3), grade(3.0, 1), grade(null, 2), grade(1.0, 0), grade(1.0, 10, false))
        assertEquals(4.5, grades.weightedAverage()!!, 0.0001)
        assertNull(listOf(grade(null, 0)).weightedAverage())
    }

    @Test fun `cancelled lessons and windows are skipped while an active lesson stays visible`() {
        val now = Instant.parse("2026-11-12T08:00:00Z")
        fun lesson(id: String, offset: Long, status: LessonStatus) = TimetableEntry(id, "s", 1, id, now.plusSeconds(offset), now.plusSeconds(offset + 2700), "L12", null, "1/2", status)
        val cancelled = lesson("cancelled", -1800, LessonStatus.CANCELLED)
        val window = lesson("window", -1200, LessonStatus.WINDOW)
        val active = lesson("active", -600, LessonStatus.SUBSTITUTION)
        val next = lesson("next", 3000, LessonStatus.SCHEDULED)
        assertEquals(active, listOf(next, window, cancelled, active).currentOrNextLesson(now))
        assertEquals(next, listOf(active, next).currentOrNextLesson(active.endsAt))
        assertNull(listOf(cancelled, window).currentOrNextLesson(now))
    }

    @Test fun `releases do not inflate attendance and late still counts present`() {
        fun entry(status: AttendanceStatus) = Attendance(status.name, "s", "math", LocalDate.of(2026, 11, 12), status)
        assertEquals(50.0, AttendanceStatus.entries.map(::entry).attendancePercent()!!, 0.0001)
        assertNull(listOf(entry(AttendanceStatus.RELEASED)).attendancePercent())
    }
}
