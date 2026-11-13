package pl.zse.dziennik.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import pl.zse.dziennik.model.*

private val Context.diaryOptions by preferencesDataStore(name = "diary_options")
class DiaryPreferences(context: Context) {
    private val store = context.applicationContext.diaryOptions
    private val modifiers = booleanPreferencesKey("modifiers")
    private val plus = doublePreferencesKey("plus")
    private val minus = doublePreferencesKey("minus")
    private val receipts = booleanPreferencesKey("receipts")
    private val sync = intPreferencesKey("sync_minutes")
    private val wifi = booleanPreferencesKey("wifi_only")
    val options = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
        DiaryOptions(GradePolicy(it[modifiers] ?: false, (it[plus] ?: 0.5).coerceIn(0.0, 1.0), (it[minus] ?: 0.25).coerceIn(0.0, 1.0)), it[receipts] ?: true, it[sync]?.takeIf { value -> value in listOf(0, 15, 60, 240, 1440) } ?: 0, it[wifi] ?: true)
    }
    suspend fun setGradePolicy(policy: GradePolicy) { store.edit { it[modifiers] = policy.useModifiers; it[plus] = policy.plus; it[minus] = policy.minus } }
    suspend fun setReadReceipts(enabled: Boolean) { store.edit { it[receipts] = enabled } }
    suspend fun setSync(minutes: Int, wifiOnly: Boolean) { require(minutes in listOf(0, 15, 60, 240, 1440)); store.edit { it[sync] = minutes; it[wifi] = wifiOnly } }
}
