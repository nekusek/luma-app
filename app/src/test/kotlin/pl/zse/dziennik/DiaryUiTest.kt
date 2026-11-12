package pl.zse.dziennik

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pl.zse.dziennik.dashboard.*
import pl.zse.dziennik.data.MockDataRepository
import pl.zse.dziennik.designsystem.ZseTheme
import pl.zse.dziennik.model.*

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi", application = Application::class)
class DiaryUiTest {
    @get:Rule val compose = createComposeRule()
    private val clock = Clock.fixed(Instant.parse("2026-11-12T07:15:00Z"), SchoolZone)
    private val snapshot = runBlocking { MockDataRepository(clock).fetch(MockDataRepository.DEMO_STUDENT_ID) }
    private val state = DashboardUiState(snapshot, clock.instant())

    @Test fun dashboardTilesOpenTheirDestinations() {
        var destination: DashboardDestination? = null
        compose.setContent {
            ZseTheme(ThemeMode.LIGHT, dynamicColor = false) {
                DashboardScreen(state, {}, onNavigate = { destination = it })
            }
        }
        compose.onNodeWithText("ZSE / RZESZÓW").assertDoesNotExist()
        capture("pulpit")
        compose.onNodeWithText("Frekwencja").performClick()
        assertEquals(DashboardDestination.ATTENDANCE, destination)
        compose.onNodeWithText("Dzisiaj").performClick()
        assertEquals(DashboardDestination.TIMETABLE, destination)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Projekt"))
        compose.onNodeWithText("Projekt").performClick()
        assertEquals(DashboardDestination.GRADES, destination)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Najbliższy sprawdzian"))
        compose.onNodeWithText("Najbliższy sprawdzian").performClick()
        assertEquals(DashboardDestination.ASSESSMENTS, destination)
    }

    @Test fun attendanceFiltersKeepTheOverallPercentageIndependentOfHistory() {
        compose.setContent {
            ZseTheme(ThemeMode.LIGHT, dynamicColor = false) {
                DemoSection(3, state, ThemeMode.LIGHT, {})
            }
        }
        compose.onNodeWithText("Obecność na 28 z 30 lekcji").assertIsDisplayed()
        capture("frekwencja")
        compose.onNodeWithText("Ten miesiąc").performClick()
        compose.onNodeWithText("Obecność na 10 z 11 lekcji").assertIsDisplayed()
        compose.onNodeWithText("Wszystkie dane").performClick()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Historia obecności"))
        compose.onNodeWithTag("attendance_filters").performScrollTo()
        compose.onNodeWithTag("attendance_filters").performScrollToNode(hasText("Nieobecności"))
        compose.onNode(hasText("Nieobecności") and hasClickAction()).performClick()
        compose.onNodeWithText("Nieobecność").assertExists()
        compose.onNodeWithText("Obecność").assertDoesNotExist()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Twoja frekwencja"))
        compose.onNodeWithText("Obecność na 28 z 30 lekcji").assertIsDisplayed()
    }

    @Test fun emptyAttendanceDoesNotPresentZeroAsAValidPercentage() {
        compose.setContent {
            ZseTheme(ThemeMode.LIGHT, dynamicColor = false) {
                DemoSection(3, state.copy(snapshot = snapshot.copy(attendance = emptyList())), ThemeMode.LIGHT, {})
            }
        }
        compose.onNodeWithText("Brak lekcji do obliczenia frekwencji").assertIsDisplayed()
        compose.onNodeWithText("0,0%").assertDoesNotExist()
    }

    private fun capture(name: String) {
        System.getProperty("luma.previewDir")?.let { directory ->
            File(directory).mkdirs()
            compose.onRoot().captureRoboImage(File(directory, "$name.png").absolutePath)
        }
    }
}
