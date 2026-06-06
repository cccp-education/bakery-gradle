package bakery

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.slf4j.LoggerFactory
import java.io.File

class ProfilePublisherTest {

    @TempDir
    lateinit var tempDir: File

    private val logger = LoggerFactory.getLogger(ProfilePublisherTest::class.java)

    // ── Resolve Credentials ──

    @Nested
    inner class ResolveCredentialsTest {

        private fun gitConfig(
            username: String = "yamlUser",
            password: String = "yamlPass"
        ) = GitPushConfiguration(
            from = "",
            to = "profile-cvs",
            repo = RepositoryConfiguration(
                name = "test",
                repository = "https://example.com/test.git",
                credentials = RepositoryCredentials(username, password)
            ),
            branch = "main",
            message = "deploy"
        )

        @Test
        fun `returns CLI credentials when both CLI and YAML are provided`() {
            val config = gitConfig()
            val (user, pass) = ProfilePublisher.resolveCredentials(config, "cliUser", "cliToken")

            assertThat(user).isEqualTo("cliUser")
            assertThat(pass).isEqualTo("cliToken")
        }

        @Test
        fun `falls back to YAML credentials when CLI is blank`() {
            val config = gitConfig()
            val (user, pass) = ProfilePublisher.resolveCredentials(config, "", "")

            assertThat(user).isEqualTo("yamlUser")
            assertThat(pass).isEqualTo("yamlPass")
        }

        @Test
        fun `falls back to YAML credentials when CLI username is blank only`() {
            val config = gitConfig()
            val (user, pass) = ProfilePublisher.resolveCredentials(config, "", "cliToken")

            assertThat(user).isEqualTo("yamlUser")
            assertThat(pass).isEqualTo("cliToken")
        }

        @Test
        fun `falls back to YAML credentials when CLI password is blank only`() {
            val config = gitConfig()
            val (user, pass) = ProfilePublisher.resolveCredentials(config, "cliUser", "")

            assertThat(user).isEqualTo("cliUser")
            assertThat(pass).isEqualTo("yamlPass")
        }

        @Test
        fun `throws when both CLI and YAML are blank`() {
            val config = gitConfig(username = "", password = "")

            assertThatThrownBy {
                ProfilePublisher.resolveCredentials(config, "", "")
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("credentials not found")
        }

        @Test
        fun `throws when CLI is blank and YAML username is blank`() {
            val config = gitConfig(username = "", password = "yamlPass")

            assertThatThrownBy {
                ProfilePublisher.resolveCredentials(config, "", "")
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("credentials not found")
        }

        @Test
        fun `throws when CLI is blank and YAML password is blank`() {
            val config = gitConfig(username = "yamlUser", password = "")

            assertThatThrownBy {
                ProfilePublisher.resolveCredentials(config, "", "")
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("credentials not found")
        }
    }

    // ── Copy Profile Files ──

    @Nested
    inner class CopyProfileFilesTest {

        @Test
        fun `copies a single file from projectDir to repoDir`() {
            val projectDir = tempDir.resolve("project").apply { mkdirs() }
            val repoDir = tempDir.resolve("repo").apply { mkdirs() }
            projectDir.resolve("README.md").writeText("profile content")

            ProfilePublisher.copyProfileFiles(
                profileFiles = listOf("README.md"),
                projectDir = projectDir,
                from = "",
                repoDir = repoDir,
                logger = logger
            )

            assertThat(repoDir.resolve("README.md")).exists().hasContent("profile content")
        }

        @Test
        fun `copies multiple files from projectDir to repoDir`() {
            val projectDir = tempDir.resolve("project").apply { mkdirs() }
            val repoDir = tempDir.resolve("repo").apply { mkdirs() }
            projectDir.resolve("README.md").writeText("readme")
            projectDir.resolve("CHANGELOG.md").writeText("changelog")
            projectDir.resolve("LICENSE").writeText("license")

            ProfilePublisher.copyProfileFiles(
                profileFiles = listOf("README.md", "CHANGELOG.md", "LICENSE"),
                projectDir = projectDir,
                from = "",
                repoDir = repoDir,
                logger = logger
            )

            assertThat(repoDir.resolve("README.md")).exists().hasContent("readme")
            assertThat(repoDir.resolve("CHANGELOG.md")).exists().hasContent("changelog")
            assertThat(repoDir.resolve("LICENSE")).exists().hasContent("license")
        }

        @Test
        fun `copies files from a subdirectory when from is not blank`() {
            val projectDir = tempDir.resolve("project").apply { mkdirs() }
            val repoDir = tempDir.resolve("repo").apply { mkdirs() }
            val docsDir = projectDir.resolve("docs").apply { mkdirs() }
            docsDir.resolve("README.md").writeText("from subdir")

            ProfilePublisher.copyProfileFiles(
                profileFiles = listOf("README.md"),
                projectDir = projectDir,
                from = "docs",
                repoDir = repoDir,
                logger = logger
            )

            assertThat(repoDir.resolve("README.md")).exists().hasContent("from subdir")
        }

        @Test
        fun `overwrites existing files in repoDir`() {
            val projectDir = tempDir.resolve("project").apply { mkdirs() }
            val repoDir = tempDir.resolve("repo").apply { mkdirs() }
            projectDir.resolve("README.md").writeText("new version")
            repoDir.resolve("README.md").writeText("old version")

            ProfilePublisher.copyProfileFiles(
                profileFiles = listOf("README.md"),
                projectDir = projectDir,
                from = "",
                repoDir = repoDir,
                logger = logger
            )

            assertThat(repoDir.resolve("README.md")).exists().hasContent("new version")
        }

        @Test
        fun `throws when profileFiles list is empty`() {
            val projectDir = tempDir.resolve("project").apply { mkdirs() }
            val repoDir = tempDir.resolve("repo").apply { mkdirs() }

            assertThatThrownBy {
                ProfilePublisher.copyProfileFiles(
                    profileFiles = emptyList(),
                    projectDir = projectDir,
                    from = "",
                    repoDir = repoDir,
                    logger = logger
                )
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("No profile files specified")
        }

        @Test
        fun `throws when from directory does not exist`() {
            val projectDir = tempDir.resolve("project").apply { mkdirs() }
            val repoDir = tempDir.resolve("repo").apply { mkdirs() }

            assertThatThrownBy {
                ProfilePublisher.copyProfileFiles(
                    profileFiles = listOf("README.md"),
                    projectDir = projectDir,
                    from = "nonexistent",
                    repoDir = repoDir,
                    logger = logger
                )
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("does not exist")
        }

        @Test
        fun `throws when from path is a file not a directory`() {
            val projectDir = tempDir.resolve("project").apply { mkdirs() }
            val repoDir = tempDir.resolve("repo").apply { mkdirs() }
            projectDir.resolve("notadir").writeText("i am a file")

            assertThatThrownBy {
                ProfilePublisher.copyProfileFiles(
                    profileFiles = listOf("README.md"),
                    projectDir = projectDir,
                    from = "notadir",
                    repoDir = repoDir,
                    logger = logger
                )
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("not a directory")
        }

        @Test
        fun `throws when profile file does not exist in source`() {
            val projectDir = tempDir.resolve("project").apply { mkdirs() }
            val repoDir = tempDir.resolve("repo").apply { mkdirs() }

            assertThatThrownBy {
                ProfilePublisher.copyProfileFiles(
                    profileFiles = listOf("MISSING.md"),
                    projectDir = projectDir,
                    from = "",
                    repoDir = repoDir,
                    logger = logger
                )
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Profile file not found")
        }

        @Test
        fun `throws when forbidden files are in the profile list`() {
            val projectDir = tempDir.resolve("project").apply { mkdirs() }
            val repoDir = tempDir.resolve("repo").apply { mkdirs() }
            projectDir.resolve("README.adoc").writeText("forbidden")

            assertThatThrownBy {
                ProfilePublisher.copyProfileFiles(
                    profileFiles = listOf("README.adoc"),
                    projectDir = projectDir,
                    from = "",
                    repoDir = repoDir,
                    logger = logger
                )
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Forbidden profile files")
        }

        @Test
        fun `throws when forbidden README_fr dot adoc is in the profile list`() {
            val projectDir = tempDir.resolve("project").apply { mkdirs() }
            val repoDir = tempDir.resolve("repo").apply { mkdirs() }
            projectDir.resolve("README_fr.adoc").writeText("forbidden")

            assertThatThrownBy {
                ProfilePublisher.copyProfileFiles(
                    profileFiles = listOf("README_fr.adoc"),
                    projectDir = projectDir,
                    from = "",
                    repoDir = repoDir,
                    logger = logger
                )
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Forbidden profile files")
        }
    }
}
