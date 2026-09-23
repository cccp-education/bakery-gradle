package bakery.i18n

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CHE-I18N-QUALITY US-19 — repairs the *visible text* of an already preserved
 * variant.
 *
 * S-050 repaired the attributes of a preserved variant. The same blind spot
 * remained for the visible text: `TemplateTranslationPlanner` decides *whole
 * files*, so a variant translated before the body was handled (the cheroliv.com
 * `es`/`ar`/`pt`/`ru`/`ur` contact form: translated `placeholder` but still
 * French `Contact`, `Envoyer le Message`, `Prêt à démarrer…`) differs from the
 * reference and is preserved forever while its prose stays French.
 *
 * The repair is scope-exact: only reference visible runs still present
 * **verbatim** in the target are scheduled, and only those runs are substituted.
 * A whole-file re-translation would be both wasteful and corrupting (the model
 * would receive already-translated prose as source text).
 *
 * Pure domain: no I/O, no LLM, no Gradle.
 */
class TemplateTextRepairTest {

    private val reference =
        """
        <section>
            <h2>Contact</h2>
            <p>Prêt à démarrer votre projet ? Contactez-moi !</p>
            <button>Envoyer le Message</button>
        </section>
        """.trimIndent()

    @Test
    fun `a reference run still verbatim in the target is pending`() {
        val target =
            """
            <section>
                <h2>Contact</h2>
                <p>Prêt à démarrer votre projet ? Contactez-moi !</p>
                <button>Envoyer le Message</button>
            </section>
            """.trimIndent()

        val pending = TemplateTextRepair.pending(reference, target)

        assertEquals(
            listOf("Prêt à démarrer votre projet ? Contactez-moi !", "Envoyer le Message"),
            pending,
        )
    }

    @Test
    fun `a reference run already translated is not pending`() {
        val target =
            """
            <section>
                <h2>Contact</h2>
                <p>¿Listo para empezar tu proyecto? ¡Contáctame!</p>
                <button>Enviar el Mensaje</button>
            </section>
            """.trimIndent()

        assertTrue(TemplateTextRepair.pending(reference, target).isEmpty())
    }

    @Test
    fun `a partially translated variant only repairs what is still French`() {
        val target =
            """
            <section>
                <h2>Contact</h2>
                <p>¿Listo para empezar tu proyecto? ¡Contáctame!</p>
                <button>Envoyer le Message</button>
            </section>
            """.trimIndent()

        assertEquals(listOf("Envoyer le Message"), TemplateTextRepair.pending(reference, target))
    }

    @Test
    fun `a reference without prose is a strict no-op`() {
        val plain = "<h1>React</h1><p>Android, SpringBoot</p>"

        assertTrue(TemplateTextRepair.pending(plain, plain).isEmpty())
    }

    @Test
    fun `the repair substitutes only the pending runs`() {
        val target =
            """
            <section>
                <h2>Contact</h2>
                <p>¿Listo para empezar tu proyecto? ¡Contáctame!</p>
                <button>Envoyer le Message</button>
            </section>
            """.trimIndent()
        val repaired =
            TemplateTextRepair.repair(target, mapOf("Envoyer le Message" to "Enviar el Mensaje"))

        assertTrue(repaired.contains("Enviar el Mensaje"), "The pending run must be replaced: $repaired")
        assertTrue(
            repaired.contains("¿Listo para empezar tu proyecto? ¡Contáctame!"),
            "A translated run must stay untouched: $repaired",
        )
    }

    @Test
    fun `a repaired target has nothing left pending`() {
        val service = PrefixService("EN")
        val pending = TemplateTextRepair.pending(reference, reference)
        val translator = TemplateTextTranslator(service)
        val values = translator.translateValues(pending, "fr", "en")
        val repaired = TemplateTextRepair.repair(reference, values.replacements)

        assertTrue(
            TemplateTextRepair.pending(reference, repaired).isEmpty(),
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
