package bakery

import java.io.File.separator

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
            bakery = "${TestVersions.PLUGIN_VERSION}"
     
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
