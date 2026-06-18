package contracts.pipeline

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReleaseNotesGeneratorContractTest {

    private var tempDir: File? = null

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("rng-test-").toFile()
        tempDir!!.deleteOnExit()
    }

    @AfterTest
    fun tearDown() {
        tempDir?.deleteRecursively()
    }

    @Test
    fun `generator should compose parser and renderer`() {
        val parser = object : GitLogParser {
            override fun parse(fromTag: String, toTag: String): List<ConventionalCommit> = listOf(
                ConventionalCommit("feat", null, "new feature", "abc", "2026-01-01")
            )
            override fun detectVersion(projectDir: File): String? = "1.0.0"
            override fun detectFromTag(projectDir: File, toTag: String): String? = null
        }
        val renderer = object : ReleaseNotesRenderer {
            override val format: String = "asciidoc"
            override fun render(commits: List<ConventionalCommit>, config: ReleaseNotesConfig): String =
                "= Release ${config.version}\n\n* ${commits.first().message}"
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
                val outputFile = File(tempDir, "RELEASE_NOTES.adoc")
                return renderer.renderToFile(commits, config, outputFile)
            }
        }

        val result = generator.generate(ReleaseNotesConfig(version = "1.0.0"))

        assertTrue(result.exists())
        val content = result.readText()
        assertTrue(content.contains("Release 1.0.0"))
        assertTrue(content.contains("new feature"))
    }

    @Test
    fun `generator should handle no commits gracefully`() {
        val parser = object : GitLogParser {
            override fun parse(fromTag: String, toTag: String): List<ConventionalCommit> = emptyList()
            override fun detectVersion(projectDir: File): String? = null
            override fun detectFromTag(projectDir: File, toTag: String): String? = null
        }
        val renderer = object : ReleaseNotesRenderer {
            override val format: String = "asciidoc"
            override fun render(commits: List<ConventionalCommit>, config: ReleaseNotesConfig): String =
                "No changes since last release"
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
                val outputFile = File(tempDir, "RELEASE_NOTES.adoc")
                return renderer.renderToFile(commits, config, outputFile)
            }
        }

        val result = generator.generate(ReleaseNotesConfig())

        assertTrue(result.exists())
        assertEquals("No changes since last release", result.readText())
    }
}
