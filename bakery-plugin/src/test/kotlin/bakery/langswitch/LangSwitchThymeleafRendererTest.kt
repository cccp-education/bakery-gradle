package bakery.langswitch

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.assertThrows

class LangSwitchThymeleafRendererTest {

    private val labels = mapOf(
        "fr" to "Fran\u00e7ais",
        "en" to "English",
        "ar" to "\u0627\u0644\u0639\u0631\u0628\u064a\u0629"
    )

    private fun links(
        supported: List<String>,
        default: String,
        current: String,
        path: String
    ): List<LangSwitchUrl> =
        LangSwitchMenu(supported, default, current, path).generateLinks()

    @Test
    fun `render 2 languages FR default from FR root`() {
        val renderer = LangSwitchThymeleafRenderer(labels)
        val rendered = renderer.render(links(listOf("fr", "en"), "fr", "fr", ""))
        assertTrue(rendered.contains("data-lang=\"fr\""), "missing fr data-lang")
        assertTrue(rendered.contains("data-lang=\"en\""), "missing en data-lang")
        assertTrue(rendered.contains("Fran\u00e7ais"), "missing fr label")
        assertTrue(rendered.contains("English"), "missing en label")
    }

    @Test
    fun `render 10 languages from EN subdir`() {
        val tenLabels = labels + mapOf(
            "zh" to "\u4e2d\u6587", "hi" to "\u0939\u093f\u0928\u094d\u0926\u0940",
            "es" to "Espa\u00f1ol", "bn" to "\u09ac\u09be\u0982\u09b2\u09be",
            "pt" to "Portugu\u00eas", "ru" to "\u0420\u0443\u0441\u0441\u043a\u0438\u0439",
            "ur" to "\u0627\u0631\u062f\u0648"
        )
        val supported = listOf("fr", "en", "ar", "zh", "hi", "es", "bn", "pt", "ru", "ur")
        val renderer = LangSwitchThymeleafRenderer(tenLabels)
        val rendered = renderer.render(links(supported, "fr", "en", "en/"))
        supported.forEach { lang ->
            assertTrue(rendered.contains("data-lang=\"$lang\""), "missing $lang in output")
        }
    }

    @Test
    fun `active class on current language`() {
        val renderer = LangSwitchThymeleafRenderer(labels)
        val rendered = renderer.render(links(listOf("fr", "en"), "fr", "fr", ""))
        val frAnchor = rendered.substringBefore("data-lang=\"fr\"").substringAfterLast("<a ")
        val enAnchor = rendered.substringBefore("data-lang=\"en\"").substringAfterLast("<a ")
        assertTrue(frAnchor.contains("active"), "fr (current) should have active class: $frAnchor")
        assertFalse(enAnchor.contains("active"), "en (non-current) should not have active class: $enAnchor")
    }

    @Test
    fun `th href contains resolved URL for FR root to EN subdir`() {
        val renderer = LangSwitchThymeleafRenderer(labels)
        val rendered = renderer.render(links(listOf("fr", "en"), "fr", "fr", ""))
        assertTrue(
            rendered.contains("th:href=\"'en/index.html'\""),
            "EN link from FR root should be en/index.html, got: $rendered"
        )
    }

    @Test
    fun `th href contains resolved URL for EN subdir to FR root`() {
        val renderer = LangSwitchThymeleafRenderer(labels)
        val rendered = renderer.render(links(listOf("fr", "en"), "fr", "en", "en/"))
        assertTrue(
            rendered.contains("th:href=\"'../index.html'\""),
            "FR link from EN subdir should be ../index.html, got: $rendered"
        )
    }

    @Test
    fun `th href for self-link is index html`() {
        val renderer = LangSwitchThymeleafRenderer(labels)
        val rendered = renderer.render(links(listOf("fr", "en"), "fr", "en", "en/"))
        assertTrue(
            rendered.contains("th:href=\"'index.html'\""),
            "EN self-link from EN subdir should be index.html, got: $rendered"
        )
    }

    @Test
    fun `th href for EN subdir to AR subdir`() {
        val renderer = LangSwitchThymeleafRenderer(labels)
        val rendered = renderer.render(links(listOf("fr", "en", "ar"), "fr", "en", "en/"))
        assertTrue(
            rendered.contains("th:href=\"'../ar/index.html'\""),
            "AR link from EN subdir should be ../ar/index.html, got: $rendered"
        )
    }

    @Test
    fun `no self-loop in rendered output from EN subdir`() {
        val renderer = LangSwitchThymeleafRenderer(labels)
        val rendered = renderer.render(links(listOf("fr", "en"), "fr", "en", "en/"))
        val enItem = rendered.substringAfter("data-lang=\"en\"").substringBefore("</a>")
        assertFalse(
            enItem.contains("en/index.html"),
            "EN self-link should not contain en/index.html (self-loop), got: $enItem"
        )
    }

    @Test
    fun `render empty links throws`() {
        val renderer = LangSwitchThymeleafRenderer(labels)
        assertThrows<IllegalArgumentException> {
            renderer.render(emptyList())
        }
    }

    @Test
    fun `each li has dropdown-item lang-option classes`() {
        val renderer = LangSwitchThymeleafRenderer(labels)
        val rendered = renderer.render(links(listOf("fr", "en"), "fr", "fr", ""))
        assertTrue(
            rendered.contains("dropdown-item lang-option"),
            "output should contain dropdown-item lang-option classes"
        )
    }

    @Test
    fun `render contains ul dropdown-menu wrapper`() {
        val renderer = LangSwitchThymeleafRenderer(labels)
        val rendered = renderer.render(links(listOf("fr", "en"), "fr", "fr", ""))
        assertTrue(rendered.contains("<ul"), "output should contain ul wrapper")
        assertTrue(rendered.contains("dropdown-menu"), "ul should have dropdown-menu class")
    }

    @Test
    fun `unknown language code falls back to code itself`() {
        val renderer = LangSwitchThymeleafRenderer(labels)
        val rendered = renderer.render(links(listOf("fr", "xx"), "fr", "fr", ""))
        assertTrue(rendered.contains("xx"), "unknown lang should appear as code")
    }
}