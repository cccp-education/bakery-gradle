package bakery.lens

import contracts.context.ChannelType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests unitaires pour [LensPipelineService] — BKY-LENS CS-2.
 *
 * Déplace la logique métier du [doLast] de [SiteManager] dans un service
 * testable isolément. Chaque test vérifie une étape du pipeline :
 * 1. Composite context (chargement / skip)
 * 2. Sous-graphe (extraction / skip)
 * 3. Scoring (hybride)
 * 4. Budget (application)
 * 5. Sortie JSON (écriture)
 *
 * Méthodologie : baby-step TDD — chaque test compile ET passe AVANT le suivant.
 */
class LensPipelineServiceTest {

    @TempDir
    lateinit var tempDir: File

    private val service = LensPipelineService()

    // ── Helpers ────────────────────────────────────────────────────────

    private fun dsl(enabled: Boolean = true): AugmentedContextDsl =
        AugmentedContextDsl().apply {
            this.enabled = enabled
            contextPath = tempDir.resolve("composite-context.json").absolutePath
            lens.apply {
                graphFilePath = tempDir.resolve("graph.json").absolutePath
                scope = LensScope.SUBGRAPH
                nodeTypes = listOf("file")
                edgeTypes = listOf("reference")
                maxDepth = 2
            }
            budget.apply {
                maxArticlesPerPage = 4
                minSimilarity = 0.7
            }
        }

    private fun createEmptyGraphJson(): File {
        val f = tempDir.resolve("graph.json")
        f.writeText("""{"nodes":[],"edges":[],"communities":[]}""", Charsets.UTF_8)
        return f
    }

    private fun createEmptyCompositeContext(): File {
        val f = tempDir.resolve("composite-context.json")
        f.writeText("""{
            "eagerSection": "test",
            "ragSection": "",
            "graphifySection": "",
            "docsSection": "",
            "config": {
                "totalTokenBudget": 8000,
                "budgetEagerLazy": 0.40,
                "budgetRag": 0.30,
                "budgetGraphify": 0.20,
                "budgetDocs": 0.10,
                "budgetOverhead": 0.0
            }
        }""".trimIndent(), Charsets.UTF_8)
        return f
    }

    // ── Tests ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Pipeline execute — configuration désactivée")
    inner class Disabled {

        @Test
        @DisplayName("retourne output vide quand enabled=false")
        fun `returns empty output when disabled`() {
            val config = dsl(enabled = false)
            val outputDir = tempDir.resolve("output")
            val contextFile = tempDir.resolve("composite-context.json")
            val graphFile = tempDir.resolve("graph.json")

            val result = service.execute(config, contextFile, graphFile, outputDir)

            assertThat(result.totalCandidates).isEqualTo(0)
            assertThat(result.totalAfterRules).isEqualTo(0)
            assertThat(result.totalAfterBudget).isEqualTo(0)
            assertThat(result.scoredNodes).isEmpty()
            // JSON file should NOT be created when config is disabled —
            // because the caller (SiteManager) skips execute entirely.
            // This test documents the behavior when called directly.
            assertThat(outputDir.exists()).isFalse()
        }
    }

    @Nested
    @DisplayName("Pipeline execute — fichiers absents")
    inner class MissingFiles {

        @Test
        @DisplayName("graphique absent → sous-graphe vide → 0 nœuds")
        fun `skips graph when file missing`() {
            val config = dsl(enabled = true)
            val outputDir = tempDir.resolve("output")
            createEmptyCompositeContext()
            // graph.json does NOT exist

            val result = service.execute(config, tempDir.resolve("composite-context.json"), tempDir.resolve("graph.json"), outputDir)

            assertThat(result.totalCandidates).isEqualTo(0)
            assertThat(result.totalAfterRules).isEqualTo(0)
            assertThat(result.totalAfterBudget).isEqualTo(0)
            assertThat(outputDir.resolve("augmented-context.json")).exists()
        }

        @Test
        @DisplayName("composite-context absent → RAG désactivé → pipeline continue")
        fun `skips context when file missing`() {
            val config = dsl(enabled = true)
            val outputDir = tempDir.resolve("output")
            createEmptyGraphJson()
            // composite-context.json does NOT exist

            val result = service.execute(config, tempDir.resolve("composite-context.json"), tempDir.resolve("graph.json"), outputDir)

            assertThat(result.totalCandidates).isEqualTo(0)
            assertThat(outputDir.resolve("augmented-context.json")).exists()
        }
    }

    @Nested
    @DisplayName("Pipeline execute — fichiers présents")
    inner class FullPipeline {

        @Test
        @DisplayName("pipeline complet → JSON écrit avec métriques")
        fun `full pipeline writes json with metrics`() {
            val config = dsl(enabled = true)
            val outputDir = tempDir.resolve("output")
            createEmptyGraphJson()
            createEmptyCompositeContext()

            val result = service.execute(config, tempDir.resolve("composite-context.json"), tempDir.resolve("graph.json"), outputDir)

            assertThat(result.version).isEqualTo("1.0")
            assertThat(result.pipeline).isEqualTo("LENS")
            assertThat(result.budget.maxArticlesPerPage).isEqualTo(4)
            assertThat(result.budget.minSimilarity).isEqualTo(0.7)
            assertThat(result.totalCandidates).isEqualTo(0) // empty graph
            assertThat(result.totalAfterRules).isEqualTo(0)
            assertThat(result.totalAfterBudget).isEqualTo(0)
            assertThat(outputDir.resolve("augmented-context.json")).exists()
            val jsonContent = outputDir.resolve("augmented-context.json").readText(Charsets.UTF_8)
            assertThat(jsonContent).contains("\"version\" : \"1.0\"")
            assertThat(jsonContent).contains("\"pipeline\" : \"LENS\"")
            assertThat(jsonContent).contains("\"totalCandidates\"")
            assertThat(jsonContent).contains("\"scoredNodes\"")
        }

        @Test
        @DisplayName("budget appliqué correctement avec sous-graphe vide")
        fun `budget applied with empty subgraph`() {
            val config = dsl(enabled = true)
            val outputDir = tempDir.resolve("output")
            createEmptyGraphJson()
            createEmptyCompositeContext()

            val result = service.execute(config, tempDir.resolve("composite-context.json"), tempDir.resolve("graph.json"), outputDir)

            assertThat(result.budget.maxArticlesPerPage).isEqualTo(4)
            assertThat(result.budget.minSimilarity).isEqualTo(0.7)
            assertThat(result.totalAfterBudget).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("LensPipelineOutput — data class")
    inner class OutputModel {

        @Test
        @DisplayName("valeurs par défaut correctes")
        fun `default values`() {
            val output = LensPipelineOutput(
                budget = BudgetSummary(maxArticlesPerPage = 6, minSimilarity = 0.5),
                scoredNodes = emptyList(),
                totalCandidates = 0,
                totalAfterRules = 0,
                totalAfterBudget = 0
            )
            assertThat(output.version).isEqualTo("1.0")
            assertThat(output.pipeline).isEqualTo("LENS")
        }
    }
}
