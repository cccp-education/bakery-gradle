package bakery.i18n.variant

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-LANG-NAV-8 — is an `i18n/{lang}` variant structurally bakeable?
 *
 * A variant owns a translated `templates/`; a template like `archive.thyme`
 * does `th:replace="menu.thyme::menu"`. When one reference template is missing,
 * JBake aborts the **whole** render — the lesson of S-049 (the CI aborted the
 * deploy step, and the 21 already-baked languages were never published).
 *
 * The rule therefore is: a variant must carry **every** reference template to
 * be baked. Incomplete variants are reported, never baked — the partial corpus
 * stays deployable, without the defective language that would block everything.
 *
 * Pure filesystem domain: no Gradle, no LLM, no network.
 */
class VariantDeployabilityTest {
    @Nested
    inner class UndeployableTemplates {
        @TempDir
        lateinit var dir: File

        @Test
        fun `a variant carrying every reference template is deployable`() {
            val reference = dir.resolve("reference").also { it.mkdirs() }
            val variant = dir.resolve("variant").also { it.mkdirs() }
            listOf("menu.thyme", "footer.thyme").forEach {
                reference.resolve(it).writeText("<x/>")
                variant.resolve(it).writeText("<x/>")
            }

            assertThat(VariantDeployability.undeployableTemplates(reference, variant)).isEmpty()
        }

        @Test
        fun `a missing reference template makes the variant undeployable`() {
            val reference = dir.resolve("reference").also { it.mkdirs() }
            val variant = dir.resolve("variant").also { it.mkdirs() }
            reference.resolve("menu.thyme").writeText("<x/>")
            reference.resolve("archive.thyme").writeText("<x/>")
            variant.resolve("menu.thyme").writeText("<x/>")

            assertThat(VariantDeployability.undeployableTemplates(reference, variant))
                .containsExactly("archive.thyme")
        }

        @Test
        fun `the missing templates are sorted for a deterministic report`() {
            val reference = dir.resolve("reference").also { it.mkdirs() }
            val variant = dir.resolve("variant").also { it.mkdirs() }
            listOf("z.thyme", "a.thyme", "m.thyme").forEach { reference.resolve(it).writeText("<x/>") }

            assertThat(VariantDeployability.undeployableTemplates(reference, variant))
                .containsExactly("a.thyme", "m.thyme", "z.thyme")
        }

        @Test
        fun `a message bundle in the reference templates is not a deployable template`() {
            val reference = dir.resolve("reference").also { it.mkdirs() }
            val variant = dir.resolve("variant").also { it.mkdirs() }
            reference.resolve("menu.thyme").writeText("<x/>")
            reference.resolve("messages_en.properties").writeText("menu.1=Home")
            variant.resolve("menu.thyme").writeText("<x/>")

            assertThat(VariantDeployability.undeployableTemplates(reference, variant))
                .describedAs("JBake renders .thyme only — a frozen bundle is not a template")
                .isEmpty()
        }
    }

    @Nested
    inner class UndeployableVariants {
        @TempDir
        lateinit var bakeRoot: File

        @Test
        fun `only languages whose variant exists and is incomplete are reported`() {
            val layout = VariantLayout(bakeRoot)
            layout.referenceTemplates.mkdirs()
            layout.referenceTemplates.resolve("menu.thyme").writeText("<x/>")
            layout.referenceTemplates.resolve("archive.thyme").writeText("<x/>")

            val complete = layout.variant("en")
            complete.resolve("templates").mkdirs()
            complete.resolve("templates/menu.thyme").writeText("<x/>")
            complete.resolve("templates/archive.thyme").writeText("<x/>")

            val incomplete = layout.variant("de")
            incomplete.resolve("templates").mkdirs()
            incomplete.resolve("templates/menu.thyme").writeText("<x/>")
            val reported =
                VariantDeployability.undeployableVariants(
                    layout = layout,
                    supportedLanguages = listOf("fr", "en", "de", "it"),
                    referenceLanguage = "fr",
                )

            assertThat(reported).containsOnlyKeys("de")
            assertThat(reported["de"]).containsExactly("archive.thyme")
        }

        @Test
        fun `a language without any variant tree is never reported`() {
            val layout = VariantLayout(bakeRoot)
            layout.referenceTemplates.mkdirs()
            layout.referenceTemplates.resolve("menu.thyme").writeText("<x/>")

            val reported =
                VariantDeployability.undeployableVariants(
                    layout = layout,
                    supportedLanguages = listOf("fr", "en"),
                    referenceLanguage = "fr",
                )

            assertThat(reported).isEmpty()
        }
    }
}
