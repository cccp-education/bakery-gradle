package contracts.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class LanguageCatalogTest {

    @Test
    fun `should contain exactly 10 languages`() {
        assertEquals(10, LanguageCatalog.ALL.size)
    }

    @Test
    fun `should find English by code`() {
        val lang = LanguageCatalog.findByCode("en")
        assertNotNull(lang)
        assertEquals("English", lang!!.name)
        assertEquals("en-US", lang.localeTag)
    }

    @Test
    fun `should find French by code`() {
        val lang = LanguageCatalog.findByCode("fr")
        assertNotNull(lang)
        assertEquals("Français", lang!!.nativeName)
        assertEquals("fr-FR", lang.localeTag)
    }

    @Test
    fun `should find Arabic with RTL flag`() {
        val lang = LanguageCatalog.findByCode("ar")
        assertNotNull(lang)
        assertTrue(lang!!.rtl)
        assertEquals("ar-SA", lang.localeTag)
    }

    @Test
    fun `should find Urdu with RTL flag`() {
        val lang = LanguageCatalog.findByCode("ur")
        assertNotNull(lang)
        assertTrue(lang!!.rtl)
        assertEquals("ur-PK", lang.localeTag)
    }

    @Test
    fun `should return null for unknown code`() {
        val lang = LanguageCatalog.findByCode("xx")
        assertNull(lang)
    }

    @Test
    fun `should return all supported codes`() {
        val codes = LanguageCatalog.supportedCodes()
        assertEquals(10, codes.size)
        assertTrue(codes.containsAll(listOf("en", "zh", "hi", "es", "fr", "ar", "bn", "pt", "ru", "ur")))
    }

    @Test
    fun `should have unique codes`() {
        val codes = LanguageCatalog.ALL.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `should have Chinese Mandarin with correct native name`() {
        val lang = LanguageCatalog.findByCode("zh")
        assertNotNull(lang)
        assertEquals("中文", lang!!.nativeName)
        assertEquals("zh-CN", lang.localeTag)
    }

    @Test
    fun `should have Hindi with correct native name`() {
        val lang = LanguageCatalog.findByCode("hi")
        assertNotNull(lang)
        assertEquals("हिन्दी", lang!!.nativeName)
    }
}
