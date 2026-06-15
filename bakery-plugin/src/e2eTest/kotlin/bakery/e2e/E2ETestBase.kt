package bakery.e2e

import bakery.ThymeleafRenderingTestFactory
import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.nio.file.Path

/**
 * Base class for E2E tests using Playwright — BKY-JB-9 Phase B.
 *
 * Provides:
 * - A local HTTP server serving baked HTML pages from a temp directory
 * - Playwright Browser lifecycle management
 * - Thymeleaf rendering via the existing ThymeleafRenderingTestFactory
 *
 * Usage:
 * ```kotlin
 * @Tag("e2e")
 * class MyE2ETest : E2ETestBase() {
 *     @Test
 *     fun `analytics script is visible`() {
 *         serveHtml("analytics", mapOf("analyticsProvider" to "plausible", ...))
 *         val page = navigateTo("/analytics")
 *         assertThat(page.locator("script[data-domain]").count()).isGreaterThan(0)
 *     }
 * }
 * ```
 */
@Tag("e2e")
abstract class E2ETestBase {

    companion object {
        private const val PORT = 18763

        private var playwright: Playwright? = null
        private var browser: Browser? = null
        private var httpServer: HttpServer? = null

        @TempDir
        @JvmField
        var tempDir: Path? = null

        @JvmStatic
        @BeforeAll
        fun setUpAll() {
            if (!PlaywrightHelper.isAvailable()) {
                org.junit.jupiter.api.Assumptions.assumeTrue(
                    false,
                    "Chromium not available — run ./gradlew installPlaywright"
                )
            }
            playwright = Playwright.create()
            browser = playwright!!.chromium().launch(
                com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true)
            )
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            browser?.close()
            playwright?.close()
        }

        /**
         * Starts a local HTTP server serving files from [tempDir]/www/.
         * Call serveHtml() first to create files, then navigate.
         */
        fun startServer() {
            stopServer()
            val wwwDir = tempDir!!.resolve("www").toFile()
            wwwDir.mkdirs()
            httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", PORT), 0).apply {
                createContext("/") { exchange ->
                    val requestedPath = exchange.requestURI.path.removePrefix("/")
                    val file = if (requestedPath.isEmpty()) {
                        wwwDir.resolve("index.html")
                    } else {
                        wwwDir.resolve(requestedPath)
                    }
                    if (file.exists() && file.isFile) {
                        val contentType = when (file.extension) {
                            "html" -> "text/html"
                            "css" -> "text/css"
                            "js" -> "application/javascript"
                            else -> "application/octet-stream"
                        }
                        val bytes = file.readBytes()
                        exchange.responseHeaders.set("Content-Type", contentType)
                        exchange.sendResponseHeaders(200, bytes.size.toLong())
                        exchange.responseBody.write(bytes)
                        exchange.responseBody.close()
                    } else {
                        val message = "Not Found: $requestedPath"
                        exchange.sendResponseHeaders(404, message.toByteArray().size.toLong())
                        exchange.responseBody.write(message.toByteArray())
                        exchange.responseBody.close()
                    }
                }
                start()
            }
        }

        fun stopServer() {
            httpServer?.stop(0)
            httpServer = null
        }

        fun baseUrl(): String = "http://127.0.0.1:$PORT"
    }

    protected var page: Page? = null

    @BeforeEach
    fun setUp() {
        startServer()
        page = browser!!.newPage()
    }

    @AfterEach
    fun tearDown() {
        page?.close()
        page = null
        stopServer()
    }

    /**
     * Renders a Thymeleaf template with the given context variables,
     * writes the HTML to the temp www directory, and returns the URL path.
     *
     * @param templateName template name without .thyme extension (e.g., "analytics-script")
     * @param context variables for Thymeleaf rendering
     * @param outputFileName optional custom file name (defaults to templateName.html)
     * @return the URL path to navigate to
     */
    protected fun serveHtml(
        templateName: String,
        context: Map<String, Any> = emptyMap(),
        outputFileName: String = "$templateName.html",
        language: String = "fr"
    ): String {
        val factory = ThymeleafRenderingTestFactory()
        val html = factory.render(templateName, context, language)

        val wwwDir = tempDir!!.resolve("www").toFile()
        wwwDir.mkdirs()
        wwwDir.resolve(outputFileName).writeText(html)

        // Also create a minimal index.html that links to the component
        wwwDir.resolve("index.html").writeText("""
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"><title>Bakery E2E Test</title></head>
            <body><h1>Bakery E2E Test Server</h1></body></html>
        """.trimIndent())

        return "/$outputFileName"
    }

    /**
     * Navigates the browser to a path on the local server.
     */
    protected fun navigateTo(path: String): Page {
        page!!.navigate("${baseUrl()}$path")
        return page!!
    }
}