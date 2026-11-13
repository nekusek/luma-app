package pl.zse.dziennik.model

import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class DiaryMessage(val id: String, val senderId: String, val recipientId: String, val subject: String, val body: String, val sentAt: Instant, val read: Boolean)
enum class TeachingKind { GRADE, ATTENDANCE, TOPIC, HOMEWORK }
data class TeachingEntry(val id: String, val teacherId: String, val studentId: String?, val className: String, val kind: TeachingKind, val value: String, val description: String, val weight: Int, val date: LocalDate)

interface WorkspaceRepository {
    val contacts: List<DiaryAccount>
    fun messages(accountId: String): Flow<List<DiaryMessage>>
    fun teachingEntries(teacherId: String): Flow<List<TeachingEntry>>
    fun studentEntries(studentId: String): Flow<List<TeachingEntry>>
    suspend fun send(senderId: String, recipientId: String, subject: String, body: String)
    suspend fun markRead(accountId: String, messageId: String)
    suspend fun saveEntry(teacherId: String, studentId: String?, className: String, kind: TeachingKind, value: String, description: String, weight: Int, date: LocalDate)
}
