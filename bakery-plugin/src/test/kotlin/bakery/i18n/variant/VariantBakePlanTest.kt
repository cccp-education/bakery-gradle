package bakery.i18n.variant

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-LANG-NAV-8 — the variants a full rollout must bake.
 *
 * The reference language is baked at the root by the normal `bake` path; only
 * non-default, **deployable** variants are scheduled. An undeployable variant is
 * skipped, never baked (S-049) — the partial corpus stays deployable without
 * the defective language.
 *
 * Pure filesystem domain: existence probes only, no Gradle, no JBake.
 */
class VariantBakePlanTest {
    @TempDir
    lateinit var bakeRoot: File

    private fun layoutWith(vararg languages: String): VariantLayout {
        val layout = VariantLayout(bakeRoot)
        layout.referenceTemplates.mkdirs()
        layout.referenceTemplates.resolve("menu.thyme").writeText("<x/>")
        languages.forEach { language ->
            val templates = layout.templates(language).also { it.mkdirs() }
            templates.resolve("menu.thyme").writeText("<x/>")
        }
        return layout
    }

    @Nested
    inner class LanguagesToBake {
        @Test
        fun `the reference language is never scheduled as a variant`() {
            val layout = layoutWith("en")

            val plan =
                VariantBakePlan.languagesToBake(
                    layout = layout,
                    supportedLanguages = listOf("fr", "en"),
                    referenceLanguage = "fr",
                )

            assertThat(plan).containsExactly("en")
        }

        @Test
        fun `a supported language without any variant tree is not scheduled`() {
            val layout = layoutWith("en")

            val plan =
                VariantBakePlan.languagesToBake(
                    layout = layout,
                    supportedLanguages = listOf("fr", "en", "de"),
                    referenceLanguage = "fr",
                )

            assertThat(plan).containsExactly("en")
        }

        @Test
        fun `an undeployable variant is skipped, never baked`() {
            val layout = layoutWith("en", "de")
            layout.referenceTemplates.resolve("archive.thyme").writeText("<x/>")
            layout.templates("en").resolve("archive.thyme").writeText("<x/>")

            val plan =
                VariantBakePlan.languagesToBake(
                    layout = layout,
                    supportedLanguages = listOf("fr", "en", "de"),
                    referenceLanguage = "fr",
                )

            assertThat(plan)
                .describedAs("de is missing archive.thyme — baking it would abort the whole render")
                .containsExactly("en")
        }

        @Test
        fun `the schedule preserves the supported-language order`() {
            val layout = layoutWith("de", "en", "ar")

            val plan =
                VariantBakePlan.languagesToBake(
                    layout = layout,
                    supportedLanguages = listOf("fr", "ar", "en", "de"),
                    referenceLanguage = "fr",
                )

            assertThat(plan).containsExactly("ar", "en", "de")
        }

        @Test
        fun `when only the reference is configured the variants are discovered from the i18n tree`() {
            val layout = layoutWith("en", "de")

            val plan =
                VariantBakePlan.languagesToBake(
                    layout = layout,
                    supportedLanguages = listOf("fr"),
                    referenceLanguage = "fr",
                )

            assertThat(plan)
                .describedAs("a site.yml carrying no i18n config is baked from its i18n tree")
                .containsExactly("de", "en")
        }
    }

    @Nested
    inner class DiscoverLanguages {
        @Test
        fun `the i18n sub-directories are discovered, sorted, reference excluded`() {
            val layout = layoutWith("en", "de", "ar")
            layout.i18nRoot.resolve("not-a-lang-file.txt").writeText("x")

            assertThat(layout.discoverLanguages("fr"))
                .containsExactly("ar", "de", "en")
        }

        @Test
        fun `an absent i18n tree discovers nothing`() {
            val layout = VariantLayout(bakeRoot)

            assertThat(layout.discoverLanguages("fr")).isEmpty()
        }
    }

    @Nested
    inner class DiscoverBundledLanguages {
        @Test
        fun `the frozen message bundles declare the materialisable languages`() {
            val layout = VariantLayout(bakeRoot)
            layout.referenceTemplates.mkdirs()
            layout.referenceTemplates.resolve("messages_fr.properties").writeText("a=1")
            layout.referenceTemplates.resolve("messages_en.properties").writeText("a=2")
            layout.referenceTemplates.resolve("messages_ar.properties").writeText("a=3")

            assertThat(layout.discoverBundledLanguages("fr"))
                .describedAs("the reference bundle must never be materialised as a variant")
                .containsExactly("ar", "en")
        }

        @Test
        fun `a reference without any bundle declares nothing`() {
            val layout = VariantLayout(bakeRoot)
            layout.referenceTemplates.mkdirs()
            layout.referenceTemplates.resolve("menu.thyme").writeText("<nav/>")

            assertThat(layout.discoverBundledLanguages("fr")).isEmpty()
        }
    }
}
