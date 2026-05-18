@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestClassOrder
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.ClassOrderer
import org.junit.jupiter.api.io.TempDir
import java.io.File

@TestClassOrder(ClassOrderer.OrderAnnotation::class)
class ArchitectureValidationTest {

    companion object {
        private const val SITE_NAME = "un-site-test"

        @TempDir
        @JvmStatic
        lateinit var sharedProjectDir: File

        private val sitesDir: File
            get() = sharedProjectDir.resolve("sites").also { it.mkdirs() }

        private val projectDir: File
            get() = sitesDir.resolve(SITE_NAME)

        private val jbakeDir: File
            get() = projectDir.resolve("jbake")

        private val contentDir: File
            get() = jbakeDir.resolve("content")

        private val templatesDir: File
            get() = jbakeDir.resolve("templates")

        private val assetsDir: File
            get() = jbakeDir.resolve("assets")

        private val blogDir: File
            get() = contentDir.resolve("blog").resolve("2025")

        private val configFile: File
            get() = projectDir.resolve("site.yml")

        @BeforeAll
        @JvmStatic
        fun setupSharedProject() {
            createSitesStructure()
            createGradleBuild()
            createConfigFile()
        }

        fun runner(vararg args: String): GradleRunner =
            GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(*args)
                .withProjectDir(projectDir)

        private fun createSitesStructure() {
            jbakeDir.mkdirs()
            contentDir.mkdirs()
            templatesDir.mkdirs()
            assetsDir.mkdirs()
            blogDir.mkdirs()

            contentDir.resolve("index.adoc").writeText(
                """
                = Home
                :jbake-status: published
                :jbake-type: page

                Test Homepage content.
                """.trimIndent()
            )

            blogDir.resolve("0000_test_article_post.adoc").writeText(
                """
                = Test Article Title
                :jbake-date: 2025-01-15
                :jbake-status: published
                :jbake-type: post

                Content of the test article.
                """.trimIndent()
            )

            val baseThyme = """<!DOCTYPE html><html><head><title th:text="|Test - ${'$'}{content.title}|"></title></head><body th:utext="${'$'}{content.body}"></body></html>"""
            templatesDir.resolve("index.thyme").writeText(baseThyme)
            templatesDir.resolve("post.thyme").writeText(baseThyme)
            templatesDir.resolve("page.thyme").writeText(baseThyme)
            templatesDir.resolve("archive.thyme").writeText(baseThyme)
            templatesDir.resolve("tags.thyme").writeText(baseThyme)
            templatesDir.resolve("sitemap.thyme").writeText(baseThyme)
            templatesDir.resolve("feed.thyme").writeText(baseThyme)
            templatesDir.resolve("blog.thyme").writeText(baseThyme)

            assetsDir.resolve("css").also { it.mkdirs() }.resolve("styles.css").writeText("body { color: black; }")
            jbakeDir.resolve("jbake.properties").writeText(
                """
                site.host=https://un-site-test.cheroliv.com/
                render.archive=true
                render.index=true
                render.tags=true
                render.sitemap=true
                render.blog=true
                render.feed=true
                template.index.file=index.thyme
                template.masterindex.file=index.thyme
                template.archive.file=archive.thyme
                template.tag.file=tags.thyme
                template.sitemap.file=sitemap.thyme
                template.post.file=post.thyme
                template.page.file=page.thyme
                template.feed.file=feed.thyme
                template.blog.file=blog.thyme
                destination.folder=../build/jbake-build
                """.trimIndent()
            )
        }

        private fun createGradleBuild() {
            projectDir.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    id("com.cheroliv.bakery") version "0.1.3"
                }
                bakery {
                    configPath.set(file("site.yml").absolutePath)
                }
                """.trimIndent()
            )
            projectDir.resolve("settings.gradle.kts").writeText(
                "rootProject.name = \"$SITE_NAME\""
            )
        }

        private fun createConfigFile() {
            configFile.writeText(
                """
                bake:
                  srcPath: "jbake"
                  destDirPath: "bake"
                  cname: "un-site-test.cheroliv.com"
                pushPage:
                  from: "bake"
                  to: "cvs"
                  repo:
                    name: "test-repo"
                    repository: "https://github.com/test/test.git"
                    credentials:
                      username: ""
                      password: ""
                  branch: "main"
                  message: "test deploy"
                """.trimIndent()
            )
        }
    }

    @Nested
    @Order(1)
    @DisplayName("Bake tasks")
    @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
    inner class BakeTasks {

        @Test
        @Order(1)
        @Tag("quick")
        fun `office sites structure bake produces index html`() {
            val result = runner("bake").build()

            assertThat(result.output).contains("BUILD SUCCESSFUL")
            val indexHtml = projectDir.resolve("build/bake/index.html")
            assertThat(indexHtml).exists().isFile
            assertThat(indexHtml.length()).isGreaterThan(0)
        }

        @Test
        @Order(2)
        fun `office sites structure bake produces blog post html`() {
            val result = runner("bake").build()

            val postHtml = projectDir.resolve("build/bake/blog/2025/0000_test_article_post.html")
            assertThat(postHtml).exists()
            assertThat(postHtml.readText()).contains("Test Article Title")
        }

        @Test
        fun `bake works without credentials in site yml`() {
            val siteYml = configFile.readText()
            assertThat(siteYml).doesNotContain("gho_")
            assertThat(siteYml).contains("password: \"\"")

            assertThatCode {
                runner("bake").build()
            }.doesNotThrowAnyException()
        }

        @Test
        fun `bake is incremental on second run`() {
            runner("bake").build()
            val result = runner("bake").build()

            assertThat(result.output).contains("UP-TO-DATE")
        }
    }

    @Nested
    @Order(2)
    @DisplayName("Task registration and inspection")
    inner class TaskInspection {

        @Test
        fun `tasks are registered correctly from office sites structure`() {
            val result = runner("tasks").build()

            assertThat(result.output).contains("bake")
            assertThat(result.output).contains("deploySite")
            assertThat(result.output).contains("serve")
        }

        @Test
        fun `serve task is configured and can be inspected`() {
            val result = runner("help", "--task=serve").build()

            assertThat(result.output).contains("Serves the baked site locally")
        }
    }
}
