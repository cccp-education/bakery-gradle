package bakery.i18n

import bakery.i18n.snapshot.PostMigrationSnapshot
import bakery.i18n.snapshot.PostMigrationSnapshotLoader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Boucle complète : site réel → copie → migration → golden master → idempotence.
 *
 * Vérifie que la migration d'une copie fraîche du site de production
 * produit exactement le même résultat que le golden master attendu,
 * et que re-migrer ne change rien (loi d'économie d'encre).
 *
 * Note : cheroliv.com exclut le répertoire content/draft/ (brouillons).
 */
class RealSiteI18nMigrationIntegrationTest {

    private val service = I18nMigrationService()
    private val loader = PostMigrationSnapshotLoader()
    private val workspace = File("/home/cheroliv/workspace")

    private data class SiteMapping(
        val fixtureId: String,
        val realSiteDirName: String,
        val excludeDirs: Set<String> = emptySet()
    )

    private val sites = listOf(
        SiteMapping("magic-stick", "magic-stick"),
        SiteMapping("cccp-education", "cccp.education"),
        SiteMapping("cheroliv-com", "cheroliv.com", excludeDirs = setOf("content/draft")),
    )

    @Test
    fun `migration from real site copy matches golden master for all sites`(@TempDir tempDir: File) {
        for (site in sites) {
            val siteCopy = copyRealSite(site, tempDir.resolve(site.fixtureId))
            migrateThenCompare(site, siteCopy)
        }
    }

    @Test
    fun `idempotence from real site copy produces zero changes for all sites`(@TempDir tempDir: File) {
        for (site in sites) {
            val siteCopy = copyRealSite(site, tempDir.resolve(site.fixtureId))
            service.migrate(siteCopy, listOf("fr", "en"), "fr", dryRun = false)

            val beforeChecksums = checksumTemplates(siteCopy)

            service.migrate(siteCopy, listOf("fr", "en"), "fr", dryRun = false)

            val afterChecksums = checksumTemplates(siteCopy)
            assertEquals(
                beforeChecksums, afterChecksums,
                "[${site.fixtureId}] Re-migration modifié des templates (économie d'encre violée)"
            )

            val frProps = loadPropertiesFromDir(siteCopy.resolve("templates/messages_fr.properties"))
            val enProps = loadPropertiesFromDir(siteCopy.resolve("templates/messages_en.properties"))
            assertTrue(frProps.isNotEmpty(), "[${site.fixtureId}] messages_fr ne doit pas être vide après re-migration")
            assertTrue(enProps.isNotEmpty(), "[${site.fixtureId}] messages_en ne doit pas être vide après re-migration")
        }
    }

    private fun migrateThenCompare(site: SiteMapping, siteCopy: File) {
        val result = service.migrate(siteCopy, listOf("fr", "en"), "fr", dryRun = false)

        assertFalse(result.dryRun, "[${site.fixtureId}] La migration doit être réelle")
        assertTrue(result.keysExtracted > 0, "[${site.fixtureId}] Des clés doivent être extraites")
        assertTrue(result.templatesModified > 0, "[${site.fixtureId}] Des templates doivent être modifiés")

        val translationsEn = loadTranslationsFromResources(site.fixtureId)
        val enFile = siteCopy.resolve("templates/messages_en.properties")
        I18nTranslationApplier.applyTranslations(enFile, translationsEn)

        val actual = buildSnapshot(siteCopy)
        val goldenMaster = loadGoldenMaster(site.fixtureId)

        assertSnapshotsMatch(site.fixtureId, actual, goldenMaster)
    }

    private fun assertSnapshotsMatch(siteId: String, actual: PostMigrationSnapshot, expected: PostMigrationSnapshot) {
        assertEquals(
            expected.templatesMigrated.keys, actual.templatesMigrated.keys,
            "[$siteId] Templates migrés : jeu de clés différent"
        )
        for (templateName in expected.templatesMigrated.keys) {
            assertEquals(
                expected.templatesMigrated[templateName],
                actual.templatesMigrated[templateName],
                "[$siteId] Template '$templateName' diffère du golden master"
            )
        }
        assertEquals(expected.messagesFr, actual.messagesFr, "[$siteId] messages_fr diffère du golden master")
        assertEquals(expected.messagesEn, actual.messagesEn, "[$siteId] messages_en diffère du golden master")
        assertTrue(actual.isComplete(), "[$siteId] Le snapshot réel doit être complet")
    }

    private fun copyRealSite(site: SiteMapping, targetDir: File): File {
        val realJbakeDir = workspace.resolve("office/sites/${site.realSiteDirName}/jbake")
        require(realJbakeDir.isDirectory) { "Répertoire site réel introuvable : $realJbakeDir" }

        realJbakeDir.walkTopDown().forEach { file ->
            val relativePath = file.relativeTo(realJbakeDir).path
            if (file.isDirectory) {
                if (relativePath in site.excludeDirs) return@forEach
                targetDir.resolve(relativePath).mkdirs()
                return@forEach
            }
            if (site.excludeDirs.any { relativePath.startsWith("$it/") }) return@forEach
            file.copyTo(targetDir.resolve(relativePath), overwrite = true)
        }

        val templatesDir = targetDir.resolve("templates")
        require(templatesDir.isDirectory) { "Répertoire templates introuvable dans la copie : $templatesDir" }
        return targetDir
    }

    private fun buildSnapshot(siteCopy: File): PostMigrationSnapshot {
        val templatesDir = siteCopy.resolve("templates")
        val templatesMigrated = templatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .associate { it.relativeTo(templatesDir).path to it.readText() }
        val messagesFr = loadPropertiesFromDir(templatesDir.resolve("messages_fr.properties"))
        val messagesEn = loadPropertiesFromDir(templatesDir.resolve("messages_en.properties"))
        return PostMigrationSnapshot(templatesMigrated, messagesFr, messagesEn)
    }

    private fun loadGoldenMaster(siteId: String): PostMigrationSnapshot {
        val resource = this::class.java.classLoader.getResource("i18n-fixtures/$siteId/post-migration")
            ?: throw IllegalStateException(
                "Golden master introuvable pour $siteId dans les resources de test"
            )
        return loader.load(File(resource.toURI()))
    }

    private fun loadTranslationsFromResources(siteId: String): Map<String, String> {
        val url = this::class.java.classLoader.getResource("i18n-fixtures/$siteId/translations_en.properties")
            ?: throw IllegalStateException("Traductions EN introuvables pour $siteId")
        val props = Properties()
        url.openStream().use { props.load(it) }
        return props.map { it.key.toString() to it.value.toString() }.toMap()
    }

    private fun loadPropertiesFromDir(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        val props = Properties()
        file.inputStream().use { props.load(it) }
        return props.map { it.key.toString() to it.value.toString() }.toMap()
    }

    private fun checksumTemplates(siteCopy: File): Map<String, String> {
        val templatesDir = siteCopy.resolve("templates")
        return templatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .associate { it.relativeTo(templatesDir).path to it.readText() }
    }
}
