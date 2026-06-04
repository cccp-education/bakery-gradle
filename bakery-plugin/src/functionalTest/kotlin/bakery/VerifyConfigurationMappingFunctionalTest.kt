package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * US-61a — Tests fonctionnels pour verifyConfigurationMapping.
 *
 * Valide que la tâche Gradle s'exécute correctement avec un site.yml valide
 * et échoue proprement quand le fichier est absent.
 */
class VerifyConfigurationMappingFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `verifyConfigurationMapping succeeds with valid site yml`() {
        createProjectWithValidSiteYml()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("verifyConfigurationMapping")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(result.output).contains("Configuration OK")
        assertThat(result.output).contains("credentials.password=***")
        assertThat(result.output).doesNotContain("secret-token-42")
    }

    @Test
    fun `verifyConfigurationMapping fails with missing site yml`() {
        createProjectWithoutSiteYml()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("verifyConfigurationMapping")
            .buildAndFail()

        assertThat(result.output).contains("Configuration file not found")
    }

    @Test
    fun `verifyConfigurationMapping task is registered in verification group`() {
        createProjectWithValidSiteYml()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group", "verification")
            .build()

        assertThat(result.output).contains("verifyConfigurationMapping")
    }

    private fun createProjectWithValidSiteYml() {
        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "verify-mapping-test"
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
                  password: "secret-token-42"
              branch: "main"
              message: "Deploy test"
            pushMaquette:
              from: "maquette"
              to: "cvs"
              repo:
                name: "test-maquette"
                repository: "https://github.com/user/maquette.git"
                credentials:
                  username: "user"
                  password: "another-secret"
              branch: "main"
              message: "Deploy maquette"
        """.trimIndent())

        projectDir.resolve("site").mkdirs()
        projectDir.resolve("maquette").mkdirs()
    }

    private fun createProjectWithoutSiteYml() {
        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "verify-mapping-missing-test"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins { id("education.cccp.bakery") }
            bakery {
                configPath = file("site.yml").absolutePath
            }
        """.trimIndent())

        // NO site.yml created — directories exist but config absent
        projectDir.resolve("site").mkdirs()
        projectDir.resolve("maquette").mkdirs()
    }
}
