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
 * BKY-I18N-REAL-3 — Golden master test (TDD red).
 *
 * Migrates each pre-migration fixture via [I18nMigrationService], applies
 * EN translations via [I18nTranslationApplier], then compares the result
 * against the expected snapshot loaded from `post-migration/`.
 *
 * TDD red: the test fails until post-migration snapshots exist. Once
 * snapshots are produced (REAL-4), the test goes green and freezes the
 * expected result.
 *
 * Methodology: baby step TDD. The golden master is the source of truth.
 * Any regression in the migration is detected.
 */
class PostMigrationGoldenMasterTest {

    private val service = I18nMigrationService()
    private val loader = PostMigrationSnapshotLoader()

    @Test
    fun `magic-stick migration matches golden master snapshot`(@TempDir tempDir: File) {
        assertMigrationMatchesGoldenMaster("magic-stick", tempDir)
    }

    @Test
    fun `cccp-education migration matches golden master snapshot`(@TempDir tempDir: File) {
        assertMigrationMatchesGoldenMaster("cccp-education", tempDir)
    }

    @Test
    fun `cheroliv-com migration matches golden master snapshot`(@TempDir tempDir: File) {
        assertMigrationMatchesGoldenMaster("cheroliv-com", tempDir)
    }

    /**
     * Migrates the pre-migration fixture, applies EN translations, and
     * compares against the persisted golden master snapshot.
     */
    private fun assertMigrationMatchesGoldenMaster(siteId: String, tempDir: File) {
        val preMigrationFixture = loadPreMigrationFixture(siteId)
        val siteCopy = copyFixtureToTemp(preMigrationFixture, tempDir.resolve(siteId))

        val result = service.migrate(
            siteDir = siteCopy,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = false
        )

        assertTrue(result.keysExtracted > 0, "[$siteId] Keys must be extracted")
        assertTrue(result.templatesModified > 0, "[$siteId] Templates must be modified")

        val translationsEn = loadTranslationsFromResources("i18n-fixtures/$siteId/translations_en.properties")
        val enFile = siteCopy.resolve("templates/messages_en.properties")
        I18nTranslationApplier.applyTranslations(enFile, translationsEn)

        val actualSnapshot = buildActualSnapshot(siteCopy)

        val goldenMaster = loadGoldenMaster(siteId)

        assertSnapshotsMatch(siteId, actualSnapshot, goldenMaster)
    }

    private fun assertSnapshotsMatch(
        siteId: String,
        actual: PostMigrationSnapshot,
        expected: PostMigrationSnapshot
    ) {
        assertEquals(
            expected.templatesMigrated.keys,
            actual.templatesMigrated.keys,
            "[$siteId] Migrated templates: key set mismatch (expected vs actual)"
        )
        for (templateName in expected.templatesMigrated.keys) {
            assertEquals(
                expected.templatesMigrated[templateName],
                actual.templatesMigrated[templateName],
                "[$siteId] Template '$templateName' differs from golden master"
            )
        }
        assertEquals(
            expected.messagesFr,
            actual.messagesFr,
            "[$siteId] messages_fr.properties differs from golden master"
        )
        assertEquals(
            expected.messagesEn,
            actual.messagesEn,
            "[$siteId] messages_en.properties differs from golden master"
        )
        assertTrue(actual.isComplete(), "[$siteId] Actual snapshot must be complete")
    }

    private fun buildActualSnapshot(siteCopy: File): PostMigrationSnapshot {
        val templatesDir = siteCopy.resolve("templates")
        val templatesMigrated = templatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .associate { it.relativeTo(templatesDir).path to it.readText() }
        val messagesFr = loadProperties(templatesDir.resolve("messages_fr.properties"))
        val messagesEn = loadProperties(templatesDir.resolve("messages_en.properties"))
        return PostMigrationSnapshot(templatesMigrated, messagesFr, messagesEn)
    }

    private fun loadPreMigrationFixture(siteId: String): File {
        val resource = this::class.java.classLoader.getResource("i18n-fixtures/$siteId/jbake")
            ?: throw IllegalStateException("Pre-migration fixture not found: $siteId")
        return File(resource.toURI())
    }

    private fun loadGoldenMaster(siteId: String): PostMigrationSnapshot {
        val resource = this::class.java.classLoader.getResource("i18n-fixtures/$siteId/post-migration")
            ?: throw IllegalStateException(
                "Golden master not found for $siteId. " +
                    "Run REAL-4 to produce the post-migration snapshot."
            )
        return loader.load(File(resource.toURI()))
    }

    private fun loadTranslationsFromResources(resourcePath: String): Map<String, String> {
        val url = this::class.java.classLoader.getResource(resourcePath)
            ?: throw IllegalStateException("Resource not found: $resourcePath")
        val props = Properties()
        url.openStream().use { props.load(it) }
        return props.map { it.key.toString() to it.value.toString() }.toMap()
    }

    private fun loadProperties(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        val props = Properties()
        file.inputStream().use { props.load(it) }
        return props.map { it.key.toString() to it.value.toString() }.toMap()
    }

    private fun copyFixtureToTemp(fixture: File, target: File): File {
        fixture.copyRecursively(target, overwrite = true)
        return target
    }
}