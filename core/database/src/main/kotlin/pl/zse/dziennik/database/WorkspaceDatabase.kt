package pl.zse.dziennik.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages", indices = [Index("senderId"), Index("recipientId")])
data class MessageEntity(@PrimaryKey val id: String, val senderId: String, val recipientId: String, val subject: String, val body: String, val sentAt: Long, val read: Boolean)

@Entity(tableName = "teaching_entries", indices = [Index("teacherId"), Index("studentId")])
data class TeachingEntity(@PrimaryKey val id: String, val teacherId: String, val studentId: String?, val className: String, val kind: String, val value: String, val description: String, val weight: Int, val date: Long)

@Entity(tableName = "workspace_meta")
data class WorkspaceMeta(@PrimaryKey val id: String)

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM messages WHERE senderId = :accountId OR recipientId = :accountId ORDER BY sentAt DESC, id DESC")
    fun messages(accountId: String): Flow<List<MessageEntity>>
    @Insert suspend fun insertMessage(message: MessageEntity)
    @Query("UPDATE messages SET `read` = 1 WHERE id = :messageId AND recipientId = :accountId")
    suspend fun markRead(accountId: String, messageId: String)
    @Query("SELECT * FROM teaching_entries WHERE teacherId = :teacherId ORDER BY date DESC, rowid DESC")
    fun teacherEntries(teacherId: String): Flow<List<TeachingEntity>>
    @Query("SELECT * FROM teaching_entries WHERE studentId = :studentId OR (studentId IS NULL AND className = :className) ORDER BY date DESC, rowid DESC")
    fun studentEntries(studentId: String, className: String): Flow<List<TeachingEntity>>
    @Insert suspend fun insertEntry(entry: TeachingEntity)
    @Query("DELETE FROM teaching_entries WHERE teacherId = :teacherId AND studentId = :studentId AND kind = 'ATTENDANCE' AND date = :date")
    suspend fun clearAttendance(teacherId: String, studentId: String, date: Long)
    @Query("SELECT COUNT(*) FROM workspace_meta WHERE id = 'seed_v1'") suspend fun seeded(): Int
    @Insert suspend fun seedMarker(marker: WorkspaceMeta)
}

@Database(entities = [MessageEntity::class, TeachingEntity::class, WorkspaceMeta::class, PortalInfoEntity::class, PeriodEntity::class, SchoolTaskEntity::class, SchoolNoteEntity::class, ClassificationEntity::class, HomeworkDoneEntity::class, JustificationEntity::class], version = 2, exportSchema = true, autoMigrations = [AutoMigration(from = 1, to = 2)])
abstract class WorkspaceDatabase : RoomDatabase() { abstract fun workspaceDao(): WorkspaceDao; abstract fun featuresDao(): DiaryFeaturesDao }
