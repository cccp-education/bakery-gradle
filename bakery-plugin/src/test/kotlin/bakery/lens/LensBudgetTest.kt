package bakery.lens

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Action
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * BKY-LENS-3.1 — Tests unitaires pour LensBudget (data class + DSL + apply).
 *
 * Méthodologie : DDD/TDD baby steps — chaque test compile ET passe AVANT de passer au suivant.
 */
class LensBudgetTest {

    @Nested
    @DisplayName("LensBudget — Valeurs par défaut")
    inner class Defaults {

        @Test
        @DisplayName("LensBudget maxArticlesPerPage par défaut = 4")
        fun `default maxArticlesPerPage is 4`() {
            val budget = LensBudget()
            assertThat(budget.maxArticlesPerPage).isEqualTo(4)
        }

        @Test
        @DisplayName("LensBudget minSimilarity par défaut = 0.7")
        fun `default minSimilarity is 0_7`() {
            val budget = LensBudget()
            assertThat(budget.minSimilarity).isEqualTo(0.7)
        }
    }

    @Nested
    @DisplayName("LensBudget — Modification des propriétés")
    inner class Mutation {

        @Test
        @DisplayName("LensBudget maxArticlesPerPage modifiable")
        fun `maxArticlesPerPage can be set`() {
            val budget = LensBudget()
            budget.maxArticlesPerPage = 6
            assertThat(budget.maxArticlesPerPage).isEqualTo(6)
        }

        @Test
        @DisplayName("LensBudget minSimilarity modifiable")
        fun `minSimilarity can be set`() {
            val budget = LensBudget()
            budget.minSimilarity = 0.85
            assertThat(budget.minSimilarity).isEqualTo(0.85)
        }
    }

    @Nested
    @DisplayName("LensBudget — data class copy")
    inner class DataClassCopy {

        @Test
        @DisplayName("LensBudget copié avec modifications")
        fun `LensBudget can be copied with modifications`() {
            val original = LensBudget()
            val modified = original.copy(maxArticlesPerPage = 6, minSimilarity = 0.9)
            assertThat(modified.maxArticlesPerPage).isEqualTo(6)
            assertThat(modified.minSimilarity).isEqualTo(0.9)
            // Original unchanged
            assertThat(original.maxArticlesPerPage).isEqualTo(4)
            assertThat(original.minSimilarity).isEqualTo(0.7)
        }
    }

    @Nested
    @DisplayName("LensBudget.apply() — Filtrage et troncature")
    inner class ApplyBudget {

        private val nodes = listOf(
            ScoredNode("a.md", "A", null, emptyList(), 0.0, 0.0, 0.0, 0, 0.95),
            ScoredNode("b.md", "B", null, emptyList(), 0.0, 0.0, 0.0, 0, 0.85),
            ScoredNode("c.md", "C", null, emptyList(), 0.0, 0.0, 0.0, 0, 0.75),
            ScoredNode("d.md", "D", null, emptyList(), 0.0, 0.0, 0.0, 0, 0.60),
            ScoredNode("e.md", "E", null, emptyList(), 0.0, 0.0, 0.0, 0, 0.30),
        )

        @Test
        @DisplayName("apply avec defaults : 4 articles, minSimilarity 0.7 → 3 gardés")
        fun `apply with defaults keeps 3 of 5`() {
            val budget = LensBudget()
            val result = budget.apply(nodes)
            assertThat(result).hasSize(3)
            assertThat(result.map { it.nodeId }).containsExactly("a.md", "b.md", "c.md")
        }

        @Test
        @DisplayName("apply avec maxArticlesPerPage=2 → 2 gardés")
        fun `apply with maxArticles 2 keeps 2`() {
            val budget = LensBudget(maxArticlesPerPage = 2)
            val result = budget.apply(nodes)
            assertThat(result).hasSize(2)
            assertThat(result.map { it.nodeId }).containsExactly("a.md", "b.md")
        }

        @Test
        @DisplayName("apply avec minSimilarity=0.9 → 1 gardé")
        fun `apply with minSimilarity 0_9 keeps 1`() {
            val budget = LensBudget(minSimilarity = 0.9)
            val result = budget.apply(nodes)
            assertThat(result).hasSize(1)
            assertThat(result[0].nodeId).isEqualTo("a.md")
        }

        @Test
        @DisplayName("apply avec liste vide → résultat vide")
        fun `apply with empty list returns empty`() {
            val budget = LensBudget()
            val result = budget.apply(emptyList())
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("apply exclut les scores exactement au seuil (>= est inclusif)")
        fun `apply includes score exactly at threshold`() {
            val nodes = listOf(
                ScoredNode("a.md", "A", null, emptyList(), 0.0, 0.0, 0.0, 0, 0.7),
            )
            val budget = LensBudget(minSimilarity = 0.7)
            val result = budget.apply(nodes)
            assertThat(result).hasSize(1)
        }

        @Test
        @DisplayName("apply exclut les scores juste en dessous du seuil")
        fun `apply excludes score just below threshold`() {
            val nodes = listOf(
                ScoredNode("a.md", "A", null, emptyList(), 0.0, 0.0, 0.0, 0, 0.69),
            )
            val budget = LensBudget(minSimilarity = 0.7)
            val result = budget.apply(nodes)
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("AugmentedContextDsl — budget DSL")
    inner class BudgetDsl {

        @Test
        @DisplayName("AugmentedContextDsl budget est une LensBudget")
        fun `budget is LensBudget`() {
            val dsl = AugmentedContextDsl()
            assertThat(dsl.budget).isInstanceOf(LensBudget::class.java)
            assertThat(dsl.budget.maxArticlesPerPage).isEqualTo(4)
            assertThat(dsl.budget.minSimilarity).isEqualTo(0.7)
        }

        @Test
        @DisplayName("AugmentedContextDsl budget peut être configuré via Action")
        fun `budget can be configured via Action`() {
            val dsl = AugmentedContextDsl()
            dsl.budget { budget ->
                budget.maxArticlesPerPage = 6
                budget.minSimilarity = 0.85
            }
            assertThat(dsl.budget.maxArticlesPerPage).isEqualTo(6)
            assertThat(dsl.budget.minSimilarity).isEqualTo(0.85)
        }
    }
}