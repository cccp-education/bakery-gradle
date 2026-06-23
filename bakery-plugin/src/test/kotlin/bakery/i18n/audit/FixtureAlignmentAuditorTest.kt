package bakery.i18n.audit

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * BKY-I18N-PROD-AUDIT — Tests d'audit d'alignement fixture ↔ site réel.
 *
 * TDD : ces tests sont écrits avant l'alignement des fixtures. Ils doivent donc
 * échouer aujourd'hui (fixtures ≠ sites réels) et passer après correction.
 */
class FixtureAlignmentAuditorTest {

    private val auditor = FixtureAlignmentAuditor()

    @Test
    fun `magic-stick fixture is aligned with real magic-stick site`() {
        val report = audit("i18n-fixtures/magic-stick/jbake/templates", "magic-stick")

        assertTrue(report.isAligned, buildAlignmentMessage(report))
    }

    @Test
    fun `cccp-education fixture is aligned with real cccp-education site`() {
        val report = audit("i18n-fixtures/cccp-education/jbake/templates", "cccp.education")

        assertTrue(report.isAligned, buildAlignmentMessage(report))
    }

    @Test
    fun `cheroliv-com fixture is aligned with real cheroliv-com site`() {
        val report = audit("i18n-fixtures/cheroliv-com/jbake/templates", "cheroliv.com")

        assertTrue(report.isAligned, buildAlignmentMessage(report))
    }

    @Test
    fun `fixture and real site with identical content produce aligned report`(@org.junit.jupiter.api.io.TempDir tempDir: File) {
        val fixtureDir = tempDir.resolve("fixture/templates").apply { mkdirs() }
        val realDir = tempDir.resolve("real/templates").apply { mkdirs() }

        fixtureDir.resolve("index.thyme").writeText("<html>\n  <body>Hello</body>\n</html>")
        realDir.resolve("index.thyme").writeText("<html>\n  <body>Hello</body>\n</html>")

        val report = auditor.audit(fixtureDir, realDir)

        assertTrue(report.isAligned)
        assertEquals(emptyList(), report.missingInFixture)
        assertEquals(emptyList(), report.extraInFixture)
        assertEquals(emptyList(), report.mismatchedContent)
    }

    @Test
    fun `fixture missing template is reported as missingInFixture`(@org.junit.jupiter.api.io.TempDir tempDir: File) {
        val fixtureDir = tempDir.resolve("fixture/templates").apply { mkdirs() }
        val realDir = tempDir.resolve("real/templates").apply { mkdirs() }

        realDir.resolve("menu.thyme").writeText("<nav>Menu</nav>")

        val report = auditor.audit(fixtureDir, realDir)

        assertFalse(report.isAligned)
        assertEquals(listOf("menu.thyme"), report.missingInFixture)
    }

    @Test
    fun `fixture extra template is reported as extraInFixture`(@org.junit.jupiter.api.io.TempDir tempDir: File) {
        val fixtureDir = tempDir.resolve("fixture/templates").apply { mkdirs() }
        val realDir = tempDir.resolve("real/templates").apply { mkdirs() }

        fixtureDir.resolve("extra.thyme").writeText("<div>Extra</div>")

        val report = auditor.audit(fixtureDir, realDir)

        assertFalse(report.isAligned)
        assertEquals(listOf("extra.thyme"), report.extraInFixture)
    }

    @Test
    fun `template with different content is reported as mismatched`(@org.junit.jupiter.api.io.TempDir tempDir: File) {
        val fixtureDir = tempDir.resolve("fixture/templates").apply { mkdirs() }
        val realDir = tempDir.resolve("real/templates").apply { mkdirs() }

        fixtureDir.resolve("index.thyme").writeText("<html>Fixture</html>")
        realDir.resolve("index.thyme").writeText("<html>Real</html>")

        val report = auditor.audit(fixtureDir, realDir)

        assertFalse(report.isAligned)
        assertEquals(1, report.mismatchedContent.size)
        assertEquals("index.thyme", report.mismatchedContent.single().templateName)
    }

    private fun audit(fixtureResourcePath: String, siteDirName: String): FixtureAlignmentAuditor.AlignmentReport {
        val fixtureDir = loadResourceDir(fixtureResourcePath)
        val realSiteDir = File("/home/cheroliv/workspace/office/sites/$siteDirName/jbake/templates")
        require(realSiteDir.isDirectory) { "Répertoire site réel introuvable: $realSiteDir" }
        return auditor.audit(fixtureDir, realSiteDir)
    }

    private fun loadResourceDir(resourcePath: String): File {
        val resource = this::class.java.classLoader.getResource(resourcePath)
            ?: throw IllegalStateException("Resource non trouvée: $resourcePath")
        return File(resource.toURI())
    }

    private fun buildAlignmentMessage(report: FixtureAlignmentAuditor.AlignmentReport): String {
        val lines = mutableListOf(
            "Fixture ${report.fixtureDir} non alignée avec ${report.realSiteDir} :"
        )
        if (report.missingInFixture.isNotEmpty()) {
            lines.add("  Templates manquants dans la fixture : ${report.missingInFixture}")
        }
        if (report.extraInFixture.isNotEmpty()) {
            lines.add("  Templates en trop dans la fixture : ${report.extraInFixture}")
        }
        if (report.mismatchedContent.isNotEmpty()) {
            lines.add("  Templates avec contenu différent : ${report.mismatchedContent.map { it.templateName }}")
        }
        return lines.joinToString("\n")
    }
}
