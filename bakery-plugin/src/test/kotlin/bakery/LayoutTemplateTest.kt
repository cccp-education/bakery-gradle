package bakery

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class LayoutTemplateTest {

    private val templatesDir = File("src/main/resources/site/templates")

    @Test
    fun `layout-full-width thyme template contains thif guard on layoutType`() {
        val template = templatesDir.resolve("layout-full-width.thyme")
        assertTrue(template.exists(), "layout-full-width.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("th:if"), "must have th:if condition")
        assertTrue(content.contains("FULL_WIDTH"), "th:if must guard on FULL_WIDTH")
        assertTrue(content.contains("layout-full-width"), "must have layout-full-width class")
    }

    @Test
    fun `layout-sidebar-left thyme template contains thif guard on layoutType`() {
        val template = templatesDir.resolve("layout-sidebar-left.thyme")
        assertTrue(template.exists(), "layout-sidebar-left.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("th:if"), "must have th:if condition")
        assertTrue(content.contains("SIDEBAR_LEFT"), "th:if must guard on SIDEBAR_LEFT")
        assertTrue(content.contains("layout-sidebar-left"), "must have layout-sidebar-left class")
    }

    @Test
    fun `layout-sidebar-right thyme template contains thif guard on layoutType`() {
        val template = templatesDir.resolve("layout-sidebar-right.thyme")
        assertTrue(template.exists(), "layout-sidebar-right.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("th:if"), "must have th:if condition")
        assertTrue(content.contains("SIDEBAR_RIGHT"), "th:if must guard on SIDEBAR_RIGHT")
        assertTrue(content.contains("layout-sidebar-right"), "must have layout-sidebar-right class")
    }

    @Test
    fun `layout-centered thyme template contains thif guard on layoutType`() {
        val template = templatesDir.resolve("layout-centered.thyme")
        assertTrue(template.exists(), "layout-centered.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("th:if"), "must have th:if condition")
        assertTrue(content.contains("CENTERED"), "th:if must guard on CENTERED")
        assertTrue(content.contains("layout-centered"), "must have layout-centered class")
    }

    @Test
    fun `post thyme includes layout fragments`() {
        val template = templatesDir.resolve("post.thyme")
        assertTrue(template.exists(), "post.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("layout-full-width"), "post must include layout-full-width fragment")
        assertTrue(content.contains("layout-sidebar-left"), "post must include layout-sidebar-left fragment")
        assertTrue(content.contains("layout-sidebar-right"), "post must include layout-sidebar-right fragment")
        assertTrue(content.contains("layout-centered"), "post must include layout-centered fragment")
    }

    @Test
    fun `page thyme includes layout fragments`() {
        val template = templatesDir.resolve("page.thyme")
        assertTrue(template.exists(), "page.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("layout-full-width"), "page must include layout-full-width fragment")
        assertTrue(content.contains("layout-sidebar-left"), "page must include layout-sidebar-left fragment")
        assertTrue(content.contains("layout-sidebar-right"), "page must include layout-sidebar-right fragment")
        assertTrue(content.contains("layout-centered"), "page must include layout-centered fragment")
    }
}