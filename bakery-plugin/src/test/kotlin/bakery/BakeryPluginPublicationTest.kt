package bakery

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.text.Charsets.UTF_8

class BakeryPluginPublicationTest {

    private val pluginDir = File(System.getProperty("user.dir")).absoluteFile

    private val rootDir = pluginDir.parentFile
        ?: throw IllegalStateException("Cannot resolve repo root from plugin dir")

    @Test
    fun `plugin version matches root consumer catalog version`() {
        val pluginVersion = pluginDir.resolve("build.gradle.kts").readText(UTF_8)
            .lineSequence()
            .first { it.trimStart().startsWith("version =") }
            .substringAfter("\"").substringBefore("\"")

        val rootCatalogVersion = rootDir.resolve("gradle/libs.versions.toml").readText(UTF_8)
            .lineSequence()
            .first { it.trimStart().startsWith("bakery =") }
            .substringAfter("\"").substringBefore("\"")

        val pluginCatalogVersion = pluginDir.resolve("gradle/libs.versions.toml").readText(UTF_8)
            .lineSequence()
            .first { it.trimStart().startsWith("bakery =") }
            .substringAfter("\"").substringBefore("\"")

        assertThat(pluginVersion)
            .withFailMessage("build.gradle.kts version ($pluginVersion) must match root catalog ($rootCatalogVersion)")
            .isEqualTo(rootCatalogVersion)

        assertThat(pluginCatalogVersion)
            .withFailMessage("plugin catalog version ($pluginCatalogVersion) must match plugin build.gradle.kts ($pluginVersion)")
            .isEqualTo(pluginVersion)
    }

    @Test
    fun `plugin group and id are stable for publication`() {
        val buildScript = pluginDir.resolve("build.gradle.kts").readText(UTF_8)
        val pluginId = pluginDir.resolve("gradle/libs.versions.toml").readText(UTF_8)
            .lineSequence()
            .filter { it.contains("id = \"education.cccp.bakery\"") }
            .first()
            .substringAfter("id = \"").substringBefore("\"")

        assertThat(buildScript).contains("group = \"education.cccp\"")
        assertThat(pluginId).isEqualTo("education.cccp.bakery")
    }
}
