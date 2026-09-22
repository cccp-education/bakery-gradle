package bakery.i18n

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * CHE-I18N-QUALITY — the deterministic language-switcher block is never sent to
 * the model.
 *
 * `injectLangSwitch` (site domain) writes the switcher between explicit markers
 * (`<!-- che-i18n:lang-switcher:start -->` … `end`). The block lists every
 * language in its *own* name — it is generated, not author-written prose — and it
 * is **re-injected after** every translation anyway (`LangSwitchSiteApplier`).
 * Sending it to the LLM wastes 81 lines × 22 languages and, worse, the model
 * corrupts the big `menu.thyme` skeleton: `ja` was rejected on every run
 * (`RÉSultat structurellement invalide menu.thyme`).
 *
 * The block must be masked before extraction and restored verbatim after
 * substitution, so it is never seen as translatable (Ink Economy Law).
 *
 * Pure domain: no I/O, no LLM, no Gradle.
 */
class SwitcherBlockMaskTest {

    private val template =
        listOf(
            "<nav>",
            "    <h1>Accueil</h1>",
            "</nav>",
            SwitcherBlockMask.START_MARKER,
            "<div class=\"lang-switcher-container\">",
            "    <a data-lang=\"en\">English</a>",
            "    <a data-lang=\"fr\">Français</a>",
            "</div>",
            SwitcherBlockMask.END_MARKER,
            "<footer>Contactez-moi</footer>",
        ).joinToString("\n")

    @Test
    fun `the marked block is masked out of the translated text`() {
        val masked = SwitcherBlockMask.mask(template)

        assertFalse(masked.text.contains("English"), "A language name must never be sent: ${masked.text}")
        assertFalse(masked.text.contains("lang-switcher-container"), "The block must be masked")
        assertTrue(masked.text.contains("Accueil"), "Author prose must stay")
        assertTrue(masked.text.contains("Contactez-moi"), "Author prose must stay")
    }

    @Test
    fun `unmasking restores the block byte for byte`() {
        val masked = SwitcherBlockMask.mask(template)
        val restored = SwitcherBlockMask.unmask(masked.text, masked.blocks)

        assertEquals(template, restored)
    }

    @Test
    fun `a template without markers is a strict no-op`() {
        val plain = "<nav><h1>Accueil</h1></nav>"
        val masked = SwitcherBlockMask.mask(plain)

        assertEquals(plain, masked.text)
        assertTrue(masked.blocks.isEmpty())
        assertEquals(plain, SwitcherBlockMask.unmask(masked.text, masked.blocks))
    }

    @Test
    fun `the mask round-trips a translated skeleton`() {
        val masked = SwitcherBlockMask.mask(template)
        val translated = masked.text.replace("Accueil", "Home").replace("Contactez-moi", "Contact me")
        val restored = SwitcherBlockMask.unmask(translated, masked.blocks)

        assertTrue(restored.contains("Home"))
        assertTrue(restored.contains("Contact me"))
        assertTrue(restored.contains("English"), "The block must survive translation verbatim")
        assertFalse(restored.contains("Accueil"))
    }
}
