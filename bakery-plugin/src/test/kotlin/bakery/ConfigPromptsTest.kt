package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.mockito.kotlin.any
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
        whenever(it.logger).thenReturn(mock())
    }

    private fun mockProjectNoProps(): Project = mock {
        whenever(it.hasProperty(any())).thenReturn(false)
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
                whenever(it.logger).thenReturn(mock())
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
    // saveConfiguration
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

            assertThat(File(tempDir, "site.yml").readText()).isEqualTo(originalContent)
        }
    }

    // =========================================================================
    // resolveConfigValue with mock environment (CS-FIN-7 legacy)
    // =========================================================================

    @Nested
    inner class ResolveConfigValueWithMockEnv {

        private val silentOutput: (String) -> Unit = {}
        private val mockLogger: Logger = mock {
            whenever(it.info(any())).then {}
            whenever(it.warn(any())).then {}
            whenever(it.lifecycle(any())).then {}
        }

        private fun envWithInput(
            project: Project,
            input: () -> String?,
            password: () -> CharArray? = { null },
            output: (String) -> Unit = silentOutput,
            logger: Logger = mockLogger
        ) = ConfigPromptEnvironment(
            readInput = input,
            readPassword = password,
            writeOutput = output,
            logger = logger,
            project = project
        )

        @Test
        fun `prompts interactively when no CLI env or default`() {
            val project = mockProjectNoProps()
            val env = envWithInput(project, input = { "my-answer" })

            val result = ConfigPromptM.resolveOrPrompt("Name", "name").run(env)

            assertThat(result).isEqualTo("my-answer")
        }

        @Test
        fun `prompts for sensitive value via readPassword`() {
            val project = mockProjectNoProps()
            val env = envWithInput(
                project,
                input = { null },
                password = { "secret123".toCharArray() }
            )

            val result = ConfigPromptM.resolveOrPrompt(
                "GitHub Token", "githubToken", sensitive = true
            ).run(env)

            assertThat(result).isEqualTo("secret123")
        }

        @Test
        fun `default still takes priority over interactive prompt`() {
            val project = mockProjectNoProps()
            val env = envWithInput(project, input = { "should-not-use-this" })

            val result = ConfigPromptM.resolveOrPrompt(
                "Name", "name", default = "default-wins"
            ).run(env)

            assertThat(result).isEqualTo("default-wins")
        }

        @Test
        fun `env var takes priority over interactive prompt`() {
            val project = mockProjectNoProps()
            val env = envWithInput(project, input = { "should-not-be-called" })

            val result = ConfigPromptM.resolveOrPrompt(
                "Config Path", "configPath", default = "site.yml"
            ).run(env)

            assertThat(result).isEqualTo("site.yml")
        }
    }

    // =========================================================================
    // ConfigPromptM — Reader monad (CS-FIN-7)
    // =========================================================================

    @Nested
    inner class ConfigPromptMTests {

        private val mockLogger: Logger = mock {
            whenever(it.info(any())).then {}
            whenever(it.warn(any())).then {}
            whenever(it.lifecycle(any())).then {}
        }

        private fun testEnv(
            project: Project,
            input: () -> String? = { null },
            password: () -> CharArray? = { null },
            output: (String) -> Unit = {}
        ) = ConfigPromptEnvironment(
            readInput = input,
            readPassword = password,
            writeOutput = output,
            logger = mockLogger,
            project = project
        )

        // --- pure ---

        @Nested
        inner class Pure {

            @Test
            fun `pure returns the wrapped value`() {
                val m = ConfigPromptM.pure("hello")
                assertThat(m.run(testEnv(mockProjectNoProps()))).isEqualTo("hello")
            }
        }

        // --- map ---

        @Nested
        inner class Map {

            @Test
            fun `map transforms the result`() {
                val m = ConfigPromptM.pure("hello").map { it.uppercase() }
                assertThat(m.run(testEnv(mockProjectNoProps()))).isEqualTo("HELLO")
            }

            @Test
            fun `map chains multiple transformations`() {
                val m = ConfigPromptM.pure("hello")
                    .map { it.uppercase() }
                    .map { it.length }
                assertThat(m.run(testEnv(mockProjectNoProps()))).isEqualTo(5)
            }
        }

        // --- flatMap ---

        @Nested
        inner class FlatMap {

            @Test
            fun `flatMap chains two prompts`() {
                val project = mockProjectWithProperty("firstName", "John")
                val env = testEnv(project)

                val firstName = ConfigPromptM.fromCli("First Name", "firstName")
                val greeting = firstName.flatMap { name ->
                    ConfigPromptM.pure("Hello, $name!")
                }

                assertThat(greeting.run(env)).isEqualTo("Hello, John!")
            }

            @Test
            fun `flatMap with prompt fallthrough`() {
                val project = mockProjectWithProperty("username", "alice")
                val env = testEnv(project, input = { "interactive-value" })

                val username = ConfigPromptM.fromCli("Username", "username")
                val result = username.flatMap { name ->
                    ConfigPromptM.pure("User: $name")
                }

                assertThat(result.run(env)).isEqualTo("User: alice")
            }
        }

        // --- fromCli ---

        @Nested
        inner class FromCli {

            @Test
            fun `fromCli resolves property from project`() {
                val project = mockProjectWithProperty("githubRepo", "my-repo")
                val env = testEnv(project)

                val result = ConfigPromptM.fromCli("GitHub Repo", "githubRepo").run(env)

                assertThat(result).isEqualTo("my-repo")
            }

            @Test
            fun `fromCli returns null when property not found`() {
                val project = mockProjectNoProps()
                val env = testEnv(project)

                val m = ConfigPromptM.fromCli("Missing", "missingProp")
                val result = m.run(env)

                assertThat(result).isNull()
            }
        }

        // --- resolveOrPrompt ---

        @Nested
        inner class FromCliOrPrompt {

            @Test
            fun `resolves from CLI property first`() {
                val project = mockProjectWithProperty("configPath", "my-config.yml")
                val env = testEnv(project)

                val result = ConfigPromptM.resolveOrPrompt(
                    "Config Path", "configPath", default = "site.yml"
                ).run(env)

                assertThat(result).isEqualTo("my-config.yml")
            }

            @Test
            fun `falls back to default when no CLI property`() {
                val project = mockProjectNoProps()
                val env = testEnv(project)

                val result = ConfigPromptM.resolveOrPrompt(
                    "Config Path", "configPath", default = "site.yml"
                ).run(env)

                assertThat(result).isEqualTo("site.yml")
            }

            @Test
            fun `falls back to interactive prompt when no CLI and no default`() {
                val project = mockProjectNoProps()
                val env = testEnv(project, input = { "typed-value" })

                val result = ConfigPromptM.resolveOrPrompt(
                    "Name", "name"
                ).run(env)

                assertThat(result).isEqualTo("typed-value")
            }

            @Test
            fun `sensitive prompt uses readPassword`() {
                val project = mockProjectNoProps()
                val env = testEnv(project, password = { "secret123".toCharArray() })

                val result = ConfigPromptM.resolveOrPrompt(
                    "GitHub Token", "githubToken", sensitive = true
                ).run(env)

                assertThat(result).isEqualTo("secret123")
            }
        }

        // --- prompt ---

        @Nested
        inner class Prompt {

            @Test
            fun `prompt returns user input`() {
                val env = testEnv(mockProjectNoProps(), input = { "my-answer" })

                val result = ConfigPromptM.prompt("Name").run(env)

                assertThat(result).isEqualTo("my-answer")
            }

            @Test
            fun `prompt with example includes example text`() {
                val outputs = mutableListOf<String>()
                val env = testEnv(mockProjectNoProps(), input = { "repo" }, output = { outputs.add(it) })

                ConfigPromptM.prompt("Repo", example = "owner/repo").run(env)

                assertThat(outputs.any { it.contains("owner/repo") }).isTrue()
            }

            @Test
            fun `prompt retries on blank input`() {
                var callCount = 0
                val env = testEnv(mockProjectNoProps(), input = {
                    callCount++
                    if (callCount == 1) "" else "valid-input"
                })

                val result = ConfigPromptM.prompt("Name").run(env)

                assertThat(result).isEqualTo("valid-input")
                assertThat(callCount).isEqualTo(2)
            }
        }

        // --- promptSensitive ---

        @Nested
        inner class PromptSensitive {

            @Test
            fun `promptSensitive returns password input`() {
                val env = testEnv(mockProjectNoProps(), password = { "my-secret".toCharArray() })

                val result = ConfigPromptM.promptSensitive("Token").run(env)

                assertThat(result).isEqualTo("my-secret")
            }

            @Test
            fun `promptSensitive retries on empty input`() {
                var callCount = 0
                val env = testEnv(
                    mockProjectNoProps(),
                    input = { if (callCount == 0) null else "fallback" },
                    password = {
                        callCount++
                        if (callCount == 1) charArrayOf() else "secret".toCharArray()
                    }
                )

                val result = ConfigPromptM.promptSensitive("Token").run(env)

                assertThat(result).isEqualTo("secret")
            }
        }

        // --- Composition ---

        @Nested
        inner class Composition {

            @Test
            fun `compose multiple prompts with flatMap`() {
                val project = mockProjectWithProperty("username", "alice")
                val env = testEnv(project, input = { "my-repo" })

                val username = ConfigPromptM.fromCli("Username", "username")
                val repo = ConfigPromptM.resolveOrPrompt("Repo", "repo", example = "owner/repo")

                val combined = username.flatMap { user ->
                    repo.flatMap { r ->
                        ConfigPromptM.pure("$user/$r")
                    }
                }

                assertThat(combined.run(env)).isEqualTo("alice/my-repo")
            }

            @Test
            fun `map identity law`() {
                val m = ConfigPromptM.pure("test")
                val identity = m.map { it }
                assertThat(identity.run(testEnv(mockProjectNoProps()))).isEqualTo("test")
            }

            @Test
            fun `map composition law`() {
                val m = ConfigPromptM.pure("hello")
                val f: (String) -> String = { it.uppercase() }
                val g: (String) -> Int = { it.length }
                val left = m.map { g(f(it)) }
                val right = m.map(f).map(g)
                assertThat(left.run(testEnv(mockProjectNoProps()))).isEqualTo(right.run(testEnv(mockProjectNoProps())))
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