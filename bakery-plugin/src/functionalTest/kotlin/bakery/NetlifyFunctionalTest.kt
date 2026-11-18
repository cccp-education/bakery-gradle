package bakery

import bakery.deploy.DeployPayload
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

/**
 * BKY-NETLIFY (US-5/US-6) — the task is conditional, the token is required,
 * the dry-run is silent, the skip needs no token and the upload reaches the
 * API seam (loopback stub, never the real Netlify).
 */
class NetlifyFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    private fun runner(vararg arguments: String): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(*arguments)

    private fun createProject(
        netlifyYaml: String,
        payload: Boolean = true,
    ) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "netlify-test"
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
        val baseYaml =
            """
            bake:
              srcPath: site
              destDirPath: site
            pushMaquette:
              from: maquette
              to: cvs
            """.trimIndent()
        projectDir.resolve("site.yml").writeText("$baseYaml\n$netlifyYaml\n")
        if (payload) {
            projectDir.resolve("build/site/en").mkdirs()
            projectDir.resolve("build/site/index.html").writeText("<html>home</html>")
            projectDir.resolve("build/site/en/index.html").writeText("<html>home en</html>")
            projectDir.resolve("build/site/CNAME").writeText("example.org")
        }
    }

    private val netlifySection =
        """
        netlify:
          siteId: "site-123"
          tokenEnv: NETLIFY_TEST_TOKEN
        """.trimIndent()

    private fun skipBakeArgs(vararg extra: String): Array<String> =
        arrayOf(
            "deployNetlify",
            "-x",
            "bake",
            "-x",
            "bakeVariants",
            "-x",
            "pagefind",
            *extra,
        )

    @Test
    fun `deployNetlify is not registered without a netlify section`() {
        createProject("")

        val result = runner("tasks", "--group", "deploy").build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(result.output).doesNotContain("deployNetlify")
    }

    @Test
    fun `a missing token fails explicitly without uploading`() {
        createProject(netlifySection)

        val result =
            runner(*skipBakeArgs())
                .withEnvironment(System.getenv().filterKeys { it != "NETLIFY_TEST_TOKEN" })
                .buildAndFail()

        assertThat(result.output).contains("Netlify token is empty")
        assertThat(result.output).contains("NETLIFY_TEST_TOKEN")
    }

    @Test
    fun `dry run plans without a token and without a network call`() {
        createProject(
            """
            netlify:
              siteId: "site-123"
              tokenEnv: NETLIFY_TEST_TOKEN
              deploy:
                dryRun: true
            """.trimIndent(),
        )

        val result =
            runner(*skipBakeArgs("-Pnetlify.apiBaseUrl=http://127.0.0.1:1/v1"))
                .build()

        assertThat(result.output).contains("dry-run")
        assertThat(result.output).contains("files=2")
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `an unchanged payload is skipped before the token is read`() {
        createProject(netlifySection)
        val payload = projectDir.resolve("build/site")
        val digest = DeployPayload.digest(DeployPayload.collect(payload, DeployPayload.BASE_EXCLUDES + DeployPayload.NETLIFY_EXCLUDES))
        projectDir.resolve("build/bakery/netlify").mkdirs()
        projectDir.resolve("build/bakery/netlify/last-deploy.json").writeText(
            """{"siteId":"site-123","digest":"$digest","deployId":"dep-previous","deployUrl":"https://x","deployedAt":"now"}""",
        )

        val result = runner(*skipBakeArgs()).build()

        assertThat(result.output).contains("skipped")
        assertThat(result.output).contains("dep-previous")
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `platform files are written only when enabled`() {
        createProject(
            """
            netlify:
              siteId: "site-123"
              toml:
                enabled: true
                publishDir: "public"
                redirects: true
            """.trimIndent(),
        )

        val result = runner("generateNetlifyToml", "-x", "bake").build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        val outputDir = projectDir.resolve("build/bakery/netlify")
        assertThat(outputDir.resolve("netlify.toml")).exists()
        assertThat(outputDir.resolve("netlify.toml").readText()).contains("publish = \"public\"")
        assertThat(outputDir.resolve("_headers")).exists()
        assertThat(outputDir.resolve("_redirects")).exists()
    }

    @Test
    fun `a deployment reaches the API seam and records the deploy`() {
        createProject(netlifySection)
        val calls = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/sites/site-123/deploys") { exchange ->
            calls.incrementAndGet()
            val body = exchange.requestBody.readBytes()
            val response =
                if (body.isEmpty()) {
                    """{"error":"empty"}"""
                } else {
                    """{"id":"dep-1","state":"ready","deploy_ssl_url":"https://dep-1--site.netlify.app"}"""
                }
            val bytes = response.toByteArray()
            exchange.sendResponseHeaders(201, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val baseUrl = "http://127.0.0.1:${server.address.port}/v1"
            val result =
                runner(*skipBakeArgs("-Pnetlify.apiBaseUrl=$baseUrl"))
                    .withEnvironment(System.getenv() + mapOf("NETLIFY_TEST_TOKEN" to "test-token"))
                    .build()

            assertThat(result.output).contains("deployed https://dep-1--site.netlify.app")
            assertThat(result.output).contains("BUILD SUCCESSFUL")
            assertThat(calls.get()).isEqualTo(1)
            val record = projectDir.resolve("build/bakery/netlify/last-deploy.json").readText()
            assertThat(record).contains("dep-1")
            assertThat(record).doesNotContain("test-token")
        } finally {
            server.stop(0)
        }
    }
}
