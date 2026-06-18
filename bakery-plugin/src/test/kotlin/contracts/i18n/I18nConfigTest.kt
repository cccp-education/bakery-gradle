package contracts.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class I18nConfigTest {

    @Test
    fun `should construct with defaults`() {
        val config = I18nConfig()
        assertEquals("fr", config.activeLanguage)
        assertEquals(listOf("fr"), config.supportedLanguages)
        assertEquals("en", config.fallbackLanguage)
    }

    @Test
    fun `should validate valid config`() {
        val config = I18nConfig(
            activeLanguage = "fr",
            supportedLanguages = listOf("fr", "en"),
            fallbackLanguage = "en"
        )
        assertTrue(config.validate() is I18nValidationResult.Valid)
    }

    @Test
    fun `should reject active language not in supported`() {
        val config = I18nConfig(
            activeLanguage = "zh",
            supportedLanguages = listOf("fr", "en"),
            fallbackLanguage = "en"
        )
        val result = config.validate()
        assertTrue(result is I18nValidationResult.Invalid)
        val invalid = result as I18nValidationResult.Invalid
        assertTrue(invalid.message.contains("zh"))
        assertTrue(invalid.message.contains("not in supported"))
    }

    @Test
    fun `should reject fallback language not in supported`() {
        val config = I18nConfig(
            activeLanguage = "fr",
            supportedLanguages = listOf("fr"),
            fallbackLanguage = "en"
        )
        val result = config.validate()
        assertTrue(result is I18nValidationResult.Invalid)
        val invalid = result as I18nValidationResult.Invalid
        assertTrue(invalid.message.contains("en"))
        assertTrue(invalid.message.contains("Fallback"))
    }

    @Test
    fun `should validate with all 10 languages`() {
        val config = I18nConfig(
            activeLanguage = "ar",
            supportedLanguages = LanguageCatalog.supportedCodes().toList(),
            fallbackLanguage = "en"
        )
        assertTrue(config.validate() is I18nValidationResult.Valid)
    }

    @Test
    fun `should validate with single language`() {
        val config = I18nConfig(
            activeLanguage = "en",
            supportedLanguages = listOf("en"),
            fallbackLanguage = "en"
        )
        assertTrue(config.validate() is I18nValidationResult.Valid)
    }

    @Test
    fun `should support custom fallback`() {
        val config = I18nConfig(
            activeLanguage = "es",
            supportedLanguages = listOf("es", "pt"),
            fallbackLanguage = "pt"
        )
        assertTrue(config.validate() is I18nValidationResult.Valid)
    }

    @Test
    fun `should have data class equality`() {
        val a = I18nConfig(activeLanguage = "fr", supportedLanguages = listOf("fr", "en"), fallbackLanguage = "en")
        val b = I18nConfig(activeLanguage = "fr", supportedLanguages = listOf("fr", "en"), fallbackLanguage = "en")
        assertEquals(a, b)
    }
}
