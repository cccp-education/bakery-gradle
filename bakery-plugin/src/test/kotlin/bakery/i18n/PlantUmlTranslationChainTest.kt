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
import java.io.File

/**
 * GBL-007 / PLT-DIAGRAM-OWNERSHIP US-6a — the **end-to-end transmission chain**
 * (contract level, no bake).
 *
 * Proves on a real AsciiDoc article that the chain
 * bakery → document (`ContentTranslationService`) → N0 `PlantUmlTranslationPort`
 * (plantuml) → N0 validator:
 * 1. extracts the `[plantuml]` block;
 * 2. translates its labels (the port modifies the block);
 * 3. keeps the translated block **re-parsable** (round-trip validation).
 *
 * The LLM is replaced by a deterministic N0 [TranslationService] fake: the test
 * is network-free and CI-friendly (no metered call, no PlantUML render).
 */
class PlantUmlTranslationChainTest {
    @TempDir
    lateinit var tempDir: File

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
        actor Utilisateur
        Utilisateur -> Système : Commander
        @enduml
        ----
        """.trimIndent() + "\n"

    @Test
    fun `the translated plantuml block keeps its labels translated and its syntax valid`() {
        val source = tempDir.resolve("source.adoc").apply { writeText(article) }
        val target = tempDir.resolve("target.adoc")

        val port =
            PlantumlTranslationPortAdapter(
                translator =
                    FakeTranslator(
                        mapOf(
                            "Utilisateur" to "User",
                            "Système" to "System",
                            "Commander" to "Order",
                        ),
                    ),
                validator = PlantumlSyntaxValidatorAdapter(),
            )
        val service = ContentTranslationService(FakeTranslator(emptyMap()), plantUmlPort = port)

        service.translateSingleFileWithBlockDelta(
            sourceFile = source,
            targetFile = target,
            previousBlockChecksums = emptyMap(),
            sourceLanguage = "fr",
            targetLanguage = "en",
        )

        val translated = target.readText()
        assertThat(translated).contains("@startuml").contains("@enduml")
        assertThat(translated).contains("User")
        assertThat(translated).contains("System")
        assertThat(translated).contains("Order")
        assertThat(translated).doesNotContain("Utilisateur")

        val rawBlock =
            Regex("""\[plantuml]\s*----\s*(.*?)\s*----""", RegexOption.DOT_MATCHES_ALL)
                .find(translated)
                ?.groupValues
                ?.get(1)
        assertThat(rawBlock).isNotNull()
        assertThat(PlantumlSyntaxValidatorAdapter().validate(rawBlock!!))
            .isInstanceOf(SyntaxValidationResult.Valid::class.java)
    }
}
