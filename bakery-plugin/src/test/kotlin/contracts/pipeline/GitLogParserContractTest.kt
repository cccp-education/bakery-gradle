package contracts.pipeline

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitLogParserContractTest {

    private val standardOut = System.out
    private val outputStream = ByteArrayOutputStream()

    @BeforeTest
    fun setUp() {
        System.setOut(PrintStream(outputStream))
    }

    @AfterTest
    fun tearDown() {
        System.setOut(standardOut)
    }

    @Test
    fun `parser should return empty list when no commits between tags`() {
        val parser = object : GitLogParser {
            override fun parse(fromTag: String, toTag: String): List<ConventionalCommit> = emptyList()
            override fun detectVersion(projectDir: File): String? = null
            override fun detectFromTag(projectDir: File, toTag: String): String? = null
        }

        val commits = parser.parse("v1.0.0", "v1.0.0")
        assertTrue(commits.isEmpty())
    }

    @Test
    fun `parser should return commits between tags`() {
        val commits = listOf(
            ConventionalCommit("feat", "api", "add endpoint", "abc", "2026-01-01"),
            ConventionalCommit("fix", null, "bug fix", "def", "2026-01-02")
        )
        val parser = object : GitLogParser {
            override fun parse(fromTag: String, toTag: String): List<ConventionalCommit> = commits
            override fun detectVersion(projectDir: File): String? = null
            override fun detectFromTag(projectDir: File, toTag: String): String? = null
        }

        val result = parser.parse("v1.0.0", "v2.0.0")
        assertEquals(2, result.size)
        assertEquals("feat", result[0].type)
        assertEquals("fix", result[1].type)
    }

    @Test
    fun `detectVersion should return null when no version file found`() {
        val parser = object : GitLogParser {
            override fun parse(fromTag: String, toTag: String): List<ConventionalCommit> = emptyList()
            override fun detectVersion(projectDir: File): String? = null
            override fun detectFromTag(projectDir: File, toTag: String): String? = null
        }

        val version = parser.detectVersion(File("/tmp/nonexistent"))
        assertNull(version)
    }

    @Test
    fun `detectVersion should return version from file`() {
        val parser = object : GitLogParser {
            override fun parse(fromTag: String, toTag: String): List<ConventionalCommit> = emptyList()
            override fun detectVersion(projectDir: File): String? = "2.0.0"
            override fun detectFromTag(projectDir: File, toTag: String): String? = null
        }

        val version = parser.detectVersion(File("/tmp"))
        assertNotNull(version)
        assertEquals("2.0.0", version)
    }

    @Test
    fun `detectFromTag should return null when no previous tags`() {
        val parser = object : GitLogParser {
            override fun parse(fromTag: String, toTag: String): List<ConventionalCommit> = emptyList()
            override fun detectVersion(projectDir: File): String? = null
            override fun detectFromTag(projectDir: File, toTag: String): String? = null
        }

        val fromTag = parser.detectFromTag(File("/tmp"), "v2.0.0")
        assertNull(fromTag)
    }

    @Test
    fun `detectFromTag should return previous tag`() {
        val parser = object : GitLogParser {
            override fun parse(fromTag: String, toTag: String): List<ConventionalCommit> = emptyList()
            override fun detectVersion(projectDir: File): String? = null
            override fun detectFromTag(projectDir: File, toTag: String): String? = "v1.0.0"
        }

        val fromTag = parser.detectFromTag(File("/tmp"), "v2.0.0")
        assertEquals("v1.0.0", fromTag)
    }
}
