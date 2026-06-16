package bakery.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class I18nMigrationIntentionDslTest {

    @Test
    fun `toIntention converts DSL values to domain model`() {
        val dsl = I18nMigrationIntentionDsl().apply {
            siteDir = "/home/site"
            languages = listOf("en", "ar")
            defaultLanguage = "en"
            dryRun = false
        }

        val intention = dsl.toIntention()

        assertEquals("/home/site", intention.siteDir)
        assertEquals(listOf("en", "ar"), intention.languages)
        assertEquals("en", intention.defaultLanguage)
        assertEquals(false, intention.dryRun)
    }

    @Test
    fun `toIntention uses defaults for unspecified values`() {
        val dsl = I18nMigrationIntentionDsl().apply {
            siteDir = "/path"
        }

        val intention = dsl.toIntention()

        assertEquals("/path", intention.siteDir)
        assertEquals(listOf("en"), intention.languages)
        assertEquals("fr", intention.defaultLanguage)
        assertEquals(true, intention.dryRun)
    }

    @Test
    fun `toIntention throws when siteDir is blank`() {
        val dsl = I18nMigrationIntentionDsl()

        assertThrows<IllegalArgumentException> {
            dsl.toIntention()
        }
    }

    @Test
    fun `toIntention throws when languages is empty`() {
        val dsl = I18nMigrationIntentionDsl().apply {
            siteDir = "/path"
            languages = emptyList()
        }

        assertThrows<IllegalArgumentException> {
            dsl.toIntention()
        }
    }

    @Test
    fun `toIntention throws when language is unsupported`() {
        val dsl = I18nMigrationIntentionDsl().apply {
            siteDir = "/path"
            languages = listOf("de")
        }

        assertThrows<IllegalArgumentException> {
            dsl.toIntention()
        }
    }
}
