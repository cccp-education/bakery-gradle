package bakery.i18n

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import contracts.plantuml.SyntaxValidationResult
import document.translation.ContentTranslationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import plantuml.boundary.PlantumlSyntaxValidatorAdapter
import plantuml.boundary.PlantumlTranslationPortAdapter
import plantuml.service.DiagramOutputProbe
import plantuml.service.DiagramOutputVerdict
import plantuml.service.PlantumlService
import java.io.File

/**
 * GBL-007 / PLT-DIAGRAM-OWNERSHIP US-6 — the **end-to-end transmission chain**
 * bakery → document → N0 port (plantuml) → N0 validator → render.
 *
 * - **US-6a (contract)**: the translated `[plantuml]` block keeps translated
 *   labels and is re-parsable (`SyntaxValidationResult.Valid`).
 * - **US-6b (render)**: the translated block produces a **real image** (shared
 *   `DiagramOutputProbe` verdict `Rendered`), never PlantUML's textual fallback —
 *   the "white diagram" vector (CHE-DIAGRAM D1/D2).
 *
 * The LLM is replaced by a deterministic N0 [TranslationService] fake: the test is
 * network-free. The render uses the PlantUML library already on the classpath
 * (sequence diagram: no external toolchain).
 */
class PlantUmlTranslationChainTest {
    @TempDir
    lateinit var tempDir: File

    private val labels =
        mapOf(
            "Utilisateur" to "User",
            "Système" to "System",
            "Commander" to "Order",
        )

    private class FakeTranslator(
        private val labels: Map<String, String>,
    ) : TranslationService {
        override fun translate(request: TranslationRequest): TranslationResult {
            val translated = labels.entries.fold(request.sourceText) { text, (source, target) -> text.replace(source, target) }
            return TranslationResult.Success(translated)
        }
    }

    private val article =
        """
        = Article de test

        Ce paragraphe décrit l'architecture.

        [plantuml]
        ----
        @startuml
        actor "Utilisateur" as U
        U -> "Système" : "Commander"
        @enduml
        ----
        """.trimIndent() + "\n"

    private fun translate(): String {
        val source = tempDir.resolve("source.adoc").apply { writeText(article) }
        val target = tempDir.resolve("target.adoc")
        val port =
            PlantumlTranslationPortAdapter(
                translator = FakeTranslator(labels),
                validator = PlantumlSyntaxValidatorAdapter(),
            )
        val service = ContentTranslationService(FakeTranslator(labels), plantUmlPort = port)
        service.translateSingleFileWithBlockDelta(
            sourceFile = source,
            targetFile = target,
            previousBlockChecksums = emptyMap(),
            sourceLanguage = "fr",
            targetLanguage = "en",
        )
        return target.readText()
    }

    private fun rawBlock(translated: String): String =
        Regex("""\[plantuml]\s*----\s*(.*?)\s*----""", RegexOption.DOT_MATCHES_ALL)
            .find(translated)
            ?.groupValues
            ?.get(1)
            ?: error("no plantuml block in the translated output:\n$translated")

    @Test
    fun `the translated plantuml block keeps its labels translated and its syntax valid`() {
        val translated = translate()

        assertThat(translated).contains("@startuml").contains("@enduml")
        assertThat(translated).contains("User")
        assertThat(translated).contains("System")
        assertThat(translated).contains("Order")
        assertThat(translated).doesNotContain("Utilisateur")

        assertThat(PlantumlSyntaxValidatorAdapter().validate(rawBlock(translated)))
            .isInstanceOf(SyntaxValidationResult.Valid::class.java)
    }

    @Test
    fun `the translated block renders as a real image, never the textual fallback`() {
        val raw = rawBlock(translate())

        val bytes = PlantumlService().renderPng(raw)

        assertThat(bytes).isNotNull()
        val verdict = DiagramOutputProbe.probe(bytes!!, raw, PlantumlSyntaxValidatorAdapter())
        assertThat(verdict).isInstanceOf(DiagramOutputVerdict.Rendered::class.java)
        assertThat((verdict as DiagramOutputVerdict.Rendered).byteLength).isGreaterThan(0L)
    }
}
