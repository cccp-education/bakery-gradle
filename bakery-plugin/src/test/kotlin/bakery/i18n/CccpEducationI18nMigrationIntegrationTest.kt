package bakery.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * BKY-I18N-PROD-GEN — Tests d'intégration sur la fixture cccp-education.
 *
 * Valide migrateToI18n en dryRun et réel, l'absence de faux positifs,
 * la génération des fichiers de messages et l'application des traductions EN.
 */
class CccpEducationI18nMigrationIntegrationTest {

    private val service = I18nMigrationService()

    private fun loadFixture(): File {
        val resource = this::class.java.classLoader.getResource("i18n-fixtures/cccp-education/jbake")
            ?: throw IllegalStateException("Fixture cccp-education non trouvée")
        return File(resource.toURI())
    }

    private fun loadTranslationsFromResources(): Map<String, String> {
        val url = this::class.java.classLoader.getResource("i18n-fixtures/cccp-education/translations_en.properties")
            ?: throw IllegalStateException("Traductions EN cccp-education non trouvées")
        val props = Properties()
        url.openStream().use { props.load(it) }
        return props.map { it.key.toString() to it.value.toString() }.toMap()
    }

    private fun copyFixtureToTemp(tempDir: File): File {
        val fixture = loadFixture()
        val target = tempDir.resolve("cccp-education")
        fixture.copyRecursively(target, overwrite = true)
        return target
    }

    @Test
    fun `dry-run on cccp-education fixture scans templates without modifying them`(@TempDir tempDir: File) {
        val siteDir = copyFixtureToTemp(tempDir)
        val templatesDir = siteDir.resolve("templates")
        val originalChecksums = templatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .associate { it.name to it.readText() }

        val result = service.migrate(
            siteDir = siteDir,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = true
        )

        assertTrue(result.keysExtracted > 0, "Des clés doivent être extraites")
        assertEquals(0, result.filesGenerated, "Dry-run ne génère aucun fichier")
        assertEquals(0, result.templatesModified, "Dry-run ne modifie aucun template")

        templatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .forEach { file ->
                assertEquals(
                    originalChecksums[file.name],
                    file.readText(),
                    "Le template ${file.name} ne doit pas être modifié en dry-run"
                )
            }
    }

    @Test
    fun `proper names and project identifiers are not extracted`(@TempDir tempDir: File) {
        val siteDir = copyFixtureToTemp(tempDir)
        val allValues = collectAllExtractedValues(siteDir)
        assertFalse(
            allValues.any { it.contains("cccp.education", ignoreCase = true) },
            "Le nom propre 'cccp.education' ne doit pas être extrait"
        )
        assertFalse(
            allValues.any { it.contains("Common Content Creator Proletarian", ignoreCase = true) },
            "'Common Content Creator Proletarian' ne doit pas être extrait"
        )
        assertFalse(
            allValues.any { it.contains("plantuml-gradle", ignoreCase = true) },
            "Les identifiants de projet ne doivent pas être extraits"
        )
    }

    @Test
    fun `real migration generates messages files and modifies templates`(@TempDir tempDir: File) {
        val siteDir = copyFixtureToTemp(tempDir)

        val result = service.migrate(
            siteDir = siteDir,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = false
        )

        assertFalse(result.dryRun)
        assertTrue(result.keysExtracted > 0)
        assertEquals(2, result.filesGenerated)
        assertTrue(result.templatesModified > 0)

        val templatesDir = siteDir.resolve("templates")
        assertTrue(templatesDir.resolve("messages_fr.properties").exists())
        assertTrue(templatesDir.resolve("messages_en.properties").exists())
    }

    @Test
    fun `english translations cover all extracted keys and fill messages_en`(@TempDir tempDir: File) {
        val siteDir = copyFixtureToTemp(tempDir)

        service.migrate(siteDir, listOf("fr", "en"), "fr", dryRun = false)

        val translations = loadTranslationsFromResources()
        val frProps = Properties()
        siteDir.resolve("templates/messages_fr.properties").inputStream().use { frProps.load(it) }

        val missingKeys = frProps.keys.filter { !translations.containsKey(it) }
        assertTrue(missingKeys.isEmpty(), "Toutes les clés FR doivent avoir une traduction EN : $missingKeys")

        val enFile = siteDir.resolve("templates/messages_en.properties")
        val updated = I18nTranslationApplier.applyTranslations(enFile, translations)
        assertTrue(updated, "Les traductions doivent être appliquées")

        val enProps = Properties()
        enFile.inputStream().use { enProps.load(it) }
        assertTrue(enProps.values.any { (it as String).isNotBlank() }, "messages_en.properties doit contenir des traductions")
    }

    @Test
    fun `real migration injects site language into jbake properties`(@TempDir tempDir: File) {
        val siteDir = copyFixtureToTemp(tempDir)
        service.migrate(siteDir, listOf("fr", "en"), "fr", dryRun = false)
        val content = siteDir.resolve("jbake.properties").readText()
        assertTrue(content.contains("site.language=fr"))
    }

    private fun collectAllExtractedValues(siteDir: File): List<String> {
        val templatesDir = siteDir.resolve("templates")
        return templatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .flatMap { service.extractHardcodedText(it).values }
            .toList()
    }
}
