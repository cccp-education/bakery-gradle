package bakery.i18n

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CHE-I18N-QUALITY — repairs the attributes of an *already preserved* variant.
 *
 * The S-049 corpus is the trap this object exists for: the variants' templates
 * were translated *before* attributes were translated at all, so
 * [TemplateTranslationPlanner] preserves them (their text differs from the
 * reference) and their `placeholder="Nom"` never converges — a whole-template
 * re-translation would be both wasteful and corrupting (the model would be asked
 * to translate already-translated prose).
 *
 * The repair is scope-exact: only reference attribute values still present
 * **verbatim** in the target are scheduled, and only those values are
 * substituted. Translated prose is never re-sent, the file converges, and a
 * second pass is content-idempotent.
 *
 * Pure domain: no I/O, no LLM, no Gradle.
 */
class TemplateAttributeRepairTest {

    private val reference =
        """
        <form>
            <input placeholder="Nom"/>
            <textarea placeholder="Votre message"></textarea>
            <a aria-label="Retour en haut de page" href="#"><i class="bi"></i></a>
        </form>
        """.trimIndent()

    @Test
    fun `a reference value still verbatim in the target is pending`() {
        val target = """<form><input placeholder="Nom"/><textarea placeholder="Votre message"></textarea></form>"""

        val pending = TemplateAttributeRepair.pending(reference, target)

        assertEquals(listOf("Nom", "Votre message"), pending)
    }

    @Test
    fun `a reference value already translated is not pending`() {
        val target = """<form><input placeholder="Naam"/><textarea placeholder="Uw bericht"></textarea></form>"""

        val pending = TemplateAttributeRepair.pending(reference, target)

        assertEquals(emptyList(), pending)
    }

    @Test
    fun `a partially translated variant only repairs what is still French`() {
        val target = """<form><input placeholder="Name"/><textarea placeholder="Votre message"></textarea></form>"""

        val pending = TemplateAttributeRepair.pending(reference, target)

        assertEquals(listOf("Votre message"), pending)
    }

    @Test
    fun `a reference without attributes is a strict no-op`() {
        val plain = "<h1>Développeur</h1>"

        assertTrue(TemplateAttributeRepair.pending(plain, plain).isEmpty())
    }

    @Test
    fun `the repair substitutes only the pending values`() {
        val target =
            """<form><input placeholder="Name"/><textarea placeholder="Votre message"></textarea></form>"""
        val repaired =
            TemplateAttributeRepair.repair(target, mapOf("Votre message" to "Your message"))

        assertEquals(
            """<form><input placeholder="Name"/><textarea placeholder="Your message"></textarea></form>""",
            repaired,
        )
    }

    @Test
    fun `a repaired target has nothing left pending`() {
        val service = PrefixService("EN")
        val pending = TemplateAttributeRepair.pending(reference, reference)
        val translator = TemplateTextTranslator(service)
        val values = translator.translateValues(pending, "fr", "en")
        val repaired = TemplateAttributeRepair.repair(reference, values.replacements)

        assertTrue(
            TemplateAttributeRepair.pending(reference, repaired).isEmpty(),
            "A converged variant must not be scheduled again: $repaired",
        )
    }

    private class PrefixService(
        private val prefix: String,
    ) : TranslationService {
        override fun translate(request: TranslationRequest): TranslationResult =
            TranslationResult.Success("$prefix:${request.sourceText}")
    }
}
