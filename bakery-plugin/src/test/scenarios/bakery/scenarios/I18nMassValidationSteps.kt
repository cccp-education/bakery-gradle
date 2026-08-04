package bakery.scenarios

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import document.translation.AsciiDocParser
import document.translation.ContentTranslationService
import document.translation.JbakeNativeRenderer
import document.translation.plantuml.PlantUmlTranslationAdapter
import document.translation.validation.ValidationMode
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import java.nio.file.Files

class I18nMassValidationSteps {
    private lateinit var fixtureDir: File
    private lateinit var sourceDir: File
    private lateinit var outputDir: File
    private lateinit var fakeTranslator: TranslationService
    private var lastReportFile: File? = null

    @io.cucumber.java.en.Given("a cheroliv-com-i18n-deploy fixture with 3 French articles")
    fun setupFixture() {
        val resourceUrl =
            this::class.java.classLoader.getResource("fixtures/cheroliv-com-i18n-deploy")
                ?: throw IllegalStateException("fixture cheroliv-com-i18n-deploy not found on classpath")
        val sourceFixture = File(resourceUrl.toURI())
        fixtureDir = Files.createTempDirectory("i18n-mass-validation-").toFile()
        sourceFixture.copyRecursively(fixtureDir, overwrite = true)
        sourceDir = fixtureDir.resolve("content")
        outputDir = fixtureDir.resolve("build/i18n")

        fakeTranslator =
            object : TranslationService {
                override fun translate(request: TranslationRequest): TranslationResult {
                    val target = request.targetLanguage
                    val sourceText = request.sourceText
                    if (sourceText.isBlank()) return TranslationResult.Success(sourceText)
                    return TranslationResult.Success("[$target] $sourceText")
                }
            }
    }

    @When("the validation task migrates content from fr to {string} with validation mode {string}")
    fun migrateWithValidationMode(
        targetLang: String,
        validationMode: String,
    ) {
        val langDir = outputDir.resolve(targetLang)
        langDir.mkdirs()

        val adocFiles = sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "adoc" }
            .toList()

        val fileList = adocFiles.map { src ->
            val relPath = src.relativeTo(sourceDir).path
            val tgt = langDir.resolve(relPath)
            tgt.parentFile.mkdirs()
            src.copyTo(tgt, overwrite = true)
            tgt
        }

        val mode = try {
            ValidationMode.valueOf(validationMode)
        } catch (e: IllegalArgumentException) {
            ValidationMode.LENIENT
        }

        val plantUmlAdapter = PlantUmlTranslationAdapter(fakeTranslator, plantUmlValidationMode = mode)
        val contentService =
            ContentTranslationService(
                fakeTranslator,
                parser = AsciiDocParser(),
                renderer = JbakeNativeRenderer(),
                jbakeRenderer = JbakeNativeRenderer(),
                plantUmlAdapter = plantUmlAdapter,
            )
        contentService.translateFiles(
            files = fileList,
            langDir = langDir,
            sourceLanguage = "fr",
            targetLanguage = targetLang,
        )

        val tableResults = contentService.drainTableValidationResults()
        val plantUmlResults = contentService.drainPlantUmlValidationResults()

        val tableReport = document.translation.validation.TableValidationReport.fromResults(tableResults)
        val plantUmlReport = document.translation.validation.PlantUmlValidationReport.fromResults(plantUmlResults)

        val consolidated = bakery.i18n.ValidationReport(
            table = tableReport.entries,
            plantUml = plantUmlReport.entries,
        )
        val reportFile = outputDir.resolve("validation-report.json")
        reportFile.writeText(consolidated.toJson())
        lastReportFile = reportFile
    }

    @Then("a validation report should exist at {string}")
    fun assertReportExists(path: String) {
        val reportFile = outputDir.resolve("validation-report.json")
        assertThat(reportFile)
            .describedAs("validation-report.json should exist at $path")
            .exists()
        lastReportFile = reportFile
    }

    @io.cucumber.java.en.And("the validation report should contain {string} and {string} sections")
    fun assertReportContainsSections(
        section1: String,
        section2: String,
    ) {
        val report = lastReportFile?.readText() ?: throw IllegalStateException("No report file")
        assertThat(report).contains(section1)
        assertThat(report).contains(section2)
    }

    @Then("the validation report should be valid JSON")
    fun assertReportIsValidJson() {
        val reportFile = outputDir.resolve("validation-report.json")
        val report = reportFile.readText()
        assertThat(report.trim()).startsWith("{")
        assertThat(report.trim()).endsWith("}")
        assertThat(report).contains("\"table\"")
        assertThat(report).contains("\"plantUml\"")
    }

    @Then("the validation report should have zero invalid entries")
    fun assertReportHasZeroInvalidEntries() {
        val reportFile = outputDir.resolve("validation-report.json")
        val report = reportFile.readText()
        assertThat(report).doesNotContain("\"INVALID\"")
    }
}
