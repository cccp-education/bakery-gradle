package bakery

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.jgit.api.Git
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DeployProfileTaskTest {

    @TempDir
    lateinit var tempDir: File

    private fun createProject(): org.gradle.api.Project {
        val projectDir = tempDir.resolve("project").apply { mkdirs() }
        return ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .withName("test-deploy-profile")
            .build()
    }

    private fun registerExtension(project: org.gradle.api.Project): BakeryExtension =
        project.extensions.create("bakery", BakeryExtension::class.java, project.objects)

    private fun writeSiteYml(projectDir: File, content: String) {
        projectDir.resolve("site.yml").writeText(content)
    }

    private fun createBareRemote(): Pair<File, String> {
        val remoteDir = tempDir.resolve("simulated-remote").apply { mkdirs() }
        Git.init().setBare(true).setDirectory(remoteDir).call().close()
        return remoteDir to remoteDir.toURI().toString()
    }

    private fun createTask(
        project: org.gradle.api.Project
    ): DeployProfileTask = project.tasks.register(
        "deployProfile",
        DeployProfileTask::class.java
    ).get()

    @Nested
    inner class PushProfileMissingTest {

        @Test
        fun `throws when pushProfile section is missing from site yml`() {
            val project = createProject()
            registerExtension(project)
            writeSiteYml(project.projectDir, minimalSiteYmlEmpty)
            project.extensions.extraProperties.set("profileToken", "token")
            project.extensions.extraProperties.set("profileUsername", "user")

            val task = createTask(project)

            assertThatThrownBy { task.deployProfile() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("pushProfile section not found in site.yml")
        }
    }

    @Nested
    inner class CredentialsResolutionTest {

        @Test
        fun `uses CLI credentials when provided via project properties`() {
            val project = createProject()
            registerExtension(project)
            val (_, remoteUri) = createBareRemote()
            writeSiteYml(project.projectDir, makeSiteYml(remoteUri, yamlUsername = "yamlUser", yamlPassword = "yamlPass", profileFilesYaml = "  - README.md"))
            project.extensions.extraProperties.set("profileToken", "cliToken")
            project.extensions.extraProperties.set("profileUsername", "cliUser")
            project.projectDir.resolve("README.md").writeText("cli content")

            val task = createTask(project)
            task.deployProfile()

            val cloneDir = tempDir.resolve("clone-verify").apply { mkdirs() }
            Git.cloneRepository()
                .setURI(remoteUri)
                .setDirectory(cloneDir)
                .call().use { _ -> }
            assertThat(cloneDir.resolve("README.md")).exists().hasContent("cli content")
        }

        @Test
        fun `falls back to YAML credentials when CLI properties are blank`() {
            val project = createProject()
            registerExtension(project)
            val (_, remoteUri) = createBareRemote()
            writeSiteYml(project.projectDir, makeSiteYml(remoteUri, yamlUsername = "yamlUser", yamlPassword = "yamlPass"))
            project.extensions.extraProperties.set("profileToken", "")
            project.extensions.extraProperties.set("profileUsername", "")
            project.projectDir.resolve("README.md").writeText("yaml creds used")

            val task = createTask(project)
            task.deployProfile()

            val cloneDir = tempDir.resolve("clone-verify").apply { mkdirs() }
            Git.cloneRepository()
                .setURI(remoteUri)
                .setDirectory(cloneDir)
                .call().use { _ -> }
            assertThat(cloneDir.resolve("README.md")).exists()
        }

        @Test
        fun `throws when no credentials found both CLI and YAML blank`() {
            val project = createProject()
            registerExtension(project)
            val (_, remoteUri) = createBareRemote()
            writeSiteYml(project.projectDir, makeSiteYml(remoteUri, yamlUsername = "", yamlPassword = "", profileFilesYaml = "  - README.md"))
            project.extensions.extraProperties.set("profileToken", "")
            project.extensions.extraProperties.set("profileUsername", "")
            project.projectDir.resolve("README.md").writeText("test")

            val task = createTask(project)

            assertThatThrownBy { task.deployProfile() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("credentials not found")
        }

        @Test
        fun `uses CLI credentials even when YAML credentials are also set`() {
            val project = createProject()
            registerExtension(project)
            val (_, remoteUri) = createBareRemote()
            writeSiteYml(project.projectDir, makeSiteYml(remoteUri, yamlUsername = "yamlUser", yamlPassword = "yamlPass", profileFilesYaml = "  - README.md"))
            project.extensions.extraProperties.set("profileToken", "cliToken")
            project.extensions.extraProperties.set("profileUsername", "cliUser")
            project.projectDir.resolve("README.md").writeText("cli wins")

            val task = createTask(project)
            task.deployProfile()

            val cloneDir = tempDir.resolve("clone-verify").apply { mkdirs() }
            Git.cloneRepository()
                .setURI(remoteUri)
                .setDirectory(cloneDir)
                .call().use { _ -> }
            assertThat(cloneDir.resolve("README.md")).exists()
        }
    }

    @Nested
    inner class ProfileFilesValidationTest {

        @Test
        fun `throws when profileFiles is empty`() {
            val project = createProject()
            registerExtension(project)
            val (_, remoteUri) = createBareRemote()
            writeSiteYml(project.projectDir, makeSiteYml(remoteUri, profileFilesYaml = "[]"))
            project.extensions.extraProperties.set("profileToken", "token")
            project.extensions.extraProperties.set("profileUsername", "user")

            val task = createTask(project)

            assertThatThrownBy { task.deployProfile() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("No profile files specified")
        }

        @Test
        fun `throws when from directory does not exist`() {
            val project = createProject()
            registerExtension(project)
            val (_, remoteUri) = createBareRemote()
            writeSiteYml(project.projectDir, makeSiteYml(remoteUri, pushFrom = "nonexistent_dir", profileFilesYaml = "  - README.md"))
            project.extensions.extraProperties.set("profileToken", "token")
            project.extensions.extraProperties.set("profileUsername", "user")

            val task = createTask(project)

            assertThatThrownBy { task.deployProfile() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("does not exist")
        }

        @Test
        fun `throws when profile file is not found on disk`() {
            val project = createProject()
            registerExtension(project)
            val (_, remoteUri) = createBareRemote()
            writeSiteYml(project.projectDir, makeSiteYml(remoteUri, profileFilesYaml = "- MISSING_FILE.md"))
            project.extensions.extraProperties.set("profileToken", "token")
            project.extensions.extraProperties.set("profileUsername", "user")

            val task = createTask(project)

            assertThatThrownBy { task.deployProfile() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Profile file not found")
        }

        @Test
        fun `throws when forbidden files are in profileFiles`() {
            val project = createProject()
            registerExtension(project)
            val (_, remoteUri) = createBareRemote()
            writeSiteYml(project.projectDir, makeSiteYml(remoteUri, profileFilesYaml = "- README.adoc"))
            project.extensions.extraProperties.set("profileToken", "token")
            project.extensions.extraProperties.set("profileUsername", "user")
            project.projectDir.resolve("README.adoc").writeText("forbidden")

            val task = createTask(project)

            assertThatThrownBy { task.deployProfile() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Forbidden profile files")
        }

        @Test
        fun `throws when from path is a file not a directory`() {
            val project = createProject()
            registerExtension(project)
            val (_, remoteUri) = createBareRemote()
            project.projectDir.resolve("notadir").writeText("i am a file")
            writeSiteYml(project.projectDir, makeSiteYml(remoteUri, pushFrom = "notadir", profileFilesYaml = "  - README.md"))
            project.extensions.extraProperties.set("profileToken", "token")
            project.extensions.extraProperties.set("profileUsername", "user")

            val task = createTask(project)

            assertThatThrownBy { task.deployProfile() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("not a directory")
        }
    }

    @Nested
    inner class CleanupAfterErrorTest {

        @Test
        fun `cleans up repoDir even when copyProfileFiles throws`() {
            val project = createProject()
            registerExtension(project)
            val (_, remoteUri) = createBareRemote()
            writeSiteYml(project.projectDir, makeSiteYml(remoteUri, profileFilesYaml = "[]"))
            project.extensions.extraProperties.set("profileToken", "token")
            project.extensions.extraProperties.set("profileUsername", "user")

            val task = createTask(project)
            val buildDir = project.layout.buildDirectory.get().asFile

            try {
                task.deployProfile()
                fail("Expected IllegalStateException")
            } catch (e: IllegalStateException) {
                assertThat(e.message).contains("No profile files specified")
            }

            val repoDir = buildDir.resolve("profile-cvs")
            assertThat(repoDir).doesNotExist()
        }
    }

    @Nested
    inner class HappyPathWithMultipleFilesTest {

        @Test
        fun `deploys multiple profile files to simulated remote and cleans up repoDir`() {
            val project = createProject()
            registerExtension(project)
            val (_, remoteUri) = createBareRemote()
            writeSiteYml(project.projectDir, makeSiteYml(remoteUri, pushFrom = "docs", yamlUsername = "cliUser", yamlPassword = "cliToken", profileFilesYaml = "  - README.md\n  - README_fr.md"))
            project.extensions.extraProperties.set("profileToken", "")
            project.extensions.extraProperties.set("profileUsername", "")
            val docsDir = project.projectDir.resolve("docs").apply { mkdirs() }
            docsDir.resolve("README.md").writeText("English readme")
            docsDir.resolve("README_fr.md").writeText("Lisez-moi en francais")

            val task = createTask(project)
            task.deployProfile()

            val cloneDir = tempDir.resolve("clone-verify-multi").apply { mkdirs() }
            Git.cloneRepository()
                .setURI(remoteUri)
                .setDirectory(cloneDir)
                .call().use { _ -> }
            assertThat(cloneDir.resolve("README.md")).exists().hasContent("English readme")
            assertThat(cloneDir.resolve("README_fr.md")).exists().hasContent("Lisez-moi en francais")

            val buildDir = project.layout.buildDirectory.get().asFile
            val repoDir = buildDir.resolve("profile-cvs")
            assertThat(repoDir).doesNotExist()
        }
    }

    companion object {
        private val minimalSiteYmlEmpty = """
bake:
  srcPath: "site"
  destDirPath: "build/bake"
pushPage:
  from: "site"
  to: "cvs"
  repo:
    name: "test-site"
    repository: "https://example.com/test.git"
    credentials:
      username: ""
      password: ""
  branch: "main"
  message: "Deploy test"
pushMaquette:
  from: "maquette"
  to: "cvs"
  repo:
    name: "test-maquette"
    repository: "https://example.com/maquette.git"
    credentials:
      username: ""
      password: ""
  branch: "main"
  message: "Deploy maquette"
""".trimStart()

        fun makeSiteYml(
            remoteUri: String,
            pushFrom: String = "",
            yamlUsername: String = "user",
            yamlPassword: String = "pass",
            profileFilesYaml: String = "  - README.md"
        ): String = """
bake:
  srcPath: "site"
  destDirPath: "build/bake"
pushPage:
  from: "site"
  to: "cvs"
  repo:
    name: "test-site"
    repository: "https://example.com/test.git"
    credentials:
      username: ""
      password: ""
  branch: "main"
  message: "Deploy test"
pushMaquette:
  from: "maquette"
  to: "cvs"
  repo:
    name: "test-maquette"
    repository: "https://example.com/maquette.git"
    credentials:
      username: ""
      password: ""
  branch: "main"
  message: "Deploy maquette"
pushProfile:
  from: "$pushFrom"
  to: "profile-cvs"
  repo:
    name: "testprofile"
    repository: "$remoteUri"
    credentials:
      username: "$yamlUsername"
      password: "$yamlPassword"
  branch: "main"
  message: "update profile"
profileFiles${if (profileFilesYaml.startsWith("[")) ": $profileFilesYaml" else ":\n$profileFilesYaml"}
""".trimStart()
    }
}
