package bakery.lens

import bakery.BakeConfiguration
import bakery.ResolvedConfigs
import bakery.SiteConfiguration
import bakery.site.GenerateSiteService
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
 * Refactor session 139 : le miroir privé a été supprimé au profit de l'API
 * publique [GenerateSiteService.injectConfigIntoJbakeProperties] qui teste
 * la vraie fonction de production (CS-FIN-2 clôture).
 *
 * Méthodologie : DDD/TDD baby steps.
 */
class LensBudgetInjectionTest {

    @TempDir
    lateinit var tempDir: File

    private fun defaultConfigs() = ResolvedConfigs(
        firebase = bakery.FirebaseProjectInfo(projectId = "", apiKey = ""),
        googleForms = bakery.GoogleFormsConfig(),
        firebaseAuth = bakery.FirebaseAuthConfig(),
        comments = bakery.CommentsConfig(),
        analytics = bakery.AnalyticsConfig(),
        newsletter = bakery.NewsletterConfig(),
        theme = bakery.ThemeConfig(),
        layout = bakery.LayoutConfig()
    )

    private fun siteWithSrcPath(srcPath: String = "site") =
        SiteConfiguration(bake = BakeConfiguration(srcPath, "build"))

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

            val augmentedDsl = AugmentedContextDsl()
            augmentedDsl.enabled = true

            val result = GenerateSiteService.injectConfigIntoJbakeProperties(
                tempDir, siteWithSrcPath(), defaultConfigs(), augmentedDsl
            )

            assertThat(result).isTrue()
            val props = jbakeProps.readText(UTF_8)
            assertThat(props).contains("augmentedContextEnabled=true")
            assertThat(props).contains("lensBudgetMaxArticlesPerPage=4")
            assertThat(props).contains("lensBudgetMinSimilarity=0.7")
            assertThat(props).contains("template.index.file=index.thyme")
        }

        @Test
        @DisplayName("budget personnalisé → injecte les valeurs custom")
        fun `custom budget injects custom values`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("template.index.file=index.thyme\n", UTF_8)

            val augmentedDsl = AugmentedContextDsl()
            augmentedDsl.enabled = true
            augmentedDsl.budget.maxArticlesPerPage = 6
            augmentedDsl.budget.minSimilarity = 0.85

            val result = GenerateSiteService.injectConfigIntoJbakeProperties(
                tempDir, siteWithSrcPath(), defaultConfigs(), augmentedDsl
            )

            assertThat(result).isTrue()
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

            val augmentedDsl = AugmentedContextDsl()
            augmentedDsl.enabled = false

            val result = GenerateSiteService.injectConfigIntoJbakeProperties(
                tempDir, siteWithSrcPath(), defaultConfigs(), augmentedDsl
            )

            assertThat(result).isTrue()
            val props = jbakeProps.readText(UTF_8)
            assertThat(props).doesNotContain("augmentedContextEnabled")
            assertThat(props).doesNotContain("lensBudgetMaxArticlesPerPage")
            assertThat(props).doesNotContain("lensBudgetMinSimilarity")
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

            val augmentedDsl = AugmentedContextDsl()
            augmentedDsl.enabled = true
            augmentedDsl.budget.maxArticlesPerPage = 4
            augmentedDsl.budget.minSimilarity = 0.7

            val result = GenerateSiteService.injectConfigIntoJbakeProperties(
                tempDir, siteWithSrcPath(), defaultConfigs(), augmentedDsl
            )

            assertThat(result).isTrue()
            val props = jbakeProps.readText(UTF_8)
            assertThat(props).contains("augmentedContextEnabled=true")
            assertThat(props).contains("lensBudgetMaxArticlesPerPage=4")
            assertThat(props).contains("lensBudgetMinSimilarity=0.7")
            assertThat(props).doesNotContain("augmentedContextEnabled=false")
            assertThat(props).doesNotContain("lensBudgetMaxArticlesPerPage=2")
        }
    }
}