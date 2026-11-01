package bakery

/**
 * XB-FP-7 (S-322) — plugin version used by the shared test fixtures.
 *
 * Lives in the `sharedTest` source set because both `test` (unit + BDD) and
 * `functionalTest` (TestKit) build a throwaway project whose `libs.versions.toml`
 * pins the plugin under test. `BakeryPluginFunctionalTests.Companion.VERSION`
 * delegates here, so there is a single source of truth.
 */
object TestVersions {
    const val PLUGIN_VERSION = "0.0.9"
}
