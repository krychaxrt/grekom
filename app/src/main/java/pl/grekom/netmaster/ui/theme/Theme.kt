package pl.grekom.netmaster.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import pl.grekom.netmaster.data.SignalLevel

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = BrandBlueLight,
    background = SurfaceLight,
    surface = Color.White,
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = Color.White
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueLight,
    onPrimary = Color.Black,
    secondary = BrandBlue,
    background = SurfaceDark,
    surface = Color(0xFF2A3444),
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = Color.White
)

@Composable
fun GrekomNetMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}

fun signalColor(level: SignalLevel): Color = when (level) {
    SignalLevel.EXCELLENT -> SignalExcellent
    SignalLevel.GOOD -> SignalGood
    SignalLevel.FAIR -> SignalFair
    SignalLevel.POOR -> SignalPoor
    SignalLevel.VERY_POOR -> SignalVeryPoor
    SignalLevel.UNKNOWN -> Color.Gray
}
