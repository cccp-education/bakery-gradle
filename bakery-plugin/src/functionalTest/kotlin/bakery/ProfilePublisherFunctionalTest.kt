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
    @DisplayName("deployProfile full pipeline — history preservation")
    inner class FullPipelineHistoryPreservationTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should preserve git history across multiple deploys`() {
            val bareRemoteDir = projectDir.resolve("remote.git").apply { mkdirs() }
            Git.init().setBare(true).setDirectory(bareRemoteDir).call().close()
            val remoteUri = bareRemoteDir.toURI().toString()

            projectDir.resolve("README.md").writeText("deploy v1", UTF_8)
            projectDir.resolve("settings.gradle.kts").writeText("""
                pluginManagement.repositories.gradlePluginPortal()
                rootProject.name = "deploy-history-ft"
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
    name: "test-history"
    repository: "$remoteUri"
    credentials:
      username: "histuser"
      password: "histpass"
  branch: "main"
  message: "update profile"
profileFiles:
  - README.md
""".trimIndent(), UTF_8)

            createMinimalSiteYml(projectDir)

            val r1 = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("deployProfile", "-PprofileUsername=histuser", "-PprofileToken=histpass")
                .build()
            assertThat(r1.output).contains("BUILD SUCCESSFUL")

            projectDir.resolve("README.md").writeText("deploy v2", UTF_8)

            val r2 = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("deployProfile", "-PprofileUsername=histuser", "-PprofileToken=histpass")
                .build()
            assertThat(r2.output).contains("BUILD SUCCESSFUL")

            val cloneDir = projectDir.resolve("clone-verify").apply { mkdirs() }
            val cloned = Git.cloneRepository()
                .setURI(remoteUri)
                .setDirectory(cloneDir)
                .call()
            assertThat(cloneDir.resolve("README.md")).hasContent("deploy v2")

            val log = cloned.log().call()
            assertThat(log).hasSizeGreaterThan(1)
            cloned.close()
        }
    }

    @Nested
    @DisplayName("deployProfile full pipeline — custom from subdirectory")
    inner class FullPipelineCustomFromTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should deploy profile files from a custom from subdirectory`() {
            val bareRemoteDir = projectDir.resolve("remote.git").apply { mkdirs() }
            Git.init().setBare(true).setDirectory(bareRemoteDir).call().close()
            val remoteUri = bareRemoteDir.toURI().toString()

            val docsDir = projectDir.resolve("profile-assets").apply { mkdirs() }
            docsDir.resolve("README.md").writeText("custom from dir", UTF_8)
            docsDir.resolve("CONTRIBUTING.md").writeText("contribute here", UTF_8)

            projectDir.resolve("settings.gradle.kts").writeText("""
                pluginManagement.repositories.gradlePluginPortal()
                rootProject.name = "deploy-custom-from-ft"
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
  from: "profile-assets"
  to: "profile-cvs"
  repo:
    name: "test-custom-from"
    repository: "$remoteUri"
    credentials:
      username: "customuser"
      password: "custompass"
  branch: "main"
  message: "deploy from custom dir"
profileFiles:
  - README.md
  - CONTRIBUTING.md
""".trimIndent(), UTF_8)

            createMinimalSiteYml(projectDir)

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("deployProfile", "-PprofileUsername=customuser", "-PprofileToken=custompass")
                .build()

            assertThat(result.output).contains("BUILD SUCCESSFUL")

            val cloneDir = projectDir.resolve("clone-verify").apply { mkdirs() }
            Git.cloneRepository()
                .setURI(remoteUri)
                .setDirectory(cloneDir)
                .call().use { _ -> }
            assertThat(cloneDir.resolve("README.md")).exists().hasContent("custom from dir")
            assertThat(cloneDir.resolve("CONTRIBUTING.md")).exists().hasContent("contribute here")
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

            // BKY-FIX-1 : deployProfile est toujours enregistrée ; l'absence de
            // pushProfile est détectée au runtime par la tâche.
            assertThat(result.output).contains("pushProfile section not found in site.yml")
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
