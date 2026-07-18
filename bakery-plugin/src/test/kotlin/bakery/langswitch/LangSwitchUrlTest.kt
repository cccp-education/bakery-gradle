package bakery.langswitch

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.assertThrows

class LangSwitchUrlTest {

    @Test
    fun `resolve self-link when target language equals current language`() {
        val url = LangSwitchUrl(
            targetLanguage = "en",
            currentLanguage = "en",
            currentPath = "en/",
            defaultLanguage = "fr"
        )
        assertEquals("index.html", url.resolve())
    }

    @Test
    fun `resolve FR from EN subdir goes up to root`() {
        val url = LangSwitchUrl(
            targetLanguage = "fr",
            currentLanguage = "en",
            currentPath = "en/",
            defaultLanguage = "fr"
        )
        assertEquals("../index.html", url.resolve())
    }

    @Test
    fun `resolve EN from FR root goes down into subdir`() {
        val url = LangSwitchUrl(
            targetLanguage = "en",
            currentLanguage = "fr",
            currentPath = "",
            defaultLanguage = "fr"
        )
        assertEquals("en/index.html", url.resolve())
    }

    @Test
    fun `resolve AR from EN subdir goes up then down`() {
        val url = LangSwitchUrl(
            targetLanguage = "ar",
            currentLanguage = "en",
            currentPath = "en/",
            defaultLanguage = "fr"
        )
        assertEquals("../ar/index.html", url.resolve())
    }

    @Test
    fun `resolve FR from FR root is self-link`() {
        val url = LangSwitchUrl(
            targetLanguage = "fr",
            currentLanguage = "fr",
            currentPath = "",
            defaultLanguage = "fr"
        )
        assertEquals("index.html", url.resolve())
    }

    @Test
    fun `resolve EN from FR root for non-default language goes down`() {
        val url = LangSwitchUrl(
            targetLanguage = "en",
            currentLanguage = "fr",
            currentPath = "",
            defaultLanguage = "fr"
        )
        assertEquals("en/index.html", url.resolve())
    }

    @Test
    fun `no self-loop when EN page links to EN`() {
        val url = LangSwitchUrl(
            targetLanguage = "en",
            currentLanguage = "en",
            currentPath = "en/",
            defaultLanguage = "fr"
        )
        val resolved = url.resolve()
        assertFalse(resolved.contains("en/index.html"),
            "Self-loop detected: EN page should not link to en/index.html, got: $resolved")
    }

    @Test
    fun `rootpath for nested path en goes up one level`() {
        val url = LangSwitchUrl(
            targetLanguage = "fr",
            currentLanguage = "en",
            currentPath = "en/",
            defaultLanguage = "fr"
        )
        assertEquals("../", url.rootpath())
    }

    @Test
    fun `rootpath for root is empty`() {
        val url = LangSwitchUrl(
            targetLanguage = "en",
            currentLanguage = "fr",
            currentPath = "",
            defaultLanguage = "fr"
        )
        assertEquals("", url.rootpath())
    }

    @Test
    fun `isSelfLink true when target equals current`() {
        val url = LangSwitchUrl(
            targetLanguage = "fr",
            currentLanguage = "fr",
            currentPath = "",
            defaultLanguage = "fr"
        )
        assertTrue(url.isSelfLink())
    }

    @Test
    fun `isSelfLink false when target differs from current`() {
        val url = LangSwitchUrl(
            targetLanguage = "en",
            currentLanguage = "fr",
            currentPath = "",
            defaultLanguage = "fr"
        )
        assertFalse(url.isSelfLink())
    }

    @Test
    fun `structural equality holds`() {
        val url1 = LangSwitchUrl("en", "fr", "", "fr")
        val url2 = LangSwitchUrl("en", "fr", "", "fr")
        assertEquals(url1, url2)
        assertEquals(url1.hashCode(), url2.hashCode())
    }

    @Test
    fun `blank target language throws`() {
        assertThrows<IllegalArgumentException> {
            LangSwitchUrl(targetLanguage = "", currentLanguage = "fr", currentPath = "", defaultLanguage = "fr")
        }
    }

    @Test
    fun `blank current language throws`() {
        assertThrows<IllegalArgumentException> {
            LangSwitchUrl(targetLanguage = "en", currentLanguage = "", currentPath = "", defaultLanguage = "fr")
        }
    }
}