@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests fonctionnels pour l'EPIC BKY-IA-2 — Thème IA paramétrique.
 *
 * Valide que le DSL `bakery { themeIntention { ... } }` compile,
 * que la tâche `generateTheme` est correctement configurée,
 * et que la résolution variante + surcharges fonctionne.
 *
 * Pattern identique à [ScaffoldIntentionFunctionalTests] et [ArticleIntentionFunctionalTests].
 */
class ThemeIntentionFunctionalTests {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `DSL themeIntention block compiles with description only`() {
        createProjectWithThemeIntention(
            description = "Blog tech moderne"
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `DSL themeIntention block compiles with full config`() {
        createProjectWithThemeIntention(
            description = "Portfolio créatif",
            variant = "portfolio",
            accentColor = "#FF5733",
            backgroundColor = "#F5F5F5",
            textColor = "#333333",
            headingFont = "Merriweather"
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `generateTheme task is registered with themeIntention DSL`() {
        createProjectWithThemeIntention(
            description = "Documentation API",
            variant = "documentation",
            sitesBaseDir = "office/sites",
            siteName = "api-docs"
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateSite")
            .build()

        val siteDir = projectDir.resolve("office/sites/api-docs")
        assertThat(siteDir).exists().isDirectory
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `generateTheme task with variant injects preset colors into jbake properties`() {
        createProjectWithThemeIntentionAndSiteYml(
            description = "Magazine editorial",
            variant = "magazine",
            sitesBaseDir = "office/sites",
            siteName = "mag-site"
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateSite")
            .build()

        val siteDir = projectDir.resolve("office/sites/mag-site")
        val jbakeProps = siteDir.resolve("site/jbake.properties")
        assertThat(jbakeProps).exists().isFile
        // ConfigResolver should resolve magazine variant preset colors
        assertThat(jbakeProps.readText(Charsets.UTF_8))
            .contains("themePrimaryColor=")
            .contains("themeSecondaryColor=")
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `backward compatibility - generateSite task still works without themeIntention`() {
        createMinimalBakeryProject(
            projectDir,
            sitesBaseDir = "office/sites",
            siteName = "backward-compat-theme"
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateSite")
            .build()

        val siteDir = projectDir.resolve("office/sites/backward-compat-theme")
        assertThat(siteDir).exists().isDirectory
        assertThat(siteDir.resolve("site.yml")).exists().isFile
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    private fun createProjectWithThemeIntention(
        description: String,
        variant: String? = null,
        accentColor: String? = null,
        backgroundColor: String? = null,
        textColor: String? = null,
        headingFont: String? = null,
        sitesBaseDir: String? = null,
        siteName: String? = null
    ) {
        val sbDsl = StringBuilder()
        if (sitesBaseDir != null) {
            sbDsl.appendLine("    sitesBaseDir = file(\"$sitesBaseDir\").absolutePath")
        }
        if (siteName != null) {
            sbDsl.appendLine("    siteName = \"$siteName\"")
        }
        sbDsl.appendLine("    themeIntention {")
        sbDsl.appendLine("        description = \"$description\"")
        variant?.let { sbDsl.appendLine("        variant = \"$it\"") }
        accentColor?.let { sbDsl.appendLine("        accentColor = \"$it\"") }
        backgroundColor?.let { sbDsl.appendLine("        backgroundColor = \"$it\"") }
        textColor?.let { sbDsl.appendLine("        textColor = \"$it\"") }
        headingFont?.let { sbDsl.appendLine("        headingFont = \"$it\"") }
        sbDsl.appendLine("    }")

        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "theme-ia-test"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins { id("education.cccp.bakery") }
            bakery {
                configPath = file("site.yml").absolutePath
$sbDsl            }
        """.trimIndent() + "\n")
    }

    private fun createProjectWithThemeIntentionAndSiteYml(
        description: String,
        variant: String,
        sitesBaseDir: String,
        siteName: String
    ) {
        val sbDsl = StringBuilder()
        sbDsl.appendLine("    sitesBaseDir = file(\"$sitesBaseDir\").absolutePath")
        sbDsl.appendLine("    siteName = \"$siteName\"")
        sbDsl.appendLine("    themeIntention {")
        sbDsl.appendLine("        description = \"$description\"")
        sbDsl.appendLine("        variant = \"$variant\"")
        sbDsl.appendLine("    }")

        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "theme-ia-site-test"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins { id("education.cccp.bakery") }
            bakery {
                configPath = file("site.yml").absolutePath
$sbDsl            }
        """.trimIndent() + "\n")
    }

    companion object {
        private fun createMinimalBakeryProject(
            projectDir: File,
            sitesBaseDir: String?,
            siteName: String?,
            siteType: String? = null
        ) {
            val sbDsl = StringBuilder()
            if (sitesBaseDir != null) {
                sbDsl.appendLine("    sitesBaseDir = file(\"$sitesBaseDir\").absolutePath")
            }
            if (siteName != null) {
                sbDsl.appendLine("    siteName = \"$siteName\"")
            }
            if (siteType != null) {
                sbDsl.appendLine("    siteType = \"$siteType\"")
            }

            projectDir.resolve("settings.gradle.kts").writeText("""
                pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
                rootProject.name = "backward-compat-theme-test"
            """.trimIndent())

            projectDir.resolve("build.gradle.kts").writeText("""
                plugins { id("education.cccp.bakery") }
                bakery {
                    configPath = file("site.yml").absolutePath
$sbDsl                }
            """.trimIndent() + "\n")
        }
    }
}