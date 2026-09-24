package bakery.langswitch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.text.Charsets.UTF_8

/**
 * BKY-LANG-NAV-3 — the client-host parity guard (D6).
 *
 * Parity here means *presence* **and** the behaviour surface of the module: the
 * JS host must exist in both trees (`maquette/js/` and the publication tree) and
 * the two copies must be byte-identical. A `setLang`-only module without the
 * shared resolver would be a false green, so the contract surface
 * (`resolveLangPath` + `attachLangSwitch`) is asserted too. The *behaviour* of
 * the resolver itself is replayed by the node test against the same shared
 * vectors (`lang-switch-path-vectors.json`).
 */
class LangSwitchClientParityTest {
    private val compliantModule =
        """
        function resolveLangPath(a, b, c, d, e) { return 'x'; }
        function attachLangSwitch(config) { return config; }
        """.trimIndent()

    @Nested
    inner class Pure {
        @Test
        fun `a module absent from the maquette tree is a violation`() {
            val report = LangSwitchClientParity.verify(null, compliantModule)

            assertThat(report.isSuccess).isFalse()
            assertThat(report.violations.map { it.kind })
                .containsExactly(LangSwitchClientParity.MISSING_MODULE)
        }

        @Test
        fun `a module absent from the publication tree is a violation`() {
            val report = LangSwitchClientParity.verify(compliantModule, null)

            assertThat(report.isSuccess).isFalse()
            assertThat(report.violations.map { it.kind })
                .containsExactly(LangSwitchClientParity.MISSING_MODULE)
        }

        @Test
        fun `byte-different copies are a drift`() {
            val report = LangSwitchClientParity.verify(compliantModule, "$compliantModule\n")

            assertThat(report.isSuccess).isFalse()
            assertThat(report.violations.map { it.kind })
                .containsExactly(LangSwitchClientParity.DRIFTED_MODULE)
        }

        @Test
        fun `a module that drops the shared resolver is a violation`() {
            val noResolver = "function attachLangSwitch(config) { return config; }"

            val report = LangSwitchClientParity.verify(noResolver, noResolver)

            assertThat(report.isSuccess).isFalse()
            assertThat(report.violations.map { it.kind })
                .containsExactly(LangSwitchClientParity.MISSING_RESOLVER)
        }

        @Test
        fun `a module that drops the DOM adapter is a violation`() {
            val noAdapter = "function resolveLangPath(a, b, c, d, e) { return 'x'; }"

            val report = LangSwitchClientParity.verify(noAdapter, noAdapter)

            assertThat(report.isSuccess).isFalse()
            assertThat(report.violations.map { it.kind })
                .containsExactly(LangSwitchClientParity.MISSING_ADAPTER)
        }

        @Test
        fun `byte-identical copies exposing the contract surface pass`() {
            val report = LangSwitchClientParity.verify(compliantModule, compliantModule)

            assertThat(report.isSuccess).isTrue()
            assertThat(report.violations).isEmpty()
        }
    }

    @Nested
    inner class ShippedResources {
        /**
         * The real guard: the module shipped in the plugin jar must be present in
         * both resource trees and byte-identical. This is what a scaffolded site
         * receives — `generateSite` copies both trees verbatim.
         */
        @Test
        fun `the shipped maquette and publication modules are present and byte-identical`() {
            val report =
                LangSwitchClientParity.verify(
                    resource("maquette/js/${LangSwitchClientParity.MODULE_NAME}"),
                    resource("site/assets/js/${LangSwitchClientParity.MODULE_NAME}"),
                )

            assertThat(report.violations)
                .describedAs("client-host parity violations")
                .isEmpty()
        }

        private fun resource(path: String): String? = javaClass.classLoader.getResourceAsStream(path)?.use { it.readBytes().toString(UTF_8) }
    }
}
