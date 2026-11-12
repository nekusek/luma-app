package pl.zse.dziennik

import android.app.Application
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pl.zse.dziennik.designsystem.ZseTheme
import pl.zse.dziennik.model.*

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi", application = Application::class)
class GradesUiTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 11, 12)
    private val grades = listOf(
        Grade("g1", "s", "it", "Informatyka", "5", 5.0, 3, "Projekt", "Aplikacja w Kotlinie", today),
        Grade("g2", "s", "it", "Informatyka", "4", 4.0, 1, "Odpowiedź", "Kolekcje", today.minusDays(1)),
        Grade("g3", "s", "it", "Informatyka", "np", null, 0, "Nieprzygotowanie", "", today.minusDays(2), false),
        Grade("g4", "s", "math", "Matematyka", "3", 3.0, 1, "Test", "Funkcje", LocalDate.of(2026, 8, 31)),
    )

    @Test fun subjectOpensDetailsWithCorrectAverageAndSimulatorDoesNotChangeGrades() {
        compose.setContent { ZseTheme(ThemeMode.LIGHT, dynamicColor = false) { Surface { GradesScreen(grades, today, "Anna Kowalska", {}) } } }
        compose.onNodeWithText("4,75").assertIsDisplayed()
        compose.onNodeWithText("Matematyka").assertDoesNotExist()
        capture("oceny")
        compose.onNodeWithText("Informatyka").performClick()
        compose.onNodeWithText("LICZBA OCEN").assertIsDisplayed()
        compose.onNodeWithText("Projekt · waga: 3").assertIsDisplayed()
        compose.onNodeWithText("Data: 12.11.2026").assertIsDisplayed()
        compose.onNodeWithText("Nie wlicza się do średniej").assertExists()
        capture("oceny-szczegoly")
        compose.onNodeWithText("Symulator średniej").performScrollTo().performClick()
        compose.onNodeWithText("Po dodaniu: 4,83").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Zamknij szczegóły ocen").performScrollTo().performClick()
        compose.onNodeWithText("4,75").assertIsDisplayed()
        compose.onNodeWithText("Po dodaniu: 4,83").assertDoesNotExist()
    }

    @Test fun schoolYearArrowsFilterGradesAcrossTheSeptemberBoundary() {
        compose.setContent { ZseTheme(ThemeMode.DARK, dynamicColor = false) { Surface { GradesScreen(grades, today, "Anna Kowalska", {}) } } }
        compose.onNodeWithContentDescription("Następny rok szkolny").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Poprzedni rok szkolny").performClick()
        compose.onNodeWithText("2025/2026").assertIsDisplayed()
        compose.onNodeWithText("Matematyka").assertIsDisplayed()
        compose.onNodeWithText("Informatyka").assertDoesNotExist()
        compose.onNodeWithContentDescription("Poprzedni rok szkolny").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Następny rok szkolny").performClick()
        compose.onNodeWithText("Informatyka").assertIsDisplayed()
        compose.onNodeWithText("Informatyka").performClick()
        capture("oceny-szczegoly-ciemne")
    }

    @Test fun emptyAndMidyearViewsDoNotInventGradesOrZeroAverages() {
        compose.setContent { ZseTheme(ThemeMode.AMOLED, dynamicColor = false) { Surface { GradesScreen(emptyList(), today, "Anna Kowalska", {}) } } }
        compose.onNodeWithText("Nie ma jeszcze ocen w tym roku szkolnym.").assertIsDisplayed()
        compose.onNodeWithText("0,00").assertDoesNotExist()
        compose.onNodeWithText("Oceny śródroczne").performClick()
        compose.onNodeWithText("Oceny śródroczne nie są dostępne w danych demo.").assertIsDisplayed()
        compose.onNodeWithText("Nie ma jeszcze ocen w tym roku szkolnym.").assertDoesNotExist()
    }

    private fun capture(name: String) {
        System.getProperty("luma.previewDir")?.let { path ->
            File(path).mkdirs()
            compose.waitForIdle()
            compose.onAllNodes(isRoot()).onLast().captureRoboImage(File(path, "$name.png").absolutePath)
        }
    }
}
