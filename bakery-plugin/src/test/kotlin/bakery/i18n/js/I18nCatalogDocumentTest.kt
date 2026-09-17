package bakery.i18n.js

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-6.
 *
 * Unit tests for the pure [I18nCatalogDocument] structural reader. The flat
 * [I18nJsDictionary] sees no key in the nested catalogue; translation needs the
 * ordered list of quoted literals with a stable [I18nCatalogDocument.Literal.path]
 * so the writer can substitute translated values and the delta can target only
 * what is missing (Ink Economy Law).
 */
class I18nCatalogDocumentTest {
    private val catalogue =
        """
        |TALARIA.I18N.CATALOG = {
        |    fr: {
        |      fpa: {
        |        title: "Formateur",
        |        tagline: "Concevoir",
        |        objectives: [
        |          "a",
        |          "b"
        |        ],
        |        modules: [
        |          { title: "M1", desc: "D1" }
        |        ]
        |      },
        |      cda: {
        |        title: "Concepteur"
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
    inner class Literals {
        @Test
        fun `every quoted literal of a language is exposed in document order`() {
            assertThat(I18nCatalogDocument.literals(catalogue, "fr"))
                .containsExactly(
                    I18nCatalogDocument.Literal("fpa.title", "Formateur"),
                    I18nCatalogDocument.Literal("fpa.tagline", "Concevoir"),
                    I18nCatalogDocument.Literal("fpa.objectives[0]", "a"),
                    I18nCatalogDocument.Literal("fpa.objectives[1]", "b"),
                    I18nCatalogDocument.Literal("fpa.modules[0].title", "M1"),
                    I18nCatalogDocument.Literal("fpa.modules[0].desc", "D1"),
                    I18nCatalogDocument.Literal("cda.title", "Concepteur"),
                )
        }

        @Test
        fun `a language without a block owns no literal`() {
            assertThat(I18nCatalogDocument.literals(catalogue, "es")).isEmpty()
        }

        @Test
        fun `a flat source exposes no catalogue literal`() {
            assertThat(I18nCatalogDocument.literals("var DICT = { fr: { \"a\": \"b\" } };", "fr")).isEmpty()
        }
    }

    @Nested
    inner class MissingLiterals {
        @Test
        fun `a partial target only misses the literals of its absent units`() {
            assertThat(I18nCatalogDocument.missingLiterals(catalogue, "fr", "en"))
                .containsExactly(
                    I18nCatalogDocument.Literal("fpa.tagline", "Concevoir"),
                    I18nCatalogDocument.Literal("fpa.objectives[0]", "a"),
                    I18nCatalogDocument.Literal("fpa.objectives[1]", "b"),
                    I18nCatalogDocument.Literal("fpa.modules[0].title", "M1"),
                    I18nCatalogDocument.Literal("fpa.modules[0].desc", "D1"),
                    I18nCatalogDocument.Literal("cda.title", "Concepteur"),
                )
        }

        @Test
        fun `an absent target language misses every literal of the reference`() {
            assertThat(I18nCatalogDocument.missingLiterals(catalogue, "fr", "es"))
                .isEqualTo(I18nCatalogDocument.literals(catalogue, "fr"))
        }

        @Test
        fun `the reference language misses nothing`() {
            assertThat(I18nCatalogDocument.missingLiterals(catalogue, "fr", "fr")).isEmpty()
        }
    }

    @Nested
    inner class Escaping {
        @Test
        fun `escaped sequences are unescaped in the literal value`() {
            val source =
                "TALARIA.I18N.CATALOG = {\n" +
                    "    fr: {\n" +
                    "      fpa: {\n" +
                    "        title: \"Ligne \\\"1\\\"\\net 2\",\n" +
                    "        note: \"a\\\\b\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "};\n"

            assertThat(I18nCatalogDocument.literals(source, "fr"))
                .containsExactly(
                    I18nCatalogDocument.Literal("fpa.title", "Ligne \"1\"\net 2"),
                    I18nCatalogDocument.Literal("fpa.note", "a\\b"),
                )
        }
    }}
