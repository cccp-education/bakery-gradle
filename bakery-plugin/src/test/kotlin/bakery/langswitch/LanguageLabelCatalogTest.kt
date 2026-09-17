package bakery.langswitch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import contracts.i18n.LanguageCatalog

/**
 * CHE-I18N-22 US-2 — the language switcher labels were a 10-entry hardcoded map
 * in `InjectLangSwitchTask`, so the 12 languages added to the N0
 * [LanguageCatalog] (`it nl de el tr vi th id ko ja sr fa`) rendered as raw
 * codes (`it`, `nl`, …) in the dropdown.
 *
 * This object derives every label from the N0 contract — the same anti
 * split-brain rule as `BakeryConstants.SUPPORTED_LANGS` (S-217).
 */
class LanguageLabelCatalogTest {
    @Test
    fun `every supported language has a native label`() {
        val labels = LanguageLabelCatalog.labels()

        LanguageCatalog.ALL.forEach { language ->
            assertTrue(labels.containsKey(language.code), "Missing label for '${language.code}'")
            assertEquals(
                language.nativeName,
                labels[language.code],
                "Label for '${language.code}' must be the native name",
            )
        }
    }

    @Test
    fun `covers all 22 languages`() {
        assertEquals(22, LanguageLabelCatalog.labels().size)
    }

    @Test
    fun `the previously missing twelve languages carry a native label`() {
        val labels = LanguageLabelCatalog.labels()

        listOf("it", "nl", "de", "el", "tr", "vi", "th", "id", "ko", "ja", "sr", "fa").forEach { code ->
            assertTrue(labels.containsKey(code), "Missing label for '$code'")
            assertTrue(labels.getValue(code).isNotBlank(), "Blank label for '$code'")
        }
    }
}
