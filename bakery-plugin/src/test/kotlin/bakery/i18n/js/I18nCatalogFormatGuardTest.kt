package bakery.i18n.js

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-6.
 *
 * Unit tests for [I18nCatalogFormatGuard], the pure structural guard of the
 * nested catalogue: the catalogue body must be balanced, the completed language
 * must own every reference literal path (the S-054 completeness contract), and
 * the writer must be idempotent (Ink Economy Law).
 */
class I18nCatalogFormatGuardTest {
    private val catalogue =
        """
        |TALARIA.I18N.CATALOG = {
        |    fr: {
        |      fpa: {
        |        title: "Formateur",
        |        modules: [
        |          { title: "M1", desc: "D1" }
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
    inner class Valid {
        @Test
        fun `a completion that fills every reference path is valid`() {
            val report =
                I18nCatalogFormatGuard.verify(
                    source = catalogue,
                    referenceSource = catalogue,
                    referenceLanguage = "fr",
                    language = "en",
                    translations = emptyMap(),
                )

            assertThat(report.isValid).isTrue()
        }

        @Test
        fun `a flat source is trivially valid`() {
            val report =
                I18nCatalogFormatGuard.verify(
                    source = "var DICT = { fr: { \"a\": \"b\" } };",
                    referenceSource = catalogue,
                    referenceLanguage = "fr",
                    language = "fr",
                    translations = emptyMap(),
                )

            assertThat(report.isValid).isTrue()
        }
    }

    @Nested
    inner class Unbalanced {
        @Test
        fun `a catalogue with an unbalanced body is reported`() {
            val unbalanced = "TALARIA.I18N.CATALOG = {\n    fr: {\n      fpa: {\n        title: \"X\"\n"

            val report =
                I18nCatalogFormatGuard.verify(
                    source = unbalanced,
                    referenceSource = catalogue,
                    referenceLanguage = "fr",
                    language = "fr",
                    translations = emptyMap(),
                )

            assertThat(report.isValid).isFalse()
            assertThat(report.violations.map { it.kind })
                .contains(I18nCatalogFormatGuard.UNBALANCED_CATALOGUE_BLOCK)
        }
    }

    @Nested
    inner class StructureDrift {
        @Test
        fun `a rewriter that leaves the language incomplete is reported`() {
            val report =
                I18nCatalogFormatGuard.verify(
                    source = catalogue,
                    referenceSource = catalogue,
                    referenceLanguage = "fr",
                    language = "en",
                    translations = emptyMap(),
                    rewriter = { source, _, _, _, _ -> source },
                )

            assertThat(report.isValid).isFalse()
            assertThat(report.violations.map { it.kind })
                .contains(I18nCatalogFormatGuard.LANGUAGE_STRUCTURE_DRIFT)
        }

        @Test
        fun `a rewriter that is not idempotent is reported`() {
            val report =
                I18nCatalogFormatGuard.verify(
                    source = catalogue,
                    referenceSource = catalogue,
                    referenceLanguage = "fr",
                    language = "en",
                    translations = emptyMap(),
                    rewriter = { source, referenceSource, referenceLanguage, language, translations ->
                        I18nCatalogWriter.complete(source, referenceSource, referenceLanguage, language, translations) + "\n"
                    },
                )

            assertThat(report.isValid).isFalse()
            assertThat(report.violations.map { it.kind })
                .contains(I18nCatalogFormatGuard.ROUND_TRIP_DRIFT)
        }
    }
}
