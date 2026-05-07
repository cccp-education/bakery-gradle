@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner.create
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

class ScaffoldFunctionalTests {

    @Nested
    @DisplayName("initSite avec sites.base.dir + site.name")
    inner class HappyPathTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should scaffold site into office-sites-mycompany when both properties are defined`() {
            createMinimalBakeryProject(projectDir, sitesBaseDir = "office/sites", siteName = "my-company")

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("initSite")
                .build()

            val siteDir = projectDir.resolve("office/sites/my-company")
            assertThat(siteDir).exists().isDirectory
            assertThat(siteDir.resolve("site.yml")).exists().isFile
            assertThat(siteDir.resolve("site/jbake.properties")).exists().isFile
            assertThat(siteDir.resolve("maquette/index.html")).exists().isFile
            assertThat(siteDir.resolve(".gitignore")).exists().isFile
            assertThat(siteDir.resolve(".gitattributes")).exists().isFile
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }
    }

    @Nested
    @DisplayName("initSite failures")
    inner class ErrorHandlingTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should fail when sitesBaseDir is defined but siteName is missing`() {
            createMinimalBakeryProject(projectDir, sitesBaseDir = "office/sites", siteName = null)

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("initSite", "--stacktrace")
                .buildAndFail()

            assertThat(result.output).contains("siteName must be defined")
        }

        @Test
        fun `should fail when site directory already exists`() {
            createMinimalBakeryProject(projectDir, sitesBaseDir = "office/sites", siteName = "existing-site")
            projectDir.resolve("office/sites/existing-site").mkdirs()

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("initSite", "--stacktrace")
                .buildAndFail()

            assertThat(result.output).contains("already exists")
        }
    }

    @Nested
    @DisplayName("initSite with siteName only")
    inner class SiteNameOnlyTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should scaffold site into projectDir-siteName when only siteName is defined`() {
            createMinimalBakeryProject(projectDir, sitesBaseDir = null, siteName = "mysite")

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("initSite")
                .build()

            val siteDir = projectDir.resolve("mysite")
            assertThat(siteDir).exists().isDirectory
            assertThat(siteDir.resolve("site.yml")).exists().isFile
            assertThat(siteDir.resolve("site/jbake.properties")).exists().isFile
            assertThat(siteDir.resolve("maquette/index.html")).exists().isFile
        }
    }

    @Nested
    @DisplayName("initSite backward compat (ni sites.base.dir ni site.name)")
    inner class BackwardCompatTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should scaffold into projectDir root when no scaffolding properties are defined`() {
            createMinimalBakeryProject(projectDir, sitesBaseDir = null, siteName = null)

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("initSite")
                .build()

            assertThat(projectDir.resolve("site.yml")).exists().isFile
            assertThat(projectDir.resolve("site/jbake.properties")).exists().isFile
            assertThat(projectDir.resolve("maquette/index.html")).exists().isFile
            assertThat(projectDir.resolve(".gitignore")).exists().isFile
            assertThat(projectDir.resolve(".gitattributes")).exists().isFile
        }
    }

    companion object {
        private fun createMinimalBakeryProject(
            projectDir: File,
            sitesBaseDir: String?,
            siteName: String?
        ) {
            val sbDsl = StringBuilder()
            if (sitesBaseDir != null) {
                sbDsl.append("    sitesBaseDir = file(\"$sitesBaseDir\").absolutePath\n")
            }
            if (siteName != null) {
                sbDsl.append("    siteName = \"$siteName\"\n")
            }

            projectDir.resolve("settings.gradle.kts").writeText("""
                pluginManagement.repositories.gradlePluginPortal()
                rootProject.name = "scaffold-test"
            """.trimIndent(), UTF_8)

            projectDir.resolve("build.gradle.kts").writeText("""
                plugins { id("com.cheroliv.bakery") }
                bakery {
                    configPath = file("site.yml").absolutePath
            $sbDsl    }
            """.trimIndent() + "\n", UTF_8)
        }
    }
}
