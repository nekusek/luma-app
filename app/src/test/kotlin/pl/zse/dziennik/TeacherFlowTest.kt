package pl.zse.dziennik

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi", application = ZseApplication::class)
class TeacherFlowTest {
    @get:Rule(order = 0) val accountsIsolation = AccountsIsolationRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private fun waitText(text: String) { compose.waitUntil(30_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun waitAccount(name: String) { compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Konta: $name").fetchSemanticsNodes().isNotEmpty() } }

    @Test fun studentMessagesReachTeacherAndTeacherGradesReachStudent() {
        waitText("Anna Kowalska")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Kontynuuj w trybie demo"))
        compose.onNodeWithText("Kontynuuj w trybie demo").performClick()
        waitAccount("Anna Kowalska")
        compose.onNodeWithText("Wiadomości").performClick()
        compose.onNodeWithContentDescription("Napisz wiadomość").performClick()
        compose.onNodeWithText("Temat").performScrollTo().performTextInput("Projekt testowy")
        compose.onNodeWithText("Treść wiadomości").performScrollTo().performTextInput("Czy mogę użyć Room?")
        compose.onNodeWithText("Wyślij lokalnie (demo)").performScrollTo().performClick()
        waitText("Projekt testowy")
        compose.onNodeWithText("Pulpit").performClick()
        compose.onNodeWithContentDescription("Konta: Anna Kowalska").performClick()
        compose.onNodeWithText("Dodaj konto").performClick()
        waitText("Piotr Wiśniewski")
        compose.onNodeWithText("Piotr Wiśniewski").performScrollTo().performClick()
        compose.onNodeWithText("Kontynuuj w trybie demo").performScrollTo().performClick()
        waitAccount("Piotr Wiśniewski")
        compose.onNodeWithText("Pulpit nauczyciela").assertIsDisplayed()
        System.getProperty("luma.previewDir")?.let { path -> File(path).mkdirs(); compose.onAllNodes(isRoot()).onLast().captureRoboImage(File(path, "nauczyciel.png").absolutePath) }
        compose.onNode(hasText("Wiadomości") and isSelectable()).performClick()
        waitText("Projekt testowy")
        compose.onNodeWithText("Projekt testowy").performClick()
        compose.onAllNodesWithText("Czy mogę użyć Room?").onLast().assertIsDisplayed()
        compose.onNodeWithContentDescription("Zamknij wiadomość").performClick()
        compose.onNodeWithText("Dziennik").performClick()
        compose.onNodeWithText("Opis oceny").performScrollTo().performTextInput("Projekt z Room")
        compose.onNodeWithText("Zapisz ocenę demo").performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Zapisano w lokalnym dzienniku demo.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Pulpit").performClick()
        compose.onNodeWithContentDescription("Konta: Piotr Wiśniewski").performClick()
        compose.onNode(hasText("Anna Kowalska") and hasClickAction()).performClick()
        waitAccount("Anna Kowalska")
        compose.onNodeWithText("Oceny").performClick()
        waitText("Programowanie")
        compose.onNodeWithText("Programowanie").performClick()
        compose.onNodeWithText("Projekt z Room").assertExists()
    }
}
