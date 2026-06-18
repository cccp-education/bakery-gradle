package contracts.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.Locale

class LocaleResolverTest {

    private class MockLocaleResolver : LocaleResolver {
        override fun resolve(config: I18nConfig): Locale {
            val lang = LanguageCatalog.findByCode(config.activeLanguage)
            return if (lang != null) Locale.forLanguageTag(lang.localeTag) else Locale.FRENCH
        }

        override fun resolveWithFallback(config: I18nConfig): Locale {
            val lang = LanguageCatalog.findByCode(config.activeLanguage)
            if (lang != null && config.activeLanguage in config.supportedLanguages) {
                return Locale.forLanguageTag(lang.localeTag)
            }
            val fallback = LanguageCatalog.findByCode(config.fallbackLanguage)
            return if (fallback != null) Locale.forLanguageTag(fallback.localeTag) else Locale.ENGLISH
        }
    }

    @Test
    fun `should resolve French locale`() {
        val resolver = MockLocaleResolver()
        val config = I18nConfig(activeLanguage = "fr", supportedLanguages = listOf("fr", "en"), fallbackLanguage = "en")
        val locale = resolver.resolve(config)
        assertEquals("fr", locale.language)
    }

    @Test
    fun `should resolve English locale`() {
        val resolver = MockLocaleResolver()
        val config = I18nConfig(activeLanguage = "en", supportedLanguages = listOf("en"), fallbackLanguage = "en")
        val locale = resolver.resolve(config)
        assertEquals("en", locale.language)
    }

    @Test
    fun `should resolve Arabic locale`() {
        val resolver = MockLocaleResolver()
        val config = I18nConfig(activeLanguage = "ar", supportedLanguages = listOf("ar", "en"), fallbackLanguage = "en")
        val locale = resolver.resolve(config)
        assertEquals("ar", locale.language)
    }

    @Test
    fun `should fallback when active not in supported`() {
        val resolver = MockLocaleResolver()
        val config = I18nConfig(activeLanguage = "zh", supportedLanguages = listOf("fr", "en"), fallbackLanguage = "en")
        val locale = resolver.resolveWithFallback(config)
        assertEquals("en", locale.language)
    }

    @Test
    fun `should resolve Chinese locale`() {
        val resolver = MockLocaleResolver()
        val config = I18nConfig(activeLanguage = "zh", supportedLanguages = listOf("zh", "en"), fallbackLanguage = "en")
        val locale = resolver.resolve(config)
        assertEquals("zh", locale.language)
    }
}
