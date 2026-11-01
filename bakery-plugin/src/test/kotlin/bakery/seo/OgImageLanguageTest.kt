package bakery.seo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * BKY-OUT-1 (i18n_review S-263) — which `og-images/{lang}/` directory a variant
 * head must point to.
 *
 * Measured on the published site: only the reference (`fr`, 110 cards) and `en`
 * (107) hold complete card sets; the 20 other languages hold none. A variant
 * therefore reads its own directory when it exists and the reference otherwise —
 * a partial set is worse than a reference card, because an article without an
 * image in it would 404.
 *
 * Pure rule: the caller probes the disk once and passes the directory names.
 */
class OgImageLanguageTest {
    @Test
    fun `a language with its own cards reads its own directory`() {
        assertThat(OgImageLanguage.resolve("en", "fr", setOf("fr", "en")))
            .isEqualTo("en")
    }

    @Test
    fun `a language without cards falls back to the reference`() {
        assertThat(OgImageLanguage.resolve("de", "fr", setOf("fr", "en")))
            .isEqualTo("fr")
    }

    @Test
    fun `an unrelated directory is never selected`() {
        assertThat(OgImageLanguage.resolve("de", "fr", setOf("fr", "en", "it")))
            .isEqualTo("fr")
    }

    @Test
    fun `the reference language reads its own directory`() {
        assertThat(OgImageLanguage.resolve("fr", "fr", setOf("fr")))
            .isEqualTo("fr")
    }

    @Test
    fun `an absent reference still yields the reference, never a missing directory`() {
        assertThat(OgImageLanguage.resolve("de", "fr", emptySet()))
            .isEqualTo("fr")
    }

    @Test
    fun `a blank language is rejected`() {
        assertThrows<IllegalArgumentException> { OgImageLanguage.resolve(" ", "fr", setOf("fr")) }
    }

    @Test
    fun `a blank reference language is rejected`() {
        assertThrows<IllegalArgumentException> { OgImageLanguage.resolve("en", " ", setOf("fr")) }
    }
}
