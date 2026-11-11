package pl.zse.dziennik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import pl.zse.dziennik.dashboard.*
import pl.zse.dziennik.model.ThemeMode
import pl.zse.dziennik.data.ThemePreferences
import pl.zse.dziennik.designsystem.ZseTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: DashboardViewModel by viewModels()
    @Inject lateinit var preferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val theme by preferences.theme.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val scope = rememberCoroutineScope()
            var destination by rememberSaveable { mutableIntStateOf(0) }
            ZseTheme(theme) {
                val lightSystemBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = lightSystemBars
                        isAppearanceLightNavigationBars = lightSystemBars
                    }
                }
                Scaffold(bottomBar = {
                    NavigationBar {
                        val titles = listOf("Pulpit", "Oceny", "Plan", "Frekwencja", "Więcej")
                        val glyphs = listOf("⌂", "★", "▦", "✓", "⋯")
                        titles.forEachIndexed { index, title ->
                            NavigationBarItem(selected = destination == index, onClick = { destination = index },
                                icon = { Text(glyphs[index]) }, label = { Text(title, maxLines = 1) })
                        }
                    }
                }) { padding ->
                    if (destination == 0) DashboardScreen(state, viewModel::refresh, Modifier.padding(padding))
                    else DemoSection(destination, state, theme, { mode -> scope.launch { preferences.setTheme(mode) } }, Modifier.padding(padding))
                }
            }
        }
    }
}
