package bakery.i18n.js

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-6.
 *
 * Unit tests for the pure [I18nCatalogWriter]. The nested catalogue can never go
 * through the flat writer, so this writer substitutes the quoted literals of one
 * language in place (`translate`) and fulfils the cadrage S-054 completeness
 * contract (`complete`): an absent language is cloned from the reference and an
 * existing block is completed with its missing formations/fields. Both are
 * Ink-Economy-compliant: an unchanged catalogue is a strict no-op.
 */
class I18nCatalogWriterTest {
    private val catalogue =
        """
        |TALARIA.I18N.CATALOG = {
        |    fr: {
        |      fpa: {
        |        title: "Formateur",
        |        objectives: [
        |          "a"
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
    inner class Translate {
        @Test
        fun `substitutes only the literals the language already owns`() {
            val result =
                I18nCatalogWriter.translate(
                    catalogue,
                    "en",
                    mapOf("fpa.title" to "Trainer X", "cda.title" to "ignored"),
                )

            val literals = I18nCatalogDocument.literals(result, "en").associate { it.path to it.value }
            assertThat(literals["fpa.title"]).isEqualTo("Trainer X")
            assertThat(literals).doesNotContainKey("cda.title")
        }

        @Test
        fun `leaves the reference language untouched`() {
            val result = I18nCatalogWriter.translate(catalogue, "en", mapOf("fpa.title" to "Trainer X"))

            assertThat(I18nCatalogDocument.literals(result, "fr"))
                .isEqualTo(I18nCatalogDocument.literals(catalogue, "fr"))
        }

        @Test
        fun `an empty translation map is a strict no-op`() {
            assertThat(I18nCatalogWriter.translate(catalogue, "en", emptyMap())).isEqualTo(catalogue)
        }

        @Test
        fun `an already translated catalogue is a strict no-op`() {
            val once = I18nCatalogWriter.translate(catalogue, "en", mapOf("fpa.title" to "Trainer X"))
            val twice = I18nCatalogWriter.translate(once, "en", mapOf("fpa.title" to "Trainer X"))

            assertThat(twice).isEqualTo(once)
        }

        @Test
        fun `a non-catalogue source is returned unchanged`() {
            val flat = "var DICT = { fr: { \"a\": \"b\" } };"

            assertThat(I18nCatalogWriter.translate(flat, "fr", mapOf("a" to "b"))).isEqualTo(flat)
        }

        @Test
        fun `translated values are escaped`() {
            val result = I18nCatalogWriter.translate(catalogue, "en", mapOf("fpa.title" to "Ligne\n\"2\""))

            assertThat(result).contains("\"Ligne\\n\\\"2\\\"\"")
            assertThat(I18nCatalogDocument.literals(result, "en").first { it.path == "fpa.title" }.value)
                .isEqualTo("Ligne\n\"2\"")
        }
    }

    @Nested
    inner class Complete {
        @Test
        fun `an absent language is created by cloning the reference structure`() {
            val result =
                I18nCatalogWriter.complete(
                    source = catalogue,
                    referenceSource = catalogue,
                    referenceLanguage = "fr",
                    language = "es",
                    translations =
                        mapOf(
                            "fpa.title" to "Formador",
                            "cda.title" to "Conceptor",
                        ),
                )

            assertThat(I18nCatalogDocument.literals(result, "es").map { it.path })
                .isEqualTo(I18nCatalogDocument.literals(catalogue, "fr").map { it.path })
            assertThat(I18nCatalogDocument.literals(result, "es").first { it.path == "fpa.title" }.value)
                .isEqualTo("Formador")
        }

        @Test
        fun `a missing formation and field of an existing language are completed`() {
            val result =
                I18nCatalogWriter.complete(
                    source = catalogue,
                    referenceSource = catalogue,
                    referenceLanguage = "fr",
                    language = "en",
                    translations = mapOf("fpa.objectives[0]" to "Goal A"),
                )

            val paths = I18nCatalogDocument.literals(result, "en").map { it.path }
            assertThat(paths).contains("fpa.title", "fpa.objectives[0]", "fpa.modules[0].title", "cda.title")
            assertThat(I18nCatalogDocument.literals(result, "en").first { it.path == "fpa.title" }.value)
                .isEqualTo("Trainer")
            assertThat(I18nCatalogDocument.literals(result, "en").first { it.path == "fpa.objectives[0]" }.value)
                .isEqualTo("Goal A")
            assertThat(I18nCatalogDocument.literals(result, "en").first { it.path == "cda.title" }.value)
                .isEqualTo("Concepteur")
        }

        @Test
        fun `an existing value always wins over the translation`() {
            val result =
                I18nCatalogWriter.complete(
                    source = catalogue,
                    referenceSource = catalogue,
                    referenceLanguage = "fr",
                    language = "en",
                    translations = mapOf("fpa.title" to "should not win"),
                )

            assertThat(I18nCatalogDocument.literals(result, "en").first { it.path == "fpa.title" }.value)
                .isEqualTo("Trainer")
        }

        @Test
        fun `a complete language is a strict no-op`() {
            val completeSource =
                I18nCatalogWriter.complete(
                    source = catalogue,
                    referenceSource = catalogue,
                    referenceLanguage = "fr",
                    language = "en",
                    translations = emptyMap(),
                )
            val again =
                I18nCatalogWriter.complete(
                    source = completeSource,
                    referenceSource = catalogue,
                    referenceLanguage = "fr",
                    language = "en",
                    translations = emptyMap(),
                )

            assertThat(again).isEqualTo(completeSource)
        }

        @Test
        fun `the reference language is never rewritten`() {
            val result =
                I18nCatalogWriter.complete(
                    source = catalogue,
                    referenceSource = catalogue,
                    referenceLanguage = "fr",
                    language = "fr",
                    translations = mapOf("fpa.title" to "X"),
                )

            assertThat(result).isEqualTo(catalogue)
        }

        @Test
        fun `creating a language preserves the existing languages byte-identically`() {
            val result =
                I18nCatalogWriter.complete(
                    source = catalogue,
                    referenceSource = catalogue,
                    referenceLanguage = "fr",
                    language = "es",
                    translations = emptyMap(),
                )

            assertThat(I18nCatalogDocument.literals(result, "fr"))
                .isEqualTo(I18nCatalogDocument.literals(catalogue, "fr"))
            assertThat(I18nCatalogDocument.literals(result, "en"))
                .isEqualTo(I18nCatalogDocument.literals(catalogue, "en"))
        }
    }
}
