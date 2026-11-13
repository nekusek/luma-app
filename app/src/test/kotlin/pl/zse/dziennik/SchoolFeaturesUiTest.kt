package pl.zse.dziennik

import android.app.Application
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pl.zse.dziennik.data.*
import pl.zse.dziennik.designsystem.ZseTheme
import pl.zse.dziennik.model.*

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi", application = Application::class)
class SchoolFeaturesUiTest {
    @get:Rule val compose = createComposeRule()
    private val clock = Clock.fixed(Instant.parse("2026-11-13T07:15:00Z"), SchoolZone)
    private val today = LocalDate.of(2026, 11, 13)
    private val data = runBlocking { MockDiaryFeaturesSource(clock).fetch(MockDataRepository.DEMO_STUDENT_ID) }

    @Test fun homeworkCanBeCompletedAndAttachmentsOpenedWithoutSendingAnAnswer() {
        var content by mutableStateOf(data)
        compose.setContent { ZseTheme(ThemeMode.LIGHT, dynamicColor = false) { Surface { SchoolCalendarScreen(content, today, { id, done -> content = content.copy(completedHomework = if (done) content.completedHomework + id else content.completedHomework - id) }) } } }
        capture("kalendarz")
        compose.onNodeWithText("Zadania").performClick()
        compose.onNodeWithTag("homework:homework:room").performScrollTo().performClick()
        assertTrue("homework:room" in content.completedHomework)
        compose.onNodeWithText("Przygotuj projekt Room").performScrollTo().performClick()
        compose.onNodeWithText("instrukcja-demo.txt").performScrollTo().performClick()
        compose.onNodeWithText("1. Utwórz encję Task.", substring = true).assertExists()
        compose.onNodeWithText("Oznaczenie wykonania nie wysyła odpowiedzi do dziennika.").assertExists()
    }
    @Test fun notesFilterKeepsPraiseAndNegativeNotesSeparate() {
        compose.setContent { ZseTheme(ThemeMode.DARK, dynamicColor = false) { Surface { SchoolNotesScreen(data.notes) } } }
        compose.onNodeWithText("Pochwały").performClick()
        compose.onNodeWithText("Aktywność").assertExists()
        compose.onNodeWithText("Organizacja").assertDoesNotExist()
        capture("uwagi")
        compose.onNodeWithText("Uwagi").performClick()
        compose.onNodeWithText("Organizacja").assertExists()
        compose.onNodeWithText("Punkty w wybranym widoku: -2").assertExists()
    }
    @Test fun semestersFilterActualDatesAndClassificationUsesSourceValues() {
        val grades = listOf(Grade("g", "s", "it", "Programowanie", "5", 5.0, 1, "Test", "", today))
        compose.setContent { ZseTheme(ThemeMode.LIGHT, dynamicColor = false) { Surface { GradesScreen(grades, today, "Anna Kowalska", {}, features = data) } } }
        compose.onNodeWithText("Semestr 2").performClick()
        compose.onNodeWithText("Nie ma jeszcze ocen w wybranym semestrze.").assertIsDisplayed()
        compose.onNodeWithText("Semestr 1").performClick()
        compose.onNodeWithText("Programowanie").assertIsDisplayed()
        compose.onNodeWithText("Oceny śródroczne").performClick()
        compose.onNodeWithContentDescription("Poprzedni rok szkolny").performClick()
        compose.onNodeWithText("Roczna: 5").assertIsDisplayed()
        capture("klasyfikacja")
    }
    @Test fun parentRequestRequiresSelectionReasonAndExplicitConfirmation() {
        val parent = DemoProfiles.accounts.first { it.role == AccountRole.PARENT }
        var submitted = false
        val attendance = listOf(Attendance("a", parent.studentId, "it", today.minusDays(1), AttendanceStatus.ABSENT))
        compose.setContent { ZseTheme(ThemeMode.LIGHT, dynamicColor = false) { Surface { JustificationsScreen(parent, attendance, FeaturesUiState(), { ids, reason, success -> assertEquals(listOf("a"), ids); assertEquals("Wizyta lekarska", reason); submitted = true; success() }, { _, _ -> }, DemoProfiles.accounts) } } }
        compose.onNodeWithText("Złóż wniosek demo").assertIsNotEnabled()
        compose.onNodeWithTag("absence:a").performClick()
        compose.onNodeWithText("Powód nieobecności").performTextInput("Wizyta lekarska")
        compose.onNodeWithText("Złóż wniosek demo").performScrollTo().performClick()
        assertFalse(submitted)
        compose.onNodeWithText("Potwierdź wniosek").performClick()
        assertTrue(submitted)
    }
    private fun capture(name: String) { System.getProperty("luma.previewDir")?.let { path -> File(path).mkdirs(); compose.waitForIdle(); compose.onAllNodes(isRoot()).onLast().captureRoboImage(File(path, "$name.png").absolutePath) } }
}
