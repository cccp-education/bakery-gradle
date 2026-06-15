package bakery.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("e2e")
@DisplayName("E2E - i18n Language Support")
class E2EI18nTest : E2ETestBase() {

    @Test
    @DisplayName("i18n: English site renders with lang=en, dir=ltr, and English messages")
    fun `english site renders with correct lang dir and messages`() {
        val path = serveHtml(
            "blog",
            mapOf(
                "published_posts" to emptyList<Any>(),
                "content" to mapOf("rootpath" to "")
            ),
            language = "en"
        )

        val page = navigateTo(path)

        assertThat(page.locator("html").getAttribute("lang")).isEqualTo("en")
        assertThat(page.locator("html").getAttribute("dir")).isEqualTo("ltr")

        assertThat(page.locator("h1").textContent()).contains("Blog")
        assertThat(page.locator("a.nav-link").all().any { it.textContent().contains("Home") }).isTrue()
        assertThat(page.locator("a.nav-link").all().any { it.textContent().contains("Contact") }).isTrue()
    }

    @Test
    @DisplayName("i18n: Arabic site renders with lang=ar, dir=rtl, and Arabic messages")
    fun `arabic site renders with correct lang dir and arabic messages`() {
        val path = serveHtml(
            "blog",
            mapOf(
                "published_posts" to emptyList<Any>(),
                "content" to mapOf("rootpath" to "")
            ),
            language = "ar"
        )

        val page = navigateTo(path)

        assertThat(page.locator("html").getAttribute("lang")).isEqualTo("ar")
        assertThat(page.locator("html").getAttribute("dir")).isEqualTo("rtl")

        assertThat(page.locator("h1").textContent()).contains("المدونة")
        assertThat(page.locator("a.nav-link").all().any { it.textContent().contains("الرئيسية") }).isTrue()
        assertThat(page.locator("a.nav-link").all().any { it.textContent().contains("اتصل بنا") }).isTrue()
    }
}
