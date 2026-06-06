@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.GradleRunner.create
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

class ProfilePublisherFunctionalTest {

    @Nested
    @DisplayName("deployProfile happy path with bare git remote")
    inner class HappyPathTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should push profile files to a bare git remote`() {
            val bareRemoteDir = projectDir.resolve("remote.git").apply { mkdirs() }
            Git.init().setBare(true).setDirectory(bareRemoteDir).call().close()
            val remoteUri = bareRemoteDir.toURI().toString()

            projectDir.resolve("README.md").writeText("Readme content", UTF_8)
            projectDir.resolve("CHANGELOG.md").writeText("Changelog content", UTF_8)

            projectDir.resolve("settings.gradle.kts").writeText("""
                pluginManagement.repositories.gradlePluginPortal()
                rootProject.name = "deploy-profile-ft"
            """.trimIndent(), UTF_8)

            projectDir.resolve("build.gradle.kts").writeText("""
                plugins { id("education.cccp.bakery") }
                bakery { configPath = file("site.yml").absolutePath }
            """.trimIndent() + "\n", UTF_8)

            projectDir.resolve("site.yml").writeText("""
bake:
  srcPath: "site"
  destDirPath: "build/bake"
pushProfile:
  from: ""
  to: "profile-cvs"
  repo:
    name: "test-profile"
    repository: "$remoteUri"
    credentials:
      username: "testuser"
      password: "testpass"
  branch: "main"
  message: "update profile"
profileFiles:
  - README.md
  - CHANGELOG.md
""".trimIndent(), UTF_8)

            createMinimalSiteYml(projectDir)

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("deployProfile", "-PprofileUsername=testuser", "-PprofileToken=testpass")
                .build()

            assertThat(result.output).contains("BUILD SUCCESSFUL")

            val cloneDir = projectDir.resolve("clone-verify").apply { mkdirs() }
            Git.cloneRepository()
                .setURI(remoteUri)
                .setDirectory(cloneDir)
                .call().use { _ -> }
            assertThat(cloneDir.resolve("README.md")).exists().hasContent("Readme content")
            assertThat(cloneDir.resolve("CHANGELOG.md")).exists().hasContent("Changelog content")
        }

        @Test
        fun `should fallback to YAML credentials when CLI is blank and push successfully`() {
            val bareRemoteDir = projectDir.resolve("remote.git").apply { mkdirs() }
            Git.init().setBare(true).setDirectory(bareRemoteDir).call().close()
            val remoteUri = bareRemoteDir.toURI().toString()

            projectDir.resolve("README.md").writeText("yaml driven push", UTF_8)

            projectDir.resolve("settings.gradle.kts").writeText("""
                pluginManagement.repositories.gradlePluginPortal()
                rootProject.name = "deploy-profile-yaml-ft"
            """.trimIndent(), UTF_8)

            projectDir.resolve("build.gradle.kts").writeText("""
                plugins { id("education.cccp.bakery") }
                bakery { configPath = file("site.yml").absolutePath }
            """.trimIndent() + "\n", UTF_8)

            projectDir.resolve("site.yml").writeText("""
bake:
  srcPath: "site"
  destDirPath: "build/bake"
pushProfile:
  from: ""
  to: "profile-cvs"
  repo:
    name: "test-profile-yaml"
    repository: "$remoteUri"
    credentials:
      username: "yamlUser"
      password: "yamlPass"
  branch: "main"
  message: "update profile"
profileFiles:
  - README.md
""".trimIndent(), UTF_8)

            createMinimalSiteYml(projectDir)

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("deployProfile", "-PprofileUsername=", "-PprofileToken=")
                .build()

            assertThat(result.output).contains("BUILD SUCCESSFUL")

            val cloneDir = projectDir.resolve("clone-verify").apply { mkdirs() }
            Git.cloneRepository()
                .setURI(remoteUri)
                .setDirectory(cloneDir)
                .call().use { _ -> }
            assertThat(cloneDir.resolve("README.md")).exists().hasContent("yaml driven push")
        }
    }

    @Nested
    @DisplayName("deployProfile failure cases")
    inner class FailureCasesTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should fail when pushProfile section is missing from site yml`() {
            projectDir.resolve("settings.gradle.kts").writeText("""
                pluginManagement.repositories.gradlePluginPortal()
                rootProject.name = "deploy-no-profile"
            """.trimIndent(), UTF_8)

            projectDir.resolve("build.gradle.kts").writeText("""
                plugins { id("education.cccp.bakery") }
                bakery { configPath = file("site.yml").absolutePath }
            """.trimIndent() + "\n", UTF_8)

            projectDir.resolve("site.yml").writeText("""
bake:
  srcPath: "site"
  destDirPath: "build/bake"
""".trimIndent(), UTF_8)

            createMinimalSiteYml(projectDir)

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("deployProfile", "-PprofileUsername=user", "-PprofileToken=pass")
                .buildAndFail()

            assertThat(result.output).contains("Task 'deployProfile' not found")
        }

        @Test
        fun `should fail when credentials are missing in both CLI and YAML`() {
            val bareRemoteDir = projectDir.resolve("remote.git").apply { mkdirs() }
            Git.init().setBare(true).setDirectory(bareRemoteDir).call().close()
            val remoteUri = bareRemoteDir.toURI().toString()

            projectDir.resolve("README.md").writeText("no credentials", UTF_8)

            projectDir.resolve("settings.gradle.kts").writeText("""
                pluginManagement.repositories.gradlePluginPortal()
                rootProject.name = "deploy-no-creds"
            """.trimIndent(), UTF_8)

            projectDir.resolve("build.gradle.kts").writeText("""
                plugins { id("education.cccp.bakery") }
                bakery { configPath = file("site.yml").absolutePath }
            """.trimIndent() + "\n", UTF_8)

            projectDir.resolve("site.yml").writeText("""
bake:
  srcPath: "site"
  destDirPath: "build/bake"
pushProfile:
  from: ""
  to: "profile-cvs"
  repo:
    name: "test-no-creds"
    repository: "$remoteUri"
    credentials:
      username: ""
      password: ""
  branch: "main"
  message: "update profile"
profileFiles:
  - README.md
""".trimIndent(), UTF_8)

            createMinimalSiteYml(projectDir)

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("deployProfile", "-PprofileUsername=", "-PprofileToken=")
                .buildAndFail()

            assertThat(result.output).contains("credentials not found")
        }
    }

    private companion object {
        fun createMinimalSiteYml(projectDir: File) {
            projectDir.resolve("site").mkdirs()
            projectDir.resolve("maquette").mkdirs()
        }
    }
}
