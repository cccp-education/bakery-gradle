package contracts.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SupportedLanguageTest {

    @Test
    fun `should construct with all fields`() {
        val lang = SupportedLanguage(
            code = "fr",
            name = "French",
            nativeName = "Français",
            rtl = false,
            localeTag = "fr-FR"
        )
        assertEquals("fr", lang.code)
        assertEquals("French", lang.name)
        assertEquals("Français", lang.nativeName)
        assertFalse(lang.rtl)
        assertEquals("fr-FR", lang.localeTag)
    }

    @Test
    fun `should default rtl to false`() {
        val lang = SupportedLanguage(code = "en", name = "English", nativeName = "English")
        assertFalse(lang.rtl)
    }

    @Test
    fun `should default localeTag to code`() {
        val lang = SupportedLanguage(code = "hi", name = "Hindi", nativeName = "हिन्दी")
        assertEquals("hi", lang.localeTag)
    }

    @Test
    fun `should support RTL languages`() {
        val lang = SupportedLanguage(
            code = "ar",
            name = "Arabic",
            nativeName = "العربية",
            rtl = true,
            localeTag = "ar-SA"
        )
        assertTrue(lang.rtl)
    }

    @Test
    fun `should support data class equality`() {
        val a = SupportedLanguage(code = "fr", name = "French", nativeName = "Français")
        val b = SupportedLanguage(code = "fr", name = "French", nativeName = "Français")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
