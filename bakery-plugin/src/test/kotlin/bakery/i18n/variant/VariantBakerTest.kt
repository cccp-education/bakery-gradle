package bakery.i18n.variant

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-LANG-NAV-8 — bake every deployable variant into a `{lang}/` sub-directory.
 *
 * The reference language is baked at the root by the normal `bake` path. Each
 * non-default deployable variant is baked from a throwaway assembled root
 * ([VariantBakeAssembler]) into `{dest}/{lang}/`, then the throwaway root is
 * removed. The output sub-directory is **wiped first**: `bake` never cleans its
 * output (S-049), so a stale page would shadow a real one.
 *
 * The actual bake is an injected `(sourceRoot, outputDir) -> Unit` — the domain
 * stays jbake-free, and the orchestration is unit-testable with a fake bake.
 */
class VariantBakerTest {
    @TempDir
    lateinit var root: File

    private lateinit var layout: VariantLayout
    private val bakedRoots = linkedMapOf<String, File>()
    private val assembledHadSharedAssets = linkedMapOf<String, Boolean>()

    private fun setUpLayout(vararg languages: String) {
        layout = VariantLayout(root.resolve("jbake"))
        layout.referenceTemplates.mkdirs()
        layout.referenceTemplates.resolve("menu.thyme").writeText("<nav>fr</nav>")
        layout.sharedAssets.mkdirs()
        layout.sharedAssets.resolve("styles.css").writeText("body{}")
        layout.jbakeProperties.writeText("site.host=https://example.org")
        languages.forEach { language ->
            layout.templates(language).mkdirs()
            layout.templates(language).resolve("menu.thyme").writeText("<nav>$language</nav>")
            layout.content(language).mkdirs()
            layout.content(language).resolve("index.adoc").writeText("= $language")
        }
    }

    private fun baker() =
        VariantBaker(layout) { sourceRoot, outputDir ->
            bakedRoots[outputDir.name] = sourceRoot
            assembledHadSharedAssets[outputDir.name] =
                sourceRoot.resolve("assets/styles.css").exists() &&
                    sourceRoot.resolve("jbake.properties").exists()
            outputDir.mkdirs()
            outputDir.resolve("index.html").writeText("<html>${sourceRoot.name}</html>")
        }

    @Nested
    inner class BakeAll {
        @Test
        fun `each language is baked into its own sub-directory`() {
            setUpLayout("en", "de")
            val dest = root.resolve("dest")

            val baked = baker().bakeAll(listOf("en", "de"), dest)

            assertThat(baked).containsExactly("en", "de")
            assertThat(dest.resolve("en/index.html")).exists()
            assertThat(dest.resolve("de/index.html")).exists()
        }

        @Test
        fun `the bake receives the assembled root, not the variant tree`() {
            setUpLayout("en")
            val dest = root.resolve("dest")

            baker().bakeAll(listOf("en"), dest)

            assertThat(bakedRoots.getValue("en").name).isEqualTo("en")
            assertThat(assembledHadSharedAssets.getValue("en"))
                .describedAs("the assembled root must carry the shared assets and jbake.properties")
                .isTrue()
        }

        @Test
        fun `the throwaway assembled roots are removed after the bake`() {
            setUpLayout("en")
            val dest = root.resolve("dest")

            baker().bakeAll(listOf("en"), dest)

            assertThat(dest.parentFile.resolve("i18n-assembled")).doesNotExist()
        }

        @Test
        fun `a stale destination sub-directory is wiped before the bake`() {
            setUpLayout("en")
            val dest = root.resolve("dest")
            dest.resolve("en").mkdirs()
            dest.resolve("en/stale.html").writeText("stale")

            baker().bakeAll(listOf("en"), dest)

            assertThat(dest.resolve("en/stale.html"))
                .describedAs("bake never cleans its output (S-049) — the baker must")
                .doesNotExist()
            assertThat(dest.resolve("en/index.html")).exists()
        }

        @Test
        fun `an empty language list bakes nothing and leaves no assembled root`() {
            setUpLayout("en")
            val dest = root.resolve("dest")

            val baked = baker().bakeAll(emptyList(), dest)

            assertThat(baked).isEmpty()
            assertThat(dest.parentFile.resolve("i18n-assembled")).doesNotExist()
        }
    }
}
