package bakery.lens

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * BKY-LENS-1.2 — Tests unitaires pour LensConfig, LensScope, AugmentedContextDsl.
 *
 * Méthodologie : DDD/TDD baby steps — chaque test compile ET passe AVANT de passer au suivant.
 */
class LensConfigTest {

    @Nested
    @DisplayName("LensConfig — Valeurs par défaut")
    inner class Defaults {

        @Test
        @DisplayName("LensConfig scope par défaut = SUBGRAPH")
        fun `scope default is SUBGRAPH`() {
            val config = LensConfig()
            assertThat(config.scope).isEqualTo(LensScope.SUBGRAPH)
        }

        @Test
        @DisplayName("LensConfig nodeTypes par défaut = [file]")
        fun `default nodeTypes is file`() {
            val config = LensConfig()
            assertThat(config.nodeTypes).containsExactly("file")
        }

        @Test
        @DisplayName("LensConfig edgeTypes par défaut = [reference, agent_reference]")
        fun `default edgeTypes`() {
            val config = LensConfig()
            assertThat(config.edgeTypes).containsExactly("reference", "agent_reference")
        }

        @Test
        @DisplayName("LensConfig maxDepth par défaut = 2")
        fun `default maxDepth is 2`() {
            val config = LensConfig()
            assertThat(config.maxDepth).isEqualTo(2)
        }

        @Test
        @DisplayName("LensConfig fileExtensions par défaut = [adoc, md, html]")
        fun `default fileExtensions`() {
            val config = LensConfig()
            assertThat(config.fileExtensions).containsExactly("adoc", "md", "html")
        }

        @Test
        @DisplayName("LensConfig communities par défaut = liste vide")
        fun `default communities is empty`() {
            val config = LensConfig()
            assertThat(config.communities).isEmpty()
        }

        @Test
        @DisplayName("LensConfig graphFilePath par défaut = office/graph.json")
        fun `default graphFilePath`() {
            val config = LensConfig()
            assertThat(config.graphFilePath).isEqualTo("office/graph.json")
        }
    }

    @Nested
    @DisplayName("LensConfig — Modification des propriétés")
    inner class Mutation {

        @Test
        @DisplayName("LensConfig modifiable : communautés ajoutées")
        fun `communities can be set`() {
            val config = LensConfig()
            config.communities = listOf("bakery-gradle", "codebase-gradle", "slider-gradle")
            assertThat(config.communities).containsExactly("bakery-gradle", "codebase-gradle", "slider-gradle")
        }

        @Test
        @DisplayName("LensConfig modifiable : scope changé à FULL")
        fun `scope can be changed to FULL`() {
            val config = LensConfig()
            config.scope = LensScope.FULL
            assertThat(config.scope).isEqualTo(LensScope.FULL)
        }

        @Test
        @DisplayName("LensConfig modifiable : scope changé à SEMANTIC_ONLY")
        fun `scope can be changed to SEMANTIC_ONLY`() {
            val config = LensConfig()
            config.scope = LensScope.SEMANTIC_ONLY
            assertThat(config.scope).isEqualTo(LensScope.SEMANTIC_ONLY)
        }

        @Test
        @DisplayName("LensConfig modifiable : maxDepth changé")
        fun `maxDepth can be changed`() {
            val config = LensConfig()
            config.maxDepth = 3
            assertThat(config.maxDepth).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("LensScope — Enum")
    inner class ScopeEnum {

        @Test
        @DisplayName("LensScope a exactement 3 valeurs")
        fun `LensScope has 3 values`() {
            assertThat(LensScope.values()).hasSize(3)
            assertThat(LensScope.values().map { it.name }).containsExactlyInAnyOrder(
                "SUBGRAPH", "FULL", "SEMANTIC_ONLY"
            )
        }
    }

    @Nested
    @DisplayName("AugmentedContextDsl — Valeurs par défaut")
    inner class AugmentedContextDslDefaults {

        @Test
        @DisplayName("AugmentedContextDsl enabled par défaut = false")
        fun `enabled default is false`() {
            val dsl = AugmentedContextDsl()
            assertThat(dsl.enabled).isFalse()
        }

        @Test
        @DisplayName("AugmentedContextDsl contextPath par défaut = build/bakery/composite-context.json")
        fun `default contextPath`() {
            val dsl = AugmentedContextDsl()
            assertThat(dsl.contextPath).isEqualTo("build/bakery/composite-context.json")
        }

        @Test
        @DisplayName("AugmentedContextDsl budget.maxArticlesPerPage par défaut = 4")
        fun `default budget maxArticlesPerPage is 4`() {
            val dsl = AugmentedContextDsl()
            assertThat(dsl.budget.maxArticlesPerPage).isEqualTo(4)
        }

        @Test
        @DisplayName("AugmentedContextDsl budget.minSimilarity par défaut = 0.7")
        fun `default budget minSimilarity is 0_7`() {
            val dsl = AugmentedContextDsl()
            assertThat(dsl.budget.minSimilarity).isEqualTo(0.7)
        }

        @Test
        @DisplayName("AugmentedContextDsl lens est une LensConfig")
        fun `lens is LensConfig`() {
            val dsl = AugmentedContextDsl()
            assertThat(dsl.lens).isInstanceOf(LensConfig::class.java)
            assertThat(dsl.lens.scope).isEqualTo(LensScope.SUBGRAPH)
        }
    }

    @Nested
    @DisplayName("AugmentedContextDsl — Modification via Action")
    inner class AugmentedContextDslMutation {

        @Test
        @DisplayName("AugmentedContextDsl lens peut être configuré via Action")
        fun `lens can be configured via Action`() {
            val dsl = AugmentedContextDsl()
            dsl.lens { lens ->
                lens.communities = listOf("bakery-gradle")
                lens.maxDepth = 3
                lens.scope = LensScope.FULL
            }
            assertThat(dsl.lens.communities).containsExactly("bakery-gradle")
            assertThat(dsl.lens.maxDepth).isEqualTo(3)
            assertThat(dsl.lens.scope).isEqualTo(LensScope.FULL)
        }

        @Test
        @DisplayName("AugmentedContextDsl enabled peut être activé")
        fun `enabled can be set to true`() {
            val dsl = AugmentedContextDsl()
            dsl.enabled = true
            dsl.budget.maxArticlesPerPage = 6
            assertThat(dsl.enabled).isTrue()
            assertThat(dsl.budget.maxArticlesPerPage).isEqualTo(6)
        }
    }

    @Nested
    @DisplayName("LensConfig — data class copy")
    inner class DataClassCopy {

        @Test
        @DisplayName("LensConfig copié avec modifications")
        fun `LensConfig can be copied with modifications`() {
            val original = LensConfig()
            val modified = original.copy(
                communities = listOf("bakery-gradle"),
                maxDepth = 5,
                scope = LensScope.SEMANTIC_ONLY
            )
            assertThat(modified.communities).containsExactly("bakery-gradle")
            assertThat(modified.maxDepth).isEqualTo(5)
            assertThat(modified.scope).isEqualTo(LensScope.SEMANTIC_ONLY)
            // Original unchanged
            assertThat(original.communities).isEmpty()
            assertThat(original.maxDepth).isEqualTo(2)
            assertThat(original.scope).isEqualTo(LensScope.SUBGRAPH)
        }
    }
}