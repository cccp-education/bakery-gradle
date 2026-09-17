package bakery.i18n.js

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-3.
 *
 * Unit tests for the pure [I18nClientDelta] planner. The reference behaviour is
 * the talaria node harness `tools/translate-i18n.mjs` (S-044): the chrome
 * dictionary and the additive patch are merged before the delta is computed,
 * then every missing key is dispatched back to the file that owns the language
 * block. `.html` variants are never translated (ink economy + no markup
 * transfer); a complete dictionary yields no plan at all.
 */
class I18nClientDeltaTest {
    private val chrome =
        """
        |(function (ns) {
        |  var DICT = {
        |    fr: {
        |      "nav.home": "Accueil",
        |      "nav.cart": "Panier",
        |      "hero.html": "<b>Salut</b>"
        |    },
        |    en: {
        |      "nav.home": "Home"
        |    }
        |  };
        |})(window.TALARIA);
        """.trimMargin()

    private val patch =
        """
        |(function(){
        |  TALARIA.I18N.extend({
        |    it: {
        |      "nav.home": "Home"
        |    },
        |    de: {}
        |  });
        |})();
        """.trimMargin()

    private val catalogue =
        """
        |TALARIA.I18N.CATALOG = {
        |    fr: {
        |      fpa: {
        |        title: "Formateur Professionnel d'Adultes"
        |      }
        |    },
        |    en: {
        |      fpa: {
        |        title: "Professional Adult Trainer"
        |      }
        |    }
        |  };
        """.trimMargin()

    private fun files(vararg entries: Pair<String, String>): Map<String, String> =
        linkedMapOf(*entries)

    @Nested
    inner class ReferenceFloor {
        @Test
        fun `floor exposes the reference keys and drops html variants`() {
            val dictionaries = I18nJsDictionary.parse(chrome)

            val floor = I18nClientDelta.referenceFloor(dictionaries, "fr")

            assertThat(floor).containsExactly("nav.cart", "nav.home")
        }

        @Test
        fun `floor is empty without a reference block`() {
            assertThat(I18nClientDelta.referenceFloor(I18nJsDictionary.parse(patch), "fr")).isEmpty()
        }
    }

    @Nested
    inner class MissingKeys {
        @Test
        fun `missing keys are the floor keys not owned by the target`() {
            val dictionaries = I18nJsDictionary.parse(chrome, patch)

            val missing = I18nClientDelta.missingKeys(dictionaries, "fr", listOf("en", "it", "de"))

            assertThat(missing["en"]).containsExactly("nav.cart")
            assertThat(missing["it"]).containsExactly("nav.cart")
            assertThat(missing["de"]).containsExactly("nav.cart", "nav.home")
        }

        @Test
        fun `the reference language is never missing anything`() {
            val dictionaries = I18nJsDictionary.parse(chrome)

            assertThat(I18nClientDelta.missingKeys(dictionaries, "fr", listOf("fr"))["fr"]).isEmpty()
        }

        @Test
        fun `a complete dictionary yields no missing key`() {
            val dictionaries =
                I18nJsDictionary.parse(
                    """
                    |  var DICT = {
                    |    fr: {
                    |      "nav.home": "Accueil",
                    |      "nav.cart": "Panier"
                    |    },
                    |    en: {
                    |      "nav.home": "Home",
                    |      "nav.cart": "Cart"
                    |    }
                    |  };
                    """.trimMargin(),
                )

            assertThat(I18nClientDelta.missingKeys(dictionaries, "fr", listOf("fr", "en"))["en"]).isEmpty()
        }

        @Test
        fun `html variants are never part of the floor`() {
            val dictionaries = I18nJsDictionary.parse(chrome)

            val missing = I18nClientDelta.missingKeys(dictionaries, "fr", listOf("en"))

            assertThat(missing["en"]).doesNotContain("hero.html")
        }

        @Test
        fun `missing keys are sorted for determinism`() {
            val dictionaries =
                I18nJsDictionary.parse(
                    """
                    |  var DICT = {
                    |    fr: {
                    |      "zeta": "Z",
                    |      "alpha": "A"
                    |    },
                    |    en: {}
                    |  };
                    """.trimMargin(),
                )

            assertThat(I18nClientDelta.missingKeys(dictionaries, "fr", listOf("en"))["en"])
                .containsExactly("alpha", "zeta")
        }
    }

    @Nested
    inner class LanguageOwners {
        @Test
        fun `owner is the first file that owns the language block`() {
            val owners =
                I18nClientDelta.languageOwners(
                    files = files("chrome" to chrome, "patch" to patch),
                    languages = listOf("en", "it", "de", "fa"),
                )

            assertThat(owners).containsExactlyInAnyOrderEntriesOf(
                mapOf("en" to "chrome", "it" to "patch", "de" to "patch"),
            )
        }

        @Test
        fun `a nested catalogue never owns a flat language block`() {
            val owners =
                I18nClientDelta.languageOwners(
                    files =
                        files(
                            "i18n-content.js" to catalogue,
                            "i18n.js" to chrome,
                            "i18n-extra-langs.js" to patch,
                        ),
                    languages = listOf("fr", "en", "it"),
                )

            assertThat(owners).containsExactlyInAnyOrderEntriesOf(
                mapOf("fr" to "i18n.js", "en" to "i18n.js", "it" to "i18n-extra-langs.js"),
            )
        }

        @Test
        fun `sorted real tree keeps the flat chrome as the owner, not the catalogue`() {
            val owners =
                I18nClientDelta.languageOwners(
                    files =
                        linkedMapOf(
                            "i18n-content.js" to catalogue,
                            "i18n-extra-langs.js" to patch,
                            "i18n.js" to chrome,
                        ),
                    languages = listOf("en", "it"),
                )

            assertThat(owners)
                .describedAs("the alphabetical tree order puts the catalogue first — it must not win")
                .containsExactlyInAnyOrderEntriesOf(mapOf("en" to "i18n.js", "it" to "i18n-extra-langs.js"))
        }
    }

    @Nested
    inner class Plan {
        @Test
        fun `plan merges chrome and patch then dispatches keys to the owning file`() {
            val plan =
                I18nClientDelta.plan(
                    files = files("chrome" to chrome, "patch" to patch),
                    referenceLanguage = "fr",
                    targetLanguages = listOf("en", "it", "de"),
                )

            assertThat(plan).containsExactly(
                I18nClientFilePlan("chrome", "en", listOf("nav.cart")),
                I18nClientFilePlan("patch", "it", listOf("nav.cart")),
                I18nClientFilePlan("patch", "de", listOf("nav.cart", "nav.home")),
            )
        }

        @Test
        fun `a language without any owning block is not planned`() {
            val plan =
                I18nClientDelta.plan(
                    files = files("chrome" to chrome),
                    referenceLanguage = "fr",
                    targetLanguages = listOf("it"),
                )

            assertThat(plan).isEmpty()
        }

        @Test
        fun `a complete dictionary yields an empty plan`() {
            val complete =
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

            val plan =
                I18nClientDelta.plan(
                    files = files("chrome" to complete),
                    referenceLanguage = "fr",
                    targetLanguages = listOf("fr", "en"),
                )

            assertThat(plan).isEmpty()
        }

        @Test
        fun `plans are deterministic in target language order`() {
            val plan =
                I18nClientDelta.plan(
                    files = files("chrome" to chrome, "patch" to patch),
                    referenceLanguage = "fr",
                    targetLanguages = listOf("de", "it", "en"),
                )

            assertThat(plan.map { it.language }).containsExactly("de", "it", "en")
        }
    }
}
