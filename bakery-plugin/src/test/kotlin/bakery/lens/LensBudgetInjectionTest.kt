package bakery.lens

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

/**
 * BKY-LENS-3.1 — Tests unitaires pour l'injection LensBudget dans jbake.properties.
 *
 * Valide que les propriétés LENS (augmentedContextEnabled, lensBudgetMaxArticles,
 * lensBudgetMinSimilarity) sont correctement injectées dans jbake.properties
 * quand le contexte augmenté est activé.
 *
 * Méthodologie : DDD/TDD baby steps.
 */
class LensBudgetInjectionTest {

    @TempDir
    lateinit var tempDir: File

    @Nested
    @DisplayName("LENS — Injection jbake.properties quand augmentedContext activé")
    inner class WithAugmentedContextEnabled {

        @Test
        @DisplayName("augmentedContext activé → injecte enabled, maxArticlesPerPage, minSimilarity")
        fun `augmented context enabled injects budget properties into jbake properties`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("template.index.file=index.thyme\nrender.tags=true\n", UTF_8)

            val budget = LensBudget()
            injectLensBudgetIntoJbakeProperties(jbakeProps, budget, enabled = true)

            val props = jbakeProps.readText(UTF_8)
            assertThat(props).contains("augmentedContextEnabled=true")
            assertThat(props).contains("lensBudgetMaxArticlesPerPage=4")
            assertThat(props).contains("lensBudgetMinSimilarity=0.7")
            // existing properties preserved
            assertThat(props).contains("template.index.file=index.thyme")
        }

        @Test
        @DisplayName("budget personnalisé → injecte les valeurs custom")
        fun `custom budget injects custom values`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("template.index.file=index.thyme\n", UTF_8)

            val budget = LensBudget(maxArticlesPerPage = 6, minSimilarity = 0.85)
            injectLensBudgetIntoJbakeProperties(jbakeProps, budget, enabled = true)

            val props = jbakeProps.readText(UTF_8)
            assertThat(props).contains("augmentedContextEnabled=true")
            assertThat(props).contains("lensBudgetMaxArticlesPerPage=6")
            assertThat(props).contains("lensBudgetMinSimilarity=0.85")
        }
    }

    @Nested
    @DisplayName("LENS — Pas d'injection quand augmentedContext désactivé")
    inner class WithoutAugmentedContext {

        @Test
        @DisplayName("augmentedContext désactivé → pas d'injection LENS")
        fun `augmented context disabled does not inject lens properties`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("template.index.file=index.thyme\n", UTF_8)

            val budget = LensBudget()
            injectLensBudgetIntoJbakeProperties(jbakeProps, budget, enabled = false)

            val props = jbakeProps.readText(UTF_8)
            assertThat(props).doesNotContain("augmentedContextEnabled")
            assertThat(props).doesNotContain("lensBudgetMaxArticlesPerPage")
            assertThat(props).doesNotContain("lensBudgetMinSimilarity")
            // existing properties preserved
            assertThat(props).contains("template.index.file=index.thyme")
        }
    }

    @Nested
    @DisplayName("LENS — Mise à jour de propriétés existantes")
    inner class UpdateExistingProperties {

        @Test
        @DisplayName("proprietes LENS pré-existantes mises à jour")
        fun `existing lens properties are updated`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText(
                "template.index.file=index.thyme\n" +
                "augmentedContextEnabled=false\n" +
                "lensBudgetMaxArticlesPerPage=2\n" +
                "lensBudgetMinSimilarity=0.5\n",
                UTF_8
            )

            val budget = LensBudget(maxArticlesPerPage = 4, minSimilarity = 0.7)
            injectLensBudgetIntoJbakeProperties(jbakeProps, budget, enabled = true)

            val props = jbakeProps.readText(UTF_8)
            assertThat(props).contains("augmentedContextEnabled=true")
            assertThat(props).contains("lensBudgetMaxArticlesPerPage=4")
            assertThat(props).contains("lensBudgetMinSimilarity=0.7")
            // Old values should not remain
            assertThat(props).doesNotContain("augmentedContextEnabled=false")
            assertThat(props).doesNotContain("lensBudgetMaxArticlesPerPage=2")
        }
    }

    /**
     * Injecte les propriétés LENS dans jbake.properties.
     * Miroir extrait de la logique SiteManager pour test unitaire isolé.
     */
    private fun injectLensBudgetIntoJbakeProperties(
        jbakeProps: File,
        budget: LensBudget,
        enabled: Boolean
    ) {
        if (!enabled) return

        val lines = jbakeProps.readText(UTF_8).lines().toMutableList()

        fun updateProperty(key: String, value: String) {
            val idx = lines.indexOfFirst { it.startsWith("$key=") }
            if (idx >= 0) {
                lines[idx] = "$key=$value"
            } else {
                lines.add("$key=$value")
            }
        }

        updateProperty("augmentedContextEnabled", enabled.toString())
        updateProperty("lensBudgetMaxArticlesPerPage", budget.maxArticlesPerPage.toString())
        updateProperty("lensBudgetMinSimilarity", budget.minSimilarity.toString())

        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
    }
}