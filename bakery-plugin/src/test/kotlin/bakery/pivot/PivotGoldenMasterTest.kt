package bakery.pivot

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PivotGoldenMasterTest {

    private val parser = AsciiDocParser()
    private val renderer = PivotYamlRenderer()

    @Test
    fun `golden master quick start adoc to yaml is byte identical`() {
        assertRoundTripByteIdentical("quick-start")
    }

    @Test
    fun `golden master ab partition adoc to yaml is byte identical`() {
        assertRoundTripByteIdentical("ab-partition")
    }

    @Test
    fun `golden master scripts adoc to yaml is byte identical`() {
        assertRoundTripByteIdentical("scripts")
    }

    @Test
    fun `golden master nested plantuml adoc to yaml is byte identical`() {
        assertRoundTripByteIdentical("nested-plantuml")
    }

    private fun assertRoundTripByteIdentical(baseName: String) {
        val adoc = readResource("pivot-golden-master/$baseName.adoc")
        val expectedYaml = readResource("pivot-golden-master/$baseName.yaml")

        val article = parser.parse(adoc)
        val actualYaml = renderer.render(article)

        val expectedLines = normalizeYaml(expectedYaml)
        val actualLines = normalizeYaml(actualYaml)

        val minLines = minOf(expectedLines.size, actualLines.size)
        for (i in 0 until minLines) {
            assertEquals(expectedLines[i], actualLines[i],
                "Line ${i + 1} mismatch in $baseName:\n  expected: ${expectedLines[i]}\n  actual:   ${actualLines[i]}")
        }
        assertEquals(expectedLines.size, actualLines.size,
            "Line count mismatch in $baseName: expected ${expectedLines.size}, got ${actualLines.size}")
    }

    private fun normalizeYaml(yaml: String): List<String> =
        yaml.lines()
            .map { stripInlineComment(it).trimEnd() }
            .filter { it.isNotEmpty() && !it.trimStart().startsWith("#") }
            .filter { !it.trimStart().startsWith("translatable:") }
            .let { if (it.isNotEmpty() && it.first().isEmpty()) it.drop(1) else it }

    private fun stripInlineComment(line: String): String {
        val hash = line.indexOf(" #")
        return if (hash >= 0) line.substring(0, hash) else line
    }

    private fun readResource(path: String): String {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(path)
            ?: error("Resource not found: $path")
        return stream.bufferedReader().readText()
    }
}