package bakery.langswitch

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse

class LangSwitchInjectorTest {

    private val labels = mapOf("fr" to "Fran\u00e7ais", "en" to "English")

    private val menuThyme = """
        <nav class="navbar">
            <div class="container">
                <a class="navbar-brand" href="#">Fixture</a>
                <!-- LANG-SWITCHER: bakery will inject the language switcher here -->
                <div class="lang-switcher-container">
                </div>
            </div>
        </nav>
    """.trimIndent()

    private fun render(supported: List<String>, default: String, current: String, path: String): String {
        val links = LangSwitchMenu(supported, default, current, path).generateLinks()
        return LangSwitchThymeleafRenderer(labels).render(links)
    }

    @Test
    fun `inject replaces empty lang-switcher-container with rendered fragment`() {
        val injector = LangSwitchInjector()
        val fragment = render(listOf("fr", "en"), "fr", "fr", "")
        val result = injector.inject(menuThyme, fragment)
        assertTrue(result.contains("dropdown-menu"), "injected output should contain dropdown-menu")
        assertTrue(result.contains("data-lang=\"fr\""), "injected output should contain fr")
        assertTrue(result.contains("data-lang=\"en\""), "injected output should contain en")
    }

    @Test
    fun `inject preserves the lang-switcher-container opening div`() {
        val injector = LangSwitchInjector()
        val fragment = render(listOf("fr", "en"), "fr", "fr", "")
        val result = injector.inject(menuThyme, fragment)
        assertTrue(
            result.contains("lang-switcher-container"),
            "the lang-switcher-container div should still be present"
        )
    }

    @Test
    fun `inject replaces existing content inside lang-switcher-container`() {
        val injector = LangSwitchInjector()
        val menuWithExisting = """
            <nav>
                <div class="lang-switcher-container">
                    <p>old content</p>
                </div>
            </nav>
        """.trimIndent()
        val fragment = render(listOf("fr", "en"), "fr", "fr", "")
        val result = injector.inject(menuWithExisting, fragment)
        assertFalse(result.contains("old content"), "old content should be replaced")
        assertTrue(result.contains("dropdown-menu"), "new fragment should be present")
    }

    @Test
    fun `inject is idempotent`() {
        val injector = LangSwitchInjector()
        val fragment = render(listOf("fr", "en"), "fr", "fr", "")
        val first = injector.inject(menuThyme, fragment)
        val second = injector.inject(first, fragment)
        assertEquals(first, second, "inject should be idempotent")
    }

    @Test
    fun `inject handles no lang-switcher-container gracefully`() {
        val injector = LangSwitchInjector()
        val menuWithoutContainer = "<nav><p>no container</p></nav>"
        val fragment = render(listOf("fr", "en"), "fr", "fr", "")
        val result = injector.inject(menuWithoutContainer, fragment)
        assertEquals(menuWithoutContainer, result, "output should be unchanged if no container")
    }
}