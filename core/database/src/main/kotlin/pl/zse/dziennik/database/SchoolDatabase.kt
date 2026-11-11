package pl.zse.dziennik.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import pl.zse.dziennik.model.*
import java.time.Instant
import java.time.LocalDate

data class StoredSnapshot(
    @Embedded val student: StudentEntity,
    @Relation(parentColumn = "id", entityColumn = "studentId") val grades: List<GradeEntity>,
    @Relation(parentColumn = "id", entityColumn = "studentId") val timetable: List<TimetableEntryEntity>,
    @Relation(parentColumn = "id", entityColumn = "studentId") val attendance: List<AttendanceEntity>,
    @Relation(parentColumn = "id", entityColumn = "studentId") val assessments: List<AssessmentEntity>,
) {
    fun toModel() = SchoolSnapshot(
        Student(student.id, student.name, student.className, student.school),
        grades.map { it.toModel() }.sortedByDescending { it.receivedOn },
        timetable.map { it.toModel() }.sortedBy { it.startsAt },
        attendance.map { Attendance(it.id, it.studentId, it.subjectId, LocalDate.ofEpochDay(it.date), it.status) },
        assessments.map { Assessment(it.id, it.subject, it.title, LocalDate.ofEpochDay(it.date)) }.sortedBy { it.date },
        Instant.ofEpochMilli(student.syncedAt),
    )
}

@Dao
abstract class SchoolDao {
    @Transaction
    @Query("SELECT * FROM students WHERE id = :studentId")
    abstract fun observeSnapshot(studentId: String): Flow<StoredSnapshot?>

    @Query("SELECT * FROM grades WHERE studentId = :studentId ORDER BY receivedOn DESC")
    abstract fun observeGrades(studentId: String): Flow<List<GradeEntity>>

    @Query("SELECT * FROM timetable WHERE studentId = :studentId AND startsAt >= :from AND startsAt < :until ORDER BY startsAt")
    abstract fun observeTimetable(studentId: String, from: Long, until: Long): Flow<List<TimetableEntryEntity>>

    @Upsert abstract suspend fun upsertStudent(student: StudentEntity)
    @Upsert abstract suspend fun upsertGrades(grades: List<GradeEntity>)
    @Upsert abstract suspend fun upsertTimetable(entries: List<TimetableEntryEntity>)
    @Upsert abstract suspend fun upsertAttendance(entries: List<AttendanceEntity>)
    @Upsert abstract suspend fun upsertAssessments(entries: List<AssessmentEntity>)
    @Query("DELETE FROM grades WHERE studentId = :studentId") abstract suspend fun clearGrades(studentId: String)
    @Query("DELETE FROM timetable WHERE studentId = :studentId") abstract suspend fun clearTimetable(studentId: String)
    @Query("DELETE FROM attendance WHERE studentId = :studentId") abstract suspend fun clearAttendance(studentId: String)
    @Query("DELETE FROM assessments WHERE studentId = :studentId") abstract suspend fun clearAssessments(studentId: String)
    @Query("DELETE FROM students WHERE id = :studentId") abstract suspend fun deleteStudent(studentId: String)

    /** Replace only after the complete response has been validated; transaction rolls back on failure. */
    @Transaction
    open suspend fun replaceSnapshot(snapshot: SchoolSnapshot) {
        val s = snapshot.student
        require(snapshot.grades.all { it.studentId == s.id } && snapshot.timetable.all { it.studentId == s.id } && snapshot.attendance.all { it.studentId == s.id })
        upsertStudent(StudentEntity(s.id, s.name, s.className, s.school, snapshot.syncedAt.toEpochMilli()))
        clearGrades(s.id); clearTimetable(s.id); clearAttendance(s.id); clearAssessments(s.id)
        upsertGrades(snapshot.grades.map { it.toEntity() })
        upsertTimetable(snapshot.timetable.map { it.toEntity() })
        upsertAttendance(snapshot.attendance.map { AttendanceEntity(it.id, it.studentId, it.subjectId, it.date.toEpochDay(), it.status) })
        upsertAssessments(snapshot.assessments.map { AssessmentEntity(it.id, s.id, it.subject, it.title, it.date.toEpochDay()) })
    }
}

@Database(entities = [StudentEntity::class, GradeEntity::class, TimetableEntryEntity::class, AttendanceEntity::class, AssessmentEntity::class], version = 1, exportSchema = true)
abstract class SchoolDatabase : RoomDatabase() { abstract fun schoolDao(): SchoolDao }
