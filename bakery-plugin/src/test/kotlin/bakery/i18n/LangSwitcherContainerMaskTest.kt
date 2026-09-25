package bakery.i18n

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * BKY-LANG-NAV-8 — strips the generated language-switcher content before a
 * reference template is keyed for frozen-bundle materialisation.
 *
 * `injectLangSwitch` writes the selector fragment **in place** into the
 * reference `menu.thyme` (the `lang-switcher-container` inner content). That
 * fragment is generated markup — its labels are language names, not author
 * prose — and it is re-injected after every materialisation. If the keyer sees
 * it, the generated labels become new `menu.N` keys absent from the frozen
 * bundle: every key is unresolved and the task writes **nothing**, leaving the
 * materialised variant stale (Ink Economy Law broken, constat dogfooding S-238).
 *
 * Stripping the container's inner content — leaving the empty container so
 * `injectLangSwitch` can refill it — keeps materialisation idempotent whether or
 * not the selector was already injected.
 *
 * Domain-pure: strings in, strings out. No I/O, no LLM, no Gradle.
 */
class LangSwitcherContainerMaskTest {
    private val injectedSwitcher =
        """
        <div class="nav-item dropdown lang-switcher-container ms-lg-2">
            <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="langDropdown">
            <li><a class="dropdown-item lang-option" th:href="${'$'}{content.uri != null ? X : Y}" data-lang="fr">Français</a></li>
            <li><a class="dropdown-item lang-option active" th:href="${'$'}{content.uri != null ? X : Y}" data-lang="en">English</a></li>
            </ul>
                </div>
        """.trimIndent()

    private val menuWithSwitcher =
        """
        <nav>
            <ul>
                <li><a>Accueil</a></li>
                <li><a>Plugins</a></li>
            </ul>
        $injectedSwitcher
            <div class="theme-switcher-container"></div>
        </nav>
        """.trimIndent()

    @Nested
    inner class Strip {
        @Test
        fun `removes the generated labels from a filled container`() {
            val stripped = LangSwitcherContainerMask.strip(menuWithSwitcher)

            assertThat(stripped)
                .describedAs("generated labels must not be visible to the keyer")
                .doesNotContain("Français")
                .doesNotContain("English")
                .doesNotContain("lang-option")
            assertThat(stripped)
                .describedAs("the author nav items must survive")
                .contains("Accueil")
                .contains("Plugins")
        }

        @Test
        fun `keeps the empty container so the selector can be re-injected`() {
            val stripped = LangSwitcherContainerMask.strip(menuWithSwitcher)

            assertThat(stripped).contains("lang-switcher-container")
            assertThat(stripped).contains("theme-switcher-container")
        }

        @Test
        fun `an empty container is a strict no-op`() {
            val menu =
                """
                <nav><div class="lang-switcher-container"></div></nav>
                """.trimIndent()

            assertThat(LangSwitcherContainerMask.strip(menu)).isEqualTo(menu)
        }

        @Test
        fun `a menu without the container is a strict no-op`() {
            val menu = "<nav><a>Accueil</a></nav>"

            assertThat(LangSwitcherContainerMask.strip(menu)).isEqualTo(menu)
        }

        @Test
        fun `stripping is idempotent`() {
            val once = LangSwitcherContainerMask.strip(menuWithSwitcher)

            assertThat(LangSwitcherContainerMask.strip(once)).isEqualTo(once)
        }
    }

    @Nested
    inner class KeyerIntegration {
        @Test
        fun `the injected switcher labels are never keyed`() {
            val extractor = I18nMigrationService()
            val stripped = LangSwitcherContainerMask.strip(menuWithSwitcher)

            val extractions = extractor.extractHardcodedText(stripped, "menu")

            assertThat(extractions.values)
                .describedAs("generated language labels must not become keys")
                .doesNotContain("Français", "English")
            assertThat(extractions.values)
                .describedAs("author prose must still be keyed")
                .contains("Accueil", "Plugins")
        }
    }
}
