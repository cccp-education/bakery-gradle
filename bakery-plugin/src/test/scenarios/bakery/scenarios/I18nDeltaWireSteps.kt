package bakery.scenarios

import document.translation.delta.ContentChecksum
import document.translation.ContentTranslationService
import document.translation.delta.I18nDeltaApplier
import document.translation.plantuml.PlantUmlTranslationAdapter
import document.translation.AsciiDocParser
import document.translation.JbakeNativeRenderer
import document.translation.delta.ArticleModification
import document.translation.delta.I18nDelta
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import java.nio.file.Files

class I18nDeltaWireSteps {

    private lateinit var fixtureDir: File
    private lateinit var sourceDir: File
    private lateinit var outputDir: File
    private lateinit var fakeTranslator: TranslationService
    private var lastTranslatedCount: Int = -1
    private var lastPreservedCount: Int = -1

    @Given("a cheroliv-com-i18n-deploy fixture with 3 French articles")
    fun setupFixture() {
        val resourceUrl = this::class.java.classLoader.getResource("fixtures/cheroliv-com-i18n-deploy")
            ?: throw IllegalStateException("fixture cheroliv-com-i18n-deploy not found on classpath")
        val sourceFixture = File(resourceUrl.toURI())
        fixtureDir = Files.createTempDirectory("i18n-delta-wire-").toFile()
        sourceFixture.copyRecursively(fixtureDir, overwrite = true)
        sourceDir = fixtureDir.resolve("content")
        outputDir = fixtureDir.resolve("build/i18n")

        fakeTranslator = object : TranslationService {
            override fun translate(request: TranslationRequest): TranslationResult {
                val target = request.targetLanguage
                val sourceText = request.sourceText
                if (sourceText.isBlank()) return TranslationResult.Success(sourceText)
                return TranslationResult.Success("[$target] $sourceText")
            }
        }
    }

    @Given("the delta-wire task has already migrated content from fr to {string}")
    fun alreadyMigrated(targetLang: String) {
        executeMigration(targetLang)
    }

    @Given("the source article {string} is modified")
    fun modifySourceArticle(articleName: String) {
        val article = sourceDir.resolve("blog/$articleName")
        assertThat(article).exists()
        article.writeText(article.readText() + "\n\nModified content for delta test.")
    }

    @When("the delta-wire task migrates content from fr to {string} for the first time")
    fun migrateFirstTime(targetLang: String) {
        executeMigration(targetLang)
    }

    @When("the delta-wire task migrates content from fr to {string} again")
    fun migrateAgain(targetLang: String) {
        executeMigration(targetLang)
    }

    @Then("the delta-wire task should report {string} files translated for language {string}")
    fun assertTranslatedCount(expected: String, lang: String) {
        assertThat(lastTranslatedCount)
            .describedAs("Expected $expected files translated for $lang")
            .isEqualTo(expected.toInt())
    }

    @And("the delta-wire task should report {string} files preserved for language {string}")
    fun assertPreservedCount(expected: String, lang: String) {
        assertThat(lastPreservedCount)
            .describedAs("Expected $expected files preserved for $lang")
            .isEqualTo(expected.toInt())
    }

    private fun executeMigration(targetLang: String) {
        val langDir = outputDir.resolve(targetLang)
        val currentChecksums = ContentChecksum.computeChecksums(sourceDir)
        val storedChecksums = loadStoredChecksums(langDir)
        val delta = computeDelta(storedChecksums, currentChecksums)

        val existingTargetFiles = if (langDir.exists()) {
            langDir.walkTopDown()
                .filter { it.isFile && it.extension == "adoc" }
                .map { it.relativeTo(langDir).path }
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
            val sourceFile = sourceDir.resolve(relPath)
            val targetFile = langDir.resolve(relPath)
            targetFile.parentFile.mkdirs()
            sourceFile.copyTo(targetFile, overwrite = true)
        }

        if (filesToTranslate.isNotEmpty()) {
            val fileList = filesToTranslate.map { langDir.resolve(it) }
            val plantUmlAdapter = PlantUmlTranslationAdapter(fakeTranslator)
            val contentService = ContentTranslationService(
                fakeTranslator,
                parser = AsciiDocParser(),
                renderer = JbakeNativeRenderer(),
                jbakeRenderer = JbakeNativeRenderer(),
                plantUmlAdapter = plantUmlAdapter
            )
            contentService.translateFiles(
                files = fileList,
                langDir = langDir,
                sourceLanguage = "fr",
                targetLanguage = targetLang
            )
        }

        storeChecksums(langDir, currentChecksums)
    }

    private fun loadStoredChecksums(langDir: File): Map<String, String> {
        val checksumFile = langDir.resolve(".bakery-checksums.properties")
        if (!checksumFile.exists()) return emptyMap()
        return checksumFile.readLines()
            .filter { it.contains("=") }
            .associate { line ->
                val (path, hash) = line.split("=", limit = 2)
                path to hash
            }
    }

    private fun storeChecksums(langDir: File, checksums: Map<String, String>) {
        val checksumFile = langDir.resolve(".bakery-checksums.properties")
        checksumFile.parentFile.mkdirs()
        checksumFile.writeText(
            checksums.entries.joinToString("\n") { "${it.key}=${it.value}" }
        )
    }

    private fun computeDelta(
        beforeChecksums: Map<String, String>,
        afterChecksums: Map<String, String>
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
