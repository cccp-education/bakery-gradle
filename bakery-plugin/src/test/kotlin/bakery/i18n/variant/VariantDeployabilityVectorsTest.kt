package bakery.i18n.variant

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-VARIANT-UNIFY — the anti split-brain guard for the i18n-variant rule.
 *
 * Every vector of the shared fixture [variant-bake-vectors.json] is replayed
 * against [VariantDeployability.undeployableTemplates]. The cheroliv.com site
 * host replays the *same* file: one spec, two hosts, shared vectors. A
 * divergence between the hosts turns this fixture red on one side.
 */
class VariantDeployabilityVectorsTest {
    private val vectors: JsonNode =
        javaClass.classLoader
            .getResourceAsStream(VECTORS_RESOURCE)!!
            .use { jacksonObjectMapper().readTree(it) }

    @Test
    fun `the fixture is non-empty and versioned`() {
        assertEquals(1, vectors["version"].asInt())
        assertTrue(vectors["vectors"].isArray)
        assertTrue(vectors["vectors"].size() >= 5)
    }

    @Test
    fun `every shared vector resolves to its expected undeployable templates`(
        @TempDir dir: File,
    ) {
        val failures = mutableListOf<String>()
        vectors["vectors"].forEachIndexed { index, vector ->
            val caseRoot = dir.resolve("case-$index")
            val reference = materialize(caseRoot, "reference", vector["reference"])
            val variant = materialize(caseRoot, "variant", vector["variant"])
            val expected = vector["expected"].map { it.asText() }

            val actual = VariantDeployability.undeployableTemplates(reference, variant)

            if (actual != expected) {
                failures +=
                    "vector '${vector["name"].asText()}': " +
                    "undeployableTemplates(reference, variant) = $actual but expected $expected"
            }
        }
        assertTrue(failures.isEmpty()) { failures.joinToString("\n") }
    }

    /**
     * Materialise a template set: a name ending in `/` is a directory, any other
     * name is a file. Both hosts build their fixtures this way, so the vectors
     * stay host-neutral.
     */
    private fun materialize(
        dir: File,
        name: String,
        entries: JsonNode,
    ): File {
        val root = dir.resolve(name)
        root.mkdirs()
        entries.forEach { entry ->
            val entryName = entry.asText()
            if (entryName.endsWith("/")) {
                root.resolve(entryName.removeSuffix("/")).mkdirs()
            } else {
                root.resolve(entryName).writeText("<x/>")
            }
        }
        return root
    }

    private companion object {
        const val VECTORS_RESOURCE = "bakery/i18n/variant/variant-bake-vectors.json"
    }
}
