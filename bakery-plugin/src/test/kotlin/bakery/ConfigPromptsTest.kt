package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@Execution(ExecutionMode.SAME_THREAD)
class ConfigPromptsTest {

    private fun mockProjectWithProperty(
        cliProperty: String,
        value: String?,
        hasProp: Boolean = value != null
    ): Project = mock {
        whenever(it.hasProperty(cliProperty)).thenReturn(hasProp)
        if (value != null) whenever(it.property(cliProperty)).thenReturn(value)
    }

    private fun mockProjectNoProps(): Project = mock {
        whenever(it.hasProperty(org.mockito.kotlin.any())).thenReturn(false)
        whenever(it.logger).thenReturn(mock())
    }

    // =========================================================================
    // CLI property resolution
    // =========================================================================

    @Nested
    inner class CliPropertyResolution {

        @Test
        fun `resolves from -P property when present and non-blank`() {
            val project = mockProjectWithProperty("githubRepo", "my-repo-value")
            val result = with(ConfigPrompts) { project.getOrPrompt("GitHub Repo", "githubRepo") }
            assertThat(result).isEqualTo("my-repo-value")
        }

        @Test
        fun `skips blank -P property and falls to default`() {
            val project = mockProjectWithProperty("githubRepo", "   ")
            val result = with(ConfigPrompts) {
                project.getOrPrompt("GitHub Repo", "githubRepo", default = "fallback")
            }
            assertThat(result).isEqualTo("fallback")
        }

        @Test
        fun `not called when -P is not present`() {
            val project = mock<Project> {
                whenever(it.hasProperty("githubRepo")).thenReturn(false)
            }
            val result = with(ConfigPrompts) {
                project.getOrPrompt("GitHub Repo", "githubRepo", default = "from-default")
            }
            assertThat(result).isEqualTo("from-default")
        }

        @Test
        fun `-P has priority over default`() {
            val project = mockProjectWithProperty("githubRepo", "cli-value")
            val result = with(ConfigPrompts) {
                project.getOrPrompt("GitHub Repo", "githubRepo", default = "default-val")
            }
            assertThat(result).isEqualTo("cli-value")
        }
    }

    // =========================================================================
    // Default value
    // =========================================================================

    @Nested
    inner class DefaultResolution {

        @Test
        fun `resolves from default when no other source available`() {
            val project = mockProjectNoProps()
            val result = with(ConfigPrompts) {
                project.getOrPrompt("Name", "name", default = "default-name")
            }
            assertThat(result).isEqualTo("default-name")
        }

        @Test
        fun `default works with sensitive flag`() {
            val project = mockProjectNoProps()
            val result = with(ConfigPrompts) {
                project.getOrPrompt("GitHub Token", "githubToken", sensitive = true, default = "my-token-value")
            }
            assertThat(result).isEqualTo("my-token-value")
        }

        @Test
        fun `default works with example text`() {
            val project = mockProjectNoProps()
            val result = with(ConfigPrompts) {
                project.getOrPrompt("Repo", "repo", example = "owner/repo", default = "default/repo")
            }
            assertThat(result).isEqualTo("default/repo")
        }
    }

    // =========================================================================
    // saveConfiguration — persister les valeurs dans site.yml
    // =========================================================================

    @Nested
    inner class SaveConfiguration {

        @TempDir
        lateinit var tempDir: File

        private fun writeSiteYml(content: String): File {
            val file = File(tempDir, "site.yml")
            file.writeText(content)
            return file
        }

        private fun readSiteYml(): String =
            File(tempDir, "site.yml").readText()

        @Test
        fun `saves push credentials into existing site yml`() {
            val siteYml = writeSiteYml("""
                |bake:
                |  srcPath: src
                |  destDirPath: build
                |pushPage:
                |  from: build
                |  to: gh-pages
                |  repo:
                |    name: origin
                |    repository: https://github.com/old/repo.git
            """.trimMargin())
            val site = FileSystemManager.yamlMapper.readValue(siteYml, SiteConfiguration::class.java)

            with(ConfigPrompts) {
                tempDir.saveConfiguration(site, siteYml, "newuser", "https://github.com/new/repo.git", "ghp_token123")
            }

            // Re-read to verify round-trip
            val updated = FileSystemManager.yamlMapper.readValue(siteYml, SiteConfiguration::class.java)
            assertThat(updated.pushPage.repo.credentials.username).isEqualTo("newuser")
            assertThat(updated.pushPage.repo.repository).isEqualTo("https://github.com/new/repo.git")
            assertThat(updated.pushPage.repo.credentials.password).isEqualTo("ghp_token123")
        }

        @Test
        fun `creates pushPage section if missing in site yml`() {
            val siteYml = writeSiteYml("""
                |bake:
                |  srcPath: src
                |  destDirPath: build
            """.trimMargin())
            val site = FileSystemManager.yamlMapper.readValue(siteYml, SiteConfiguration::class.java)

            with(ConfigPrompts) {
                tempDir.saveConfiguration(site, siteYml, "testuser", "https://github.com/test/repo.git", "ghp_abc")
            }

            val updated = FileSystemManager.yamlMapper.readValue(siteYml, SiteConfiguration::class.java)
            assertThat(updated.pushPage.repo.credentials.username).isEqualTo("testuser")
            assertThat(updated.pushPage.repo.repository).isEqualTo("https://github.com/test/repo.git")
            assertThat(updated.pushPage.repo.credentials.password).isEqualTo("ghp_abc")
        }

        @Test
        fun `preserves existing bake configuration when saving credentials`() {
            val siteYml = writeSiteYml("""
                |bake:
                |  srcPath: content
                |  destDirPath: output
                |  cname: example.com
                |pushPage:
                |  from: build
                |  to: gh-pages
                |  repo:
                |    name: origin
                |    repository: https://github.com/old/repo.git
            """.trimMargin())
            val site = FileSystemManager.yamlMapper.readValue(siteYml, SiteConfiguration::class.java)

            with(ConfigPrompts) {
                tempDir.saveConfiguration(site, siteYml, "updateduser", "https://github.com/new/repo.git", "ghp_new")
            }

            val updated = FileSystemManager.yamlMapper.readValue(siteYml, SiteConfiguration::class.java)
            assertThat(updated.bake.srcPath).isEqualTo("content")
            assertThat(updated.bake.destDirPath).isEqualTo("output")
            assertThat(updated.bake.cname).isEqualTo("example.com")
            assertThat(updated.pushPage.repo.credentials.username).isEqualTo("updateduser")
        }

        @Test
        fun `does not overwrite site yml when all credentials are empty`() {
            val siteYml = writeSiteYml("""
                |bake:
                |  srcPath: src
                |pushPage:
                |  repo:
                |    repository: https://github.com/existing/repo.git
            """.trimMargin())
            val originalContent = siteYml.readText()
            val site = FileSystemManager.yamlMapper.readValue(siteYml, SiteConfiguration::class.java)

            with(ConfigPrompts) {
                tempDir.saveConfiguration(site, siteYml, "", "", "")
            }

            // File should be unchanged when no credentials provided
            assertThat(File(tempDir, "site.yml").readText()).isEqualTo(originalContent)
        }
    }

    // =========================================================================
    // camelCase → UPPER_SNAKE conversion (pure logic, no env needed)
    // =========================================================================

    @Nested
    inner class CamelCaseToUpperSnake {

        private fun camelToUpperSnake(input: String): String =
            input.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()

        @Test
        fun `single word produces uppercase`() {
            assertThat(camelToUpperSnake("repo")).isEqualTo("REPO")
        }

        @Test
        fun `two camelCase words produce upper snake`() {
            assertThat(camelToUpperSnake("githubRepo")).isEqualTo("GITHUB_REPO")
        }

        @Test
        fun `three camelCase words produce correct env key`() {
            assertThat(camelToUpperSnake("githubRepoName")).isEqualTo("GITHUB_REPO_NAME")
        }

        @Test
        fun `acronym-style PascalCase produces correct output`() {
            assertThat(camelToUpperSnake("apiKey")).isEqualTo("API_KEY")
        }
    }
}
