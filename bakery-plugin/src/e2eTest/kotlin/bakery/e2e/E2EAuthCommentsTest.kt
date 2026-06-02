package bakery.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * E2E tests for auth-header and comments components — BKY-JB-9 Phase B.
 *
 * Run with: ./gradlew e2eTest
 * Prerequisite: npx playwright install chromium
 */
@Tag("e2e")
@DisplayName("E2E - Firebase Auth & Comments")
class E2EAuthCommentsTest : E2ETestBase() {

    @Test
    @DisplayName("Auth: Login button is visible when Firebase Auth is configured")
    fun `login button is visible when firebase auth is configured`() {
        val path = serveHtml(
            "auth-header",
            mapOf(
                "firebaseAuthApiKey" to "AIzaSyTest123",
                "firebaseAuthDomain" to "my-project.firebaseapp.com"
            )
        )

        val page = navigateTo(path)

        // Verify auth button is present (id=auth-btn)
        assertThat(page.locator("#auth-btn").count()).isGreaterThan(0)
        // Verify the label "Connexion" text exists (id=auth-label)
        assertThat(page.locator("#auth-label").textContent()).isEqualTo("Connexion")

        // No Thymeleaf guards in rendered output
        assertThat(page.content()).doesNotContain("th:if")
    }

    @Test
    @DisplayName("Auth: No login button when Firebase Auth is not configured")
    fun `no login button when firebase auth is not configured`() {
        val path = serveHtml("auth-header")

        val page = navigateTo(path)

        // Verify auth button is NOT rendered
        assertThat(page.locator("#auth-btn").count()).isEqualTo(0)
    }

    @Test
    @DisplayName("Comments: Section is visible when comments are enabled")
    fun `comments section is visible when enabled`() {
        val path = serveHtml(
            "comments",
            mapOf(
                "commentsEnabled" to "true",
                "commentsCollection" to "blog-comments",
                "content" to mapOf("uri" to "/blog/my-post")
            )
        )

        val page = navigateTo(path)

        // Verify comments section is rendered (class=comments-section)
        assertThat(page.locator(".comments-section").count()).isGreaterThan(0)
        // Verify comment form elements (using id selectors)
        assertThat(page.locator("#comment-text").count()).isGreaterThan(0)
        assertThat(page.locator("#comment-submit").count()).isGreaterThan(0)
    }

    @Test
    @DisplayName("Comments: No section when comments are disabled")
    fun `no comments section when disabled`() {
        val path = serveHtml(
            "comments",
            mapOf("commentsEnabled" to "false")
        )

        val page = navigateTo(path)

        // Verify comments section is NOT rendered
        assertThat(page.locator(".comments-section").count()).isEqualTo(0)
        assertThat(page.locator("#comment-text").count()).isEqualTo(0)
    }
}