package bakery.langswitch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.StringTemplateResolver

/**
 * BKY-LANG-NAV-2 — the D3 anti split-brain proof at the renderer level.
 *
 * [LangSwitchThymeleafRenderer] is the actual host that `injectLangSwitch` wires
 * into `menu.thyme`. This test takes its *real* output, evaluates it through the
 * Thymeleaf engine per page, and asserts that each `data-lang` anchor resolves
 * to [LangSwitchPath.resolveSamePage] — the same rule replayed by the JS host
 * (NAV-3) against the shared vectors. A divergence turns one side red.
 */
class LangSwitchRendererEvaluationTest {
    private val labels = mapOf("fr" to "Fran\u00e7ais", "en" to "English", "ar" to "\u0627\u0644\u0639\u0631\u0628\u064a\u0629")

    private val engine =
        TemplateEngine().apply {
            setTemplateResolver(
                StringTemplateResolver().apply {
                    templateMode = TemplateMode.HTML
                    isCacheable = false
                },
            )
        }

    private fun render(
        supported: List<String>,
        default: String,
        current: String,
        path: String,
    ): String {
        val links = LangSwitchMenu(supported, default, current, path).generateLinks()
        return LangSwitchThymeleafRenderer(labels).render(links)
    }

    private fun evaluate(
        fragment: String,
        currentPageUri: String,
        currentLang: String,
        defaultLang: String,
    ): Map<String, String> {
        val normalised = currentPageUri.trim().trimStart('/')
        val withinTree =
            if (currentLang == defaultLang) normalised else normalised.removePrefix("$currentLang/")
        val dir = withinTree.substringBeforeLast('/', missingDelimiterValue = "")
        val rootpath = if (dir.isEmpty()) "" else "../".repeat(dir.split('/').count { it.isNotEmpty() })

        val context = Context()
        context.setVariable("content", mapOf("rootpath" to rootpath, "uri" to withinTree))
        val rendered = engine.process(fragment, context)

        return Regex("""data-lang="([a-z]{2})"[^>]*""").findAll(rendered).associate { match ->
            val lang = match.groupValues[1]
            val anchor = rendered.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
            val href = anchor.substringAfter("href=\"").substringBefore("\"")
            lang to href
        }
    }

    @Test
    fun `nested default page to non-default renders the translation of the same page`() {
        val fragment = render(listOf("fr", "en"), "fr", "fr", "")
        val hrefs = evaluate(fragment, "blog/foo.html", "fr", "fr")
        assertEquals("../en/blog/foo.html", hrefs["en"])
        assertEquals(
            LangSwitchPath.resolveSamePage("blog/foo.html", "fr", "en", "fr"),
            hrefs["en"],
        )
    }

    @Test
    fun `nested non-default page back to default renders the translation of the same page`() {
        val fragment = render(listOf("fr", "en"), "fr", "en", "en/")
        val hrefs = evaluate(fragment, "en/blog/foo.html", "en", "fr")
        assertEquals("../../blog/foo.html", hrefs["fr"])
        assertEquals(
            LangSwitchPath.resolveSamePage("en/blog/foo.html", "en", "fr", "fr"),
            hrefs["fr"],
        )
    }

    @Test
    fun `non-default page to another non-default keeps the page and matches the pure rule`() {
        val fragment = render(listOf("fr", "en", "ar"), "fr", "en", "en/")
        val hrefs = evaluate(fragment, "en/blog/foo.html", "en", "fr")
        assertEquals("../../ar/blog/foo.html", hrefs["ar"])
        assertEquals(
            LangSwitchPath.resolveSamePage("en/blog/foo.html", "en", "ar", "fr"),
            hrefs["ar"],
        )
    }

    @Test
    fun `self link evaluates to the current file name only`() {
        val fragment = render(listOf("fr", "en"), "fr", "en", "en/")
        val hrefs = evaluate(fragment, "en/blog/foo.html", "en", "fr")
        assertEquals("foo.html", hrefs["en"])
    }
}
