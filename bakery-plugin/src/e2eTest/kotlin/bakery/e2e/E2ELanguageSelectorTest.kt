package bakery.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("e2e")
@DisplayName("E2E - Language Selector UI")
class E2ELanguageSelectorTest : E2ETestBase() {

    private val supportedLanguages = listOf(
        mapOf("code" to "en", "nativeName" to "English", "rtl" to false),
        mapOf("code" to "zh", "nativeName" to "中文", "rtl" to false),
        mapOf("code" to "hi", "nativeName" to "हिन्दी", "rtl" to false),
        mapOf("code" to "es", "nativeName" to "Español", "rtl" to false),
        mapOf("code" to "fr", "nativeName" to "Français", "rtl" to false),
        mapOf("code" to "ar", "nativeName" to "العربية", "rtl" to true),
        mapOf("code" to "bn", "nativeName" to "বাংলা", "rtl" to false),
        mapOf("code" to "pt", "nativeName" to "Português", "rtl" to false),
        mapOf("code" to "ru", "nativeName" to "Русский", "rtl" to false),
        mapOf("code" to "ur", "nativeName" to "اردو", "rtl" to true),
    )

    @Test
    @DisplayName("Language selector dropdown renders in nav with all 10 languages")
    fun `language selector renders with all supported languages`() {
        val path = serveHtml(
            "blog",
            mapOf(
                "published_posts" to emptyList<Any>(),
                "content" to mapOf("rootpath" to ""),
                "supportedLanguages" to supportedLanguages,
            ),
            language = "fr"
        )

        val page = navigateTo(path)
        val selector = page.locator(".language-switcher-container")

        assertThat(selector.count()).isGreaterThan(0)
        assertThat(selector.locator(".language-dropdown-btn").count()).isGreaterThan(0)

        selector.locator(".language-dropdown-btn").click()

        val items = selector.locator(".dropdown-item")
        assertThat(items.count()).isEqualTo(10)

        val texts = items.allTextContents()
        assertThat(texts).anyMatch { it.contains("English") }
        assertThat(texts).anyMatch { it.contains("中文") }
        assertThat(texts).anyMatch { it.contains("हिन्दी") }
        assertThat(texts).anyMatch { it.contains("Español") }
        assertThat(texts).anyMatch { it.contains("Français") }
        assertThat(texts).anyMatch { it.contains("العربية") }
        assertThat(texts).anyMatch { it.contains("বাংলা") }
        assertThat(texts).anyMatch { it.contains("Português") }
        assertThat(texts).anyMatch { it.contains("Русский") }
        assertThat(texts).anyMatch { it.contains("اردو") }
    }

    @Test
    @DisplayName("Active language is highlighted with active class")
    fun `active language shows active class`() {
        val path = serveHtml(
            "blog",
            mapOf(
                "published_posts" to emptyList<Any>(),
                "content" to mapOf("rootpath" to ""),
                "supportedLanguages" to supportedLanguages,
            ),
            language = "fr"
        )

        val page = navigateTo(path)
        val selector = page.locator(".language-switcher-container")
        selector.locator(".language-dropdown-btn").click()

        val activeItem = selector.locator(".dropdown-item.active")
        assertThat(activeItem.count()).isEqualTo(1)
        assertThat(activeItem.textContent()).contains("Français")
    }

    @Test
    @DisplayName("Current language shows native name in trigger button")
    fun `trigger button shows current language native name`() {
        val path = serveHtml(
            "blog",
            mapOf(
                "published_posts" to emptyList<Any>(),
                "content" to mapOf("rootpath" to ""),
                "supportedLanguages" to supportedLanguages,
            ),
            language = "en"
        )

        val page = navigateTo(path)
        val button = page.locator(".language-dropdown-btn")

        assertThat(button.textContent()).contains("English")
    }

    @Test
    @DisplayName("Arabic trigger button shows Arabic native name")
    fun `arabic trigger shows Arabic native name`() {
        val path = serveHtml(
            "blog",
            mapOf(
                "published_posts" to emptyList<Any>(),
                "content" to mapOf("rootpath" to ""),
                "supportedLanguages" to supportedLanguages,
            ),
            language = "ar"
        )

        val page = navigateTo(path)
        val button = page.locator(".language-dropdown-btn")

        assertThat(button.textContent()).contains("العربية")
    }

    @Test
    @DisplayName("RTL languages have rtl indicator in dropdown")
    fun `rtl languages show rtl badge`() {
        val path = serveHtml(
            "blog",
            mapOf(
                "published_posts" to emptyList<Any>(),
                "content" to mapOf("rootpath" to ""),
                "supportedLanguages" to supportedLanguages,
            ),
            language = "fr"
        )

        val page = navigateTo(path)
        val selector = page.locator(".language-switcher-container")
        selector.locator(".language-dropdown-btn").click()

        val arItem = selector.locator(".dropdown-item").all().first { it.textContent().contains("العربية") }
        assertThat(arItem.locator(".rtl-badge").count()).isGreaterThan(0)
    }

    @Test
    @DisplayName("Language selector has aria-label for accessibility")
    fun `language selector has aria-label`() {
        val path = serveHtml(
            "blog",
            mapOf(
                "published_posts" to emptyList<Any>(),
                "content" to mapOf("rootpath" to ""),
                "supportedLanguages" to supportedLanguages,
            ),
            language = "fr"
        )

        val page = navigateTo(path)
        val button = page.locator(".language-dropdown-btn")

        assertThat(button.getAttribute("aria-label")).isNotNull
    }

    @Test
    @DisplayName("Language links use code as data attribute")
    fun `language links have data-lang attribute`() {
        val path = serveHtml(
            "blog",
            mapOf(
                "published_posts" to emptyList<Any>(),
                "content" to mapOf("rootpath" to ""),
                "supportedLanguages" to supportedLanguages,
            ),
            language = "fr"
        )

        val page = navigateTo(path)
        val selector = page.locator(".language-switcher-container")
        selector.locator(".language-dropdown-btn").click()

        val items = selector.locator(".dropdown-item[data-lang]")
        assertThat(items.count()).isEqualTo(10)

        assertThat(selector.locator(".dropdown-item[data-lang=\"fr\"]").count()).isGreaterThan(0)
        assertThat(selector.locator(".dropdown-item[data-lang=\"ar\"]").count()).isGreaterThan(0)
        assertThat(selector.locator(".dropdown-item[data-lang=\"zh\"]").count()).isGreaterThan(0)
    }
}
