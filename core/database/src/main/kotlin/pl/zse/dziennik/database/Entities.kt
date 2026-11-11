package pl.zse.dziennik.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import pl.zse.dziennik.model.*
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "students", primaryKeys = ["id"])
data class StudentEntity(val id: String, val name: String, val className: String, val school: String, val syncedAt: Long)

@Entity(
    tableName = "grades", primaryKeys = ["studentId", "id"],
    foreignKeys = [ForeignKey(entity = StudentEntity::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["studentId", "subjectId", "receivedOn"])],
)
data class GradeEntity(
    val id: String, val studentId: String, val subjectId: String, val subjectName: String,
    val displayValue: String, val numericValue: Double?, val weight: Int,
    val category: String, val description: String, val receivedOn: Long, val countsTowardsAverage: Boolean,
)

@Entity(
    tableName = "timetable", primaryKeys = ["studentId", "id"],
    foreignKeys = [ForeignKey(entity = StudentEntity::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["studentId", "startsAt"])],
)
data class TimetableEntryEntity(
    val id: String, val studentId: String, val number: Int, val subjectName: String,
    val startsAt: Long, val endsAt: Long, val room: String?, val teacherName: String?, val group: String?,
    val status: LessonStatus, val changeDescription: String?,
)

@Entity(tableName = "attendance", primaryKeys = ["studentId", "id"], indices = [Index("studentId")],
    foreignKeys = [ForeignKey(entity = StudentEntity::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE)])
data class AttendanceEntity(val id: String, val studentId: String, val subjectId: String, val date: Long, val status: AttendanceStatus)

@Entity(tableName = "assessments", primaryKeys = ["studentId", "id"], indices = [Index("studentId")],
    foreignKeys = [ForeignKey(entity = StudentEntity::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE)])
data class AssessmentEntity(val id: String, val studentId: String, val subject: String, val title: String, val date: Long)

fun Grade.toEntity() = GradeEntity(id, studentId, subjectId, subjectName, displayValue, numericValue, weight, category, description, receivedOn.toEpochDay(), countsTowardsAverage)
fun GradeEntity.toModel() = Grade(id, studentId, subjectId, subjectName, displayValue, numericValue, weight, category, description, LocalDate.ofEpochDay(receivedOn), countsTowardsAverage)
fun TimetableEntry.toEntity() = TimetableEntryEntity(id, studentId, number, subjectName, startsAt.toEpochMilli(), endsAt.toEpochMilli(), room, teacherName, group, status, changeDescription)
fun TimetableEntryEntity.toModel() = TimetableEntry(id, studentId, number, subjectName, Instant.ofEpochMilli(startsAt), Instant.ofEpochMilli(endsAt), room, teacherName, group, status, changeDescription)
