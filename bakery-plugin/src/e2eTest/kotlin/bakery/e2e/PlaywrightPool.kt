package bakery.e2e

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Playwright

/**
 * Singleton JVM-wide pool for Playwright + Browser instances.
 *
 * Mutualizes heavy object creation across all E2E test classes running
 * in the same JVM. With forkEvery=1 removed, all test classes share
 * a single Playwright.create() and chromium().launch() call.
 *
 * Lifecycle:
 * - [getOrCreate]: lazily initializes Playwright + Browser on first call
 * - [shutdown]: closes Browser + Playwright (call from @AfterAll or JVM shutdown hook)
 *
 * Thread-safe via synchronized blocks.
 */
object PlaywrightPool {

    @Volatile
    private var playwright: Playwright? = null

    @Volatile
    private var browser: Browser? = null

    init {
        Runtime.getRuntime().addShutdownHook(Thread { shutdown() })
    }

    /**
     * Returns the shared Browser instance, creating Playwright + Chromium
     * on first call. Subsequent calls reuse the same instances.
     */
    @Synchronized
    fun getOrCreate(): Browser {
        if (browser != null) return browser!!

        playwright = Playwright.create()
        browser = playwright!!.chromium().launch(
            com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true)
        )
        return browser!!
    }

    /**
     * Checks if Playwright + Chromium are available without keeping
     * the instances alive. Used for skip-if-missing assumptions.
     */
    fun isAvailable(): Boolean = try {
        Playwright.create().use { pw ->
            pw.chromium().launch(
                com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true)
            ).use { browser ->
                browser.newPage().use { page ->
                    page.navigate("data:text/html,<h1>test</h1>")
                }
            }
        }
        true
    } catch (_: Throwable) {
        false
    }

    /**
     * Closes the shared Browser + Playwright. Safe to call multiple times.
     */
    @Synchronized
    fun shutdown() {
        browser?.close()
        browser = null
        playwright?.close()
        playwright = null
    }
}