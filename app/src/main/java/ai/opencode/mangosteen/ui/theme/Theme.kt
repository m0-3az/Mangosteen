package ai.opencode.mangosteen.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Dark Mangosteen palette. */
object MangosteenColors {
    val Background = Color(0xFF1A1020)
    val SurfaceLowest = Color(0xFF120A17)
    val SurfaceLow = Color(0xFF24172C)
    val Surface = Color(0xFF2C1D35)
    val SurfaceHigh = Color(0xFF35243F)
    val SurfaceHighest = Color(0xFF3F2C4A)

    val Primary = Color(0xFFD34B84)
    val OnPrimary = Color(0xFF2B0A1A)
    val Purple = Color(0xFF6A2C91)
    val OnPurple = Color(0xFFF5EAF9)

    val Yellow = Color(0xFFFFC300)
    val OnYellow = Color(0xFF3A2A00)
    val YellowContainer = Color(0xFF5C4600)
    val OnYellowContainer = Color(0xFFFFE7A3)

    val Teal = Color(0xFF4ECDC4)
    val OnTeal = Color(0xFF003733)

    val TextPrimary = Color(0xFFE2E2E2)
    val TextSecondary = Color(0xFFC9B8D1)
    val Outline = Color(0xFF9B87A5)
    val OutlineVariant = Color(0xFF4E3B59)

    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)
    val ErrorContainer = Color(0xFF93000A)
    val OnErrorContainer = Color(0xFFFFDAD6)

    val CodeBg = Color(0xFF35243F)
    val Link = Teal
}

private val Scheme = darkColorScheme(
    primary = MangosteenColors.Primary,
    onPrimary = MangosteenColors.OnPrimary,
    primaryContainer = MangosteenColors.Purple,
    onPrimaryContainer = MangosteenColors.OnPurple,
    secondary = MangosteenColors.Yellow,
    onSecondary = MangosteenColors.OnYellow,
    secondaryContainer = MangosteenColors.YellowContainer,
    onSecondaryContainer = MangosteenColors.OnYellowContainer,
    tertiary = MangosteenColors.Teal,
    onTertiary = MangosteenColors.OnTeal,
    tertiaryContainer = MangosteenColors.Teal,
    onTertiaryContainer = MangosteenColors.OnTeal,
    background = MangosteenColors.Background,
    onBackground = MangosteenColors.TextPrimary,
    surface = MangosteenColors.Background,
    onSurface = MangosteenColors.TextPrimary,
    surfaceVariant = MangosteenColors.SurfaceHigh,
    onSurfaceVariant = MangosteenColors.TextSecondary,
    surfaceTint = MangosteenColors.Primary,
    surfaceContainerLowest = MangosteenColors.SurfaceLowest,
    surfaceContainerLow = MangosteenColors.SurfaceLow,
    surfaceContainer = MangosteenColors.Surface,
    surfaceContainerHigh = MangosteenColors.SurfaceHigh,
    surfaceContainerHighest = MangosteenColors.SurfaceHighest,
    outline = MangosteenColors.Outline,
    outlineVariant = MangosteenColors.OutlineVariant,
    error = MangosteenColors.Error,
    onError = MangosteenColors.OnError,
    errorContainer = MangosteenColors.ErrorContainer,
    onErrorContainer = MangosteenColors.OnErrorContainer,
    inverseSurface = MangosteenColors.TextPrimary,
    inverseOnSurface = MangosteenColors.Surface,
    inversePrimary = MangosteenColors.Primary,
    scrim = Color.Black,
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MangosteenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
