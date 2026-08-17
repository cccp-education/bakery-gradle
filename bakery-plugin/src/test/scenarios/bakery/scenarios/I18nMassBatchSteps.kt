package bakery.scenarios

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import document.translation.AsciiDocParser
import document.translation.ContentTranslationService
import document.translation.JbakeNativeRenderer
import document.translation.delta.ArticleModification
import document.translation.delta.ContentChecksum
import document.translation.delta.I18nDelta
import document.translation.delta.I18nDeltaApplier
import document.translation.plantuml.PlantUmlTranslationAdapter
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import java.nio.file.Files

class I18nMassBatchSteps {
    private lateinit var fixtureDir: File
    private lateinit var contentRoot: File
    private lateinit var outputDir: File
    private lateinit var langDir: File
    private lateinit var fakeTranslator: TranslationService
    private var lastTranslatedCount: Int = -1
    private var lastPreservedCount: Int = -1

    @Given("a mass-batch fixture with 2 annual lots and 4 French articles")
    fun setupFixture() {
        val resourceUrl =
            this::class.java.classLoader.getResource("fixtures/cheroliv-com-mass-batch")
                ?: throw IllegalStateException("fixture cheroliv-com-mass-batch not found on classpath")
        val sourceFixture = File(resourceUrl.toURI())
        fixtureDir = Files.createTempDirectory("i18n-mass-batch-").toFile()
        sourceFixture.copyRecursively(fixtureDir, overwrite = true)
        contentRoot = fixtureDir.resolve("content")
        outputDir = fixtureDir.resolve("build/i18n")
        langDir = outputDir.resolve("en")

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

    @Given("the mass-batch task has already migrated lot {string} from fr to {string}")
    fun alreadyMigratedLot(
        year: String,
        targetLang: String,
    ) {
        executeLotMigration(year, targetLang)
    }

    @When("the mass-batch task migrates lot {string} from fr to {string}")
    fun migrateLot(
        year: String,
        targetLang: String,
    ) {
        executeLotMigration(year, targetLang)
    }

    @When("the mass-batch task migrates lot {string} from fr to {string} again")
    fun migrateLotAgain(
        year: String,
        targetLang: String,
    ) {
        executeLotMigration(year, targetLang)
    }

    @Then("the mass-batch task should report {string} files translated for language {string}")
    fun assertTranslatedCount(
        expected: String,
        lang: String,
    ) {
        assertThat(lastTranslatedCount)
            .describedAs("Expected $expected files translated for $lang")
            .isEqualTo(expected.toInt())
    }

    @And("the mass-batch task should report {string} files preserved for language {string}")
    fun assertPreservedCount(
        expected: String,
        lang: String,
    ) {
        assertThat(lastPreservedCount)
            .describedAs("Expected $expected files preserved for $lang")
            .isEqualTo(expected.toInt())
    }

    @Then("the mass-batch {string} article {string} should contain {string}")
    fun assertArticleContains(
        lang: String,
        articleName: String,
        expected: String,
    ) {
        val article = outputDir.resolve(lang).resolve(articleName)
        assertThat(article).exists()
        assertThat(article.readText()).contains(expected)
    }

    private fun executeLotMigration(
        year: String,
        targetLang: String,
    ) {
        val lotSourceDir = contentRoot.resolve("blog/$year")
        val targetLangDir = outputDir.resolve(targetLang)
        val currentChecksums = ContentChecksum.computeChecksums(lotSourceDir)
        val storedChecksums = loadStoredChecksums(targetLangDir)
        val delta = computeDelta(storedChecksums, currentChecksums)

        val existingTargetFiles =
            if (targetLangDir.exists()) {
                targetLangDir
                    .walkTopDown()
                    .filter { it.isFile && it.extension == "adoc" }
                    .map { it.relativeTo(targetLangDir).path }
                    .toSet()
            } else {
                emptySet()
            }

        val applier = I18nDeltaApplier(delta, existingTargetFiles)
        val result = applier.apply()

        val filesToTranslate = result.toTranslate.paths
        lastTranslatedCount = filesToTranslate.size
        lastPreservedCount = result.toPreserve.paths.size

        for (relPath in filesToTranslate) {
            val sourceFile = lotSourceDir.resolve(relPath)
            val targetFile = targetLangDir.resolve(relPath)
            targetFile.parentFile.mkdirs()
            sourceFile.copyTo(targetFile, overwrite = true)
        }

        if (filesToTranslate.isNotEmpty()) {
            val fileList = filesToTranslate.map { targetLangDir.resolve(it) }
            val plantUmlAdapter = PlantUmlTranslationAdapter(fakeTranslator)
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
                langDir = targetLangDir,
                sourceLanguage = "fr",
                targetLanguage = targetLang,
            )
        }

        val merged = storedChecksums + currentChecksums
        storeChecksums(targetLangDir, merged)
    }

    private fun loadStoredChecksums(langDir: File): Map<String, String> {
        val checksumFile = langDir.resolve(".bakery-checksums.properties")
        if (!checksumFile.exists()) return emptyMap()
        return checksumFile
            .readLines()
            .filter { it.contains("=") }
            .associate { line ->
                val (path, hash) = line.split("=", limit = 2)
                path to hash
            }
    }

    private fun storeChecksums(
        langDir: File,
        checksums: Map<String, String>,
    ) {
        val checksumFile = langDir.resolve(".bakery-checksums.properties")
        checksumFile.parentFile.mkdirs()
        checksumFile.writeText(
            checksums.entries.joinToString("\n") { "${it.key}=${it.value}" },
        )
    }

    private fun computeDelta(
        beforeChecksums: Map<String, String>,
        afterChecksums: Map<String, String>,
    ): I18nDelta {
        val modified = mutableListOf<ArticleModification>()
        for ((path, afterHash) in afterChecksums) {
            val beforeHash = beforeChecksums[path]
            if (beforeHash == null || beforeHash != afterHash) {
                modified.add(ArticleModification(path, beforeHash, afterHash, 0))
            }
        }
        return I18nDelta(modified, emptyList(), afterChecksums)
    }
}