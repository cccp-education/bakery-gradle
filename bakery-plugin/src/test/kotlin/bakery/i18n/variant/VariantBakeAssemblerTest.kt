package bakery.i18n.variant

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-LANG-NAV-8 — assemble a throwaway bake root for one language variant.
 *
 * JBake's `Oven` reads `<root>/content`, `<root>/templates`, `<root>/assets` and
 * `<root>/jbake.properties`. A variant owns only `content/` and `templates/`, so
 * the bake root is assembled by overlaying the shared `assets/` and
 * `jbake.properties` onto a copy of the variant. The shared assets are never
 * written back into the variant.
 *
 * A **partial corpus** (D8) may ship a variant's templates before its articles:
 * JBake refuses a root without a `content/` folder, and copying the French
 * reference in would be the copy-FR trap. An empty content shell is materialised
 * instead — the reference content is never inherited.
 *
 * Pure filesystem domain: no Gradle, no LLM, no network.
 */
class VariantBakeAssemblerTest {
    @TempDir
    lateinit var bakeRoot: File

    private fun layout(vararg languages: String): VariantLayout {
        val layout = VariantLayout(bakeRoot)
        layout.referenceTemplates.mkdirs()
        layout.referenceTemplates.resolve("menu.thyme").writeText("<nav>fr</nav>")
        layout.sharedAssets.mkdirs()
        layout.sharedAssets.resolve("styles.css").writeText("body{}")
        layout.jbakeProperties.writeText("site.host=https://example.org")
        languages.forEach { language ->
            layout.templates(language).mkdirs()
            layout.templates(language).resolve("menu.thyme").writeText("<nav>$language</nav>")
        }
        return layout
    }

    @Nested
    inner class Assemble {
        @Test
        fun `the variant content and templates are overlaid onto the bake root`() {
            val layout = layout("en")
            layout.content("en").mkdirs()
            layout.content("en").resolve("index.html").writeText("<h1>Hello</h1>")
            val target = bakeRoot.resolve("assembled/en")

            VariantBakeAssembler(layout).assemble("en", target)

            assertThat(target.resolve("templates/menu.thyme").readText()).contains("en")
            assertThat(target.resolve("content/index.html").readText()).contains("Hello")
        }

        @Test
        fun `the shared assets and jbake properties are copied once`() {
            val layout = layout("en")
            val target = bakeRoot.resolve("assembled/en")

            VariantBakeAssembler(layout).assemble("en", target)

            assertThat(target.resolve("assets/styles.css").readText()).isEqualTo("body{}")
            assertThat(target.resolve("jbake.properties").readText()).contains("site.host")
        }

        @Test
        fun `a partial corpus materialises an empty content shell, never the reference`() {
            val layout = layout("nl")
            layout.content("nl").deleteRecursively()
            layout.referenceTemplates.parentFile
                .resolve("content")
                .mkdirs()
            layout.referenceTemplates.parentFile
                .resolve("content/index.html")
                .writeText("<h1>FR reference</h1>")
            val target = bakeRoot.resolve("assembled/nl")

            VariantBakeAssembler(layout).assemble("nl", target)

            assertThat(target.resolve("content")).isDirectory()
            assertThat(target.resolve("content/index.html"))
                .describedAs("the French reference must never be inherited")
                .doesNotExist()
        }

        @Test
        fun `an absent variant is a hard error`() {
            val layout = layout("en")
            val target = bakeRoot.resolve("assembled/de")

            org.assertj.core.api.Assertions
                .assertThatThrownBy { VariantBakeAssembler(layout).assemble("de", target) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("de")
        }

        @Test
        fun `a previous assembly is wiped before a new one`() {
            val layout = layout("en")
            val target = bakeRoot.resolve("assembled/en")
            target.mkdirs()
            target.resolve("stale.txt").writeText("stale")

            VariantBakeAssembler(layout).assemble("en", target)

            assertThat(target.resolve("stale.txt")).doesNotExist()
        }
    }
}
