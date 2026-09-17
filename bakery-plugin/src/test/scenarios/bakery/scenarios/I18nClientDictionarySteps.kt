package bakery.scenarios

import bakery.i18n.js.I18nClientDelta
import bakery.i18n.js.I18nClientMigrationIntention
import bakery.i18n.js.I18nJsDictionary
import bakery.i18n.js.TranslateI18nClientTask
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-3.
 *
 * BDD steps for feature 64 (`translateI18nClient`). The task runs through the
 * real [TranslateI18nClientTask] against a temporary copy of the
 * `talaria-i18n-client` fixture, with a label-aware fake translator — no LLM is
 * ever called.
 *
 * The reported missing count is the size of the delta *planned before* the run
 * (the post-run files are the translated ones).
 */
class I18nClientDictionarySteps {
    private lateinit var projectDir: File
    private lateinit var chromeFile: File
    private lateinit var patchFile: File
    private lateinit var task: TranslateI18nClientTask
    private lateinit var recording: RecordingTranslationService
    private var lastMissing = -1
    private var dryRun = false
    private var originalChrome: String = ""
    private var originalPatch: String = ""

    @Given("a talaria i18n client fixture with chrome and patch dictionaries")
    fun setupFixture() {
        val resourceUrl =
            this::class.java.classLoader.getResource("fixtures/talaria-i18n-client")
                ?: throw IllegalStateException("fixture talaria-i18n-client not found on classpath")
        val sourceFixture = File(resourceUrl.toURI())
        projectDir = Files.createTempDirectory("i18n-client-").toFile()
        sourceFixture.copyRecursively(projectDir, overwrite = true)

        chromeFile = projectDir.resolve("maquette/js/i18n.js")
        patchFile = projectDir.resolve("maquette/js/i18n-extra-langs.js")
        originalChrome = chromeFile.readText()
        originalPatch = patchFile.readText()
        dryRun = false
        recording = RecordingTranslationService()

        val project =
            ProjectBuilder
                .builder()
                .withProjectDir(projectDir)
                .withName("talaria-i18n-client")
                .build()
        project.pluginManager.apply("java-base")
        task = project.tasks.register("translateI18nClient", TranslateI18nClientTask::class.java).get()
    }

    @Given("the i18n client translation service always fails")
    fun serviceAlwaysFails() {
        recording = RecordingTranslationService(alwaysFails = true)
    }

    @Given("the i18n client task runs in dry-run mode")
    fun runsInDryRun() {
        dryRun = true
    }

    @Given("a stale jbake publication copy of the chrome dictionary")
    fun stalePublicationCopy() {
        publicationFile().also {
            it.parentFile.mkdirs()
            it.writeText("var DICT = { stale };")
        }
    }

    @Given("an aligned jbake publication copy of the chrome dictionary")
    fun alignedPublicationCopy() {
        publicationFile().also {
            it.parentFile.mkdirs()
            it.writeText(chromeFile.readText())
        }
    }

    @Given("the i18n client task has already translated the dictionaries from fr to {string}")
    fun alreadyTranslated(targetLang: String) {
        translate(targetLang)
    }

    @When("the i18n client task translates the dictionaries from fr to {string}")
    fun translates(
        targetLang: String,
    ) {
        translate(targetLang)
    }

    @When("the i18n client task translates the dictionaries from fr to {string} again")
    fun translatesAgain(
        targetLang: String,
    ) {
        translate(targetLang)
    }

    @Then("the i18n client task should report {string} missing keys")
    fun assertMissingCount(
        expected: String,
    ) {
        assertThat(lastMissing)
            .describedAs("Expected $expected missing keys")
            .isEqualTo(expected.toInt())
    }

    @Then("no translation request should have been sent")
    fun assertNoRequest() {
        assertThat(recording.requests).isEmpty()
    }

    @Then("the translation service should have received exactly {string} request")
    fun assertRequestCountSingular(
        expected: String,
    ) {
        assertRequestCount(expected)
    }

    @Then("the translation service should have received exactly {string} requests")
    fun assertRequestCountPlural(
        expected: String,
    ) {
        assertRequestCount(expected)
    }

    private fun assertRequestCount(expected: String) {
        assertThat(recording.requests)
            .describedAs("Expected $expected translation request(s)")
            .hasSize(expected.toInt())
    }

    @Then("the translation request for the key {string} should be sourced from {string}")
    fun assertRequestSource(
        key: String,
        expectedSource: String,
    ) {
        val request = recording.requests.single()
        assertThat(request.sourceText)
            .describedAs("Source text for $key")
            .isEqualTo(expectedSource)
    }

    @Then("the i18n client dictionary {string} key {string} should be {string}")
    fun assertDictionaryValue(
        language: String,
        key: String,
        expected: String,
    ) {
        val dictionary = I18nJsDictionary.parse(chromeFile.readText(), patchFile.readText())
        assertThat(dictionary[language]).describedAs("Language $language block").isNotNull
        assertThat(dictionary[language]!![key]).isEqualTo(expected)
    }

    @Then("the source dictionary should be byte-identical to the fixture")
    fun assertByteIdentical() {
        assertThat(chromeFile.readText()).isEqualTo(originalChrome)
        assertThat(patchFile.readText()).isEqualTo(originalPatch)
    }

    @Then("the jbake publication copy should be byte-identical to the maquette source")
    fun assertPublicationAligned() {
        assertThat(publicationFile().readText()).isEqualTo(chromeFile.readText())
    }

    @Then("the jbake publication copy should still hold the stale content")
    fun assertPublicationUntouched() {
        assertThat(publicationFile().readText()).isEqualTo("var DICT = { stale };")
    }

    private fun publicationFile(): File = projectDir.resolve("jbake/assets/js/i18n.js")

    private fun translate(targetLang: String) {
        lastMissing = plan(targetLang).sumOf { it.keys.size }
        task.dslIntention =
            I18nClientMigrationIntention(
                sourceDirs = listOf("maquette/js"),
                referenceLanguage = "fr",
                targetLanguages = listOf(targetLang),
                dryRun = dryRun,
            )
        task.translationService = recording
        task.executeI18nClientTranslation()
    }

    private fun plan(targetLang: String) =
        I18nClientDelta.plan(
            files =
                mapOf(
                    "i18n.js" to chromeFile.readText(),
                    "i18n-extra-langs.js" to patchFile.readText(),
                ),
            referenceLanguage = "fr",
            targetLanguages = listOf(targetLang),
        )

    private class RecordingTranslationService(
        private val alwaysFails: Boolean = false,
    ) : TranslationService {
        val requests = mutableListOf<TranslationRequest>()

        override fun translate(request: TranslationRequest): TranslationResult {
            requests.add(request)
            if (alwaysFails) return TranslationResult.Failure("quota exceeded")
            return TranslationResult.Success("«${request.targetLanguage}» ${request.sourceText}")
        }
    }
}
