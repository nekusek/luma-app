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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import java.io.IOException
import pl.zse.dziennik.dashboard.*
import pl.zse.dziennik.model.*
import pl.zse.dziennik.data.ThemePreferences
import pl.zse.dziennik.designsystem.ZseTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val accounts: AccountsViewModel by viewModels()
    @Inject lateinit var preferences: ThemePreferences
    @Inject lateinit var dashboardFactory: DashboardViewModelFactory
    @Inject lateinit var workspaceFactory: WorkspaceViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val accountState by accounts.state.collectAsStateWithLifecycle()
            val theme by preferences.theme.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            var addingAccount by rememberSaveable { mutableStateOf(false) }
            var showingAccounts by rememberSaveable { mutableStateOf(false) }
            var handledLogin by rememberSaveable { mutableStateOf<String?>(null) }
            val session = accountState.session
            val active = session?.activeAccount
            LaunchedEffect(accountState.completedLogin, session?.activeId) {
                accountState.completedLogin?.let { login ->
                    if (login.receiptId != handledLogin && login.accountId == session?.activeId) {
                        addingAccount = false
                        handledLogin = login.receiptId
                    }
                }
            }
            ZseTheme(theme) {
                val lightBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = lightBars
                        isAppearanceLightNavigationBars = lightBars
                    }
                }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (session == null) {
                        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            if (accountState.error == null) CircularProgressIndicator()
                            else {
                                Text(accountState.error.orEmpty())
                                TextButton(onClick = accounts::retryLoad) { Text("Spróbuj ponownie") }
                            }
                        }
                    } else if (active == null || addingAccount) {
                        BackHandler(enabled = active != null && !accountState.busy) { addingAccount = false }
                        LoginScreen(accounts.demoProfiles, session.accounts, accountState.busy, accountState.error,
                            { accounts.signIn(AuthenticationRequest.Demo(it)) }, if (active != null) ({ addingAccount = false }) else null)
                    } else {
                        key(active.id) {
                            DiaryShell(active, dashboardFactory, workspaceFactory, theme, preferences, accountState.error, accounts::dismissError) { showingAccounts = true }
                        }
                        if (showingAccounts) AccountSheet(session, accountState.busy,
                            onSelect = { showingAccounts = false; accounts.activate(it) },
                            onAdd = { showingAccounts = false; accounts.dismissError(); addingAccount = true },
                            onRemove = { showingAccounts = false; accounts.remove(it) },
                            onDismiss = { showingAccounts = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryShell(account: DiaryAccount, factory: DashboardViewModelFactory, workspaceFactory: WorkspaceViewModelFactory, theme: ThemeMode, preferences: ThemePreferences, accountError: String?, onErrorConsumed: () -> Unit, onAccounts: () -> Unit) {
    val store = remember(account.id) { ViewModelStore() }
    DisposableEffect(store) { onDispose { store.clear() } }
    val teacher = account.role == AccountRole.TEACHER
    val workspace = remember(account.id, store) { ViewModelProvider(store, viewModelFactory { initializer { workspaceFactory.create(account) } })[WorkspaceViewModel::class.java] }
    val workspaceState by workspace.state.collectAsStateWithLifecycle()
    val viewModel = remember(account.id, store) { if (teacher) null else ViewModelProvider(store, viewModelFactory { initializer { factory.create(account.studentId) } })[DashboardViewModel::class.java] }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_START) viewModel?.refresh() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val state = viewModel?.state?.collectAsStateWithLifecycle()?.value ?: DashboardUiState()
    val scope = rememberCoroutineScope()
    var destination by rememberSaveable { mutableIntStateOf(0) }
    var showAssessments by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val screens = rememberSaveableStateHolder()
    LaunchedEffect(accountError) {
        accountError?.let { snackbar.showSnackbar(it); onErrorConsumed() }
    }
    LaunchedEffect(workspaceState.notice) {
        workspaceState.notice?.let { snackbar.showSnackbar(it); workspace.dismissNotice() }
    }
    BackHandler(enabled = destination != 0) { destination = 0 }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        NavigationBar {
            val destinations = listOf(0, 1, 2, 5, 4)
            val titles = if (teacher) listOf("Pulpit", "Dziennik", "Plan", "Wiadomości", "Więcej") else listOf("Pulpit", "Oceny", "Plan", "Wiadomości", "Więcej")
            val icons = listOf(R.drawable.ic_dashboard, R.drawable.ic_grades, R.drawable.ic_timetable, R.drawable.ic_messages, R.drawable.ic_more)
            titles.forEachIndexed { index, title ->
                val target = destinations[index]
                NavigationBarItem(selected = destination == target || (target == 4 && destination == 3), onClick = { destination = target; showAssessments = false },
                    icon = { Icon(painterResource(icons[index]), contentDescription = null) }, label = { Text(title, maxLines = 1, style = MaterialTheme.typography.labelSmall) })
            }
        }
    }) { padding ->
        screens.SaveableStateProvider(destination) {
            if (destination == 5) MessagesScreen(account, workspace.contacts, workspaceState, workspace::read, workspace::send, workspace::retry, workspace::clearError, Modifier.padding(padding))
            else if (teacher && destination != 4) TeacherScreen(destination, workspace, workspaceState, { destination = it }, onAccounts, Modifier.padding(padding))
            else if (destination == 0) DashboardRoute(requireNotNull(viewModel), Modifier.padding(padding), onNavigate = { target ->
                showAssessments = target == DashboardDestination.ASSESSMENTS
                destination = when (target) {
                    DashboardDestination.GRADES -> 1
                    DashboardDestination.TIMETABLE -> 2
                    DashboardDestination.ATTENDANCE -> 3
                    DashboardDestination.ASSESSMENTS -> 4
                }
            }, accountAction = {
                IconButton(onClick = onAccounts) { Icon(painterResource(R.drawable.ic_switch_account), "Konta: ${account.displayName}") }
            })
            else DemoSection(destination, state, theme, { mode ->
                scope.launch {
                    try { preferences.setTheme(mode) }
                    catch (_: IOException) { snackbar.showSnackbar("Nie udało się zapisać motywu. Spróbuj ponownie.") }
                }
            }, Modifier.padding(padding), onError = { message -> scope.launch { snackbar.showSnackbar(message) } }, showAssessments = showAssessments, onShowAssessments = { showAssessments = it }, onAccounts = onAccounts, onAttendance = if (!teacher) ({ destination = 3 }) else null)
        }
    }
}
