package bakery.langswitch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * BKY-LANG-NAV-2 — [LangSwitchUrl] exposes the page-aware `th:href` of the
 * single same-page rule. The language-root [LangSwitchUrl.resolve] is kept as
 * the degradation target; the emitted link is page-aware (D4).
 */
class LangSwitchUrlThymeleafHrefTest {
    private fun url(
        target: String,
        current: String,
        default: String = "fr",
    ) = LangSwitchUrl(
        targetLanguage = target,
        currentLanguage = current,
        currentPath = if (current == default) "" else "$current/",
        defaultLanguage = default,
    )

    @Test
    fun `default to non-default is page-aware under the language dir`() {
        assertEquals(
            "\${content.uri != null ? content.rootpath + 'en/' + content.uri : content.rootpath + 'en/index.html'}",
            url("en", "fr").thymeleafHref(),
        )
    }

    @Test
    fun `non-default to default is page-aware exiting the language tree`() {
        assertEquals(
            "\${content.uri != null ? '../' + content.rootpath + '' + content.uri : '../' + content.rootpath + 'index.html'}",
            url("fr", "en").thymeleafHref(),
        )
    }

    @Test
    fun `non-default to another non-default is page-aware`() {
        assertEquals(
            "\${content.uri != null ? '../' + content.rootpath + 'ar/' + content.uri : '../' + content.rootpath + 'ar/index.html'}",
            url("ar", "en").thymeleafHref(),
        )
    }

    @Test
    fun `self link points at the current page not the language root`() {
        val self = url("en", "en")
        assertTrue(self.isSelfLink())
        assertEquals(
            "\${content.uri != null ? content.uri.substring(content.uri.lastIndexOf('/') + 1) : content.rootpath + 'index.html'}",
            self.thymeleafHref(),
        )
    }

    @Test
    fun `the emitted href is page-aware and every index html is null-guarded`() {
        val hrefs = listOf(url("en", "fr"), url("fr", "en"), url("ar", "en")).map { it.thymeleafHref() }
        hrefs.forEach { href ->
            assertTrue(href.contains("content.uri"), "page-aware href must keep the page: $href")
            if (href.contains("index.html")) {
                assertTrue(
                    href.contains("content.uri != null"),
                    "index.html is only the null-safe degradation, got: $href",
                )
            }
        }
    }
}
