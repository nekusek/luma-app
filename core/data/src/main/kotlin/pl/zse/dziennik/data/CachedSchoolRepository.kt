package pl.zse.dziennik.data

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.zse.dziennik.database.SchoolDao
import pl.zse.dziennik.model.*

class CachedSchoolRepository(private val dao: SchoolDao, private val source: SnapshotSource) : SchoolRepository {
    private val refreshMutex = Mutex()
    override fun observe(studentId: String) = dao.observeSnapshot(studentId).map { it?.toModel() }
    override suspend fun refresh(studentId: String) = refreshMutex.withLock {
        val snapshot = source.fetch(studentId)
        require(snapshot.student.id == studentId)
        dao.replaceSnapshot(snapshot)
    }
}
