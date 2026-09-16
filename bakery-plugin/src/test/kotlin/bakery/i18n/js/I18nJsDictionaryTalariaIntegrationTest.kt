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
