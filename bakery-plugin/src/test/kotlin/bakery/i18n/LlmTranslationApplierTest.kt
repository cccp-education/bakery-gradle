package bakery.i18n

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LlmTranslationApplierTest {

    @Test
    fun `translateFrenchToEnglish returns empty map for empty input`() {
        val applier = LlmTranslationApplier(FakeTranslationService())

        val result = applier.translateFrenchToEnglish(emptyMap())

        assertThat(result).isEmpty()
    }

    @Test
    fun `translateFrenchToEnglish translates each value`() {
        val fake = FakeTranslationService()
        val applier = LlmTranslationApplier(fake)
        val input = mapOf("menu.home" to "Accueil", "menu.contact" to "Contact")

        val result = applier.translateFrenchToEnglish(input)

        assertThat(result).containsExactlyInAnyOrderEntriesOf(
            mapOf("menu.home" to "[en] Accueil", "menu.contact" to "[en] Contact")
        )
        assertThat(fake.requestsReceived).hasSize(2)
        assertThat(fake.requestsReceived.map { it.sourceText }).containsExactlyInAnyOrder("Accueil", "Contact")
    }

    @Test
    fun `translateFrenchToEnglish falls back to french on failure`() {
        val fake = FakeTranslationService()
        fake.enqueueResult(TranslationResult.Failure("LLM unavailable"))
        val applier = LlmTranslationApplier(fake)
        val input = mapOf("blog.read_more" to "Lire la suite")

        val result = applier.translateFrenchToEnglish(input)

        assertThat(result).containsExactlyInAnyOrderEntriesOf(mapOf("blog.read_more" to "Lire la suite"))
    }

    @Test
    fun `translateFrenchToEnglish preserves key order`() {
        val fake = FakeTranslationService()
        val applier = LlmTranslationApplier(fake)
        val input = linkedMapOf(
            "a" to "Premier",
            "b" to "Deuxième",
            "c" to "Troisième"
        )

        val result = applier.translateFrenchToEnglish(input)

        assertThat(result.keys).containsExactly("a", "b", "c")
    }

    private class FakeTranslationService : TranslationService {

        val requestsReceived = mutableListOf<TranslationRequest>()
        private val resultQueue: MutableList<TranslationResult> = mutableListOf()

        fun enqueueResult(result: TranslationResult) {
            resultQueue.add(result)
        }

        override fun translate(request: TranslationRequest): TranslationResult {
            requestsReceived.add(request)
            return if (resultQueue.isNotEmpty()) {
                resultQueue.removeAt(0)
            } else {
                TranslationResult.Success("[en] ${request.sourceText}")
            }
        }
    }
}
