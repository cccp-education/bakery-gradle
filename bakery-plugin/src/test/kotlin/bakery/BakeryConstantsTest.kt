package bakery

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BakeryConstantsTest {

    @Test
    fun `SUPPORTED_LANGS contains all 10 languages`() {
        val expected = setOf("fr", "en", "zh", "hi", "es", "ar", "bn", "pt", "ru", "ur")
        assertEquals(expected, BakeryConstants.SUPPORTED_LANGS)
        assertEquals(10, BakeryConstants.SUPPORTED_LANGS.size)
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
}
