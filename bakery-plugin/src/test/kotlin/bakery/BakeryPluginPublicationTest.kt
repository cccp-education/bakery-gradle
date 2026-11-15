package bakery

import bakery.workspace.PublishedCatalogVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.text.Charsets.UTF_8

/**
 * GBL-014 (S-279) — publication-channel invariant: one version source, two
 * projections. The channel is a build property (`-Pchannel`, default `jitpack`),
 * never a branch, and this guard turns any drift red instead of silent:
 *
 * 1. `gradle.properties` is the single source of versions — `workspaceCatalogVersion`
 *    (the served catalogue) and `workspaceBomVersion` (the Central BOM platform),
 *    both non-blank semver. The catalogue pin carries **no `v` prefix**: JitPack
 *    relabels the tag `vX.Y.Z` as version `X.Y.Z` (`workspace-catalog:0.0.136` is
 *    served, `workspace-catalog:v0.0.136` is not).
 * 2. `settings.gradle.kts` builds the `ws` catalogue from `$workspaceCatalogVersion`
 *    and chooses the coordinate **by channel**: the JitPack-relabelled
 *    `com.github.cccp-education.workspace-bom:workspace-catalog` on `jitpack`,
 *    `education.cccp:workspace-catalog` on `central`.
 * 3. `build.gradle.kts` imports `platform(...workspace-bom:$workspaceBomVersion)`
 *    **only** inside an `if (centralChannel)` block: the JitPack BOM POM is not a
 *    platform (the relabel loses `dependencyManagement` and the module metadata).
 *
 * The catalogue pin must equal the `workspace-bom` version the served catalogue
 * declares: that is what proves it points at the relabelled projection and not at
 * a stale Central-flavoured catalogue.
 */
class BakeryPluginPublicationTest {
    private val pluginDir = File(System.getProperty("user.dir")).absoluteFile

    @Test
    fun `the two workspace versions are declared once in gradle properties`() {
        val properties = pluginFile(PROPERTIES_FILE)

        val catalogPin = propertyValue(properties, CATALOG_VERSION_KEY)
        val bomPin = propertyValue(properties, BOM_VERSION_KEY)

        assertThat(catalogPin)
            .withFailMessage("workspaceCatalogVersion must be a semver, got '$catalogPin'")
            .matches(SEMVER_PATTERN)
        assertThat(bomPin)
            .withFailMessage("workspaceBomVersion must be a semver, got '$bomPin'")
            .matches(SEMVER_PATTERN)

        // JitPack serves the tag `vX.Y.Z` as `X.Y.Z`: a `v` here points at a 404.
        assertThat(catalogPin)
            .withFailMessage("workspaceCatalogVersion must not carry the tag-only 'v' prefix (JitPack serves 'X.Y.Z')")
            .doesNotStartWith("v")

        assertThat(declarations(properties, CATALOG_VERSION_KEY)).isEqualTo(1)
        assertThat(declarations(properties, BOM_VERSION_KEY)).isEqualTo(1)
    }

    @Test
    fun `the workspace catalogue is projected by channel from the single pin`() {
        val settingsScript = pluginFile(SETTINGS_FILE)
        val buildScript = pluginFile(BUILD_FILE)
        val catalogPin = propertyValue(pluginFile(PROPERTIES_FILE), CATALOG_VERSION_KEY)
        val settings = settingsScript.withoutWhitespace()

        assertThat(settings)
            .withFailMessage("settings must branch on the publication channel")
            .contains("publicationChannel==\"central\"")
        assertThat(settings)
            .withFailMessage("the jitpack channel must read the relabelled catalogue at \$workspaceCatalogVersion")
            .contains("com.github.cccp-education.workspace-bom:workspace-catalog:${'$'}workspaceCatalogVersion")
        assertThat(settings)
            .withFailMessage("the central channel must read education.cccp:workspace-catalog at \$workspaceCatalogVersion")
            .contains("education.cccp:workspace-catalog:${'$'}workspaceCatalogVersion")

        // A hardcoded workspace version is the S-083 bug class: the pin lives in
        // gradle.properties, neither settings nor build may duplicate it.
        val literals =
            Regex("""workspace-(?:bom|catalog):\d""")
                .findAll(settingsScript + buildScript)
                .map { it.value }
                .toList()
        assertThat(literals)
            .withFailMessage("no hardcoded workspace version may remain: $literals")
            .isEmpty()

        // The pin must serve a catalogue whose declared workspace-bom equals the
        // pin: `0.0.136` (relabelled projection) or `v0.0.133` (stale Central
        // flavour) diverge here, so a stale pointer is a red build.
        val declaredBomVersion = PublishedCatalogVersion.require(BOM_VERSION_PROPERTY)
        assertThat(catalogPin)
            .withFailMessage(
                "workspaceCatalogVersion ($catalogPin) must equal the workspace-bom version " +
                    "declared by the served catalogue ($declaredBomVersion)",
            ).isEqualTo(declaredBomVersion)
    }

    @Test
    fun `the workspace-bom platform is imported only on the central channel`() {
        val buildScript = pluginFile(BUILD_FILE)
        val bomPin = PublishedCatalogVersion.require(PLATFORM_BOM_VERSION_PROPERTY)

        assertThat(propertyValue(pluginFile(PROPERTIES_FILE), BOM_VERSION_KEY)).isEqualTo(bomPin)

        val platformFragment = "workspace-bom:${'$'}workspaceBomVersion"
        val occurrences = buildScript.indicesOf(platformFragment)
        assertThat(occurrences)
            .withFailMessage(
                "expected at least one platform import of `$platformFragment` " +
                    "(the central channel needs the BOM platform)",
            ).isNotEmpty()

        val guarded = buildScript.channelGuardedRanges()
        assertThat(guarded)
            .withFailMessage("expected `if (centralChannel)` blocks in build.gradle.kts")
            .isNotEmpty()

        val outside = occurrences.filter { index -> guarded.none { it.contains(index) } }
        assertThat(outside)
            .withFailMessage(
                "the workspace-bom platform must be imported only inside `if (centralChannel)` " +
                    "blocks (the JitPack BOM POM is not a platform); stray occurrence(s) at $outside",
            ).isEmpty()
    }

    @Test
    fun `plugin version matches root consumer catalog version`() {
        val buildScript = pluginFile(BUILD_FILE)

        // MEM-CAT-3 (D3) — the self version is derived from the published workspace
        // catalogue: `version = ws.versions.bakery.plugin.get()`. No duplicated literal.
        // Whitespace-insensitive (S-244): the guard must survive ktlint reformatting
        // the chain over several lines (`chain-method-continuation`).
        assertThat(buildScript.withoutWhitespace())
            .withFailMessage("build.gradle.kts version must use -Pversion (JitPack) with ws.versions.bakery.plugin fallback")
            .contains("version=versionProperty.getOrElse(catalogVersion)")
            .contains("ws.versions.bakery.plugin.get()")

        // Hygiene (D5): the local toml self version and the published ws catalogue
        // self version must agree — the ws catalogue is the cross-borough source of truth.
        val pluginCatalogVersion = bakeryVersionFrom(pluginFile(LOCAL_CATALOG_FILE))
        val wsCatalogVersion = PublishedCatalogVersion.require(BAKERY_VERSION_PROPERTY)

        assertThat(pluginCatalogVersion)
            .withFailMessage("plugin catalog bakery version ($pluginCatalogVersion) must match ws catalog bakery version ($wsCatalogVersion)")
            .isEqualTo(wsCatalogVersion)
    }

    @Test
    fun `plugin group and id are stable for publication`() {
        val buildScript = pluginFile(BUILD_FILE)
        val pluginId =
            pluginFile(LOCAL_CATALOG_FILE)
                .lineSequence()
                .filter { it.contains("id = \"education.cccp.bakery\"") }
                .first()
                .substringAfter("id = \"")
                .substringBefore("\"")

        assertThat(buildScript).contains("group = \"education.cccp\"")
        assertThat(pluginId).isEqualTo("education.cccp.bakery")
    }

    private fun pluginFile(name: String): String = pluginDir.resolve(name).readText(UTF_8)

    private fun propertyValue(properties: String, key: String): String =
        properties
            .lineSequence()
            .map { it.substringBefore('#').trim() }
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter('=')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: error("'$key' is missing from gradle.properties")

    private fun declarations(properties: String, key: String): Int =
        properties
            .lineSequence()
            .map { it.substringBefore('#').trim() }
            .count { it.startsWith("$key=") }

    private fun bakeryVersionFrom(content: String): String =
        content
            .lineSequence()
            .map { it.substringBefore('#').trim() }
            .first { it.startsWith("bakery-plugin =") || it.startsWith("bakery =") }
            .substringAfter("\"")
            .substringBefore("\"")

    /**
     * Ranges of every `if (centralChannel) { ... }` body. The BOM platform may only
     * live inside one of them, so the `central`-only invariant is checked on the
     * real structure instead of a fragile line adjacency.
     */
    private fun String.channelGuardedRanges(): List<IntRange> {
        val marker = "if (centralChannel)"
        val ranges = mutableListOf<IntRange>()
        var from = 0
        while (true) {
            val start = indexOf(marker, from)
            if (start < 0) break
            val open = indexOf('{', start)
            require(open >= 0) { "missing `{` after `$marker`" }
            var depth = 0
            var index = open
            while (index < length) {
                when (this[index]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) break
                    }
                }
                index++
            }
            require(depth == 0) { "unbalanced braces after `$marker`" }
            ranges += (open + 1) until index
            from = index + 1
        }
        return ranges
    }

    private fun String.indicesOf(fragment: String): List<Int> {
        val indices = mutableListOf<Int>()
        var from = 0
        while (true) {
            val index = indexOf(fragment, from)
            if (index < 0) break
            indices += index
            from = index + 1
        }
        return indices
    }

    private fun String.withoutWhitespace(): String = filterNot { it.isWhitespace() }

    private companion object {
        const val PROPERTIES_FILE = "gradle.properties"
        const val SETTINGS_FILE = "settings.gradle.kts"
        const val BUILD_FILE = "build.gradle.kts"
        const val LOCAL_CATALOG_FILE = "gradle/libs.versions.toml"
        const val CATALOG_VERSION_KEY = "workspaceCatalogVersion"
        const val BOM_VERSION_KEY = "workspaceBomVersion"
        const val BAKERY_VERSION_PROPERTY = "bakery.publishedCatalog.bakeryVersion"
        const val BOM_VERSION_PROPERTY = "bakery.publishedCatalog.bomVersion"
        const val PLATFORM_BOM_VERSION_PROPERTY = "bakery.publishedCatalog.platformBomVersion"
        const val SEMVER_PATTERN = """^\d+\.\d+\.\d+$"""
    }
}
