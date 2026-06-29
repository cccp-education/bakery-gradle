package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TreeBakeFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `tasks succeeds with tree config`() {
        createProjectWithTreeConfig()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(result.output).contains("bake")
        assertThat(result.output).contains("verifyConfigurationMapping")
    }

    @Test
    fun `verifyConfigurationMapping succeeds with tree config`() {
        createProjectWithTreeConfig()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("verifyConfigurationMapping")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(result.output).contains("Configuration OK")
    }

    private fun createProjectWithTreeConfig() {
        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "tree-bake-test"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins { id("education.cccp.bakery") }
            bakery {
                configPath = file("site.yml").absolutePath
            }
        """.trimIndent())

        projectDir.resolve("site.yml").writeText("""
            bake:
              srcPath: "site"
              destDirPath: "build/bake"
            pushPage:
              from: "site"
              to: "cvs"
              repo:
                name: "test-site"
                repository: "https://github.com/user/repo.git"
                credentials:
                  username: "user"
                  password: "secret"
              branch: "main"
              message: "Deploy test"
            pushMaquette:
              from: "maquette"
              to: "cvs"
              repo:
                name: "test-maquette"
                repository: "https://github.com/user/repo.git"
                credentials:
                  username: "user"
                  password: "secret"
              branch: "main"
              message: "Deploy maquette"
            tree:
              type: site
              path: ""
              sections:
                - type: section
                  path: blog
                  outputConfig:
                    layout: SIDEBAR_LEFT
                  articles:
                    - type: article
                      path: blog/post-1
                      outputConfig:
                        template: "custom.ftl"
        """.trimIndent())

        projectDir.resolve("site").mkdirs()
        projectDir.resolve("maquette").mkdirs()
    }
}
