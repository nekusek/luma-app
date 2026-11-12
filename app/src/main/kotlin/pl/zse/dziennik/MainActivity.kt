package pl.zse.dziennik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import java.io.IOException
import pl.zse.dziennik.dashboard.*
import pl.zse.dziennik.model.ThemeMode
import pl.zse.dziennik.data.ThemePreferences
import pl.zse.dziennik.designsystem.ZseTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: DashboardViewModel by viewModels()
    private var hasStarted = false
    @Inject lateinit var preferences: ThemePreferences

    override fun onStart() {
        super.onStart()
        if (hasStarted) viewModel.refresh()
        hasStarted = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val theme by preferences.theme.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val scope = rememberCoroutineScope()
            var destination by rememberSaveable { mutableIntStateOf(0) }
            val snackbar = remember { SnackbarHostState() }
            val screens = rememberSaveableStateHolder()
            BackHandler(enabled = destination != 0) { destination = 0 }
            ZseTheme(theme) {
                val lightSystemBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = lightSystemBars
                        isAppearanceLightNavigationBars = lightSystemBars
                    }
                }
                Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
                    NavigationBar {
                        val titles = listOf("Pulpit", "Oceny", "Plan", "Obecności", "Więcej")
                        val icons = listOf(R.drawable.ic_dashboard, R.drawable.ic_grades, R.drawable.ic_timetable, R.drawable.ic_attendance, R.drawable.ic_more)
                        titles.forEachIndexed { index, title ->
                            NavigationBarItem(selected = destination == index, onClick = { destination = index },
                                icon = { Icon(painterResource(icons[index]), contentDescription = null) }, label = { Text(title, maxLines = 2) })
                        }
                    }
                }) { padding ->
                    screens.SaveableStateProvider(destination) {
                        if (destination == 0) DashboardRoute(viewModel, Modifier.padding(padding))
                        else DemoSection(destination, state, theme, { mode ->
                            scope.launch {
                                try { preferences.setTheme(mode) }
                                catch (_: IOException) { snackbar.showSnackbar("Nie udało się zapisać motywu. Spróbuj ponownie.") }
                            }
                        }, Modifier.padding(padding), onError = { message -> scope.launch { snackbar.showSnackbar(message) } })
                    }
                }
            }
        }
    }
}
