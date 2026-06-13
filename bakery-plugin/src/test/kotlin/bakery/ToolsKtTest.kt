package bakery

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

class ToolsKtTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `createBuildScriptFile creates build gradle kts`() {
        tempDir.createBuildScriptFile()

        val buildFile = tempDir.resolve("build.gradle.kts")
        assertThat(buildFile).exists().isFile
        assertThat(buildFile.readText(UTF_8)).contains("alias(libs.plugins.bakery)")
    }

    @Test
    fun `createSettingsFile creates settings gradle kts`() {
        tempDir.createSettingsFile()

        val settingsFile = tempDir.resolve("settings.gradle.kts")
        assertThat(settingsFile).exists().isFile
        assertThat(settingsFile.readText(UTF_8)).contains("bakery-test")
    }

    @Test
    fun `createDependenciesFile creates gradle libs versions toml`() {
        tempDir.createDependenciesFile()

        val tomlFile = tempDir.resolve("gradle").resolve("libs.versions.toml")
        assertThat(tomlFile).exists().isFile
        assertThat(tomlFile.readText(UTF_8)).contains("[versions]")
        assertThat(tomlFile.readText(UTF_8)).contains("[plugins]")
    }

    @Test
    fun `createDependenciesFile overwrites existing toml`() {
        val gradleDir = tempDir.resolve("gradle")
        gradleDir.mkdirs()
        val tomlFile = gradleDir.resolve("libs.versions.toml")
        tomlFile.writeText("old content", UTF_8)

        tempDir.createDependenciesFile()

        assertThat(tomlFile.readText(UTF_8)).contains("education.cccp.bakery")
        assertThat(tomlFile.readText(UTF_8)).doesNotContain("old content")
    }

    @Test
    fun `FuncTestsConstants has expected values`() {
        assertThat(FuncTestsConstants.BAKERY_GROUP).isEqualTo("bakery")
        assertThat(FuncTestsConstants.BAKE_TASK).isEqualTo("bake")
        assertThat(FuncTestsConstants.CONFIG_FILE).isEqualTo("site.yml")
        assertThat(FuncTestsConstants.BUILD_FILE).isEqualTo("build.gradle.kts")
        assertThat(FuncTestsConstants.SETTINGS_FILE).isEqualTo("settings.gradle.kts")
        assertThat(FuncTestsConstants.GRADLE_DIR).isEqualTo("gradle")
        assertThat(FuncTestsConstants.LIBS_VERSIONS_TOML_FILE).isEqualTo("libs.versions.toml")
    }

    @Test
    fun `FuncTestsConstants buildScriptListOfStringContained contains bakery plugin`() {
        assertThat(FuncTestsConstants.buildScriptListOfStringContained)
            .contains("alias(libs.plugins.bakery)")
            .contains("bakery { configPath = file(\"site.yml\").absolutePath }")
    }

    @Test
    fun `FuncTestsConstants settingsListOfStringContained contains rootProject`() {
        assertThat(FuncTestsConstants.settingsListOfStringContained)
            .contains("rootProject.name")
            .contains("bakery-test")
    }

    @Test
    fun `FuncTestsConstants tomlListOfStringContained has four sections`() {
        assertThat(FuncTestsConstants.tomlListOfStringContained)
            .contains("[versions]", "[libraries]", "[plugins]", "[bundles]")
    }

    @Test
    fun `FuncTestsConstants configListOfStringContained contains bake keys`() {
        assertThat(FuncTestsConstants.configListOfStringContained)
            .contains("bake:", "srcPath:", "destDirPath:")
    }
}
