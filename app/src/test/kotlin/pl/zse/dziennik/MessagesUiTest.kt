package pl.zse.dziennik

import android.app.Application
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import java.time.Instant
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pl.zse.dziennik.data.DemoProfiles
import pl.zse.dziennik.designsystem.ZseTheme
import pl.zse.dziennik.model.*

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi", application = Application::class)
class MessagesUiTest {
    @get:Rule val compose = createComposeRule()
    private val anna = DemoProfiles.accounts[0]
    private val teacher = DemoProfiles.accounts.first { it.role == AccountRole.TEACHER }

    @Test fun composingValidatesInputsAndUsesTheTeacherRecipient() {
        var result: List<String>? = null
        compose.setContent { ZseTheme(ThemeMode.LIGHT, dynamicColor = false) { Surface {
            MessagesScreen(anna, DemoProfiles.accounts, WorkspaceUiState(loading = false), {}, { recipient, subject, body, success -> result = listOf(recipient, subject, body); success() }, {}, {})
        } } }
        compose.onNodeWithContentDescription("Napisz wiadomość").performClick()
        compose.onNodeWithText("Wyślij lokalnie (demo)").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Temat").performScrollTo().performTextInput("Projekt Kotlin")
        compose.onNodeWithText("Treść wiadomości").performScrollTo().performTextInput("Dzień dobry, mam pytanie.")
        compose.onNodeWithText("Wyślij lokalnie (demo)").performScrollTo().performClick()
        assertEquals(listOf(teacher.id, "Projekt Kotlin", "Dzień dobry, mam pytanie."), result)
        compose.onNodeWithText("Nowa wiadomość").assertDoesNotExist()
    }

    @Test fun inboxDetailsMarkReadAndReplyToTheSender() {
        var readId: String? = null
        val message = DiaryMessage("m1", teacher.id, anna.id, "Pracownia", "Zabierz projekt na zajęcia.", Instant.parse("2026-11-13T07:00:00Z"), false)
        compose.setContent { ZseTheme(ThemeMode.LIGHT, dynamicColor = false) { Surface {
            MessagesScreen(anna, DemoProfiles.accounts, WorkspaceUiState(messages = listOf(message), loading = false), { readId = it }, { _, _, _, _ -> }, {}, {})
        } } }
        capture("wiadomosci")
        compose.onNodeWithText("Pracownia").performClick()
        assertEquals("m1", readId)
        compose.onNodeWithText("Odpowiedz").performClick()
        compose.onNodeWithText("Re: Pracownia").assertExists()
        compose.onNodeWithText("Do: ${teacher.displayName}").assertExists()
    }

    @Test fun dismissingUnsentTextRequiresConfirmation() {
        compose.setContent { ZseTheme(ThemeMode.DARK, dynamicColor = false) { Surface {
            MessagesScreen(anna, DemoProfiles.accounts, WorkspaceUiState(loading = false), {}, { _, _, _, _ -> }, {}, {})
        } } }
        compose.onNodeWithContentDescription("Napisz wiadomość").performClick()
        compose.onNodeWithText("Temat").performTextInput("Szkic")
        compose.onNodeWithContentDescription("Zamknij formularz wiadomości").performClick()
        compose.onNodeWithText("Odrzucić wiadomość?").assertIsDisplayed()
        compose.onNodeWithText("Wróć do pisania").performClick()
        compose.onNodeWithText("Szkic").assertExists()
    }

    private fun capture(name: String) { System.getProperty("luma.previewDir")?.let { path -> File(path).mkdirs(); compose.waitForIdle(); compose.onAllNodes(isRoot()).onLast().captureRoboImage(File(path, "$name.png").absolutePath) } }
}
