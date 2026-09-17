package bakery.i18n.js

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-6.
 *
 * Unit tests for the pure [I18nCatalogDelta] planner: per file and target
 * language, the reference literals that language does not own yet. A complete
 * language produces no entry (Ink Economy Law — a full catalogue never reaches
 * the LLM).
 */
class I18nCatalogDeltaTest {
    private val catalogue =
        """
        |TALARIA.I18N.CATALOG = {
        |    fr: {
        |      fpa: {
        |        title: "Formateur",
        |        objectives: [
        |          "a"
        |        ]
        |      }
        |    },
        |    en: {
        |      fpa: {
        |        title: "Trainer"
        |      }
        |    }
        |  };
        """.trimMargin()

    @Nested
    inner class Plan {
        @Test
        fun `a partial target language reports its missing literals`() {
            val plan = I18nCatalogDelta.plan(mapOf("i18n-content.js" to catalogue), "fr", listOf("en"))

            assertThat(plan).hasSize(1)
            assertThat(plan.single().file).isEqualTo("i18n-content.js")
            assertThat(plan.single().language).isEqualTo("en")
            assertThat(plan.single().paths).containsExactly("fpa.objectives[0]")
        }

        @Test
        fun `an absent target language reports every reference literal`() {
            val plan = I18nCatalogDelta.plan(mapOf("i18n-content.js" to catalogue), "fr", listOf("es"))

            assertThat(plan.single().paths)
                .isEqualTo(I18nCatalogDocument.literals(catalogue, "fr").map { it.path })
        }

        @Test
        fun `a complete target language produces no entry`() {
            assertThat(I18nCatalogDelta.plan(mapOf("i18n-content.js" to catalogue), "fr", listOf("fr"))).isEmpty()
        }

        @Test
        fun `a flat file produces no entry`() {
            val flat = "var DICT = { fr: { \"a\": \"b\" } };"

            assertThat(I18nCatalogDelta.plan(mapOf("i18n.js" to flat), "fr", listOf("en"))).isEmpty()
        }

        @Test
        fun `the missing count sums every target language`() {
            val plan = I18nCatalogDelta.plan(mapOf("i18n-content.js" to catalogue), "fr", listOf("en", "es"))

            assertThat(plan.sumOf { it.paths.size }).isEqualTo(1 + 2)
        }
    }
}
