package bakery.e2e

import com.microsoft.playwright.Playwright

/**
 * Helper for Playwright E2E tests — BKY-JB-9 Phase B.
 *
 * Provides browser lifecycle management and availability checks.
 * Pattern inspired by capsule-gradle's PlaywrightCapture.
 */
object PlaywrightHelper {

    /**
     * Checks if Playwright + Chromium are available on the system.
     * Returns true if a headless Chromium browser can be launched.
     * Catches all Throwables (Exception + Error) for robustness.
     */
    fun isAvailable(): Boolean = try {
        Playwright.create().use { playwright ->
            playwright.chromium().launch(
                com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true)
            ).use { browser ->
                val page = browser.newPage()
                page.navigate("data:text/html,<h1>test</h1>")
                page.close()
            }
        }
        true
    } catch (_: Throwable) {
        false
    }
}