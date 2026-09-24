package bakery.langswitch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * BKY-LANG-NAV-1 — the anti split-brain guard.
 *
 * Every vector of the shared fixture [lang-switch-path-vectors.json] is replayed
 * against the Kotlin rule. The JS host (NAV-3) replays the *same* file: this is
 * the D3 parade — one spec, two hosts, shared vectors. A divergence between the
 * hosts turns this fixture red on one side.
 */
class LangSwitchPathVectorsTest {
    private val vectors: JsonNode =
        javaClass.classLoader
            .getResourceAsStream(VECTORS_RESOURCE)!!
            .use { jacksonObjectMapper().readTree(it) }

    @Test
    fun `the fixture is non-empty and versioned`() {
        assertEquals(1, vectors["version"].asInt())
        assertTrue(vectors["vectors"].isArray)
        assertTrue(vectors["vectors"].size() >= 10)
    }

    @Test
    fun `every shared vector resolves to its expected same-page url`() {
        val failures = mutableListOf<String>()
        vectors["vectors"].forEach { vector ->
            val currentPageUri = vector["currentPageUri"].asText()
            val currentLang = vector["currentLang"].asText()
            val targetLang = vector["targetLang"].asText()
            val defaultLang = vector["defaultLang"].asText()
            val expected = vector["expected"].asText()
            val existingPages: Set<String>? =
                vector["existingPages"]?.takeIf { it.isArray }?.map { it.asText() }?.toSet()

            val pageExists: (String) -> Boolean = { candidate -> existingPages?.contains(candidate) ?: true }

            val actual =
                LangSwitchPath.resolveSamePage(currentPageUri, currentLang, targetLang, defaultLang, pageExists)

            if (actual != expected) {
                failures +=
                    "vector '${vector["name"].asText()}': " +
                    "resolveSamePage($currentPageUri, $currentLang, $targetLang, $defaultLang) " +
                    "= '$actual' but expected '$expected'"
            }
        }
        assertTrue(failures.isEmpty()) { failures.joinToString("\n") }
    }

    private companion object {
        const val VECTORS_RESOURCE = "bakery/langswitch/lang-switch-path-vectors.json"
    }
}
