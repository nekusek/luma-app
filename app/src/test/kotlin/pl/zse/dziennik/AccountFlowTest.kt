package pl.zse.dziennik

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi", application = ZseApplication::class)
class AccountFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForText(value: String) {
        compose.waitUntil(30_000) { compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitForAccount(name: String) {
        compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Konta: $name").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun loginAddSwitchAndLogoutDoNotMixStudentScreens() {
        waitForText("Kontynuuj w trybie demo")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Kontynuuj w trybie demo"))
        compose.onNodeWithText("Kontynuuj w trybie demo").performClick()
        waitForAccount("Anna Kowalska")
        compose.onNodeWithContentDescription("Konta: Anna Kowalska").performClick()
        compose.onNodeWithText("Dodaj konto").performClick()
        waitForText("Mateusz Nowak")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Kontynuuj w trybie demo"))
        compose.onNodeWithText("Kontynuuj w trybie demo").performClick()
        waitForAccount("Mateusz Nowak")
        compose.onNodeWithText("3TI · grupa 1").assertDoesNotExist()
        compose.onNodeWithContentDescription("Konta: Mateusz Nowak").performClick()
        compose.onNodeWithText("Anna Kowalska").performClick()
        waitForAccount("Anna Kowalska")
        compose.onNodeWithText("4TE · grupa 2").assertDoesNotExist()
        compose.onNodeWithContentDescription("Konta: Anna Kowalska").performClick()
        compose.onNodeWithContentDescription("Usuń konto Anna Kowalska z urządzenia").performClick()
        compose.onNodeWithText("Usuń konto").performClick()
        waitForAccount("Mateusz Nowak")
        compose.onNodeWithContentDescription("Konta: Mateusz Nowak").performClick()
        compose.onNodeWithContentDescription("Usuń konto Mateusz Nowak z urządzenia").performClick()
        compose.onNodeWithText("Usuń konto").performClick()
        waitForText("Kontynuuj w trybie demo")
        compose.onNodeWithText("Wybierz profil demo").assertExists()
    }
}
