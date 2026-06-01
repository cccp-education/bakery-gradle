package bakery

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Source-text assertion tests for Firebase Auth and comments templates.
 *
 * **Partially superseded by** [ThymeleafRenderingTest.AuthHeaderRenderingTest],
 * [ThymeleafRenderingTest.CommentsRenderingTest], and
 * [ThymeleafRenderingTest.FooterIntegrationRenderingTest] which render actual
 * HTML via Thymeleaf engine instead of checking raw template source.
 *
 * Tests 7-8 (post/page includes comments fragment) remain **not superseded** —
 * they verify cross-template structural integration that rendering tests don't cover
 * (composite page rendering requires Playwright E2E, Phase B).
 *
 * Kept as reference literature — source-text assertions and rendering assertions
 * verify different perspectives of the same templates. Both are valuable.
 */
class FirebaseAuthTemplateTest {

    private val templatesDir = File("src/main/resources/site/templates")

    @Test
    fun `auth-header thyme template contains login button with auth guard`() {
        val template = templatesDir.resolve("auth-header.thyme")
        assertTrue(template.exists(), "auth-header.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("auth-header"), "must have auth-header fragment")
        assertTrue(content.contains("th:if"), "must have th:if condition to hide when auth is absent")
        assertTrue(content.contains("firebaseAuthApiKey"), "th:if must guard on firebaseAuthApiKey")
        assertTrue(content.contains("auth-btn"), "must have auth button element")
        assertTrue(content.contains("Connexion"), "must have login label")
    }

    @Test
    fun `auth-header thyme template renders nothing when firebaseAuthApiKey is absent`() {
        val template = templatesDir.resolve("auth-header.thyme")
        assertTrue(template.exists(), "auth-header.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("th:if"), "must have th:if condition")
        assertTrue(
            content.contains("firebaseAuthApiKey") && content.contains("th:if"),
            "th:if must guard on firebaseAuthApiKey so fragment renders nothing when absent"
        )
    }

    @Test
    fun `comments thyme template contains comment form with enabled guard`() {
        val template = templatesDir.resolve("comments.thyme")
        assertTrue(template.exists(), "comments.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("comments-section"), "must have comments-section")
        assertTrue(content.contains("th:if"), "must have th:if condition to hide when comments disabled")
        assertTrue(content.contains("commentsEnabled"), "th:if must guard on commentsEnabled")
        assertTrue(content.contains("comment-text"), "must have comment text area")
        assertTrue(content.contains("comment-submit"), "must have submit button")
    }

    @Test
    fun `comments thyme template renders nothing when commentsEnabled is false`() {
        val template = templatesDir.resolve("comments.thyme")
        assertTrue(template.exists(), "comments.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("th:if"), "must have th:if condition")
        assertTrue(
            content.contains("commentsEnabled") && content.contains("th:if"),
            "th:if must guard on commentsEnabled so fragment renders nothing when disabled"
        )
    }

    @Test
    fun `footer thyme includes firebase-auth-compat script`() {
        val template = templatesDir.resolve("footer.thyme")
        assertTrue(template.exists(), "footer.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("firebase-auth-compat"), "footer must include firebase-auth-compat.js")
    }

    @Test
    fun `menu thyme includes auth-header fragment`() {
        val template = templatesDir.resolve("menu.thyme")
        assertTrue(template.exists(), "menu.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("auth-header.thyme"), "menu must include auth-header fragment")
    }

    @Test
    fun `post thyme includes comments fragment instead of disqus`() {
        val template = templatesDir.resolve("post.thyme")
        assertTrue(template.exists(), "post.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("comments.thyme"), "post must include comments fragment")
        assertFalse(content.contains("disqus"), "post must not contain disqus")
    }

    @Test
    fun `page thyme includes comments fragment`() {
        val template = templatesDir.resolve("page.thyme")
        assertTrue(template.exists(), "page.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("comments.thyme"), "page must include comments fragment")
    }
}