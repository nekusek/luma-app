package pl.zse.dziennik.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "portal_info")
data class PortalInfoEntity(@PrimaryKey val studentId: String, val luckyNumber: Int?, val luckyDate: Long?, val pupilNumber: Int?)
@Entity(tableName = "periods", primaryKeys = ["studentId", "id"])
data class PeriodEntity(val studentId: String, val id: String, val schoolYear: Int, val number: Int, val startsOn: Long, val endsOn: Long)
@Entity(tableName = "school_tasks", primaryKeys = ["studentId", "id"])
data class SchoolTaskEntity(val studentId: String, val id: String, val kind: String, val subject: String, val title: String, val description: String, val dueOn: Long, val teacher: String, val attachmentName: String?, val attachmentContent: String?)
@Entity(tableName = "school_notes", primaryKeys = ["studentId", "id"])
data class SchoolNoteEntity(val studentId: String, val id: String, val positive: Boolean?, val category: String, val body: String, val points: Int?, val date: Long, val teacher: String)
@Entity(tableName = "classification_grades", primaryKeys = ["studentId", "id"])
data class ClassificationEntity(val studentId: String, val id: String, val subject: String, val schoolYear: Int, val predicted: String?, val midyear: String?, val annual: String?)
@Entity(tableName = "homework_done", primaryKeys = ["studentId", "taskId"])
data class HomeworkDoneEntity(val studentId: String, val taskId: String)
@Entity(tableName = "justifications", indices = [Index("studentId")])
data class JustificationEntity(@PrimaryKey val id: String, val studentId: String, val requesterId: String, val attendanceIds: String, val reason: String, val status: String, val createdAt: Long, val decidedBy: String?)

data class StoredDiaryFeatures(
    @Embedded val info: PortalInfoEntity,
    @Relation(parentColumn = "studentId", entityColumn = "studentId") val periods: List<PeriodEntity>,
    @Relation(parentColumn = "studentId", entityColumn = "studentId") val tasks: List<SchoolTaskEntity>,
    @Relation(parentColumn = "studentId", entityColumn = "studentId") val notes: List<SchoolNoteEntity>,
    @Relation(parentColumn = "studentId", entityColumn = "studentId") val classification: List<ClassificationEntity>,
)

@Dao
interface DiaryFeaturesDao {
    @Transaction @Query("SELECT * FROM portal_info WHERE studentId = :studentId") fun observe(studentId: String): Flow<StoredDiaryFeatures?>
    @Upsert suspend fun upsertInfo(info: PortalInfoEntity)
    @Insert suspend fun insertPeriods(entries: List<PeriodEntity>)
    @Insert suspend fun insertTasks(entries: List<SchoolTaskEntity>)
    @Insert suspend fun insertNotes(entries: List<SchoolNoteEntity>)
    @Insert suspend fun insertClassification(entries: List<ClassificationEntity>)
    @Query("DELETE FROM periods WHERE studentId = :studentId") suspend fun clearPeriods(studentId: String)
    @Query("DELETE FROM school_tasks WHERE studentId = :studentId") suspend fun clearTasks(studentId: String)
    @Query("DELETE FROM school_notes WHERE studentId = :studentId") suspend fun clearNotes(studentId: String)
    @Query("DELETE FROM classification_grades WHERE studentId = :studentId") suspend fun clearClassification(studentId: String)
    @Query("SELECT * FROM homework_done WHERE studentId = :studentId") fun completed(studentId: String): Flow<List<HomeworkDoneEntity>>
    @Upsert suspend fun complete(entry: HomeworkDoneEntity)
    @Query("DELETE FROM homework_done WHERE studentId = :studentId AND taskId = :taskId") suspend fun uncomplete(studentId: String, taskId: String)
    @Query("SELECT * FROM justifications WHERE studentId = :studentId ORDER BY createdAt DESC") fun justifications(studentId: String): Flow<List<JustificationEntity>>
    @Query("SELECT * FROM justifications WHERE status = 'PENDING' ORDER BY createdAt") fun pending(): Flow<List<JustificationEntity>>
    @Query("SELECT * FROM justifications WHERE studentId = :studentId") suspend fun requests(studentId: String): List<JustificationEntity>
    @Query("SELECT * FROM justifications WHERE id = :id") suspend fun justification(id: String): JustificationEntity?
    @Insert suspend fun insertJustification(entry: JustificationEntity)
    @Query("UPDATE justifications SET status = :status, decidedBy = :teacherId WHERE id = :id AND status = 'PENDING'") suspend fun decide(id: String, status: String, teacherId: String): Int
}
