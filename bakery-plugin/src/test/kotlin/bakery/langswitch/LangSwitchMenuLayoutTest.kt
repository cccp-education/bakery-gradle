package bakery.langswitch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-LANG-NAV-8 — the language-variant template layout resolver.
 *
 * `materializeTemplates` writes a deployable variant under
 * `i18n/{lang}/templates/` (the frozen-bundle layout the golden masters own),
 * while the historical `injectLangSwitch` only looked at `{lang}/templates/`.
 * The two layouts must be resolved by a single rule so the materialised variant
 * can receive the same-page selector — the end-to-end chain the rollout needs.
 *
 * The rule is pure path ordering: the i18n layout wins when present, then the
 * direct variant layout (cheroliv/talaria sites), then the reference root for
 * the default language. No content parsing, no Gradle.
 */
class LangSwitchMenuLayoutTest {
    @Nested
    inner class RelativeCandidates {
        @Test
        fun `the default language lives at the reference root`() {
            assertThat(LangSwitchMenuLayout.relativeCandidates("fr", "fr"))
                .containsExactly("templates/menu.thyme")
        }

        @Test
        fun `a non-default language prefers the materialised i18n layout`() {
            assertThat(LangSwitchMenuLayout.relativeCandidates("en", "fr"))
                .containsExactly(
                    "i18n/en/templates/menu.thyme",
                    "en/templates/menu.thyme",
                )
        }
    }

    @Nested
    inner class ResolveOnDisk {
        @TempDir
        lateinit var siteRoot: File

        @Test
        fun `the reference root resolves for the default language`() {
            siteRoot.resolve("templates").mkdirs()
            siteRoot.resolve("templates/menu.thyme").writeText("<nav/>")

            val resolved = LangSwitchMenuLayout.resolve(siteRoot, "fr", "fr")

            assertThat(resolved).isEqualTo(siteRoot.resolve("templates/menu.thyme"))
        }

        @Test
        fun `the materialised i18n tree wins over the direct variant tree`() {
            siteRoot.resolve("i18n/en/templates").mkdirs()
            siteRoot.resolve("i18n/en/templates/menu.thyme").writeText("<nav>i18n</nav>")
            siteRoot.resolve("en/templates").mkdirs()
            siteRoot.resolve("en/templates/menu.thyme").writeText("<nav>legacy</nav>")

            val resolved = LangSwitchMenuLayout.resolve(siteRoot, "en", "fr")

            assertThat(resolved).isEqualTo(siteRoot.resolve("i18n/en/templates/menu.thyme"))
            assertThat(resolved!!.readText()).contains("i18n")
        }

        @Test
        fun `the direct variant tree is the backward-compatible fallback`() {
            siteRoot.resolve("en/templates").mkdirs()
            siteRoot.resolve("en/templates/menu.thyme").writeText("<nav>legacy</nav>")

            val resolved = LangSwitchMenuLayout.resolve(siteRoot, "en", "fr")

            assertThat(resolved).isEqualTo(siteRoot.resolve("en/templates/menu.thyme"))
        }

        @Test
        fun `a language with no menu at all resolves to null`() {
            siteRoot.resolve("templates").mkdirs()

            assertThat(LangSwitchMenuLayout.resolve(siteRoot, "en", "fr")).isNull()
        }
    }
}
