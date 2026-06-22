package bakery.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * BKY-I18N-PROD-VAL — Tests d'intégration sur une copie des templates magic-stick.
 *
 * Objectif : valider migrateToI18n sur des données réelles (copies en test resources)
 * sans jamais toucher au site de production.
 *
 * Méthodologie : DDD/TDD/BDD baby steps — chaque assertion correspond à un critère
 * métier (pas de faux positifs, clés cohérentes, dry-run safe).
 */
class MagicStickI18nMigrationIntegrationTest {

    private val service = I18nMigrationService()

    /**
     * Charge le répertoire jbake de la fixture magic-stick depuis les resources de test.
     */
    private fun loadMagicStickFixture(): File {
        val resource = this::class.java.classLoader.getResource("i18n-fixtures/magic-stick/jbake")
            ?: throw IllegalStateException("Fixture magic-stick non trouvée dans test resources")
        return File(resource.toURI())
    }

    @Test
    fun `dry-run on magic-stick fixture scans all 12 templates without modifying them`(@TempDir tempDir: File) {
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
    fun `proper names are not extracted as i18n keys`(@TempDir tempDir: File) {
        val siteDir = copyFixtureToTemp(tempDir)

        val result = service.migrate(
            siteDir = siteDir,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = true
        )

        val allValues = collectAllExtractedValues(siteDir)
        assertFalse(
            allValues.any { it.contains("Magic Stick", ignoreCase = true) },
            "Le nom propre 'Magic Stick' ne doit pas être extrait"
        )
        assertFalse(
            allValues.any { it.contains("cccp.education", ignoreCase = true) },
            "Le nom propre 'cccp.education' ne doit pas être extrait"
        )
    }

    @Test
    fun `technical terms and identifiers are not extracted as standalone i18n keys`(@TempDir tempDir: File) {
        val siteDir = copyFixtureToTemp(tempDir)

        service.migrate(
            siteDir = siteDir,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = true
        )

        val allValues = collectAllExtractedValues(siteDir)
        val standaloneTechnicalTerms = listOf("GitHub", "SourceForge", "Bootstrap", "RSS", "v0.1.14", "Xubuntu", "BIOS", "UEFI")
        for (term in standaloneTechnicalTerms) {
            assertFalse(
                allValues.any { it.equals(term, ignoreCase = true) },
                "Le terme technique/identifiant '$term' ne doit pas être extrait comme clé autonome"
            )
        }
    }

    @Test
    fun `URLs and email links are not extracted as i18n keys`(@TempDir tempDir: File) {
        val siteDir = copyFixtureToTemp(tempDir)

        service.migrate(
            siteDir = siteDir,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = true
        )

        val allValues = collectAllExtractedValues(siteDir)
        assertFalse(
            allValues.any { it.startsWith("http://") || it.startsWith("https://") || it.startsWith("mailto:") },
            "Les URLs ne doivent pas être extraites"
        )
    }

    @Test
    fun `real french UI text is extracted`(@TempDir tempDir: File) {
        val siteDir = copyFixtureToTemp(tempDir)

        service.migrate(
            siteDir = siteDir,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = true
        )

        val allValues = collectAllExtractedValues(siteDir)
        assertTrue(
            allValues.any { it.contains("Accueil", ignoreCase = true) },
            "'Accueil' doit être extrait"
        )
        assertTrue(
            allValues.any { it.contains("Guide d'installation", ignoreCase = true) },
            "'Guide d'installation' doit être extrait"
        )
        assertTrue(
            allValues.any { it.contains("Démarrage rapide", ignoreCase = true) || it.contains("Demarrage rapide", ignoreCase = true) },
            "'Démarrage rapide' doit être extrait"
        )
    }

    /**
     * Copie la fixture depuis les resources vers un répertoire temporaire modifiable.
     * Cela garantit que le dry-run n'altère jamais la fixture versionnée.
     */
    private fun copyFixtureToTemp(tempDir: File): File {
        val fixture = loadMagicStickFixture()
        val target = tempDir.resolve("magic-stick")
        fixture.copyRecursively(target, overwrite = true)
        return target
    }

    @Test
    fun `diagnostic print all extracted keys and values`(@TempDir tempDir: File) {
        val siteDir = copyFixtureToTemp(tempDir)

        val result = service.migrate(
            siteDir = siteDir,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = true
        )

        println("=== DIAGNOSTIC: ${result.keysExtracted} keys extracted ===")
        val templatesDir = siteDir.resolve("templates")
        templatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .forEach { file ->
                val extractions = service.extractHardcodedText(file)
                if (extractions.isNotEmpty()) {
                    println("--- ${file.name} ---")
                    extractions.forEach { (key, value) -> println("$key=$value") }
                }
            }
    }

    /**
     * Ré-collecte les valeurs extraites en scannant manuellement les templates.
     * Nécessaire car migrate en dry-run ne persiste rien.
     */
    private fun collectAllExtractedValues(siteDir: File): List<String> {
        val templatesDir = siteDir.resolve("templates")
        return templatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .flatMap { service.extractHardcodedText(it).values }
            .toList()
    }
}
