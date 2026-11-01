package ai.opencode.mangosteen.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import ai.opencode.mangosteen.settings.SecurePrefs

/** A full Mangosteen color theme: every color the UI reads. */
data class MangosteenThemeSpec(
    val id: String,
    val name: String,
    val tagline: String,
    val isDark: Boolean,
    val background: Color,
    val surfaceLowest: Color,
    val surfaceLow: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val codeBg: Color,
    val link: Color,
) {
    /** Compact preview shown in the theme picker. */
    val swatches: List<Color> get() = listOf(background, primary, secondary, tertiary)

    val allColors: List<Color> get() = listOf(
        background, surfaceLowest, surfaceLow, surface, surfaceHigh, surfaceHighest,
        primary, onPrimary, primaryContainer, onPrimaryContainer,
        secondary, onSecondary, secondaryContainer, onSecondaryContainer,
        tertiary, onTertiary, textPrimary, textSecondary, outline, outlineVariant,
        error, onError, errorContainer, onErrorContainer, codeBg, link,
    )

    fun scheme(): ColorScheme {
        val builder = if (isDark) darkColorScheme(
            background = background,
            surface = surfaceLow,
            surfaceTint = primary,
        ) else lightColorScheme(
            background = background,
            surface = surfaceLow,
            surfaceTint = primary,
        )
        return builder.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            onBackground = textPrimary,
            onSurface = textPrimary,
            onSurfaceVariant = textSecondary,
            outline = outline,
            outlineVariant = outlineVariant,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            surfaceVariant = surfaceHigh,
            surfaceContainerLowest = surfaceLowest,
            surfaceContainerLow = surfaceLow,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceHigh,
            surfaceContainerHighest = surfaceHighest,
            inverseSurface = textPrimary,
            inverseOnSurface = surface,
            inversePrimary = primary,
        )
    }
}

/** Every theme shipped with the app; Mangosteen stays first/default. */
object ThemeCatalog {
    const val DEFAULT_ID = "mangosteen"

    val all: List<MangosteenThemeSpec> = listOf(
        MangosteenThemeSpec(
            id = "mangosteen",
            name = "Mangosteen",
            tagline = "Dark · deep purple & pink",
            isDark = true,
            background = Color(0xFF1A1020),
            surfaceLowest = Color(0xFF120A17),
            surfaceLow = Color(0xFF24172C),
            surface = Color(0xFF2C1D35),
            surfaceHigh = Color(0xFF35243F),
            surfaceHighest = Color(0xFF3F2C4A),
            primary = Color(0xFFD34B84),
            onPrimary = Color(0xFF1A0510),
            primaryContainer = Color(0xFF6A2C91),
            onPrimaryContainer = Color(0xFFF5EAF9),
            secondary = Color(0xFFFFC300),
            onSecondary = Color(0xFF3A2A00),
            secondaryContainer = Color(0xFF5C4600),
            onSecondaryContainer = Color(0xFFFFE7A3),
            tertiary = Color(0xFF4ECDC4),
            onTertiary = Color(0xFF003733),
            textPrimary = Color(0xFFE2E2E2),
            textSecondary = Color(0xFFC9B8D1),
            outline = Color(0xFF9B87A5),
            outlineVariant = Color(0xFF4E3B59),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            codeBg = Color(0xFF35243F),
            link = Color(0xFF4ECDC4),
        ),
        MangosteenThemeSpec(
            id = "amoled",
            name = "AMOLED",
            tagline = "Black · true black & vivid accents",
            isDark = true,
            background = Color(0xFF000000),
            surfaceLowest = Color(0xFF000000),
            surfaceLow = Color(0xFF050505),
            surface = Color(0xFF0A0A0A),
            surfaceHigh = Color(0xFF141414),
            surfaceHighest = Color(0xFF1E1E1E),
            primary = Color(0xFFD34B84),
            onPrimary = Color(0xFF1A0510),
            primaryContainer = Color(0xFF6A2C91),
            onPrimaryContainer = Color(0xFFF5EAF9),
            secondary = Color(0xFFFFC300),
            onSecondary = Color(0xFF3A2A00),
            secondaryContainer = Color(0xFF5C4600),
            onSecondaryContainer = Color(0xFFFFE7A3),
            tertiary = Color(0xFF4ECDC4),
            onTertiary = Color(0xFF003733),
            textPrimary = Color(0xFFF2F2F2),
            textSecondary = Color(0xFFBDBDBD),
            outline = Color(0xFF8A8A8A),
            outlineVariant = Color(0xFF262626),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            codeBg = Color(0xFF141414),
            link = Color(0xFF4ECDC4),
        ),
        MangosteenThemeSpec(
            id = "ember",
            name = "Ember",
            tagline = "Dark · warm amber & peach",
            isDark = true,
            background = Color(0xFF241B17),
            surfaceLowest = Color(0xFF1B1411),
            surfaceLow = Color(0xFF2E231E),
            surface = Color(0xFF362A24),
            surfaceHigh = Color(0xFF41322A),
            surfaceHighest = Color(0xFF4C3A30),
            primary = Color(0xFFFFB77A),
            onPrimary = Color(0xFF3A2010),
            primaryContainer = Color(0xFF6B3D12),
            onPrimaryContainer = Color(0xFFF4E8DF),
            secondary = Color(0xFFE7C6AD),
            onSecondary = Color(0xFF3A2010),
            secondaryContainer = Color(0xFF6E5340),
            onSecondaryContainer = Color(0xFFFFEBDD),
            tertiary = Color(0xFFA9D3C5),
            onTertiary = Color(0xFF1E3B33),
            textPrimary = Color(0xFFF4E8DF),
            textSecondary = Color(0xFFA08D7E),
            outline = Color(0xFF7A6559),
            outlineVariant = Color(0xFF4A3830),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            codeBg = Color(0xFF41322A),
            link = Color(0xFFA9D3C5),
        ),
        MangosteenThemeSpec(
            id = "porcelain",
            name = "Porcelain",
            tagline = "Light · cream & moss",
            isDark = false,
            background = Color(0xFFF7F6EE),
            surfaceLowest = Color(0xFFFFFFFF),
            surfaceLow = Color(0xFFF1F0E8),
            surface = Color(0xFFF1F0E8),
            surfaceHigh = Color(0xFFE5E4DC),
            surfaceHighest = Color(0xFFD8D8CE),
            primary = Color(0xFF4F6700),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD7F5A5),
            onPrimaryContainer = Color(0xFF1B1D17),
            secondary = Color(0xFF5B624F),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFDDDCD2),
            onSecondaryContainer = Color(0xFF2A2D24),
            tertiary = Color(0xFF3D655E),
            onTertiary = Color(0xFFFFFFFF),
            textPrimary = Color(0xFF1B1D17),
            textSecondary = Color(0xFF5F6357),
            outline = Color(0xFF9A9C92),
            outlineVariant = Color(0xFFDAD9CF),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            codeBg = Color(0xFFE5E4DC),
            link = Color(0xFF3D655E),
        ),
        MangosteenThemeSpec(
            id = "fruit",
            name = "Fruit",
            tagline = "Light · pink, purple & teal",
            isDark = false,
            background = Color(0xFFE2E2E2),
            surfaceLowest = Color(0xFFF7F7F7),
            surfaceLow = Color(0xFFF4F4F4),
            surface = Color(0xFFF4F4F4),
            surfaceHigh = Color(0xFFE7E7E7),
            surfaceHighest = Color(0xFFD6D6D6),
            primary = Color(0xFF6A2C91),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD34B84),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFFFFC300),
            onSecondary = Color(0xFF3A2A00),
            secondaryContainer = Color(0xFFFEE8AB),
            onSecondaryContainer = Color(0xFF3A2A00),
            tertiary = Color(0xFF4ECDC4),
            onTertiary = Color(0xFF003733),
            textPrimary = Color(0xFF24152B),
            textSecondary = Color(0xFF5F5E60),
            outline = Color(0xFF8A8890),
            outlineVariant = Color(0xFFD3D1D8),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            codeBg = Color(0xFFE7E7E7),
            link = Color(0xFF6A2C91),
        ),
        MangosteenThemeSpec(
            id = "crimson",
            name = "Crimson",
            tagline = "Dark · crimson & blush",
            isDark = true,
            background = Color(0xFF211617),
            surfaceLowest = Color(0xFF1A1011),
            surfaceLow = Color(0xFF2A1C1D),
            surface = Color(0xFF2A1C1D),
            surfaceHigh = Color(0xFF3E2C2D),
            surfaceHighest = Color(0xFF4A3638),
            primary = Color(0xFFFFB4AB),
            onPrimary = Color(0xFF4A100E),
            primaryContainer = Color(0xFF93000A),
            onPrimaryContainer = Color(0xFFFFDAD6),
            secondary = Color(0xFFE8C48C),
            onSecondary = Color(0xFF3A2010),
            secondaryContainer = Color(0xFF544200),
            onSecondaryContainer = Color(0xFFFFE0A3),
            tertiary = Color(0xFFE7BDB8),
            onTertiary = Color(0xFF4A100E),
            textPrimary = Color(0xFFF5DEDC),
            textSecondary = Color(0xFFB09895),
            outline = Color(0xFF8A7472),
            outlineVariant = Color(0xFF4A3638),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            codeBg = Color(0xFF3E2C2D),
            link = Color(0xFFE7BDB8),
        ),
        MangosteenThemeSpec(
            id = "moss",
            name = "Moss",
            tagline = "Dark · deep forest green",
            isDark = true,
            background = Color(0xFF14180F),
            surfaceLowest = Color(0xFF10140C),
            surfaceLow = Color(0xFF1C2118),
            surface = Color(0xFF1C2118),
            surfaceHigh = Color(0xFF28301F),
            surfaceHighest = Color(0xFF33402A),
            primary = Color(0xFFA6C94A),
            onPrimary = Color(0xFF1B2109),
            primaryContainer = Color(0xFF3E5220),
            onPrimaryContainer = Color(0xFFE4F2C4),
            secondary = Color(0xFFB7C6A8),
            onSecondary = Color(0xFF1B2109),
            secondaryContainer = Color(0xFF33402A),
            onSecondaryContainer = Color(0xFFE4F2C4),
            tertiary = Color(0xFF6FBFA8),
            onTertiary = Color(0xFF0E2B24),
            textPrimary = Color(0xFFEDEFE4),
            textSecondary = Color(0xFFA3AC9B),
            outline = Color(0xFF7C866F),
            outlineVariant = Color(0xFF3A4132),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            codeBg = Color(0xFF28301F),
            link = Color(0xFF6FBFA8),
        ),
    )

    fun default(): MangosteenThemeSpec = all.first()

    fun byId(id: String?): MangosteenThemeSpec =
        all.firstOrNull { it.id == id } ?: default()
}

/** Holds the app-wide selected theme and persists it. */
object ThemeStore {
    var themeId: String by androidx.compose.runtime.mutableStateOf(ThemeCatalog.DEFAULT_ID)
        private set

    fun load(context: android.content.Context) {
        themeId = ThemeCatalog.byId(SecurePrefs(context).themeId()).id
    }

    fun set(context: android.content.Context, id: String) {
        val theme = ThemeCatalog.byId(id)
        SecurePrefs(context).setThemeId(theme.id)
        themeId = theme.id
    }
}

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

/**
 * Applies the selected theme's background to the activity window so the gap
 * before Compose draws matches the theme instead of the old purple launch colour.
 */
fun applyWindowBackground(activity: Activity, theme: MangosteenThemeSpec) {
    activity.window.setBackgroundDrawable(ColorDrawable(theme.background.toArgb()))
}

/**
 * App theme. Reads [ThemeStore] so a theme change in Settings recomposes
 * every screen immediately.
 */
@Composable
fun MangosteenTheme(content: @Composable () -> Unit) {
    val theme = ThemeCatalog.byId(ThemeStore.themeId)
    val view = LocalView.current

    LaunchedEffect(theme.id) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        window.setBackgroundDrawable(ColorDrawable(theme.background.toArgb()))
    }

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !theme.isDark
            isAppearanceLightNavigationBars = !theme.isDark
        }
    }

    MaterialTheme(
        colorScheme = theme.scheme(),
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}


