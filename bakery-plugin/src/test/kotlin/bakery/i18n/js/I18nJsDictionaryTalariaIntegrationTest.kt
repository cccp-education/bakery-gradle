package bakery.i18n.js

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-2. Dogfooding réel talaria.school.
 *
 * Pins the [I18nJsDictionary] contract on the three real client dictionaries
 * (maquette source of dev, `tools/translate-i18n.mjs` reference harness S-044):
 * language inventory, `fr` reference floor (205 keys excluding `.html`) and the
 * byte-identical round-trip / idempotence guarantees on production content.
 *
 * Skipped (assumeTrue) when the talaria site is absent (CI, fresh checkout).
 * Read-only: the real files are never written.
 */
class I18nJsDictionaryTalariaIntegrationTest {
    private val talariaDir = File("/home/cheroliv/workspace/office/sites/talaria.school/maquette/js")

    private val chromeFile = talariaDir.resolve("i18n.js")
    private val patchFile = talariaDir.resolve("i18n-extra-langs.js")
    private val catalogueFile = talariaDir.resolve("i18n-content.js")

    private fun read(file: File): String {
        assumeTrue(file.isFile) { "${file.name} not found — skip dogfooding (CI/fresh checkout)" }
        return file.readText()
    }

    @Nested
    inner class ReferenceFloor {
        @Test
        fun `chrome dictionary exposes 11 languages and the 205-key fr floor`() {
            val source = read(chromeFile)
            val dictionaries = I18nJsDictionary.parse(source)

            assertThat(dictionaries.keys)
                .containsExactly("fr", "en", "zh", "hi", "es", "ar", "bn", "pt", "ru", "ur", "fa")

            val floor = dictionaries["fr"].orEmpty().keys.filterNot { it.endsWith(".html") }
            assertThat(floor).hasSize(205)
        }

        @Test
        fun `additive patch exposes the 11 extra languages with the same floor`() {
            val chrome = I18nJsDictionary.parse(read(chromeFile))
            val patch = I18nJsDictionary.parse(read(patchFile))

            assertThat(patch.keys)
                .containsExactly("it", "nl", "de", "el", "tr", "vi", "th", "id", "ko", "ja", "sr")

            val floor = chrome["fr"].orEmpty().keys.filterNot { it.endsWith(".html") }
            assertThat(patch.values.all { it.keys.containsAll(floor) }).isTrue()
        }

        @Test
        fun `nested catalogue exposes all 22 languages`() {
            assertThat(I18nJsDictionary.languagesOf(read(catalogueFile))).hasSize(22)
        }
    }

    @Nested
    inner class CatalogueCoverage {
        @Test
        fun `the real catalogue exposes 22 languages and 2 formations each`() {
            val source = read(catalogueFile)

            assertThat(I18nCatalog.languagesOf(source)).hasSize(22)
            I18nCatalog.languagesOf(source).forEach { language ->
                assertThat(I18nCatalog.formationsOf(source, language))
                    .describedAs("formations of %s", language)
                    .containsExactly("fpa", "cda")
            }
        }

        @Test
        fun `every real language owns the same reference fields`() {
            val source = read(catalogueFile)
            val referenceFormations = I18nCatalog.formationsOf(source, "fr")
            val referenceFields = referenceFormations.associateWith { I18nCatalog.fieldsOf(source, "fr", it) }

            val plan = I18nCatalog.plan(source, "fr", I18nCatalog.languagesOf(source))

            assertThat(plan.missingLanguages).isEmpty()
            assertThat(plan.missingFormations).isEmpty()
            assertThat(plan.missingFields).isEmpty()
            assertThat(plan.hasGap).isFalse()
            assertThat(referenceFields["fpa"]).contains("title", "tagline", "objectives", "modules")
        }

        @Test
        fun `the real catalogue is classified as a catalogue, never as a flat dictionary`() {
            val source = read(catalogueFile)

            assertThat(I18nJsFormat.of(source).isCatalogue).isTrue()
            assertThat(I18nJsFormat.of(read(chromeFile)).isFlat).isTrue()
            assertThat(I18nJsFormat.of(read(patchFile)).isFlat).isTrue()
        }

        @Test
        fun `the catalogue is not the owner of the flat chrome languages`() {
            val owners =
                I18nClientDelta.languageOwners(
                    files =
                        linkedMapOf(
                            "i18n-content.js" to read(catalogueFile),
                            "i18n-extra-langs.js" to read(patchFile),
                            "i18n.js" to read(chromeFile),
                        ),
                    languages = listOf("fr", "en", "it"),
                )

            assertThat(owners)
                .containsExactlyInAnyOrderEntriesOf(
                    mapOf("fr" to "i18n.js", "en" to "i18n.js", "it" to "i18n-extra-langs.js"),
                )
        }
    }

    @Nested
    inner class RoundTrip {
        @Test
        fun `re-inserting an already complete chrome dictionary is a strict no-op`() {
            val source = read(chromeFile)
            val dictionaries = I18nJsDictionary.parse(source)

            dictionaries.forEach { (language, entries) ->
                assertThat(I18nJsDictionary.insertTranslations(source, language, entries))
                    .describedAs("chrome %s", language)
                    .isEqualTo(source)
            }
        }

        @Test
        fun `re-inserting an already complete patch dictionary is a strict no-op`() {
            val source = read(patchFile)
            val dictionaries = I18nJsDictionary.parse(source)

            dictionaries.forEach { (language, entries) ->
                assertThat(I18nJsDictionary.insertTranslations(source, language, entries))
                    .describedAs("patch %s", language)
                    .isEqualTo(source)
            }
        }
    }
}
