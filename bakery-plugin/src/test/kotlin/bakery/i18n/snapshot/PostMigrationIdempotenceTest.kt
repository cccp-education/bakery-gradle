package bakery.i18n.snapshot

import bakery.i18n.I18nMigrationService
import bakery.i18n.I18nTranslationApplier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * BKY-I18N-REAL-6 — Idempotence (Ink Economy Law).
 *
 * Re-running migrate on an already migrated copy produces zero changes.
 * Already migrated templates (with th:text) must not be re-extracted.
 * `messages_*.properties` must not be modified.
 *
 * Validates that [I18nMigrationService] detects already migrated templates
 * and does not re-translate (Ink Economy Law on real data).
 */
class PostMigrationIdempotenceTest {

    private val service = I18nMigrationService()

    @Test
    fun `re-migrating magic-stick already migrated produces zero changes`(@TempDir tempDir: File) {
        assertIdempotent("magic-stick", tempDir)
    }

    @Test
    fun `re-migrating cccp-education already migrated produces zero changes`(@TempDir tempDir: File) {
        assertIdempotent("cccp-education", tempDir)
    }

    @Test
    fun `re-migrating cheroliv-com already migrated produces zero changes`(@TempDir tempDir: File) {
        assertIdempotent("cheroliv-com", tempDir)
    }

    private fun assertIdempotent(siteId: String, tempDir: File) {
        val preMigrationFixture = loadPreMigrationFixture(siteId)
        val siteCopy = copyFixtureToTemp(preMigrationFixture, tempDir.resolve(siteId))

        service.migrate(
            siteDir = siteCopy,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = false
        )

        val translationsEn = loadTranslationsFromResources("i18n-fixtures/$siteId/translations_en.properties")
        I18nTranslationApplier.applyTranslations(
            siteCopy.resolve("templates/messages_en.properties"),
            translationsEn
        )

        val templatesBeforeReMigration = captureTemplates(siteCopy)
        val messagesFrBefore = loadProperties(siteCopy.resolve("templates/messages_fr.properties"))
        val messagesEnBefore = loadProperties(siteCopy.resolve("templates/messages_en.properties"))

        val secondResult = service.migrate(
            siteDir = siteCopy,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = false
        )

        assertEquals(0, secondResult.keysExtracted, "[$siteId] Re-migration must extract 0 keys")
        assertEquals(0, secondResult.templatesModified, "[$siteId] Re-migration must modify 0 templates")

        val templatesAfterReMigration = captureTemplates(siteCopy)
        val messagesFrAfter = loadProperties(siteCopy.resolve("templates/messages_fr.properties"))
        val messagesEnAfter = loadProperties(siteCopy.resolve("templates/messages_en.properties"))

        assertEquals(
            templatesBeforeReMigration,
            templatesAfterReMigration,
            "[$siteId] Templates unchanged after re-migration"
        )
        assertEquals(
            messagesFrBefore,
            messagesFrAfter,
            "[$siteId] messages_fr.properties unchanged after re-migration"
        )
        assertEquals(
            messagesEnBefore,
            messagesEnAfter,
            "[$siteId] messages_en.properties unchanged after re-migration"
        )

        assertTrue(secondResult.keysExtracted == 0, "[$siteId] Ink Economy Law respected")
    }

    private fun captureTemplates(siteDir: File): Map<String, String> {
        val templatesDir = siteDir.resolve("templates")
        return templatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .associate { it.relativeTo(templatesDir).path to it.readText() }
    }

    private fun loadProperties(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        val props = Properties()
        file.inputStream().use { props.load(it) }
        return props.map { it.key.toString() to it.value.toString() }.toMap()
    }

    private fun loadPreMigrationFixture(siteId: String): File {
        val resource = this::class.java.classLoader.getResource("i18n-fixtures/$siteId/jbake")
            ?: throw IllegalStateException("Fixture not found: $siteId")
        return File(resource.toURI())
    }

    private fun loadTranslationsFromResources(resourcePath: String): Map<String, String> {
        val url = this::class.java.classLoader.getResource(resourcePath)
            ?: throw IllegalStateException("Resource not found: $resourcePath")
        val props = Properties()
        url.openStream().use { props.load(it) }
        return props.map { it.key.toString() to it.value.toString() }.toMap()
    }

    private fun copyFixtureToTemp(fixture: File, target: File): File {
        fixture.copyRecursively(target, overwrite = true)
        return target
    }
}