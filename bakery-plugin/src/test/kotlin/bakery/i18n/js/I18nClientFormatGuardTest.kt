package bakery.i18n.js

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-4.
 *
 * Unit tests for [I18nClientFormatGuard], the pure format guard protecting the
 * client-side i18n dictionaries: every language block must be balanced (no
 * silently dropped block), and a well-formed source must survive a
 * parse↔write round-trip byte-identically (writer idempotence).
 */
class I18nClientFormatGuardTest {
    @Nested
    inner class Valid {
        @Test
        fun `a well-formed flat dictionary is valid`() {
            val report = I18nClientFormatGuard.verify(chrome)

            assertThat(report.isValid).isTrue()
            assertThat(report.violations).isEmpty()
        }

        @Test
        fun `a nested-brace value does not break the scanner`() {
            val source =
                """
                |  var DICT = {
                |    fr: {
                |      "hero.html": "<b>{0}</b>"
                |    }
                |  };
                """.trimMargin()

            assertThat(I18nClientFormatGuard.verify(source).isValid).isTrue()
        }

        @Test
        fun `several sources are merged before the check`() {
            assertThat(I18nClientFormatGuard.verify(chrome, patch).isValid).isTrue()
        }

        @Test
        fun `a source without any language block is trivially valid`() {
            assertThat(I18nClientFormatGuard.verify("(function(){ /* nothing */ })();").isValid).isTrue()
        }
    }

    @Nested
    inner class Unbalanced {
        @Test
        fun `a language block without a closing brace is reported`() {
            val source =
                """
                |  var DICT = {
                |    fr: {
                |      "nav.home": "Accueil"
                |    },
                |    en: {
                |      "nav.home": "Home"
                """.trimMargin()

            val report = I18nClientFormatGuard.verify(source)

            assertThat(report.isValid).isFalse()
            assertThat(report.violations.map { it.kind })
                .containsExactly(I18nClientFormatGuard.UNBALANCED_LANGUAGE_BLOCK)
            assertThat(report.violations.single().detail).isEqualTo("en")
        }
    }

    @Nested
    inner class RoundTrip {
        @Test
        fun `a writer that does not preserve the source is reported`() {
            val report =
                I18nClientFormatGuard.verify(chrome) { source, language, entries ->
                    if (language == "fr") "corrupted" else I18nJsDictionary.insertTranslations(source, language, entries)
                }

            assertThat(report.isValid).isFalse()
            assertThat(report.violations.map { it.kind })
                .containsExactly(I18nClientFormatGuard.ROUND_TRIP_DRIFT)
            assertThat(report.violations.single().detail).isEqualTo("fr")
        }
    }

    private val chrome =
        """
        |  var DICT = {
        |    fr: {
        |      "nav.home": "Accueil"
        |    },
        |    en: {
        |      "nav.home": "Home"
        |    }
        |  };
        """.trimMargin()

    private val patch =
        """
        |(function(){
        |  TALARIA.I18N.extend({
        |    de: {
        |      "nav.home": "Startseite"
        |    }
        |  });
        |})();
        """.trimMargin()
}
