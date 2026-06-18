package contracts.pipeline.scenarios

import contracts.pipeline.ConventionalCommit
import contracts.pipeline.GitLogParser
import contracts.pipeline.ReleaseNotesConfig
import contracts.pipeline.ReleaseNotesGenerator
import contracts.pipeline.ReleaseNotesRenderer
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PipelineContractsStepDefs {

    private var commit: ConventionalCommit? = null
    private var config: ReleaseNotesConfig? = null
    private var renderedString: String = ""
    private var outputFile: File? = null
    private var tempDir: File = Files.createTempDirectory("pc-bdd-").toFile()
    private var generatedFile: File? = null

    init { tempDir.deleteOnExit() }

    @Given("a repository with conventional commits")
    fun givenRepository() {
        // no-op
    }

    @When("a ConventionalCommit of type {string} with scope {string} and message {string} is created")
    fun whenCommit(type: String, scope: String, message: String) {
        commit = ConventionalCommit(
            type = type,
            scope = scope,
            message = message,
            hash = "abc123def",
            date = "2026-06-14T10:00:00Z"
        )
    }

    @When("a ConventionalCommit of type {string} with no scope and message {string} is created")
    fun whenCommitNoScope(type: String, message: String) {
        commit = ConventionalCommit(
            type = type,
            scope = null,
            message = message,
            hash = "def456",
            date = "2026-06-14T11:00:00Z"
        )
    }

    @When("a default ReleaseNotesConfig is created")
    fun whenDefaultConfig() {
        config = ReleaseNotesConfig()
    }

    @When("a ReleaseNotesConfig is created with fromTag {string} and toTag {string}")
    fun whenConfigTags(fromTag: String, toTag: String) {
        config = ReleaseNotesConfig(fromTag = fromTag, toTag = toTag)
    }

    @When("a ReleaseNotesConfig is created with rendererType {string}")
    fun whenConfigRenderer(rendererType: String) {
        config = ReleaseNotesConfig(rendererType = rendererType)
    }

    @When("a GitLogParser implementation returns {int} commits between {string} and {string}")
    fun whenParser(count: Int, fromTag: String, toTag: String) {
        val commits = (1..count).map { i ->
            ConventionalCommit(
                type = if (i == 1) "feat" else "fix",
                scope = null,
                message = "commit $i",
                hash = "hash$i",
                date = "2026-01-0${i}T00:00:00Z"
            )
        }
        val parser = object : GitLogParser {
            override fun parse(fromTag: String, toTag: String): List<ConventionalCommit> = commits
            override fun detectVersion(projectDir: File): String? = null
            override fun detectFromTag(projectDir: File, toTag: String): String? = null
        }
        val result = parser.parse(fromTag, toTag)
        this.commit = if (result.isNotEmpty()) result.first() else null
        org.assertj.core.api.Assertions.assertThat(result).hasSize(count)
    }

    @When("a GitLogParser implementation detects version {string} from project directory")
    fun whenDetectVersion(version: String) {
        val parser = object : GitLogParser {
            override fun parse(fromTag: String, toTag: String): List<ConventionalCommit> = emptyList()
            override fun detectVersion(projectDir: File): String? = version
            override fun detectFromTag(projectDir: File, toTag: String): String? = null
        }
        val result = parser.detectVersion(File("/tmp"))
        assertEquals(version, result)
    }

    @When("a ReleaseNotesRenderer implementation declares format {string}")
    fun whenRendererFormat(format: String) {
        val renderer = object : ReleaseNotesRenderer {
            override val format: String = format
            override fun render(commits: List<ConventionalCommit>, config: ReleaseNotesConfig): String = ""
            override fun renderToFile(commits: List<ConventionalCommit>, config: ReleaseNotesConfig, outputFile: File): File = outputFile
        }
        assertEquals(format, renderer.format)
    }

    @When("a ReleaseNotesRenderer renders {int} commits for version {string}")
    fun whenRendererRenders(count: Int, version: String) {
        val commits = (1..count).map { i ->
            ConventionalCommit("feat", null, "commit $i", "hash$i", "2026-01-0${i}")
        }
        val renderer = object : ReleaseNotesRenderer {
            override val format: String = "asciidoc"
            override fun render(commits: List<ConventionalCommit>, config: ReleaseNotesConfig): String {
                val header = "= Release ${config.version ?: "SNAPSHOT"}"
                val body = commits.joinToString("\n") { "* ${it.message}" }
                return "$header\n\n$body"
            }
            override fun renderToFile(commits: List<ConventionalCommit>, config: ReleaseNotesConfig, outputFile: File): File {
                outputFile.writeText(render(commits, config))
                return outputFile
            }
        }
        renderedString = renderer.render(commits, ReleaseNotesConfig(version = version))
        this.commit = commits.first()
    }

    @When("a ReleaseNotesRenderer writes {int} commit to a file")
    fun whenRendererWritesToFile(count: Int) {
        val commits = (1..count).map { i ->
            ConventionalCommit("feat", null, "commit $i", "hash$i", "2026-01-0${i}")
        }
        val renderer = object : ReleaseNotesRenderer {
            override val format: String = "json"
            override fun render(commits: List<ConventionalCommit>, config: ReleaseNotesConfig): String =
                commits.joinToString(",") { it.message }

            override fun renderToFile(commits: List<ConventionalCommit>, config: ReleaseNotesConfig, outputFile: File): File {
                outputFile.writeText(render(commits, config))
                return outputFile
            }
        }
        outputFile = File(tempDir, "release-notes.txt")
        renderer.renderToFile(commits, ReleaseNotesConfig(), outputFile!!)
        this.commit = commits.first()
    }

    @When("a ReleaseNotesGenerator generates release notes for version {string}")
    fun whenGeneratorGenerates(version: String) {
        val parser = object : GitLogParser {
            override fun parse(fromTag: String, toTag: String): List<ConventionalCommit> = listOf(
                ConventionalCommit("feat", null, "new feature", "abc", "2026-01-01"),
                ConventionalCommit("fix", null, "bug fix", "def", "2026-01-02")
            )
            override fun detectVersion(projectDir: File): String? = version
            override fun detectFromTag(projectDir: File, toTag: String): String? = "v1.0.0"
        }
        val renderer = object : ReleaseNotesRenderer {
            override val format: String = "asciidoc"
            override fun render(commits: List<ConventionalCommit>, config: ReleaseNotesConfig): String {
                val header = "= Release ${config.version ?: "SNAPSHOT"}"
                val body = commits.joinToString("\n") { "* ${it.message}" }
                return "$header\n\n$body"
            }
            override fun renderToFile(commits: List<ConventionalCommit>, config: ReleaseNotesConfig, outputFile: File): File {
                outputFile.writeText(render(commits, config))
                return outputFile
            }
        }
        val generator = object : ReleaseNotesGenerator {
            override val parser: GitLogParser = parser
            override val renderer: ReleaseNotesRenderer = renderer
            override fun generate(config: ReleaseNotesConfig): File {
                val commits = parser.parse(config.fromTag ?: "initial", config.toTag)
                val outFile = File(tempDir, "RELEASE_NOTES.adoc")
                return renderer.renderToFile(commits, config, outFile)
            }
        }
        generatedFile = generator.generate(ReleaseNotesConfig(version = version))
        commit = ConventionalCommit("feat", null, "new feature", "abc", "2026-01-01")
    }

    @Then("the commit type is {string}")
    fun thenCommitType(type: String) {
        assertEquals(type, commit!!.type)
    }

    @Then("the commit scope is {string}")
    fun thenScope(scope: String) {
        assertEquals(scope, commit!!.scope)
    }

    @Then("the commit scope is absent")
    fun thenScopeAbsent() {
        assertNull(commit!!.scope)
    }

    @Then("the commit message is {string}")
    fun thenCommitMessage(message: String) {
        assertEquals(message, commit!!.message)
    }

    @Then("the commit hash is not empty")
    fun thenHashNotEmpty() {
        assertTrue(commit!!.hash.isNotEmpty())
    }

    @Then("the commit date is not empty")
    fun thenDateNotEmpty() {
        assertTrue(commit!!.date.isNotEmpty())
    }

    @Then("the config has {int} categories")
    fun thenCategoryCount(count: Int) {
        assertEquals(count, config!!.categories.size)
    }

    @Then("the category {string} maps to {string}")
    fun thenCategoryMaps(type: String, label: String) {
        assertEquals(label, config!!.categories[type])
    }

    @Then("the default renderer type is {string}")
    fun thenDefaultRendererType(type: String) {
        assertEquals(type, config!!.rendererType)
    }

    @Then("the fromTag is {string}")
    fun thenFromTag(tag: String) {
        assertEquals(tag, config!!.fromTag)
    }

    @Then("the toTag is {string}")
    fun thenToTag(tag: String) {
        assertEquals(tag, config!!.toTag)
    }

    @Then("the renderer type is {string}")
    fun thenRendererType(type: String) {
        assertEquals(type, config!!.rendererType)
    }

    @Then("the parser returns {int} commits")
    fun thenParserCount(count: Int) {
        // Verified in when step via assertj
    }

    @Then("the first commit type is {string}")
    fun thenFirstCommitType(type: String) {
        assertEquals(type, commit!!.type)
    }

    @Then("the detected version is {string}")
    fun thenDetectedVersion(version: String) {
        // Verified in when step
    }

    @Then("the renderer format is {string}")
    fun thenRendererFormat(format: String) {
        // Verified in when step
    }

    @Then("the rendered string contains {string}")
    fun thenRenderedContains(text: String) {
        assertTrue(renderedString.contains(text))
    }

    @Then("the rendered string contains the first commit message")
    fun thenRenderedContainsFirstCommit() {
        assertTrue(renderedString.contains(commit!!.message))
    }

    @Then("the file exists")
    fun thenFileExists() {
        assertTrue(outputFile!!.exists())
    }

    @Then("the file contains the commit message")
    fun thenFileContainsMessage() {
        val content = outputFile!!.readText()
        assertTrue(content.contains(commit!!.message))
    }

    @Then("the output file exists")
    fun thenOutputFileExists() {
        assertNotNull(generatedFile)
        assertTrue(generatedFile!!.exists())
    }

    @Then("the output file contains {string}")
    fun thenOutputFileContains(text: String) {
        val content = generatedFile!!.readText()
        assertTrue(content.contains(text))
    }

    @Then("the output file contains all commit messages")
    fun thenOutputFileContainsAllCommits() {
        val content = generatedFile!!.readText()
        assertTrue(content.contains("new feature"))
        assertTrue(content.contains("bug fix"))
    }
}
