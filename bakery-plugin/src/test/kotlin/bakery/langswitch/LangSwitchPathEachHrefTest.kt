package bakery.langswitch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.StringTemplateResolver

/**
 * BKY-LANG-NAV-4 — the dynamic (`th:each`) host of the single same-page rule.
 *
 * Unlike the injected shared menu (NAV-2, one fixed link per language tree), the
 * ex-nihilo model (`site-basic/templates/menu.thyme`, `site/templates/menu.thyme`)
 * iterates over `${supportedLanguages}` with a single `th:each` shared by every
 * page of every tree. The symbolic expression therefore cannot hardcode the
 * target: it must read `lang.code` (target) and `config.site_language` (current)
 * at bake time.
 *
 * This is the same rule (`LangSwitchPath`) projected onto a *loop*: the evaluator
 * replays every shared vector of `lang-switch-path-vectors.json` — the fixture the
 * Kotlin pure rule (NAV-1) and the JS host (NAV-3) already share. A divergence on
 * any host turns that host red (anti split-brain, D3).
 */
class LangSwitchPathEachHrefTest {
    private val vectors: JsonNode =
        javaClass.classLoader
            .getResourceAsStream(VECTORS_RESOURCE)!!
            .use { jacksonObjectMapper().readTree(it) }

    private val engine =
        TemplateEngine().apply {
            setTemplateResolver(
                StringTemplateResolver().apply {
                    templateMode = TemplateMode.HTML
                    isCacheable = false
                },
            )
        }

    private fun eachHref(defaultLang: String = "fr") = LangSwitchPath.thymeleafEachHref(defaultLang)

    @Test
    fun `the self case resolves to the current page file name expression`() {
        val expression = eachHref()

        assertTrue(expression.contains("content.uri.lastIndexOf")) {
            "self case must strip the directory, got: $expression"
        }
    }

    @Test
    fun `the expression is page-aware and never hardcodes a page-blind target`() {
        val expression = eachHref()

        assertTrue(expression.contains("content.uri")) { "must keep the page: $expression" }
        assertTrue(expression.contains("content.rootpath")) { "must ascend the tree: $expression" }
        assertTrue(expression.contains("lang.code")) { "target must come from the loop: $expression" }
        assertTrue(expression.contains("config.site_language")) { "current must come from config: $expression" }
        assertTrue(!expression.contains("'/' + lang.code")) { "must not emit a page-blind absolute: $expression" }
        // The index.html target only appears as the null-safe degradation branch
        // (synthetic pages: archive, tags, master index).
        assertTrue(expression.contains("content.uri != null")) { "must null-guard the degradation: $expression" }
    }

    @Test
    fun `blank default language throws`() {
        assertThrows<IllegalArgumentException> { eachHref("") }
    }

    @Test
    fun `the each expression evaluates through thymeleaf to the shared vector expectation`() {
        val failures = mutableListOf<String>()
        var replayed = 0

        vectors["vectors"].forEach { vector ->
            // Degradation vectors describe a target absent from the tree: an
            // expression evaluated per page cannot know this at bake time (D8).
            if (vector["existingPages"]?.isArray == true) return@forEach

            val currentPageUri = vector["currentPageUri"].asText()
            val currentLang = vector["currentLang"].asText()
            val targetLang = vector["targetLang"].asText()
            val defaultLang = vector["defaultLang"].asText()
            val expected = vector["expected"].asText()

            val (rootpath, uri) = thymeleafContextFor(currentPageUri, currentLang, defaultLang)
            val actual = renderEach(currentLang, listOf(currentLang, targetLang, defaultLang), rootpath, uri)[targetLang]
            replayed++

            if (actual != expected) {
                failures +=
                    "vector '${vector["name"].asText()}': each expression evaluated with " +
                    "site_language='$currentLang' rootpath='$rootpath' uri='$uri' " +
                    "= '$actual' but expected '$expected'"
            }
        }

        assertTrue(replayed >= 11) { "expected at least 11 replayable vectors, got $replayed" }
        assertTrue(failures.isEmpty()) { failures.joinToString("\n") }
    }

    /**
     * Renders the dynamic selector exactly as a JBake page would: a `th:each`
     * over `${supportedLanguages}` whose `th:href` carries the generated
     * expression, with `config.site_language` as the current language.
     */
    private fun renderEach(
        currentLang: String,
        supported: List<String>,
        rootpath: String,
        uri: String,
    ): Map<String, String> {
        val template =
            "<a th:each=\"lang : \${supportedLanguages}\" class=\"lang-option\" " +
                "th:href=\"${eachHref()}\" th:attr=\"data-lang=\${lang.code}\">x</a>"

        val context = Context()
        context.setVariable("supportedLanguages", supported.distinct().map { mapOf("code" to it) })
        context.setVariable("config", mapOf("site_language" to currentLang))
        context.setVariable("content", mapOf("rootpath" to rootpath, "uri" to uri))

        val rendered = engine.process(template, context)
        return Regex("""data-lang="([a-z]{2})"[^>]*""").findAll(rendered).associate { match ->
            val lang = match.groupValues[1]
            val anchor = rendered.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
            lang to anchor.substringAfter("href=\"").substringBefore("\"")
        }
    }

    /**
     * Maps a shared vector's root-model page URI to the pair a JBake page
     * exposes to Thymeleaf: `content.uri` relative to the language tree, and
     * `content.rootpath` ascending from the page directory to that tree root.
     */
    private fun thymeleafContextFor(
        currentPageUri: String,
        currentLang: String,
        defaultLang: String,
    ): Pair<String, String> {
        val normalised = currentPageUri.trim().trimStart('/')
        val withinTree =
            if (currentLang == defaultLang) normalised else normalised.removePrefix("$currentLang/")
        val dir = withinTree.substringBeforeLast('/', missingDelimiterValue = "")
        val rootpath = if (dir.isEmpty()) "" else "../".repeat(dir.split('/').count { it.isNotEmpty() })
        return rootpath to withinTree
    }

    private companion object {
        const val VECTORS_RESOURCE = "bakery/langswitch/lang-switch-path-vectors.json"
    }
}
