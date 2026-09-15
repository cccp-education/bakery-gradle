package bakery

import contracts.i18n.LanguageCatalog
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BakeryConstantsTest {
    @Test
    fun `SUPPORTED_LANGS contains all 22 languages`() {
        val expected = setOf(
            "fr", "en", "zh", "hi", "es", "ar", "bn", "pt", "ru", "ur",
            "it", "nl", "de", "el", "tr", "vi", "th", "id", "ko", "ja", "sr", "fa"
        )
        assertEquals(expected, BakeryConstants.SUPPORTED_LANGS)
        assertEquals(22, BakeryConstants.SUPPORTED_LANGS.size)
    }

    @Test
    fun `SUPPORTED_LANGS is derived from N0 LanguageCatalog`() {
        assertEquals(LanguageCatalog.supportedCodes(), BakeryConstants.SUPPORTED_LANGS)
    }

    @Test
    fun `SUPPORTED_LANGS is immutable`() {
        assertTrue(BakeryConstants.SUPPORTED_LANGS is Set<String>)
    }

    @Test
    fun `SUPPORTED_LANGS includes most spoken languages`() {
        assertTrue("en" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("zh" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("hi" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("es" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("fr" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("ar" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("bn" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("pt" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("ru" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("ur" in BakeryConstants.SUPPORTED_LANGS)
    }

    @Test
    fun `SUPPORTED_LANGS includes talaria school languages`() {
        assertTrue("de" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("it" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("nl" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("el" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("tr" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("vi" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("th" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("id" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("ko" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("ja" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("sr" in BakeryConstants.SUPPORTED_LANGS)
        assertTrue("fa" in BakeryConstants.SUPPORTED_LANGS)
    }
}
