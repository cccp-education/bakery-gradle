package bakery.tree

import bakery.LayoutType
import bakery.ThemeConfig
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OutputConfigTest {

    @Test
    fun `empty output config has all null fields`() {
        val config = OutputConfig()
        assertNull(config.template)
        assertNull(config.layout)
        assertNull(config.cssFiles)
        assertNull(config.jsFiles)
        assertNull(config.theme)
    }

    @Test
    fun `output config with template override`() {
        val config = OutputConfig(template = "custom-page")
        assertEquals("custom-page", config.template)
    }

    @Test
    fun `output config with all fields`() {
        val theme = ThemeConfig(mode = "dark", primaryColor = "#ff0000")
        val config = OutputConfig(
            template = "full-width",
            layout = LayoutType.CENTERED,
            cssFiles = listOf("custom.css", "print.css"),
            jsFiles = listOf("analytics.js"),
            theme = theme
        )
        assertEquals("full-width", config.template)
        assertEquals(LayoutType.CENTERED, config.layout)
        assertEquals(listOf("custom.css", "print.css"), config.cssFiles)
        assertEquals(listOf("analytics.js"), config.jsFiles)
        assertEquals(theme, config.theme)
    }

    @Test
    fun `structural equality holds for same fields`() {
        val a = OutputConfig(template = "tpl", cssFiles = listOf("a.css"))
        val b = OutputConfig(template = "tpl", cssFiles = listOf("a.css"))
        assertEquals(a, b)
    }

    @Test
    fun `copy produces new instance with overridden field`() {
        val original = OutputConfig(template = "page", layout = LayoutType.FULL_WIDTH)
        val copy = original.copy(layout = LayoutType.SIDEBAR_LEFT)
        assertEquals("page", copy.template)
        assertEquals(LayoutType.SIDEBAR_LEFT, copy.layout)
    }
}
