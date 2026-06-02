package bakery.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Sanity test for Playwright availability — BKY-JB-9 Phase B.
 *
 * These tests verify that Playwright is correctly configured
 * and that Chromium can be launched in headless mode.
 *
 * Run with: ./gradlew e2eTest
 * Chromium is installed automatically via the installPlaywright task.
 */
@Tag("e2e")
@DisplayName("Playwright E2E - Sanity")
class PlaywrightSanityTest {

    @Test
    @DisplayName("Playwright library is on classpath and factory creates successfully")
    fun `playwright library is on classpath and factory creates successfully`() {
        com.microsoft.playwright.Playwright.create().use { pw ->
            assertThat(pw).isNotNull
            assertThat(pw.chromium()).isNotNull
        }
    }

    @Test
    @DisplayName("Chromium launches in headless mode when installed")
    fun `chromium launches in headless mode when installed`() {
        assumeTrue(
            PlaywrightHelper.isAvailable(),
            "Chromium is not available — run ./gradlew installPlaywright"
        )

        com.microsoft.playwright.Playwright.create().use { playwright ->
            playwright.chromium().launch(
                com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true)
            ).use { browser ->
                val page = browser.newPage()
                // data URI with HTML title element
                page.navigate("data:text/html,<html><head><title>Bakery E2E Test</title></head><body><h1>Test</h1></body></html>")
                assertThat(page.title()).isEqualTo("Bakery E2E Test")
                page.close()
            }
        }
    }
}