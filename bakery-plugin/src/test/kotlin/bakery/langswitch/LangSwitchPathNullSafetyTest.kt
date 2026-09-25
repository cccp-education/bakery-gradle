package bakery.langswitch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.StringTemplateResolver

/**
 * BKY-LANG-NAV — null-safety of the symbolic Thymeleaf hosts.
 *
 * The switcher lives in `menu.thyme`, a template shared by every page of a
 * language tree. `archive.thyme`, `tags.thyme` and the master index render that
 * menu **without** a `content.uri` (it is null on synthetic pages). Both
 * symbolic hosts of the single rule — [LangSwitchPath.thymeleafHref] (NAV-2)
 * and [LangSwitchPath.thymeleafEachHref] (NAV-4) — must therefore degrade to
 * the target language index instead of throwing, exactly like the site-side fix
 * discovered and proven on cheroliv.com (S-235, NAV-6).
 *
 * Evaluated through the real `TemplateEngine`: the bug is a runtime evaluation
 * failure (`lastIndexOf` target is null), not a shape assertion.
 */
class LangSwitchPathNullSafetyTest {
    private val engine =
        TemplateEngine().apply {
            setTemplateResolver(
                StringTemplateResolver().apply {
                    templateMode = TemplateMode.HTML
                    isCacheable = false
                },
            )
        }

    /** A synthetic page: no `content.uri`, the menu sits at the tree root. */
    private fun syntheticContext(): Context =
        Context().apply {
            setVariable("content", mapOf("rootpath" to "", "uri" to null))
        }

    private fun renderSingle(
        currentLang: String,
        targetLang: String,
        defaultLang: String = "fr",
    ): String {
        val expression = LangSwitchPath.thymeleafHref(currentLang, targetLang, defaultLang)
        val rendered = engine.process("<a th:href=\"$expression\">x</a>", syntheticContext())
        return rendered.substringAfter("href=\"").substringBefore("\"")
    }

    @Test
    fun `self link on default language degrades to the index when uri is null`() {
        assertEquals("index.html", renderSingle("fr", "fr"))
    }

    @Test
    fun `self link on non-default language degrades to the index when uri is null`() {
        assertEquals("index.html", renderSingle("en", "en"))
    }

    @Test
    fun `default to non-default degrades to the target language index when uri is null`() {
        assertEquals("en/index.html", renderSingle("fr", "en"))
    }

    @Test
    fun `non-default back to default degrades to the default index when uri is null`() {
        assertEquals("../index.html", renderSingle("en", "fr"))
    }

    @Test
    fun `non-default to another non-default degrades to the target index when uri is null`() {
        assertEquals("../ar/index.html", renderSingle("en", "ar"))
    }

    @Test
    fun `the each host degrades to a language index when uri is null`() {
        val template =
            "<a th:each=\"lang : \${supportedLanguages}\" class=\"lang-option\" " +
                "th:href=\"${LangSwitchPath.thymeleafEachHref("fr")}\" th:attr=\"data-lang=\${lang.code}\">x</a>"
        val context =
            syntheticContext().apply {
                setVariable("supportedLanguages", listOf("fr", "en", "ar").map { mapOf("code" to it) })
                setVariable("config", mapOf("site_language" to "en"))
            }
        val rendered = engine.process(template, context)

        val hrefs =
            Regex("""data-lang="([a-z]{2})"[^>]*""").findAll(rendered).associate { match ->
                val lang = match.groupValues[1]
                val anchor = rendered.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
                lang to anchor.substringAfter("href=\"").substringBefore("\"")
            }

        assertEquals("index.html", hrefs["en"])
        assertEquals("../index.html", hrefs["fr"])
        assertEquals("../ar/index.html", hrefs["ar"])
    }
}
