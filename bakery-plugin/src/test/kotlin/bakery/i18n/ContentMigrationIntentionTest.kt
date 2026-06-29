package bakery.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ContentMigrationIntentionTest {

    @Test
    fun `valid intention creates successfully`() {
        val intent = ContentMigrationIntention(
            sourceDir = "site",
            outputDir = "build/translated-site",
            targetLanguages = listOf("en", "es")
        )
        assertEquals("site", intent.sourceDir)
        assertEquals("build/translated-site", intent.outputDir)
        assertEquals("fr", intent.sourceLanguage)
        assertEquals(listOf("en", "es"), intent.targetLanguages)
    }

    @Test
    fun `blank sourceDir throws`() {
        assertThrows<IllegalArgumentException> {
            ContentMigrationIntention(sourceDir = "", outputDir = "out")
        }
    }

    @Test
    fun `blank outputDir throws`() {
        assertThrows<IllegalArgumentException> {
            ContentMigrationIntention(sourceDir = "site", outputDir = "")
        }
    }

    @Test
    fun `empty targetLanguages throws`() {
        assertThrows<IllegalArgumentException> {
            ContentMigrationIntention(
                sourceDir = "site", outputDir = "out",
                targetLanguages = emptyList()
            )
        }
    }

    @Test
    fun `unsupported target language throws`() {
        assertThrows<IllegalArgumentException> {
            ContentMigrationIntention(
                sourceDir = "site", outputDir = "out",
                targetLanguages = listOf("xx")
            )
        }
    }

    @Test
    fun `unsupported source language throws`() {
        assertThrows<IllegalArgumentException> {
            ContentMigrationIntention(
                sourceDir = "site", outputDir = "out",
                sourceLanguage = "xx"
            )
        }
    }

    @Test
    fun `source language in target languages throws`() {
        assertThrows<IllegalArgumentException> {
            ContentMigrationIntention(
                sourceDir = "site", outputDir = "out",
                sourceLanguage = "fr", targetLanguages = listOf("fr", "en")
            )
        }
    }

    @Test
    fun `dryRun defaults to true`() {
        val intent = ContentMigrationIntention(sourceDir = "site", outputDir = "out")
        assertEquals(true, intent.dryRun)
    }

    @Test
    fun `DSL produces identical intention`() {
        val dsl = ContentMigrationIntentionDsl().apply {
            sourceDir = "site"
            outputDir = "build/i18n"
            sourceLanguage = "fr"
            targetLanguages = listOf("en", "zh")
            dryRun = false
        }
        val intent = dsl.toIntention()
        assertEquals("site", intent.sourceDir)
        assertEquals("build/i18n", intent.outputDir)
        assertEquals("fr", intent.sourceLanguage)
        assertEquals(listOf("en", "zh"), intent.targetLanguages)
        assertEquals(false, intent.dryRun)
    }
}
