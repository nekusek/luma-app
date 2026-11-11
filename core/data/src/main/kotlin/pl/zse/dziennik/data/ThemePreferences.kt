package pl.zse.dziennik.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.emptyPreferences
import pl.zse.dziennik.model.ThemeMode

private val Context.settings by preferencesDataStore(name = "settings")

class ThemePreferences(context: Context) {
    private val store = context.applicationContext.settings
    private val themeKey = stringPreferencesKey("theme")
    val theme = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.SYSTEM
    }
    suspend fun setTheme(mode: ThemeMode) { store.edit { it[themeKey] = mode.name } }
}
