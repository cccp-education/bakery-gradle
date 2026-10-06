package bakery.i18n

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * CHE-I18N-UNIFY — a translated Thymeleaf template must stay structurally
 * parseable. A real LLM run ate the closing `">` of a `th:if` attribute on the
 * `hi` variant (`post.thyme:53`), which only failed later at bake time
 * (`RenderingException`), silently shipping a broken page.
 *
 * This guard is cheap and model-agnostic: it catches an unclosed tag attribute
 * (a `<` opening a tag whose `>` never arrives before the line ends) so the
 * corrupted output is rejected instead of deployed.
 */
class TemplateStructureGuardTest {
    @Test
    fun `a well-formed template passes`() {
        val template =
            """
            <section class="mt-5 pt-4 border-top" th:if="${'$'}{#lists.size(published_posts) > 1}">
                <h2>Related articles</h2>
            </section>
            """.trimIndent()

        assertTrue(TemplateStructureGuard.isWellFormed(template))
    }

    @Test
    fun `an attribute whose closing quote and angle bracket were eaten fails`() {
        val template = """<section class="mt-5" th:if="${'$'}{#lists.size(published_posts) > 1}""""

        assertFalse(
            TemplateStructureGuard.isWellFormed(template),
            "The unclosed th:if attribute must be reported as malformed",
        )
    }

    @Test
    fun `a plain text with a comparison operator is not a false positive`() {
        val template = "<p>If x &gt; 1 then the section renders.</p>"

        assertTrue(TemplateStructureGuard.isWellFormed(template))
    }

    @Test
    fun `a script body with angle brackets is ignored`() {
        val template = "<script>if (a > 1) { b = 2; }</script>"

        assertTrue(TemplateStructureGuard.isWellFormed(template))
    }

    @Test
    fun `a tag left open at the end of the document fails`() {
        assertFalse(TemplateStructureGuard.isWellFormed("<div class=\"x\" th:if=\"${'$'}{y}"))

        assertTrue(TemplateStructureGuard.isWellFormed("<div class=\"x\" th:if=\"${'$'}{y}\">"))
    }

    @Test
    fun `a tag that legitimately opens on one line and closes later passes`() {
        // S-250 — the real cheroliv.com corpus carries multi-line tags
        // (`header.thyme` `th:block th:with="…`, `menu.thyme`, `index.thyme`).
        // The old line-local rule rejected 13 of the 20 reference templates and
        // blocked id/ko/sr/fa translation. The skeleton must be tracked across
        // the whole document.
        val template =
            """
            <th:block th:with="canonUri=(${'$'}{canonOverride != null} ? ${'$'}{canonOverride} : ''),
                               pageDesc=${'$'}{#strings.defaultString(content.summary)}">
                <link rel="canonical" th:href="${'$'}{config.site_host}"/>
            </th:block>
            """.trimIndent()

        assertTrue(TemplateStructureGuard.isWellFormed(template))
    }

    @Test
    fun `an unclosed comment at the end of the document fails`() {
        assertFalse(TemplateStructureGuard.isWellFormed("<!-- a comment <div> that never closes"))

        assertTrue(TemplateStructureGuard.isWellFormed("<!-- a comment <div> -->\n<p>ok</p>"))
    }
}
