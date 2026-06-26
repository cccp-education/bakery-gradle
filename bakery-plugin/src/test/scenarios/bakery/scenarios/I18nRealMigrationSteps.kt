package bakery.scenarios

import bakery.i18n.I18nMigrationResult
import bakery.i18n.I18nMigrationService
import bakery.i18n.I18nTranslationApplier
import bakery.i18n.snapshot.PostMigrationSnapshot
import bakery.i18n.snapshot.PostMigrationSnapshotLoader
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import java.io.File
import java.nio.file.Files
import java.util.Properties

/**
 * BKY-I18N-REAL-5 — Cucumber step definitions for feature 46.
 *
 * Validates end to end: migration -> golden master -> idempotence.
 * Does not use BakeryWorld — drives I18nMigrationService directly.
 */
class I18nRealMigrationSteps {

    private val service = I18nMigrationService()
    private val loader = PostMigrationSnapshotLoader()

    private var siteCopy: File? = null
    private var siteId: String = ""
    private var firstMigrationResult: I18nMigrationResult? = null
    private var secondMigrationResult: I18nMigrationResult? = null
    private var templatesBeforeReMigration: Map<String, String> = emptyMap()

    @Given("a post-migration snapshot loader is available")
    fun givenSnapshotLoader() {
        assertNotNull(loader, "PostMigrationSnapshotLoader must be available")
    }

    @Given("a copy of pre-migration fixture {string}")
    fun givenCopyOfPreMigrationFixture(fixtureId: String) {
        siteId = fixtureId
        val resource = this::class.java.classLoader.getResource("i18n-fixtures/$fixtureId/jbake")
            ?: throw IllegalStateException("Pre-migration fixture not found: $fixtureId")
        val fixture = File(resource.toURI())
        val tempDir = Files.createTempDirectory("i18n-bdd-$fixtureId-").toFile()
        tempDir.deleteOnExit()
        siteCopy = tempDir.resolve(fixtureId).also { fixture.copyRecursively(it, overwrite = true) }
    }

    @When("the fixture is migrated with languages {string} and defaultLanguage {string}")
    fun whenFixtureIsMigrated(languagesCsv: String, defaultLanguage: String) {
        val languages = languagesCsv.split(",").map { it.trim() }
        firstMigrationResult = service.migrate(
            siteDir = siteCopy!!,
            languages = languages,
            defaultLanguage = defaultLanguage,
            dryRun = false
        )
    }

    @And("english translations from {string} are applied")
    fun andEnglishTranslationsApplied(resourcePath: String) {
        val url = this::class.java.classLoader.getResource(resourcePath)
            ?: throw IllegalStateException("Resource not found: $resourcePath")
        val props = Properties()
        url.openStream().use { props.load(it) }
        val translations = props.map { it.key.toString() to it.value.toString() }.toMap()
        val enFile = siteCopy!!.resolve("templates/messages_en.properties")
        I18nTranslationApplier.applyTranslations(enFile, translations)
    }

    @Then("the migrated templates should match the golden master for {string}")
    fun thenMigratedTemplatesMatchGoldenMaster(expectedSiteId: String) {
        val actual = buildActualSnapshot(siteCopy!!)
        val goldenMaster = loadGoldenMaster(expectedSiteId)

        assertEquals(
            goldenMaster.templatesMigrated.keys,
            actual.templatesMigrated.keys,
            "[$expectedSiteId] Migrated templates: key set mismatch with golden master"
        )
        for (templateName in goldenMaster.templatesMigrated.keys) {
            assertEquals(
                goldenMaster.templatesMigrated[templateName],
                actual.templatesMigrated[templateName],
                "[$expectedSiteId] Template '$templateName' differs from golden master"
            )
        }
    }

    @And("messages_fr.properties should match the golden master for {string}")
    fun andMessagesFrMatchGoldenMaster(expectedSiteId: String) {
        val actual = buildActualSnapshot(siteCopy!!)
        val goldenMaster = loadGoldenMaster(expectedSiteId)
        assertEquals(
            goldenMaster.messagesFr,
            actual.messagesFr,
            "[$expectedSiteId] messages_fr.properties differs from golden master"
        )
    }

    @And("messages_en.properties should match the golden master for {string}")
    fun andMessagesEnMatchGoldenMaster(expectedSiteId: String) {
        val actual = buildActualSnapshot(siteCopy!!)
        val goldenMaster = loadGoldenMaster(expectedSiteId)
        assertEquals(
            goldenMaster.messagesEn,
            actual.messagesEn,
            "[$expectedSiteId] messages_en.properties differs from golden master"
        )
    }

    @And("the already migrated fixture is migrated again")
    fun andAlreadyMigratedFixtureIsMigratedAgain() {
        templatesBeforeReMigration = captureTemplates(siteCopy!!)
        secondMigrationResult = service.migrate(
            siteDir = siteCopy!!,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = false
        )
    }

    @Then("the second migration should extract {int} keys")
    fun thenSecondMigrationShouldExtractKeys(expectedKeys: Int) {
        assertEquals(expectedKeys, secondMigrationResult!!.keysExtracted, "Second migration keys extracted")
    }

    @And("the second migration should modify {int} templates")
    fun andSecondMigrationShouldModifyTemplates(expectedTemplates: Int) {
        assertEquals(expectedTemplates, secondMigrationResult!!.templatesModified, "Second migration templates modified")
    }

    @And("the templates should be unchanged after re-migration")
    fun andTemplatesShouldBeUnchangedAfterReMigration() {
        val templatesAfter = captureTemplates(siteCopy!!)
        assertEquals(
            templatesBeforeReMigration,
            templatesAfter,
            "Templates unchanged after re-migration (idempotence)"
        )
    }

    private fun buildActualSnapshot(siteDir: File): PostMigrationSnapshot {
        val templatesDir = siteDir.resolve("templates")
        val templatesMigrated = templatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .associate { it.relativeTo(templatesDir).path to it.readText() }
        val messagesFr = loadProperties(templatesDir.resolve("messages_fr.properties"))
        val messagesEn = loadProperties(templatesDir.resolve("messages_en.properties"))
        return PostMigrationSnapshot(templatesMigrated, messagesFr, messagesEn)
    }

    private fun loadGoldenMaster(siteId: String): PostMigrationSnapshot {
        val resource = this::class.java.classLoader.getResource("i18n-fixtures/$siteId/post-migration")
            ?: throw IllegalStateException("Golden master not found for $siteId")
        return loader.load(File(resource.toURI()))
    }

    private fun loadProperties(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        val props = Properties()
        file.inputStream().use { props.load(it) }
        return props.map { it.key.toString() to it.value.toString() }.toMap()
    }

    private fun captureTemplates(siteDir: File): Map<String, String> {
        val templatesDir = siteDir.resolve("templates")
        return templatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .associate { it.relativeTo(templatesDir).path to it.readText() }
    }
}