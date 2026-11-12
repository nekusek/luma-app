package pl.zse.dziennik

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
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
class AccountUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun loginUsesTheSelectedDemoProfileWithoutRequestingAPassword() {
        var selected: String? = null
        compose.setContent { ZseTheme(ThemeMode.LIGHT, dynamicColor = false) {
            LoginScreen(DemoProfiles.accounts, emptyList(), false, null, { selected = it })
        } }
        capture("logowanie")
        compose.onNodeWithText("Hasło").assertDoesNotExist()
        compose.onNodeWithText("Mateusz Nowak").performScrollTo().performClick()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Kontynuuj w trybie demo"))
        compose.onNodeWithText("Kontynuuj w trybie demo").performClick()
        assertEquals(DemoProfiles.accounts[1].id, selected)
    }

    @Test fun addingAccountDoesNotOfferAlreadyConnectedProfiles() {
        var canceled = false
        compose.setContent { ZseTheme(ThemeMode.LIGHT, dynamicColor = false) {
            LoginScreen(DemoProfiles.accounts, listOf(DemoProfiles.accounts[0]), false, null, {}, { canceled = true })
        } }
        compose.onNodeWithText("Anna Kowalska").assertDoesNotExist()
        compose.onNodeWithText("Mateusz Nowak").assertExists()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Wróć do dziennika"))
        compose.onNodeWithText("Wróć do dziennika").performClick()
        assertTrue(canceled)
    }

    @Test fun accountSheetSwitchesProfilesAndRequiresLogoutConfirmation() {
        val anna = DemoProfiles.accounts[0]
        val mateusz = DemoProfiles.accounts[1]
        var selected: String? = null
        var removed: String? = null
        compose.setContent { ZseTheme(ThemeMode.LIGHT, dynamicColor = false) {
            AccountSheet(AccountSession(DemoProfiles.accounts, anna.id), false, { selected = it }, {}, { removed = it }, {})
        } }
        capture("konta")
        compose.onNodeWithText(mateusz.displayName).performClick()
        assertEquals(mateusz.id, selected)
        compose.onNodeWithContentDescription("Usuń konto ${anna.displayName} z urządzenia").performClick()
        assertNull(removed)
        compose.onNodeWithText("Usunąć konto z urządzenia?").assertIsDisplayed()
        compose.onNodeWithText("Usuń konto").performClick()
        assertEquals(anna.id, removed)
    }

    private fun capture(name: String) {
        System.getProperty("luma.previewDir")?.let { path ->
            File(path).mkdirs()
            compose.onRoot().captureRoboImage(File(path, "$name.png").absolutePath)
        }
    }
}
