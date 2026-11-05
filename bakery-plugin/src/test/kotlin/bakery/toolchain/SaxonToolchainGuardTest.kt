package bakery.toolchain

import bakery.toolchain.SaxonToolchainGuard.Artifact
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * BKY-PLT-3 — a Saxon-HE 11.x on the bake/functional classpath must fail the
 * build. `9.9.1-7` is the correct serializer (BKY-PLANTUML-D1); the 11.x branch
 * serialises SVG with `xmlns=''` and paints white diagrams.
 *
 * Gradle injects the resolved `net.sf.saxon` coordinates (patron
 * `PublishedCatalogVersion`), so the test never resolves a configuration itself.
 * The deliberately wrong vectors below are the RED-by-construction proof of the
 * predicate; no local test execution is required.
 */
class SaxonToolchainGuardTest {
    @Test
    fun `the 11 x branch is forbidden`() {
        val violations =
            SaxonToolchainGuard.violations(
                listOf(saxon("11.4"), saxon("11.7")),
            )

        assertEquals("net.sf.saxon:Saxon-HE:11.4", violations[0].toString())
        assertEquals(2, violations.size)
    }

    @Test
    fun `the 9 x serializer is allowed`() {
        val allowed = listOf(saxon("9.9.1-7"), saxon("9.9"), saxon("10.0"), saxon("12.5"))

        assertTrue(SaxonToolchainGuard.violations(allowed).isEmpty())
    }

    @Test
    fun `a deliberately wrong vector is rejected`() {
        // The guard must never degrade into "any Saxon is bad" nor "only 11.4 is
        // bad": the whole 11.x branch is faulty.
        assertTrue(SaxonToolchainGuard.isForbiddenSaxon(saxon("11.0")))
        assertFalse(SaxonToolchainGuard.isForbiddenSaxon(saxon("9.9.1-7")))
    }

    @Test
    fun `another artifact is never flagged`() {
        val artifacts =
            listOf(
                Artifact("org.w3c", "epubcheck", "11.4"),
                Artifact("net.sf.saxon", "Saxon", "11.4"),
            )

        assertTrue(SaxonToolchainGuard.violations(artifacts).isEmpty())
    }

    @Test
    fun `coordinates are parsed from the injected encoding`() {
        val parsed = SaxonToolchainGuard.parse("net.sf.saxon:Saxon-HE:9.9.1-7\n\norg.w3c:epubcheck:5.2.1")

        assertEquals(
            listOf(
                Artifact("net.sf.saxon", "Saxon-HE", "9.9.1-7"),
                Artifact("org.w3c", "epubcheck", "5.2.1"),
            ),
            parsed,
        )
    }

    @Test
    fun `the bake and functional classpaths carry no forbidden Saxon`() {
        val runtime = SaxonToolchainGuard.resolvedArtifacts(RUNTIME_PROPERTY)
        val functional = SaxonToolchainGuard.resolvedArtifacts(FUNCTIONAL_PROPERTY)

        assertEquals(
            emptyList(),
            SaxonToolchainGuard.violations(runtime + functional),
            "Saxon-HE 11.x is on the bake classpath — exclude org.w3c:epubcheck from document-plugin " +
                "(BKY-PLT-1) or pick the 9.9.x serializer.",
        )
    }

    private fun saxon(version: String): Artifact =
        Artifact(
            SaxonToolchainGuard.SAXON_GROUP,
            SaxonToolchainGuard.SAXON_MODULE,
            version,
        )

    private companion object {
        const val RUNTIME_PROPERTY = "bakery.saxon.runtimeClasspath"
        const val FUNCTIONAL_PROPERTY = "bakery.saxon.functionalTestRuntimeClasspath"
    }
}
