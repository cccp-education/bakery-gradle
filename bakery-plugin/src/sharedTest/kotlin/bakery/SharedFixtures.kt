package bakery

import bakery.FuncTestsConstants.CONFIG_FILE
import bakery.FuncTestsConstants.DEPS
import bakery.FuncTestsConstants.GRADLE_BUILD_CONTENT
import bakery.FuncTestsConstants.GRADLE_DIR
import bakery.FuncTestsConstants.LIBS_VERSIONS_TOML_FILE
import bakery.FuncTestsConstants.SETTINGS_GRADLE_CONTENT
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import java.io.IOException
import kotlin.text.Charsets.UTF_8

/**
 * XB-FP-7 (S-322) — fixtures shared by the unit/BDD suite and the TestKit suite.
 *
 * Extracted from `src/functionalTest/.../Tools.kt`: both suites build a throwaway
 * project (`build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`,
 * `site.yml`), but the BDD shards needed only these helpers and yet compiled the
 * whole `functionalTest` source set (30 files / ~7 100 lines). One definition,
 * consumed by both source sets through the `sharedTest` source set.
 */
fun File.createBuildScriptFile() {
    resolve("build.gradle.kts").run {
        assertThat(exists())
            .describedAs("build.gradle.kts should not exists yet.")
            .isFalse
        assertThat(createNewFile())
            .describedAs("build.gradle.kts should be created.")
            .isTrue
        writeText(GRADLE_BUILD_CONTENT.trimIndent(), UTF_8)
    }
}

fun File.createSettingsFile() {
    resolve("settings.gradle.kts").run {
        assertThat(exists())
            .describedAs("settings.gradle.kts should not exists yet.")
            .isFalse
        assertThat(createNewFile())
            .describedAs("setting.gradle.kts should be created.")
            .isTrue
        writeText(SETTINGS_GRADLE_CONTENT, UTF_8)
        assertThat(exists())
            .describedAs("settings.gradle.kts should now exists.")
            .isTrue

        assertThat(readText(UTF_8))
            .describedAs("settings.gradle.kts should contains at least 'bakery-test'")
            .contains("bakery-test")
    }
}

fun File.createDependenciesFile() {
    val gradleFolder = resolve(GRADLE_DIR)
    val tomlFile = gradleFolder.resolve(LIBS_VERSIONS_TOML_FILE)
    if (!gradleFolder.exists()) gradleFolder.mkdirs()
    if (tomlFile.exists()) {
        if (!tomlFile.delete()) {
            throw IOException("Could not delete existing $LIBS_VERSIONS_TOML_FILE file.")
        }
    }
    if (!tomlFile.exists()) tomlFile.createNewFile()
    tomlFile.writeText(DEPS.trimIndent(), UTF_8)
}

fun File.createConfigFile() {
    // BKY-CI-ISOLATION (V9) — write the in-repo canonical config instead of
    // copying `../../site.yml`, a gitignored file absent from an isolated
    // checkout (and carrying real credentials). Single source shared with
    // BakeryPluginTest via FuncTestsConstants.SITE_CONFIG_YML.
    resolve(CONFIG_FILE).writeText(FuncTestsConstants.SITE_CONFIG_YML, UTF_8)
}
