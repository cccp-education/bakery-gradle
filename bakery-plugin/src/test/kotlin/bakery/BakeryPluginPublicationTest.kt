package bakery

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.text.Charsets.UTF_8

class BakeryPluginPublicationTest {
    private val pluginDir = File(System.getProperty("user.dir")).absoluteFile

    private val rootDir =
        pluginDir.parentFile
            ?: throw IllegalStateException("Cannot resolve repo root from plugin dir")

    @Test
    fun `plugin version matches root consumer catalog version`() {
        val buildScript = pluginDir.resolve("build.gradle.kts").readText(UTF_8)
        val versionLine =
            buildScript
                .lineSequence()
                .first { it.trimStart().startsWith("version =") }

        // MEM-CAT-3 (D3) — la version self est dérivée du catalog workspace publié :
        // `version = ws.versions.bakery.plugin.get()`. Plus de version littérale en double.
        assertThat(versionLine)
            .withFailMessage("build.gradle.kts version must derive from the published workspace catalog (ws.versions.bakery.plugin)")
            .contains("ws.versions.bakery.plugin.get()")

        // Hygiène (D5) : la version self du toml local et la version self du catalog
        // ws doivent coïncider — le toml local reste pour le marker local, le ws
        // catalog (0.0.29) est la source de vérité cross-borough.
        val pluginCatalogVersion = bakeryVersionFrom(pluginDir.resolve("gradle/libs.versions.toml").readText(UTF_8))
        val wsCatalogVersion = bakeryVersionFrom(wsCatalogToml())

        assertThat(pluginCatalogVersion)
            .withFailMessage("plugin catalog bakery version ($pluginCatalogVersion) must match ws catalog bakery version ($wsCatalogVersion)")
            .isEqualTo(wsCatalogVersion)
    }

    /**
     * Lit le toml du catalog `ws` résolu par Gradle (cache modules) et extrait
     * la version `bakery-plugin`. Fallback : parse du toml du repo MEMPHIS local
     * (workspace-bom) — même fichier source du catalogue.
     */
    private fun wsCatalogToml(): String {
        val wsRepoToml = rootDir.parentFile
            ?.resolve("workspace-bom/gradle/libs.versions.toml")
        if (wsRepoToml != null && wsRepoToml.exists()) return wsRepoToml.readText(UTF_8)
        error("ws catalog toml introuvable — résolution ws impossible pour l'hygiène")
    }

    private fun bakeryVersionFrom(content: String): String =
        content
            .lineSequence()
            .map { it.substringBefore('#').trim() }
            .first { it.startsWith("bakery-plugin =") || it.startsWith("bakery =") }
            .substringAfter("\"")
            .substringBefore("\"")

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
}
