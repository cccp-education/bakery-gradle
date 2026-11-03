package bakery.i18n

import document.translation.AsciiDocParser
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * GBL-010 P3 — pins the structural-alignment vectors shared with
 * `document.translation.DocumentTranslator.isStructurallyAligned`.
 *
 * The parser is the real one: a drift in the block model is caught here, not at
 * the metered rebuild.
 */
class StructuralDivergenceTest {
    private val parser = AsciiDocParser()

    private fun aligned(
        source: String,
        variant: String,
    ): Boolean = StructuralDivergence.isAligned(parser.parse(source), parser.parse(variant))

    @Test
    fun `same block sequence is aligned whatever the language`() {
        val source = "= Title\n\n== Section\n\nBonjour le monde.\n"
        val variant = "= Title\n\n== Section\n\nHello world.\n"
        assertTrue(aligned(source, variant))
    }

    @Test
    fun `same block count but another kind is divergent`() {
        val source = "= Title\n\nHello world.\n"
        val variant = "= Title\n\n* one\n* two\n"
        assertFalse(aligned(source, variant))
    }

    @Test
    fun `a missing paragraph is divergent`() {
        val source = "= Title\n\nHello world.\n\nSecond paragraph.\n"
        val variant = "= Title\n\nHello world.\n"
        assertFalse(aligned(source, variant))
    }

    @Test
    fun `a heading level change is divergent`() {
        val source = "= Title\n\n== Section\n\nHello.\n"
        val variant = "= Title\n\n=== Section\n\nHello.\n"
        assertFalse(aligned(source, variant))
    }

    @Test
    fun `the diagram language is compared case-insensitively`() {
        val source = "= Title\n\n[source,PlantUML]\n----\n@startuml\nA -> B\n@enduml\n----\n"
        val variant = "= Title\n\n[source,plantuml]\n----\n@startuml\nA -> B\n@enduml\n----\n"
        assertTrue(aligned(source, variant))
    }

    @Test
    fun `a duplicated diagram compensating a lost paragraph is divergent`() {
        val source =
            "= Title\n\nFirst paragraph.\n\n" +
                "[plantuml]\n----\n@startuml\nA -> B\n@enduml\n----\n"
        val variant =
            "= Title\n\nFirst paragraph.\n\nSecond paragraph.\n\n" +
                "[plantuml]\n----\n@startuml\nA -> B\n@enduml\n----\n"
        assertFalse(aligned(source, variant))
    }

    @Test
    fun `the signature is stable for an empty article`() {
        assertTrue(StructuralDivergence.signature(emptyList()).isEmpty())
        assertTrue(aligned("", ""))
    }
}
