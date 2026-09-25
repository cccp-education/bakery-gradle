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
 * BKY-LANG-NAV-2 — the Thymeleaf host of the single same-page rule.
 *
 * [LangSwitchPath.thymeleafHref] is the *symbolic* sibling of
 * [LangSwitchPath.resolveSamePage]: it emits a Thymeleaf expression in terms of
 * `${content.rootpath}` and `${content.uri}`, evaluated per page at bake time
 * (D4 — the link is crawlable, no-JS, page-aware).
 *
 * The anti split-brain proof (D3) is the evaluation test below: every shared
 * vector of [lang-switch-path-vectors.json] that carries an exact page is
 * replayed *through the Thymeleaf engine* and must equal the pure rule's output.
 * Vectors carrying `existingPages` (target-page degradation, D8) are out of the
 * static-expression reach — the build-time existence check is deferred.
 */
class LangSwitchPathThymeleafHrefTest {
    private val vectors: JsonNode =
        javaClass.classLoader
            .getResourceAsStream(VECTORS_RESOURCE)!!
            .use { jacksonObjectMapper().readTree(it) }

    private fun href(
        currentLang: String,
        targetLang: String,
        defaultLang: String = "fr",
    ) = LangSwitchPath.thymeleafHref(currentLang, targetLang, defaultLang)

    @Test
    fun `self link from default language points at the current page`() {
        assertEquals(
            "\${content.uri != null ? content.uri.substring(content.uri.lastIndexOf('/') + 1) : content.rootpath + 'index.html'}",
            href("fr", "fr"),
        )
    }

    @Test
    fun `self link from non-default language points at the current page`() {
        assertEquals(
            "\${content.uri != null ? content.uri.substring(content.uri.lastIndexOf('/') + 1) : content.rootpath + 'index.html'}",
            href("en", "en"),
        )
    }

    @Test
    fun `default language to non-default keeps the page under the language dir`() {
        assertEquals(
            "\${content.uri != null ? content.rootpath + 'en/' + content.uri : content.rootpath + 'en/index.html'}",
            href("fr", "en"),
        )
    }

    @Test
    fun `non-default language back to default exits the language tree then keeps the page`() {
        assertEquals(
            "\${content.uri != null ? '../' + content.rootpath + '' + content.uri : '../' + content.rootpath + 'index.html'}",
            href("en", "fr"),
        )
    }

    @Test
    fun `non-default language to another non-default keeps the page`() {
        assertEquals(
            "\${content.uri != null ? '../' + content.rootpath + 'ar/' + content.uri : '../' + content.rootpath + 'ar/index.html'}",
            href("en", "ar"),
        )
    }

    @Test
    fun `blank current language throws`() {
        assertThrows<IllegalArgumentException> { href("", "en") }
    }

    @Test
    fun `blank target language throws`() {
        assertThrows<IllegalArgumentException> { href("fr", "") }
    }

    @Test
    fun `blank default language throws`() {
        assertThrows<IllegalArgumentException> { href("fr", "en", "") }
    }

    @Test
    fun `the emitted expression evaluates through thymeleaf to the shared vector expectation`() {
        val engine =
            TemplateEngine().apply {
                setTemplateResolver(
                    StringTemplateResolver().apply {
                        templateMode = TemplateMode.HTML
                        isCacheable = false
                    },
                )
            }

        val failures = mutableListOf<String>()
        var replayed = 0
        vectors["vectors"].forEach { vector ->
            // Degradation vectors describe a target that does not exist on the
            // tree: a static expression cannot know this at bake time. They stay
            // the pure rule's / JS host's contract, out of NAV-2's reach.
            if (vector["existingPages"]?.isArray == true) return@forEach

            val currentPageUri = vector["currentPageUri"].asText()
            val currentLang = vector["currentLang"].asText()
            val targetLang = vector["targetLang"].asText()
            val defaultLang = vector["defaultLang"].asText()
            val expected = vector["expected"].asText()

            val (rootpath, uri) = thymeleafContextFor(currentPageUri, currentLang, defaultLang)
            val expression = LangSwitchPath.thymeleafHref(currentLang, targetLang, defaultLang)

            val context = Context()
            context.setVariable("content", mapOf("rootpath" to rootpath, "uri" to uri))
            val rendered = engine.process("<a th:href=\"$expression\">x</a>", context)
            val actual = rendered.substringAfter("href=\"").substringBefore("\"")
            replayed++

            if (actual != expected) {
                failures +=
                    "vector '${vector["name"].asText()}': " +
                    "expression '$expression' evaluated with rootpath='$rootpath' uri='$uri' " +
                    "= '$actual' but expected '$expected'"
            }
        }
        assertTrue(replayed >= 11) { "expected at least 11 replayable vectors, got $replayed" }
        assertTrue(failures.isEmpty()) { failures.joinToString("\n") }
    }

    /**
     * Map the root-model page URI of a shared vector to the pair a JBake page
     * exposes to Thymeleaf: `content.uri` is relative to the language tree, and
     * `content.rootpath` ascends from the page directory to that tree root.
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
