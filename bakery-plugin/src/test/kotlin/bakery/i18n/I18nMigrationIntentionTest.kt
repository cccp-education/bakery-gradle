package bakery.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class I18nMigrationIntentionTest {

    @Test
    fun `minimal intention with siteDir only has default values`() {
        val intention = I18nMigrationIntention(siteDir = "/path/to/site")

        assertEquals("/path/to/site", intention.siteDir)
        assertEquals(listOf("en"), intention.languages)
        assertEquals("fr", intention.defaultLanguage)
        assertEquals(true, intention.dryRun)
    }

    @Test
    fun `full intention with all parameters`() {
        val intention = I18nMigrationIntention(
            siteDir = "/home/site",
            languages = listOf("en", "ar", "zh"),
            defaultLanguage = "en",
            dryRun = false
        )

        assertEquals("/home/site", intention.siteDir)
        assertEquals(listOf("en", "ar", "zh"), intention.languages)
        assertEquals("en", intention.defaultLanguage)
        assertEquals(false, intention.dryRun)
    }

    @Test
    fun `intention requires non-blank siteDir`() {
        assertThrows<IllegalArgumentException> {
            I18nMigrationIntention(siteDir = "")
        }
    }

    @Test
    fun `intention requires non-whitespace-only siteDir`() {
        assertThrows<IllegalArgumentException> {
            I18nMigrationIntention(siteDir = "   ")
        }
    }

    @Test
    fun `intention requires at least one language`() {
        assertThrows<IllegalArgumentException> {
            I18nMigrationIntention(siteDir = "/path", languages = emptyList())
        }
    }

    @Test
    fun `intention rejects unsupported language in languages list`() {
        assertThrows<IllegalArgumentException> {
            I18nMigrationIntention(siteDir = "/path", languages = listOf("en", "de"))
        }
    }

    @Test
    fun `intention rejects unsupported defaultLanguage`() {
        assertThrows<IllegalArgumentException> {
            I18nMigrationIntention(siteDir = "/path", defaultLanguage = "de")
        }
    }

    @Test
    fun `intention accepts all 10 supported languages`() {
        val codes = setOf("fr", "en", "zh", "hi", "es", "ar", "bn", "pt", "ru", "ur")
        val intention = I18nMigrationIntention(
            siteDir = "/path",
            languages = codes.toList(),
            defaultLanguage = "fr"
        )
        assertEquals(codes.toList(), intention.languages)
    }

    @Test
    fun `toPromptContext includes all fields`() {
        val intention = I18nMigrationIntention(
            siteDir = "/home/site",
            languages = listOf("en", "ar"),
            defaultLanguage = "fr",
            dryRun = true
        )
        val context = intention.toPromptContext()

        assertTrue(context.contains("/home/site"), "Must contain siteDir")
        assertTrue(context.contains("en, ar"), "Must contain languages")
        assertTrue(context.contains("fr"), "Must contain defaultLanguage")
        assertTrue(context.contains("true"), "Must contain dryRun")
    }

    @Test
    fun `toPromptContext with dryRun false`() {
        val intention = I18nMigrationIntention(
            siteDir = "/path",
            languages = listOf("en"),
            dryRun = false
        )
        val context = intention.toPromptContext()

        assertTrue(context.contains("false"), "Must contain dryRun=false")
    }
}
