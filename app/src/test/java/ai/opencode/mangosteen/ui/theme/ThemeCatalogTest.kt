package ai.opencode.mangosteen.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeCatalogTest {

    @Test
    fun mangosteenIsTheDefaultTheme() {
        assertEquals("mangosteen", ThemeCatalog.DEFAULT_ID)
        assertEquals("Mangosteen", ThemeCatalog.default().name)
    }

    @Test
    fun everyThemeHasAUniqueId() {
        val ids = ThemeCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.containsAll(listOf("mangosteen", "amoled", "ember", "porcelain", "fruit", "crimson", "moss")))
    }

    @Test
    fun amoledUsesPureBlackAndKeepsMangosteenAccents() {
        val amoled = ThemeCatalog.byId("amoled")
        val mangosteen = ThemeCatalog.byId("mangosteen")
        assertEquals(Color.Black, amoled.background)
        assertTrue(amoled.isDark)
        assertEquals(mangosteen.primary, amoled.primary)
        assertEquals(mangosteen.secondary, amoled.secondary)
        assertEquals(mangosteen.tertiary, amoled.tertiary)
    }

    @Test
    fun unknownThemeIdFallsBackToTheDefault() {
        assertEquals(ThemeCatalog.default(), ThemeCatalog.byId("does-not-exist"))
        assertEquals(ThemeCatalog.default(), ThemeCatalog.byId(null))
    }

    @Test
    fun catalogCoversBothDarkAndLightThemes() {
        assertTrue(ThemeCatalog.all.any { it.isDark })
        assertTrue(ThemeCatalog.all.any { !it.isDark })
    }

    @Test
    fun mossIsTheDarkPorcelainVariant() {
        val moss = ThemeCatalog.byId("moss")
        val porcelain = ThemeCatalog.byId("porcelain")
        assertTrue(moss.isDark)
        assertTrue(!porcelain.isDark)
    }

    @Test
    fun everyThemeExposesFourSwatchColors() {
        ThemeCatalog.all.forEach { theme ->
            assertEquals(4, theme.swatches.size)
            assertEquals(theme.swatches.distinct().size, theme.swatches.size)
        }
    }

    @Test
    fun everyThemeHasFullyOpaqueColors() {
        ThemeCatalog.all.forEach { theme ->
            theme.allColors.forEach { color ->
                assertEquals(1f, color.alpha, 0.001f)
            }
        }
    }

    @Test
    fun colorSchemeUsesTheThemePrimary() {
        val ember = ThemeCatalog.byId("ember")
        val scheme = ember.scheme()
        assertEquals(ember.primary, scheme.primary)
        assertEquals(ember.primaryContainer, scheme.primaryContainer)
        assertEquals(ember.background, scheme.background)
        assertNotEquals(ember.background, ember.surface)
    }
}
