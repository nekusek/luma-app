package pl.zse.dziennik

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi", application = ZseApplication::class)
class ParentFlowTest {
    @get:Rule(order = 0) val accountsIsolation = AccountsIsolationRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private fun waitText(text: String) { compose.waitUntil(30_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun waitAccount(name: String) { compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Konta: $name").fetchSemanticsNodes().isNotEmpty() } }
    @Test fun parentSubmitsAndTeacherApprovesJustificationLocally() {
        waitText("Maria Kowalska")
        compose.onNodeWithText("Maria Kowalska").performScrollTo().performClick()
        compose.onNodeWithText("Kontynuuj w trybie demo").performScrollTo().performClick()
        waitAccount("Maria Kowalska")
        compose.onNodeWithText("Więcej").performClick()
        compose.onNodeWithText("Usprawiedliwienia").performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodes(isToggleable() and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(isToggleable()).onFirst().performScrollTo().performClick()
        compose.onNodeWithText("Powód nieobecności").performScrollTo().performTextInput("Wizyta lekarska")
        compose.onNodeWithText("Złóż wniosek demo").performScrollTo().performClick()
        compose.onNodeWithText("Potwierdź wniosek").performClick()
        waitText("Oczekuje na decyzję")
        compose.onNodeWithText("Pulpit").performClick()
        compose.onNodeWithContentDescription("Konta: Maria Kowalska").performClick()
        compose.onNodeWithText("Dodaj konto").performClick()
        waitText("Piotr Wiśniewski")
        compose.onNodeWithText("Piotr Wiśniewski").performScrollTo().performClick()
        compose.onNodeWithText("Kontynuuj w trybie demo").performScrollTo().performClick()
        waitAccount("Piotr Wiśniewski")
        compose.onNodeWithText("Usprawiedliwienia").performScrollTo().performClick()
        waitText("Zaakceptuj")
        compose.onNodeWithText("Zaakceptuj").performClick()
        waitText("Brak wniosków.")
        compose.onNodeWithText("Pulpit").performClick()
        compose.onNodeWithContentDescription("Konta: Piotr Wiśniewski").performClick()
        compose.onNode(hasText("Maria Kowalska") and hasClickAction()).performClick()
        waitAccount("Maria Kowalska")
        compose.onNodeWithText("Więcej").performClick()
        compose.onNodeWithText("Usprawiedliwienia").performScrollTo().performClick()
        waitText("Zaakceptowany")
    }
}
