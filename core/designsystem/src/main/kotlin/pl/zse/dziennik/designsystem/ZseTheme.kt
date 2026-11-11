package pl.zse.dziennik.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import pl.zse.dziennik.model.ThemeMode

@Composable
fun ZseTheme(mode: ThemeMode = ThemeMode.SYSTEM, dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val dark = mode == ThemeMode.DARK || mode == ThemeMode.AMOLED || mode == ThemeMode.SYSTEM && isSystemInDarkTheme()
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        dark -> darkColorScheme(primary = Color(0xFF9ADBC7), secondary = Color(0xFFACC9EA))
        else -> lightColorScheme(primary = Color(0xFF006B54), secondary = Color(0xFF345F86))
    }.let { if (mode == ThemeMode.AMOLED) it.copy(background = Color.Black, surface = Color.Black, surfaceContainer = Color(0xFF101413), surfaceContainerLow = Color(0xFF0A0D0C)) else it }
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
