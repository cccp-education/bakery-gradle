package bakery.i18n.js

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-2.
 *
 * Unit tests for the pure [I18nJsDictionary] parser/writer. The reference
 * behaviour is the talaria node harness (`tools/translate-i18n.mjs`, S-044):
 * same regexes, same indentation, same ink-economy delta (only missing keys
 * are inserted, a complete dictionary is a strict no-op).
 */
class I18nJsDictionaryTest {
    private val chromeSource =
        """
        |window.TALARIA = window.TALARIA || {};
        |
        |(function (ns) {
        |  var DICT = {
        |    fr: {
        |      "nav.home": "Accueil",
        |      "nav.cart": "Panier",
        |      "nav.menu": "Menu"
        |    },
        |    en: {
        |      "nav.home": "Home"
        |    }
        |  };
        |})(window.TALARIA);
        """.trimMargin()

    private val patchSource =
        """
        |(function(){
        |  TALARIA.I18N.extend({
        |    it: {
        |      "nav.home": "Home"
        |    },
        |    de: {
        |      "nav.home": "Startseite"
        |    }
        |  });
        |})();
        """.trimMargin()

    private val catalogueSource =
        """
        |(function () {
        |  TALARIA.I18N.CATALOG = {
        |    fr: {
        |      fpa: { title: "Formateur" }
        |    },
        |    en: {
        |      fpa: { title: "Trainer" }
        |    },
        |    fa: {
        |      fpa: { title: "مربی" }
        |    }
        |  };
        |})();
        """.trimMargin()

    @Nested
    inner class Parse {
        @Test
        fun `parse extracts every language block and its keys`() {
            val dict = I18nJsDictionary.parse(chromeSource)

            assertThat(dict.keys).containsExactly("fr", "en")
            assertThat(dict["fr"]).containsOnlyKeys("nav.home", "nav.cart", "nav.menu")
            assertThat(dict["fr"]!!["nav.home"]).isEqualTo("Accueil")
            assertThat(dict["en"]).containsOnlyKeys("nav.home")
        }

        @Test
        fun `parse merges several sources`() {
            val dict = I18nJsDictionary.parse(chromeSource, patchSource)

            assertThat(dict.keys).containsExactly("fr", "en", "it", "de")
            assertThat(dict["de"]!!["nav.home"]).isEqualTo("Startseite")
        }

        @Test
        fun `parse unescapes quoted strings newlines and backslashes`() {
            val source =
                """
                |  var DICT = {
                |    fr: {
                |      "quote": "il a dit \"bonjour\"",
                |      "multi": "ligne1\nligne2",
                |      "path": "a\\b"
                |    }
                |  };
                """.trimMargin()

            val dict = I18nJsDictionary.parse(source)["fr"]!!

            assertThat(dict["quote"]).isEqualTo("il a dit \"bonjour\"")
            assertThat(dict["multi"]).isEqualTo("ligne1\nligne2")
            assertThat(dict["path"]).isEqualTo("a\\b")
        }

        @Test
        fun `parse returns an empty map when no language block is present`() {
            assertThat(I18nJsDictionary.parse("var X = 1;")).isEmpty()
        }

        @Test
        fun `parse ignores quoted entries outside a language block`() {
            val source =
                """
                |  var OUTSIDE = { "nav.home": "not a dictionary" };
                |    fr: {
                |      "nav.home": "Accueil"
                |    }
                """.trimMargin()

            val dict = I18nJsDictionary.parse(source)["fr"]!!

            assertThat(dict).containsExactlyInAnyOrderEntriesOf(mapOf("nav.home" to "Accueil"))
        }

        @Test
        fun `parse keeps the nested catalogue languages with empty flat entries`() {
            val dict = I18nJsDictionary.parse(catalogueSource)

            assertThat(dict.keys).containsExactly("fr", "en", "fa")
            assertThat(dict["fr"]).isEmpty()
        }
    }

    @Nested
    inner class LanguagesOf {
        @Test
        fun `languagesOf returns codes in document order`() {
            assertThat(I18nJsDictionary.languagesOf(chromeSource)).containsExactly("fr", "en")
            assertThat(I18nJsDictionary.languagesOf(patchSource)).containsExactly("it", "de")
        }

        @Test
        fun `languagesOf detects the nested catalogue top level languages`() {
            assertThat(I18nJsDictionary.languagesOf(catalogueSource)).containsExactly("fr", "en", "fa")
        }

        @Test
        fun `languagesOf returns an empty list without any block`() {
            assertThat(I18nJsDictionary.languagesOf("var X = 1;")).isEmpty()
        }
    }

    @Nested
    inner class InsertTranslations {
        @Test
        fun `inserting only existing keys returns the source byte-identically`() {
            val result =
                I18nJsDictionary.insertTranslations(
                    chromeSource,
                    "fr",
                    mapOf("nav.home" to "Accueil", "nav.cart" to "Panier", "nav.menu" to "Menu"),
                )

            assertThat(result).isEqualTo(chromeSource)
        }

        @Test
        fun `missing key is appended to the target language block`() {
            val result =
                I18nJsDictionary.insertTranslations(
                    chromeSource,
                    "en",
                    mapOf("nav.cart" to "Cart"),
                )

            val expected =
                """
                |window.TALARIA = window.TALARIA || {};
                |
                |(function (ns) {
                |  var DICT = {
                |    fr: {
                |      "nav.home": "Accueil",
                |      "nav.cart": "Panier",
                |      "nav.menu": "Menu"
                |    },
                |    en: {
                |      "nav.home": "Home",
                |      "nav.cart": "Cart"
                |    }
                |  };
                |})(window.TALARIA);
                """.trimMargin()

            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `multiple missing keys are appended in map order`() {
            val result =
                I18nJsDictionary.insertTranslations(
                    chromeSource,
                    "en",
                    linkedMapOf("nav.cart" to "Cart", "nav.menu" to "Menu"),
                )

            assertThat(result).contains(
                """
                |    en: {
                |      "nav.home": "Home",
                |      "nav.cart": "Cart",
                |      "nav.menu": "Menu"
                |    }
                """.trimMargin(),
            )
        }

        @Test
        fun `inserting is idempotent on a second run`() {
            val once =
                I18nJsDictionary.insertTranslations(chromeSource, "en", mapOf("nav.cart" to "Cart"))
            val twice =
                I18nJsDictionary.insertTranslations(once, "en", mapOf("nav.cart" to "Cart"))

            assertThat(twice).isEqualTo(once)
        }

        @Test
        fun `inserted values escape quotes and newlines`() {
            val result =
                I18nJsDictionary.insertTranslations(
                    chromeSource,
                    "en",
                    mapOf("msg" to "il a dit \"bonjour\"\nligne2"),
                )

            assertThat(result).contains("\"msg\": \"il a dit \\\"bonjour\\\"\\nligne2\"")
        }

        @Test
        fun `insertions preserve the surrounding document byte-identically`() {
            val result =
                I18nJsDictionary.insertTranslations(
                    patchSource,
                    "de",
                    mapOf("nav.cart" to "Warenkorb"),
                )

            assertThat(result).startsWith("(function(){\n  TALARIA.I18N.extend({\n    it: {")
            assertThat(result).endsWith("  });\n})();")
            assertThat(I18nJsDictionary.parse(result)["it"]!!["nav.home"]).isEqualTo("Home")
        }

        @Test
        fun `unknown language block is rejected`() {
            assertThatThrownBy {
                I18nJsDictionary.insertTranslations(chromeSource, "xx", mapOf("nav.home" to "Home"))
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("xx")
        }
    }
}
