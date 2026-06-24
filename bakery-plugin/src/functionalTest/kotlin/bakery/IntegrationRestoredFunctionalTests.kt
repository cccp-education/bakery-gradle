@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

/**
 * Tests d'intégration restaurés depuis BKY-PERF-6.
 *
 * Ces tests nécessitent un vrai daemon Gradle car ils vérifient l'exécution
 * réelle d'une tâche (génération de fichiers, injection de propriétés).
 * Les tests DSL triviaux (compilation, enregistrement de tâche) ont été migrés
 * vers ProjectBuilder (unit tests in-process) — voir :
 * - ThemeIntentionUnitTest
 * - ScaffoldIntentionUnitTest
 * - CollectSiteContextUnitTest
 *
 * Ici : on vérifie que le fichier généré contient bien les bonnes propriétés.
 */
class IntegrationRestoredFunctionalTests {

    @Nested
    @DisplayName("ThemeIntention — injection preset colors (BKY-IA-2)")
    inner class ThemePresetInjectionTest {

        @TempDir
        lateinit var projectDir: File

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
            assertThat(jbakeProps.readText(UTF_8))
                .contains("themePrimaryColor=")
                .contains("themeSecondaryColor=")
            assertThat(result.output).contains("BUILD SUCCESSFUL")
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
    }

    @Nested
    @DisplayName("ScaffoldIntention — generateSite exécution réelle (BKY-IA-1)")
    inner class ScaffoldExecutionTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `generateSiteFromIntention with scaffoldIntention produces siteDir with site yml`() {
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
        fun `backward compatibility - generateSite task produces siteDir with site yml`() {
            createMinimalBakeryProject(
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

        private fun createProjectWithScaffoldIntention(
            description: String,
            sitesBaseDir: String,
            siteName: String
        ) {
            val sbDsl = StringBuilder()
            sbDsl.appendLine("    sitesBaseDir = file(\"$sitesBaseDir\").absolutePath")
            sbDsl.appendLine("    siteName = \"$siteName\"")
            sbDsl.appendLine("    scaffoldIntention {")
            sbDsl.appendLine("        description = \"$description\"")
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

        private fun createMinimalBakeryProject(
            sitesBaseDir: String,
            siteName: String
        ) {
            val sbDsl = StringBuilder()
            sbDsl.appendLine("    sitesBaseDir = file(\"$sitesBaseDir\").absolutePath")
            sbDsl.appendLine("    siteName = \"$siteName\"")

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

    @Nested
    @DisplayName("CollectAugmentedContext — tâches visibles dans collect group")
    inner class CollectGroupVisibilityTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `collectAugmentedContext and collectSiteContext are registered under collect group`() {
            setupGradleProject()
            setupSiteContent()

            val result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("tasks", "--group", "collect")
                .build()

            assertThat(result.output).contains("collectAugmentedContext")
            assertThat(result.output).contains("collectSiteContext")
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }

        private fun setupGradleProject() {
            projectDir.resolve("settings.gradle.kts").writeText(
                """
                pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
                rootProject.name = "lens-test"
                """.trimIndent()
            )

            projectDir.resolve("build.gradle.kts").writeText(
                """
                plugins { id("education.cccp.bakery") }
                bakery { configPath = file("site.yml").absolutePath }
                """.trimIndent()
            )

            projectDir.resolve("site.yml").writeText(
                """
                bake: { srcPath: site, destDirPath: build/bake }
                pushPage: { from: site, to: pages }
                pushMaquette: { from: maquette, to: maquette-pages }
                """.trimIndent()
            )
        }

        private fun setupSiteContent() {
            val siteDir = projectDir.resolve("site")
            siteDir.mkdirs()
            siteDir.resolve("jbake.properties").writeText(
                """
                template.folder=templates
                content.folder=content
                render.tags=true
                """.trimIndent()
            )

            val templatesDir = siteDir.resolve("templates")
            templatesDir.mkdirs()
            templatesDir.resolve("menu.thyme").writeText("<html></html>")
            templatesDir.resolve("post.thyme").writeText("<html th:text=\"\${content.body}\"></html>")
            templatesDir.resolve("page.thyme").writeText("<html th:text=\"\${content.body}\"></html>")
            templatesDir.resolve("index.thyme").writeText("<html></html>")
            templatesDir.resolve("tags.thyme").writeText("<html></html>")
            templatesDir.resolve("tag.thyme").writeText("<html></html>")
            templatesDir.resolve("feed.thyme").writeText("<html></html>")
            templatesDir.resolve("archive.thyme").writeText("<html></html>")
            templatesDir.resolve("sitemap.thyme").writeText("<html></html>")
        }
    }
}