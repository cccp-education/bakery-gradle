package bakery

import bakery.workspace.PublishedCatalogVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.text.Charsets.UTF_8

class BakeryPluginPublicationTest {
    private val pluginDir = File(System.getProperty("user.dir")).absoluteFile

    @Test
    fun `plugin version matches root consumer catalog version`() {
        val buildScript = pluginDir.resolve("build.gradle.kts").readText(UTF_8)

        // MEM-CAT-3 (D3) — la version self est dérivée du catalog workspace publié :
        // `version = ws.versions.bakery.plugin.get()`. Plus de version littérale en double.
        // Whitespace-insensitive (S-244) : la garde ne doit pas casser quand ktlint
        // reformate la chaîne sur plusieurs lignes (`chain-method-continuation`).
        assertThat(buildScript.withoutWhitespace())
            .withFailMessage("build.gradle.kts version must use -Pversion (JitPack) with ws.versions.bakery.plugin fallback")
            .contains("version=versionProperty.getOrElse(catalogVersion)")
            .contains("ws.versions.bakery.plugin.get()")

        // Hygiène (D5) : la version self du toml local et la version self du catalog
        // ws doivent coïncider — le toml local reste pour le marker local, le ws
        // catalog est la source de vérité cross-borough.
        val pluginCatalogVersion = bakeryVersionFrom(pluginDir.resolve("gradle/libs.versions.toml").readText(UTF_8))
        val wsCatalogVersion = publishedCatalogVersion(BAKERY_VERSION_PROPERTY)

        assertThat(pluginCatalogVersion)
            .withFailMessage("plugin catalog bakery version ($pluginCatalogVersion) must match ws catalog bakery version ($wsCatalogVersion)")
            .isEqualTo(wsCatalogVersion)
    }

    /**
     * BKY-CI-ISOLATION (US-2c) — the published workspace catalog version is
     * *injected by Gradle* (`ws.versions.*`), never read from a neighbour
     * repository's working tree. The old fallback
     * (`../workspace-bom/gradle/libs.versions.toml`) is racy between sessions and
     * absent from an isolated checkout — same failure mode graphify-gradle's
     * D5-RACE EPIC fixed (S-029). A missing property is an explicit error, never
     * a silent green.
     */
    private fun publishedCatalogVersion(property: String): String = PublishedCatalogVersion.require(property)

    private fun bakeryVersionFrom(content: String): String =
        content
            .lineSequence()
            .map { it.substringBefore('#').trim() }
            .first { it.startsWith("bakery-plugin =") || it.startsWith("bakery =") }
            .substringAfter("\"")
            .substringBefore("\"")

    /**
     * MEM-CAT-3 (D4/D5) — every local `workspace-bom` platform pin must follow the
     * published catalog BOM version. A stale pin (0.0.48) is silently neutralised
     * by transitive resolution, so document-plugin resolution drifts without any
     * guard turning red. This guard makes the drift explicit at build time: all
     * platform pins are extracted and must equal the ws catalog version.
     */
    @Test
    fun `the workspace version is declared once and matches the published catalog`() {
        val buildScript = pluginDir.resolve("build.gradle.kts").readText(UTF_8)
        val settingsScript = pluginDir.resolve("settings.gradle.kts").readText(UTF_8)
        val properties = pluginDir.resolve("gradle.properties").readText(UTF_8)
        val wsBomVersion = publishedCatalogVersion(BOM_VERSION_PROPERTY)
        val platformBomVersion = publishedCatalogVersion(PLATFORM_BOM_VERSION_PROPERTY)

        // CNV-16 — two single sources in gradle.properties: the JitPack catalog
        // tag (must equal the catalog's workspace-bom version, prefix `v`) and the
        // Central BOM platform (the JitPack BOM POM is not a valid platform).
        // Neither settings nor build may carry a literal (the S-083 bug class).
        assertThat(properties).contains("workspaceCatalogVersion=v$wsBomVersion")
        assertThat(properties).contains("workspaceBomVersion=$platformBomVersion")
        assertThat(settingsScript).contains("workspace-catalog:\$workspaceCatalogVersion")
        assertThat(buildScript).contains("workspace-bom:\$workspaceBomVersion")

        val literals =
            Regex("""workspace-(?:bom|catalog):\d""")
                .findAll(settingsScript + buildScript)
                .map { it.value }
                .toList()
        assertThat(literals)
            .withFailMessage("no hardcoded workspace version may remain: $literals")
            .isEmpty()
    }

    @Test
    fun `plugin group and id are stable for publication`() {
        val buildScript = pluginDir.resolve("build.gradle.kts").readText(UTF_8)
        val pluginId =
            pluginDir
                .resolve("gradle/libs.versions.toml")
                .readText(UTF_8)
                .lineSequence()
                .filter { it.contains("id = \"education.cccp.bakery\"") }
                .first()
                .substringAfter("id = \"")
                .substringBefore("\"")

        assertThat(buildScript).contains("group = \"education.cccp\"")
        assertThat(pluginId).isEqualTo("education.cccp.bakery")
    }

    private fun String.withoutWhitespace(): String = filterNot { it.isWhitespace() }

    private companion object {
        const val BAKERY_VERSION_PROPERTY = "bakery.publishedCatalog.bakeryVersion"
        const val BOM_VERSION_PROPERTY = "bakery.publishedCatalog.bomVersion"
        const val PLATFORM_BOM_VERSION_PROPERTY = "bakery.publishedCatalog.platformBomVersion"
    }
}
