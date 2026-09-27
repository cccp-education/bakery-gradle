package bakery

import bakery.BakeryPluginFunctionalTests.Companion.VERSION
import bakery.FuncTestsConstants.CONFIG_FILE
import bakery.FuncTestsConstants.DEPS
import bakery.FuncTestsConstants.GRADLE_BUILD_CONTENT
import bakery.FuncTestsConstants.GRADLE_DIR
import bakery.FuncTestsConstants.LIBS_VERSIONS_TOML_FILE
import bakery.FuncTestsConstants.SETTINGS_GRADLE_CONTENT
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import java.io.File.separator
import java.io.IOException
import kotlin.text.Charsets.UTF_8

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

object FuncTestsConstants {
    const val BAKERY_GROUP = "bakery"
    const val BAKE_TASK = "bake"
    const val CNAME = "CNAME"
    const val CONFIG_FILE = "site.yml"
    const val GRADLE_PROPERTIES_FILE = "site.yml"
    const val BUILD_FILE = "build.gradle.kts"
    const val SETTINGS_FILE = "settings.gradle.kts"
    const val GRADLE_DIR = "gradle"
    const val LIBS_VERSIONS_TOML_FILE = "libs.versions.toml"
    val LIBS_FILE = "$GRADLE_DIR$separator$LIBS_VERSIONS_TOML_FILE"
    val PATH_GAP = "..$separator..$separator"
    val CONFIG_PATH = "${PATH_GAP}$CONFIG_FILE"
    val BUILD_FILE_PATH = "${PATH_GAP}$BUILD_FILE"
    val SETTINGS_FILE_PATH = "${PATH_GAP}$SETTINGS_FILE"
    val LIBS_VERSIONS_TOML_FILE_PATH = "${PATH_GAP}$GRADLE_DIR${separator}$LIBS_VERSIONS_TOML_FILE"
    const val GRADLE_BUILD_CONTENT = """
plugins { alias(libs.plugins.bakery) }

bakery { configPath = file("$CONFIG_FILE").absolutePath }
            """
    const val SETTINGS_GRADLE_CONTENT = """
            pluginManagement {
                repositories { gradlePluginPortal() }
            }

            rootProject.name = "bakery-test"
        """
    const val DEPS = """
            [versions]
            bakery = "$VERSION"
     
            [libraries]

            [plugins]
            bakery = { id = "education.cccp.bakery", version.ref = "bakery" }

            [bundles]
        """

    /**
     * BKY-CI-ISOLATION (V9) — canonical in-repo test configuration.
     *
     * Replaces the former copy of the gitignored `../../site.yml` (absent from an
     * isolated checkout, and carrying real credentials). The values satisfy the
     * `BakeryPluginTest.SiteConfigurationParsingTest` contract without leaking any
     * secret: username/password are the documented 8/40-char placeholders.
     */
    const val SITE_CONFIG_YML = """
bake:
  srcPath: "site"
  destDirPath: "bake"
  cname: "bakery"
pushPage:
  from: "bake"
  to: "cvs"
  repo:
    name: "thymeleaf.cheroliv.com"
    repository: "https://github.com/pages-content/bakery.git"
    credentials:
      username: "username"
      password: "passwordpasswordpasswordpasswordpassword"
  branch: "main"
  message: "thymeleaf.cheroliv.com"
pushMaquette:
  from: "maquette"
  to: "cvs"
  repo:
    name: "cheroliv-maquette"
    repository: "https://github.com/pages-content/cheroliv-maquette.git"
    credentials:
      username: "username"
      password: "passwordpasswordpasswordpasswordpassword"
  branch: "main"
  message: "cheroliv-maquette"
firebase:
  project:
    projectId: "bakery-contact-form"
    apiKey: "REMPLACER_PAR_VOTRE_API_KEY"
  firestore:
    contacts:
      name: contacts
      fields:
        - name: id
          type: string
        - name: created_at
          type: timestamp
        - name: name
          type: string
        - name: email
          type: string
        - name: phone
          type: string
      rulesEnabled: true
    messages:
      name: messages
      fields:
        - name: id
          type: string
        - name: created_at
          type: timestamp
        - name: contact_id
          type: string
        - name: subject
          type: string
        - name: message
          type: string
  callable:
    name: handleContactForm
    params:
      - name: p_name
        type: string
      - name: p_email
        type: string
      - name: p_subject
        type: string
      - name: p_message
        type: string
    """

    val buildScriptListOfStringContained =
        listOf(
            """alias(libs.plugins.bakery)""".trimIndent(),
            """bakery { configPath = file("$CONFIG_FILE").absolutePath }""".trimIndent(),
        )
    val settingsListOfStringContained =
        listOf(
            "pluginManagement",
            "repositories",
            "gradlePluginPortal()",
            "rootProject.name",
            "bakery-test",
        )
    val tomlListOfStringContained =
        listOf(
            "[versions]",
            "[libraries]",
            "[plugins]",
            "[bundles]",
        )
    val configListOfStringContained =
        listOf(
            "bake:",
            "srcPath:",
            "destDirPath:",
            "pushPage:",
            "from:",
            "to:",
            "repo:",
            "name:",
            "repository:",
            "credentials:",
            "username:",
            "password:",
            "branch:",
            "message:",
            "pushMaquette:",
            "firebase:",
            "project:",
            "projectId:",
            "apiKey:",
            "firestore:",
            "type:",
            "contacts:",
            "name:",
            "contacts",
            "fields:",
            "string",
            "timestamp",
            "id",
            "created_at",
            "name",
            "email",
            "phone",
            "rulesEnabled: true",
            "messages:",
            "messages",
            "callable:",
            "name:",
            "params:",
            "contact_id",
            "subject",
            "message",
            "handleContactForm",
            "p_name",
            "p_email",
            "p_subject",
            "p_message",
        )
    const val JBAKE_TASK_SEQUENCE = """
Detailed task information for bake

Path
     :bake

Type
     JBakeTask (org.jbake.gradle.JBakeTask)

Options
     --rerun     Causes the task to be re-run even if up-to-date.

Description
     Bake a jbake project

Group
     Documentation"""
}
