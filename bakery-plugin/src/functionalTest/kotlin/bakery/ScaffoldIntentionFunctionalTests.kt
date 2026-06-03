@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests fonctionnels pour l'EPIC BKY-IA-1 — Scaffolding assiste par IA.
 *
 * Valide que le DSL `bakery { scaffoldIntention { ... } }` compile
 * et que la tache `generateSiteFromIntention` est correctement configuree.
 *
 * Pattern identique a [ArticleIntentionFunctionalTests] et [ScaffoldFunctionalTests].
 */
class ScaffoldIntentionFunctionalTests {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `DSL scaffoldIntention block compiles with description only`() {
        createProjectWithScaffoldIntention(
            description = "Blog tech Kotlin"
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `DSL scaffoldIntention block compiles with full config`() {
        createProjectWithScaffoldIntention(
            description = "Portfolio professionnel Kotlin",
            siteType = "portfolio",
            lang = "en",
            projectName = "kotlin-dev"
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `generateSiteFromIntention task is registered with scaffoldIntention DSL`() {
        createProjectWithScaffoldIntention(
            description = "Documentation API",
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
    fun `backward compatibility - generateSite task still works without scaffoldIntention`() {
        createMinimalBakeryProject(
            projectDir,
            sitesBaseDir = "office/sites",
            siteName = "backward-compat"
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateSite")
            .build()

        val siteDir = projectDir.resolve("office/sites/backward-compat")
        assertThat(siteDir).exists().isDirectory
        assertThat(siteDir.resolve("site.yml")).exists().isFile
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `generateSiteFromIntention with scaffoldIntention and ia blocks both present`() {
        createProjectWithScaffoldIntentionAndIA(
            description = "Formation Kotlin",
            siteType = "formation",
            sitesBaseDir = "office/sites",
            siteName = "formation-kotlin"
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateSite")
            .build()

        val siteDir = projectDir.resolve("office/sites/formation-kotlin")
        assertThat(siteDir).exists().isDirectory
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    private fun createProjectWithScaffoldIntention(
        description: String,
        siteType: String? = null,
        lang: String? = null,
        projectName: String? = null,
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
        sbDsl.appendLine("    scaffoldIntention {")
        sbDsl.appendLine("        description = \"$description\"")
        siteType?.let { sbDsl.appendLine("        siteType = \"$it\"") }
        lang?.let { sbDsl.appendLine("        lang = \"$it\"") }
        projectName?.let { sbDsl.appendLine("        projectName = \"$it\"") }
        sbDsl.appendLine("    }")

        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "scaffold-ia-test"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins { id("education.cccp.bakery") }
            bakery {
                configPath = file("site.yml").absolutePath
$sbDsl            }
        """.trimIndent() + "\n")
    }

    private fun createProjectWithScaffoldIntentionAndIA(
        description: String,
        siteType: String? = null,
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
        sbDsl.appendLine("    scaffoldIntention {")
        sbDsl.appendLine("        description = \"$description\"")
        siteType?.let { sbDsl.appendLine("        siteType = \"$it\"") }
        sbDsl.appendLine("    }")
        sbDsl.appendLine("    ia {")
        sbDsl.appendLine("        baseUrl = \"http://localhost:11434\"")
        sbDsl.appendLine("        modelName = \"deepseek-v4-pro\"")
        sbDsl.appendLine("    }")

        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "scaffold-ia-with-llm-test"
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
                rootProject.name = "backward-compat-test"
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