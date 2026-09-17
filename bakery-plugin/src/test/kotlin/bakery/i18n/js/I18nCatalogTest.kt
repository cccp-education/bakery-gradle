package bakery.i18n.js

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-5.
 *
 * Unit tests for the pure [I18nCatalog] planner. The structured catalogue
 * (`TALARIA.I18N.CATALOG`) nests languages → formations → scalar fields and
 * lists, so the flat [I18nJsDictionary] parser sees its language blocks but
 * none of its keys. The delta contract (cadrage S-054) is: a missing language
 * block is a missing language, and inside an existing block every formation and
 * field of the reference language must be present.
 */
class I18nCatalogTest {
    private val catalogue =
        """
        |TALARIA.I18N.CATALOG = {
        |    fr: {
        |      fpa: {
        |        title: "Formateur",
        |        tagline: "Concevoir et animer",
        |        objectives: [
        |          "a",
        |          "b"
        |        ],
        |        modules: [
        |          { title: "M1", desc: "D1" }
        |        ]
        |      },
        |      cda: {
        |        title: "Concepteur",
        |        tagline: "Développer"
        |      }
        |    },
        |    en: {
        |      fpa: {
        |        title: "Trainer",
        |        tagline: "Design and animate"
        |      }
        |    }
        |  };
        """.trimMargin()

    @Nested
    inner class Parsing {
        @Test
        fun `languages are the 2-letter blocks of the catalogue, in document order`() {
            assertThat(I18nCatalog.languagesOf(catalogue)).containsExactly("fr", "en")
        }

        @Test
        fun `formations of a language are the nested blocks of its catalogue entry`() {
            assertThat(I18nCatalog.formationsOf(catalogue, "fr")).containsExactly("fpa", "cda")
            assertThat(I18nCatalog.formationsOf(catalogue, "en")).containsExactly("fpa")
        }

        @Test
        fun `fields of a formation are its top-level keys, not the inline module keys`() {
            assertThat(I18nCatalog.fieldsOf(catalogue, "fr", "fpa"))
                .containsExactly("title", "tagline", "objectives", "modules")
            assertThat(I18nCatalog.fieldsOf(catalogue, "en", "fpa"))
                .containsExactly("title", "tagline")
        }

        @Test
        fun `a source without catalogue marker has no languages`() {
            assertThat(I18nCatalog.languagesOf("var DICT = { fr: { \"a\": \"b\" } };")).isEmpty()
        }
    }

    @Nested
    inner class Delta {
        @Test
        fun `a target language with no block is a missing language`() {
            val plan = I18nCatalog.plan(catalogue, "fr", listOf("en", "es"))

            assertThat(plan.missingLanguages).containsExactly("es")
            assertThat(plan.languages).containsExactly("en")
        }

        @Test
        fun `an existing block missing a formation reports that formation`() {
            val plan = I18nCatalog.plan(catalogue, "fr", listOf("en"))

            assertThat(plan.missingLanguages).isEmpty()
            assertThat(plan.missingFormations).containsExactlyEntriesOf(mapOf("en" to listOf("cda")))
        }

        @Test
        fun `an existing formation missing fields reports those fields`() {
            val plan = I18nCatalog.plan(catalogue, "fr", listOf("en"))

            assertThat(plan.missingFields)
                .containsExactlyEntriesOf(mapOf("en" to mapOf("fpa" to listOf("objectives", "modules"))))
        }

        @Test
        fun `a complete target language has no gap at all`() {
            val plan = I18nCatalog.plan(catalogue, "fr", listOf("fr"))

            assertThat(plan.hasGap).isFalse()
            assertThat(plan.missingLanguages).isEmpty()
            assertThat(plan.missingFormations).isEmpty()
            assertThat(plan.missingFields).isEmpty()
        }

        @Test
        fun `the reference language is never reported as missing`() {
            val plan = I18nCatalog.plan(catalogue, "fr", listOf("fr"))

            assertThat(plan.languages).containsExactly("fr")
            assertThat(plan.hasGap).isFalse()
        }

        @Test
        fun `a target language is skipped when the reference owns no block`() {
            val flat = "var DICT = { fr: { \"a\": \"b\" } };"

            assertThat(I18nCatalog.plan(flat, "fr", listOf("en")).languages).isEmpty()
        }
    }

    @Nested
    inner class Report {
        @Test
        fun `the summary counts the gaps without exposing the raw maps`() {
            val plan = I18nCatalog.plan(catalogue, "fr", listOf("en", "es"))

            assertThat(plan.missingLanguageCount).isEqualTo(1)
            assertThat(plan.missingFormationCount).isEqualTo(1)
            assertThat(plan.missingFieldCount).isEqualTo(2)
            assertThat(plan.hasGap).isTrue()
        }
    }
}
