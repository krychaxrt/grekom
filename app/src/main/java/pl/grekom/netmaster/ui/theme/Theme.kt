package pl.grekom.netmaster.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = GrekomRed,
    onPrimary = Color.White,
    secondary = GrekomDarkRed,
    background = GrekomLight,
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = GrekomRed,
    onPrimary = Color.White,
    secondary = GrekomDarkRed,
    background = GrekomDark,
    surface = Color(0xFF2A2A2A)
)

@Composable
fun GrekomNetMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
