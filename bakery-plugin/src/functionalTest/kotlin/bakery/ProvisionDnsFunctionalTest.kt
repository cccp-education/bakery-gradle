package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-DNS-4 — Functional tests for provisionDns task + wiring.
 *
 * CI has no OVH credentials → the resolver degrades to NoOpDnsProvider
 * (pattern DnsProviderFactory), so the reconciliation is a guaranteed
 * dry-run no-op that still produces the console report.
 */
class ProvisionDnsFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `provisionDns succeeds and reports the dry-run plan when dns section is present`() {
        createProjectWithDnsSection()

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("provisionDns")
                .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(result.output).contains("[dns]")
        assertThat(result.output).contains("dry-run")
        assertThat(result.output).contains("talaria.school")
        assertThat(result.output).contains("www")
    }

    @Test
    fun `provisionDns task is not registered when dns section is absent`() {
        createProjectWithoutDnsSection()

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("tasks", "--group", "deploy")
                .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(result.output).doesNotContain("provisionDns")
    }

    @Test
    fun `deploySite depends on provisionDns when dns section is present`() {
        createProjectWithDnsSection()

        // --dry-run: nothing executes, but the task graph is resolved and
        // listed. provisionDns appears in the graph only if deploySite
        // depends on it (wiring proof, no git push side effect).
        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("deploySite", "--dry-run")
                .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(result.output).contains(":provisionDns")
    }

    private fun createProjectWithDnsSection() {
        createProjectBase()
        projectDir.resolve("site.yml").writeText(
            """
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
            dns:
              provider: "ovh"
              domain: "talaria.school"
              dryRun: true
              records:
                - type: "A"
                  name: "@"
                  value: "185.199.108.153"
                - type: "CNAME"
                  name: "www"
                  value: "pages-content.github.io."
            """.trimIndent(),
        )
    }

    private fun createProjectWithoutDnsSection() {
        createProjectBase()
        projectDir.resolve("site.yml").writeText(
            """
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
            """.trimIndent(),
        )
    }

    private fun createProjectBase() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "provision-dns-test"
            """.trimIndent(),
        )

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("education.cccp.bakery") }
            bakery {
                configPath = file("site.yml").absolutePath
            }
            """.trimIndent(),
        )

        projectDir.resolve("site").mkdirs()
        projectDir.resolve("maquette").mkdirs()
    }
}