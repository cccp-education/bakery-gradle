package bakery.lens

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * BKY-LENS-2.2 — Tests unitaires pour LensRules.
 *
 * Valide les valeurs par défaut, la mutabilité, et le comportement des règles
 * éditoriales (excludeTags, etc.).
 *
 * Méthodologie : DDD/TDD baby steps — chaque test compile ET passe AVANT de passer au suivant.
 */
class LensRulesTest {

    @Nested
    @DisplayName("LensRules — Valeurs par défaut")
    inner class Defaults {

        @Test
        @DisplayName("excludeTags par défaut = [wip, draft]")
        fun `default excludeTags`() {
            val rules = LensRules()
            assertThat(rules.excludeTags).containsExactly("wip", "draft")
        }

        @Test
        @DisplayName("prioritizeCrossReferences par défaut = true")
        fun `default prioritizeCrossReferences is true`() {
            val rules = LensRules()
            assertThat(rules.prioritizeCrossReferences).isTrue()
        }

        @Test
        @DisplayName("crossRefBonus par défaut = 0.2")
        fun `default crossRefBonus is 0_2`() {
            val rules = LensRules()
            assertThat(rules.crossRefBonus).isEqualTo(0.2)
        }

        @Test
        @DisplayName("communityAffinity par défaut = 0.3")
        fun `default communityAffinity is 0_3`() {
            val rules = LensRules()
            assertThat(rules.communityAffinity).isEqualTo(0.3)
        }
    }

    @Nested
    @DisplayName("LensRules — Modification des propriétés")
    inner class Mutation {

        @Test
        @DisplayName("excludeTags peut être modifié")
        fun `excludeTags can be changed`() {
            val rules = LensRules()
            rules.excludeTags = listOf("secret", "private")
            assertThat(rules.excludeTags).containsExactly("secret", "private")
        }

        @Test
        @DisplayName("prioritizeCrossReferences peut être désactivé")
        fun `prioritizeCrossReferences can be set to false`() {
            val rules = LensRules()
            rules.prioritizeCrossReferences = false
            assertThat(rules.prioritizeCrossReferences).isFalse()
        }

        @Test
        @DisplayName("crossRefBonus peut être changé")
        fun `crossRefBonus can be changed`() {
            val rules = LensRules()
            rules.crossRefBonus = 0.5
            assertThat(rules.crossRefBonus).isEqualTo(0.5)
        }

        @Test
        @DisplayName("communityAffinity peut être changé")
        fun `communityAffinity can be changed`() {
            val rules = LensRules()
            rules.communityAffinity = 0.8
            assertThat(rules.communityAffinity).isEqualTo(0.8)
        }
    }

    @Nested
    @DisplayName("LensRules — data class copy")
    inner class DataClassCopy {

        @Test
        @DisplayName("LensRules copié avec modifications")
        fun `LensRules can be copied with modifications`() {
            val original = LensRules()
            val modified = original.copy(
                excludeTags = listOf("deprecated"),
                prioritizeCrossReferences = false,
                crossRefBonus = 0.1,
                communityAffinity = 0.0
            )
            assertThat(modified.excludeTags).containsExactly("deprecated")
            assertThat(modified.prioritizeCrossReferences).isFalse()
            assertThat(modified.crossRefBonus).isEqualTo(0.1)
            assertThat(modified.communityAffinity).isEqualTo(0.0)
            // Original unchanged
            assertThat(original.excludeTags).containsExactly("wip", "draft")
            assertThat(original.prioritizeCrossReferences).isTrue()
            assertThat(original.crossRefBonus).isEqualTo(0.2)
            assertThat(original.communityAffinity).isEqualTo(0.3)
        }
    }

    @Nested
    @DisplayName("LensRules — Filtre sémantique (excludeTags)")
    inner class TagFiltering {

        @Test
        @DisplayName("excludeTags contient 'wip' et 'draft' par défaut")
        fun `default excludeTags blocks wip and draft`() {
            val rules = LensRules()
            assertThat(rules.excludeTags).contains("wip")
            assertThat(rules.excludeTags).contains("draft")
        }

        @Test
        @DisplayName("Un tag dans excludeTags est reconnu comme interdit")
        fun `tag in excludeTags is recognized as forbidden`() {
            val rules = LensRules()
            val forbiddenTags = rules.excludeTags.map { it.lowercase() }.toSet()
            assertThat("wip".lowercase() in forbiddenTags).isTrue()
            assertThat("draft".lowercase() in forbiddenTags).isTrue()
            assertThat("published".lowercase() in forbiddenTags).isFalse()
        }
    }
}
