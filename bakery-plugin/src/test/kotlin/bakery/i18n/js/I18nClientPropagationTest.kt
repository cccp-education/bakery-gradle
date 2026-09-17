package bakery.i18n.js

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-4.
 *
 * Unit tests for [I18nClientPropagation], the pure publication-copy planner
 * (development source `maquette/js/` → site copy `jbake/assets/js/`, decision
 * S-039). Reference behaviour: the `propagate()` function of the talaria node
 * harness `tools/translate-i18n.mjs` (S-044).
 */
class I18nClientPropagationTest {
    @Nested
    inner class TargetFor {
        @Test
        fun `maps a maquette dictionary to its jbake publication path`() {
            assertThat(I18nClientPropagation.targetFor("maquette/js/i18n.js"))
                .isEqualTo("jbake/assets/js/i18n.js")
        }

        @Test
        fun `replaces the first maquette segment of a deeper path`() {
            assertThat(I18nClientPropagation.targetFor("office/sites/talaria.school/maquette/js/i18n-content.js"))
                .isEqualTo("office/sites/talaria.school/jbake/assets/js/i18n-content.js")
        }

        @Test
        fun `is null outside a maquette source tree`() {
            assertThat(I18nClientPropagation.targetFor("src/js/i18n.js")).isNull()
        }

        @Test
        fun `a bare maquette token without a path separator is not a source tree`() {
            assertThat(I18nClientPropagation.targetFor("maquette")).isNull()
        }
    }

    @Nested
    inner class Pairs {
        @Test
        fun `pairs every propagable source in order and skips the others`() {
            val pairs =
                I18nClientPropagation.pairs(
                    listOf(
                        "maquette/js/i18n.js",
                        "src/js/local.js",
                        "maquette/js/i18n-content.js",
                    ),
                )

            assertThat(pairs)
                .containsExactly(
                    java.util.Map.entry("maquette/js/i18n.js", "jbake/assets/js/i18n.js"),
                    java.util.Map.entry("maquette/js/i18n-content.js", "jbake/assets/js/i18n-content.js"),
                )
        }

        @Test
        fun `pairs is empty when no source lives under maquette`() {
            assertThat(I18nClientPropagation.pairs(listOf("src/js/i18n.js"))).isEmpty()
        }
    }

    @Nested
    inner class Drift {
        @Test
        fun `a drifted publication copy is reported`() {
            val drift =
                I18nClientPropagation.drift(
                    sources = mapOf("maquette/js/i18n.js" to "var DICT = {};"),
                    targets = mapOf("jbake/assets/js/i18n.js" to "var DICT = { outdated };"),
                )

            assertThat(drift).containsExactly("maquette/js/i18n.js")
        }

        @Test
        fun `a missing publication copy is reported`() {
            val drift =
                I18nClientPropagation.drift(
                    sources = mapOf("maquette/js/i18n.js" to "var DICT = {};"),
                    targets = emptyMap(),
                )

            assertThat(drift).containsExactly("maquette/js/i18n.js")
        }

        @Test
        fun `byte-identical copies report no drift`() {
            val content = "var DICT = {};"
            val drift =
                I18nClientPropagation.drift(
                    sources = mapOf("maquette/js/i18n.js" to content),
                    targets = mapOf("jbake/assets/js/i18n.js" to content),
                )

            assertThat(drift).isEmpty()
        }

        @Test
        fun `non-propagable sources are never drift`() {
            val drift =
                I18nClientPropagation.drift(
                    sources = mapOf("src/js/i18n.js" to "a"),
                    targets = emptyMap(),
                )

            assertThat(drift).isEmpty()
        }
    }
}
