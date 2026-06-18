package contracts.pipeline

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReleaseNotesRendererContractTest {

    private var tempDir: File? = null

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("rn-test-").toFile()
        tempDir!!.deleteOnExit()
    }

    @AfterTest
    fun tearDown() {
        tempDir?.deleteRecursively()
    }

    @Test
    fun `renderer should expose format property`() {
        val renderer = object : ReleaseNotesRenderer {
            override val format: String = "asciidoc"
            override fun render(commits: List<ConventionalCommit>, config: ReleaseNotesConfig): String = ""
            override fun renderToFile(commits: List<ConventionalCommit>, config: ReleaseNotesConfig, outputFile: File): File = outputFile
        }

        assertEquals("asciidoc", renderer.format)
    }

    @Test
    fun `renderer should support markdown format`() {
        val renderer = object : ReleaseNotesRenderer {
            override val format: String = "markdown"
            override fun render(commits: List<ConventionalCommit>, config: ReleaseNotesConfig): String = "# Release Notes"
            override fun renderToFile(commits: List<ConventionalCommit>, config: ReleaseNotesConfig, outputFile: File): File = outputFile
        }

        assertEquals("markdown", renderer.format)
    }

    @Test
    fun `render should return empty string for no commits`() {
        val renderer = object : ReleaseNotesRenderer {
            override val format: String = "asciidoc"
            override fun render(commits: List<ConventionalCommit>, config: ReleaseNotesConfig): String {
                return if (commits.isEmpty()) "" else "content"
            }
            override fun renderToFile(commits: List<ConventionalCommit>, config: ReleaseNotesConfig, outputFile: File): File = outputFile
        }

        val result = renderer.render(emptyList(), ReleaseNotesConfig())
        assertEquals("", result)
    }

    @Test
    fun `render should produce content for commits`() {
        val renderer = object : ReleaseNotesRenderer {
            override val format: String = "asciidoc"
            override fun render(commits: List<ConventionalCommit>, config: ReleaseNotesConfig): String {
                return "= Release ${config.version ?: "SNAPSHOT"}\n\n" + commits.joinToString("\n") { "* ${it.message}" }
            }
            override fun renderToFile(commits: List<ConventionalCommit>, config: ReleaseNotesConfig, outputFile: File): File = outputFile
        }

        val commits = listOf(
            ConventionalCommit("feat", null, "add login", "abc", "2026-01-01"),
            ConventionalCommit("fix", null, "fix crash", "def", "2026-01-02")
        )
        val result = renderer.render(commits, ReleaseNotesConfig(version = "1.0.0"))

        assertTrue(result.contains("Release 1.0.0"))
        assertTrue(result.contains("add login"))
        assertTrue(result.contains("fix crash"))
    }

    @Test
    fun `renderToFile should write to specified file`() {
        val renderer = object : ReleaseNotesRenderer {
            override val format: String = "json"
            override fun render(commits: List<ConventionalCommit>, config: ReleaseNotesConfig): String = "{}"
            override fun renderToFile(commits: List<ConventionalCommit>, config: ReleaseNotesConfig, outputFile: File): File {
                outputFile.writeText(render(commits, config))
                return outputFile
            }
        }

        val outputFile = File(tempDir, "release-notes.json")
        val result = renderer.renderToFile(
            commits = listOf(ConventionalCommit("feat", null, "test", "abc", "2026-01-01")),
            config = ReleaseNotesConfig(),
            outputFile = outputFile
        )

        assertTrue(result.exists())
        assertEquals("{}", result.readText())
    }
}
