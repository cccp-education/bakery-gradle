package bakery.i18n.snapshot

import bakery.i18n.I18nMigrationService
import bakery.i18n.I18nTranslationApplier
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Properties

/**
 * BKY-I18N-REAL-4 — Post-migration golden master generator.
 *
 * One-shot utility test: migrates each pre-migration fixture, applies
 * EN translations, and persists the result into
 * `src/test/resources/i18n-fixtures/{site}/post-migration/`.
 *
 * Snapshots were produced (S170). This test is now @Disabled.
 * To regenerate: remove @Disabled, run, re-validate with
 * [PostMigrationGoldenMasterTest].
 *
 * DO NOT MODIFY — snapshots freeze the expected result.
 */
@Disabled("Golden masters generated S170. Regenerate only if migration changes.")
class PostMigrationSnapshotGenerator {

    private val service = I18nMigrationService()

    @Test
    fun `generate magic-stick post-migration snapshot`() {
        generateSnapshot("magic-stick")
    }

    @Test
    fun `generate cccp-education post-migration snapshot`() {
        generateSnapshot("cccp-education")
    }

    @Test
    fun `generate cheroliv-com post-migration snapshot`() {
        generateSnapshot("cheroliv-com")
    }

    private fun generateSnapshot(siteId: String) {
        val preMigrationFixture = loadPreMigrationFixture(siteId)
        val targetOutput = File(
            "src/test/resources/i18n-fixtures/$siteId/post-migration"
        )

        if (targetOutput.exists()) {
            targetOutput.deleteRecursively()
        }

        val siteCopy = File(targetOutput.parentFile, "$siteId-temp-copy").apply {
            deleteRecursively()
            mkdirs()
        }
        preMigrationFixture.copyRecursively(siteCopy, overwrite = true)

        service.migrate(
            siteDir = siteCopy,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = false
        )

        val translationsEn = loadTranslationsFromResources("i18n-fixtures/$siteId/translations_en.properties")
        val enFile = siteCopy.resolve("templates/messages_en.properties")
        I18nTranslationApplier.applyTranslations(enFile, translationsEn)

        val postMigrationTemplates = siteCopy.resolve("templates")
        targetOutput.mkdirs()
        val targetTemplates = targetOutput.resolve("templates")
        targetTemplates.mkdirs()

        postMigrationTemplates.walkTopDown()
            .filter { it.isFile && (it.extension == "thyme" || it.name.endsWith(".properties")) }
            .forEach { sourceFile ->
                val relativePath = sourceFile.relativeTo(postMigrationTemplates)
                val targetFile = targetTemplates.resolve(relativePath)
                targetFile.parentFile.mkdirs()
                sourceFile.copyTo(targetFile, overwrite = true)
            }

        val jbakeProps = siteCopy.resolve("jbake.properties")
        if (jbakeProps.exists()) {
            jbakeProps.copyTo(targetOutput.resolve("jbake.properties"), overwrite = true)
        }

        siteCopy.deleteRecursively()

        println("[$siteId] Golden master generated at: ${targetOutput.absolutePath}")
        val templateCount = targetTemplates.walkTopDown().filter { it.isFile && it.extension == "thyme" }.count()
        println("[$siteId] Migrated templates: $templateCount")
        println("[$siteId] messages_fr: ${targetTemplates.resolve("messages_fr.properties").exists()}")
        println("[$siteId] messages_en: ${targetTemplates.resolve("messages_en.properties").exists()}")
    }

    private fun loadPreMigrationFixture(siteId: String): File {
        val resource = this::class.java.classLoader.getResource("i18n-fixtures/$siteId/jbake")
            ?: throw IllegalStateException("Pre-migration fixture not found: $siteId")
        return File(resource.toURI())
    }

    private fun loadTranslationsFromResources(resourcePath: String): Map<String, String> {
        val url = this::class.java.classLoader.getResource(resourcePath)
            ?: throw IllegalStateException("Resource not found: $resourcePath")
        val props = Properties()
        url.openStream().use { props.load(it) }
        return props.map { it.key.toString() to it.value.toString() }.toMap()
    }
}