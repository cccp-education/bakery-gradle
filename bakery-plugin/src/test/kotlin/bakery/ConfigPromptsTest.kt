package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.junit.jupiter.api.*
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

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
    // saveConfiguration — no-op
    // =========================================================================

    @Nested
    inner class SaveConfiguration {

        @Test
        fun `is a no-op without exceptions`() {
            val project = mockProjectNoProps()
            val config = mock<SiteConfiguration>()
            assertDoesNotThrow {
                with(ConfigPrompts) { project.saveConfiguration(config, false) }
            }
        }

        @Test
        fun `is a no-op with gradleProperties enabled`() {
            val project = mockProjectNoProps()
            val config = mock<SiteConfiguration>()
            assertDoesNotThrow {
                with(ConfigPrompts) { project.saveConfiguration(config, true) }
            }
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
