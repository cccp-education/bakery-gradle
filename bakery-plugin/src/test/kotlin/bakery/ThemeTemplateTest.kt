package bakery

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class ThemeTemplateTest {

    private val templatesDir = File("src/main/resources/site/templates")

    @Test
    fun `theme-script thyme template contains css variables with guard`() {
        val template = templatesDir.resolve("theme-script.thyme")
        assertTrue(template.exists(), "theme-script.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("theme-script"), "must have theme-script fragment")
        assertTrue(content.contains("th:if"), "must have th:if condition to hide when theme absent")
        assertTrue(content.contains("themePrimaryColor"), "th:if must guard on themePrimaryColor")
        assertTrue(content.contains("--bakery-primary"), "must define --bakery-primary CSS variable")
        assertTrue(content.contains("--bakery-secondary"), "must define --bakery-secondary CSS variable")
    }

    @Test
    fun `theme-script thyme template renders nothing when themePrimaryColor is absent`() {
        val template = templatesDir.resolve("theme-script.thyme")
        assertTrue(template.exists(), "theme-script.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("th:if"), "must have th:if condition")
        assertTrue(
            content.contains("themePrimaryColor") && content.contains("th:if"),
            "th:if must guard on themePrimaryColor so fragment renders nothing when absent"
        )
    }

    @Test
    fun `theme-script thyme template uses themeFontFamily with conditional guard`() {
        val template = templatesDir.resolve("theme-script.thyme")
        assertTrue(template.exists(), "theme-script.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("themeFontFamily"), "must use themeFontFamily for font-family")
        assertTrue(content.contains("font-family"), "must set font-family CSS property")
    }

    @Test
    fun `header thyme includes theme-script fragment`() {
        val template = templatesDir.resolve("header.thyme")
        assertTrue(template.exists(), "header.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("theme-script"), "header must include theme-script fragment")
    }

    @Test
    fun `header thyme contains conditional custom favicon`() {
        val template = templatesDir.resolve("header.thyme")
        assertTrue(template.exists(), "header.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("themeFaviconUrl"), "header must reference themeFaviconUrl for custom favicon")
        assertTrue(content.contains("th:if"), "favicon must be conditional (th:if/th:unless)")
    }

    @Test
    fun `menu thyme contains conditional logo in navbar-brand`() {
        val template = templatesDir.resolve("menu.thyme")
        assertTrue(template.exists(), "menu.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("themeLogoUrl"), "navbar-brand must reference themeLogoUrl for custom logo")
        assertTrue(content.contains("th:if"), "logo must be conditional (th:if)")
    }
}