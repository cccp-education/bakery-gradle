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
        assertEquals("\${content.rootpath + 'en/' + content.uri}", url("en", "fr").thymeleafHref())
    }

    @Test
    fun `non-default to default is page-aware exiting the language tree`() {
        assertEquals("|../\${content.rootpath}\${content.uri}|", url("fr", "en").thymeleafHref())
    }

    @Test
    fun `non-default to another non-default is page-aware`() {
        assertEquals("|../\${content.rootpath}ar/\${content.uri}|", url("ar", "en").thymeleafHref())
    }

    @Test
    fun `self link points at the current page not the language root`() {
        val self = url("en", "en")
        assertTrue(self.isSelfLink())
        assertEquals("\${content.uri.substring(content.uri.lastIndexOf('/') + 1)}", self.thymeleafHref())
    }

    @Test
    fun `the emitted href is never the page-blind index html`() {
        val hrefs = listOf(url("en", "fr"), url("fr", "en"), url("ar", "en")).map { it.thymeleafHref() }
        hrefs.forEach { href ->
            assertTrue(!href.contains("'index.html'"), "page-aware href must not hardcode index.html: $href")
        }
    }
}
