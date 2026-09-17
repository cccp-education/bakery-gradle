package bakery.i18n.js

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-5.
 *
 * Unit tests for the pure [I18nJsFormat] classifier. The client tree ships two
 * shapes that must never be confused: the flat dictionaries (`var DICT` chrome
 * and `TALARIA.I18N.extend` patch) and the nested catalogue
 * (`TALARIA.I18N.CATALOG`).
 *
 * The catalogue reuses the same 4-space `code: {` block indentation, so the
 * flat parser recognises its language blocks — only the explicit catalogue
 * marker tells the two apart.
 */
class I18nJsFormatTest {
    private val chrome =
        """
        |(function (ns) {
        |  var DICT = {
        |    fr: {
        |      "nav.home": "Accueil"
        |    }
        |  };
        |})(window.TALARIA);
        """.trimMargin()

    private val patch =
        """
        |(function () {
        |  TALARIA.I18N.extend({
        |    it: {
        |      "nav.home": "Home"
        |    }
        |  });
        |})();
        """.trimMargin()

    private val catalogue =
        """
        |TALARIA.I18N.CATALOG = {
        |    fr: {
        |      fpa: {
        |        title: "Formateur"
        |      }
        |    }
        |  };
        """.trimMargin()

    @Nested
    inner class Classification {
        @Test
        fun `a var DICT chrome dictionary is flat`() {
            assertThat(I18nJsFormat.of(chrome)).isEqualTo(I18nJsFormat.FLAT)
        }

        @Test
        fun `a TALARIA I18N extend patch dictionary is flat`() {
            assertThat(I18nJsFormat.of(patch)).isEqualTo(I18nJsFormat.FLAT)
        }

        @Test
        fun `a TALARIA I18N CATALOG source is a catalogue`() {
            assertThat(I18nJsFormat.of(catalogue)).isEqualTo(I18nJsFormat.CATALOG)
        }

        @Test
        fun `a source without any marker defaults to flat`() {
            assertThat(I18nJsFormat.of("var DICT = {};")).isEqualTo(I18nJsFormat.FLAT)
        }

        @Test
        fun `the catalogue marker wins over its flat-looking language blocks`() {
            assertThat(I18nJsDictionary.parse(catalogue)).containsKey("fr")
            assertThat(I18nJsFormat.of(catalogue)).isEqualTo(I18nJsFormat.CATALOG)
        }
    }

    @Nested
    inner class Predicates {
        @Test
        fun `isFlat is the inverse of isCatalogue`() {
            assertThat(I18nJsFormat.of(chrome).isFlat).isTrue()
            assertThat(I18nJsFormat.of(chrome).isCatalogue).isFalse()
            assertThat(I18nJsFormat.of(catalogue).isCatalogue).isTrue()
            assertThat(I18nJsFormat.of(catalogue).isFlat).isFalse()
        }
    }
}
