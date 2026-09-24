package bakery.langswitch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.text.Charsets.UTF_8

/**
 * BKY-LANG-NAV-4 — the ex-nihilo model guard (decision D7).
 *
 * A brand-new site must not be born with a switcher that sends the visitor home
 * (constat S-228). The injected shared menu uses the fixed-pair renderer (NAV-2),
 * but the *model* menus are copied verbatim by `generateSite` and rely on the
 * dynamic `th:each` host (NAV-4). This object verifies the model selector is
 * page-aware — presence alone (`lang-option`) would be a false green.
 *
 * Domain-pure: template text in, violations out. No I/O, no Gradle.
 */
class LangSwitchModelGuardTest {
    private val pageAwareMenu =
        """
        <div class="nav-item dropdown language-switcher-container">
            <ul class="dropdown-menu">
                <li th:each="lang : ${'$'}{supportedLanguages}">
                    <a class="dropdown-item lang-option"
                       th:classappend="${'$'}{lang.code == config.site_language ? 'active' : ''}"
                       th:href="${'$'}{lang.code == config.site_language ? content.uri : content.rootpath + lang.code + '/' + content.uri}"
                       th:attr="data-lang=${'$'}{lang.code}">
                        <span th:text="${'$'}{lang.nativeName}">Français</span>
                    </a>
                </li>
            </ul>
        </div>
        """.trimIndent()

    @Nested
    inner class Pure {
        @Test
        fun `a menu without the switcher container is a violation`() {
            val report = LangSwitchModelGuard.verify("<nav></nav>")

            assertThat(report.isSuccess).isFalse()
            assertThat(report.violations.map { it.kind })
                .contains(LangSwitchModelGuard.MISSING_SELECTOR)
        }

        @Test
        fun `a page-aware selector passes`() {
            val report = LangSwitchModelGuard.verify(pageAwareMenu)

            assertThat(report.violations)
                .describedAs("page-aware model menu must be clean")
                .isEmpty()
        }

        @Test
        fun `a page-blind absolute href is a violation`() {
            val blind =
                """
                <div class="language-switcher-container">
                    <a class="dropdown-item lang-option"
                       th:href="'/' + ${'$'}{lang.code} + '/'" data-lang="${'$'}{lang.code}">x</a>
                </div>
                """.trimIndent()

            val report = LangSwitchModelGuard.verify(blind)

            assertThat(report.isSuccess).isFalse()
            assertThat(report.violations.map { it.kind })
                .contains(LangSwitchModelGuard.PAGE_BLIND_HREF)
        }

        @Test
        fun `a selector whose option lacks data-lang is a violation`() {
            val noDataLang =
                """
                <div class="language-switcher-container">
                    <a class="dropdown-item lang-option"
                       th:href="${'$'}{lang.code == config.site_language ? X : Y}">x</a>
                </div>
                """.trimIndent()

            val report = LangSwitchModelGuard.verify(noDataLang)

            assertThat(report.isSuccess).isFalse()
            assertThat(report.violations.map { it.kind })
                .contains(LangSwitchModelGuard.MISSING_DATA_LANG)
        }

        @Test
        fun `a selector missing the page variable in its href is a violation`() {
            val noPageVar =
                """
                <div class="language-switcher-container">
                    <a class="dropdown-item lang-option"
                       th:href="${'$'}{lang.code == config.site_language ? X : Y}"
                       th:attr="data-lang=${'$'}{lang.code}">x</a>
                </div>
                """.trimIndent()

            val report = LangSwitchModelGuard.verify(noPageVar)

            assertThat(report.isSuccess).isFalse()
            assertThat(report.violations.map { it.kind })
                .contains(LangSwitchModelGuard.MISSING_PAGE_AWARENESS)
        }
    }

    @Nested
    inner class ShippedModels {
        /**
         * The real guard: both model menus shipped in the plugin jar — copied
         * verbatim by `generateSite` — must carry the page-aware selector.
         */
        @Test
        fun `the shipped basic and blog model menus are page-aware`() {
            val basic = resource("site-basic/templates/menu.thyme")
            val blog = resource("site/templates/menu.thyme")

            assertThat(basic).describedAs("site-basic/templates/menu.thyme").isNotNull
            assertThat(blog).describedAs("site/templates/menu.thyme").isNotNull

            assertThat(LangSwitchModelGuard.verify(basic!!).violations)
                .describedAs("site-basic model menu must be page-aware")
                .isEmpty()
            assertThat(LangSwitchModelGuard.verify(blog!!).violations)
                .describedAs("site model menu must be page-aware")
                .isEmpty()
        }

        /**
         * D3 anti split-brain at the model level: the model menu must carry the
         * *exact* expression generated by the single rule — not a hand-written
         * approximation that could drift.
         */
        @Test
        fun `the shipped model menus embed the exact rule expression`() {
            val expected = LangSwitchPath.thymeleafEachHref("fr")

            assertThat(resource("site-basic/templates/menu.thyme"))
                .describedAs("site-basic model must embed the rule expression")
                .contains(expected)
            assertThat(resource("site/templates/menu.thyme"))
                .describedAs("site model must embed the rule expression")
                .contains(expected)
        }

        private fun resource(path: String): String? =
            javaClass.classLoader.getResourceAsStream(path)?.use { it.readBytes().toString(UTF_8) }
    }

    /**
     * D7 end-to-end at the model level: the *shipped* model menu, rendered by the
     * real Thymeleaf engine, resolves every `data-lang` option page-aware. This
     * catches a broken `th:each`/`th:href` that a text assertion would miss.
     */
    @Nested
    inner class ModelRendering {
        private val engine =
            org.thymeleaf.TemplateEngine().apply {
                setTemplateResolver(
                    org.thymeleaf.templateresolver.StringTemplateResolver().apply {
                        templateMode = org.thymeleaf.templatemode.TemplateMode.HTML
                        isCacheable = false
                    },
                )
            }

        /**
         * Isolates the selector container from the shipped menu. The rest of the
         * menu uses `@{/}` (web-context relative) and cross-file fragments that a
         * string template resolver cannot satisfy — the selector itself is what
         * NAV-4 must prove.
         */
        private fun selectorBlock(menu: String): String {
            val container = menu.indexOf("language-switcher-container")
            val divStart = menu.lastIndexOf("<div", container)
            val ulEnd = menu.indexOf("</ul>", container)
            val divEnd = menu.indexOf("</div>", ulEnd)
            return menu.substring(divStart, divEnd + "</div>".length)
        }

        private fun renderMenu(
            resourcePath: String,
            currentLang: String,
            currentPageUri: String,
        ): Map<String, String> {
            val menu =
                javaClass.classLoader
                    .getResourceAsStream(resourcePath)!!
                    .use { it.readBytes().toString(UTF_8) }
            val template = selectorBlock(menu)

            val normalised = currentPageUri.trim().trimStart('/')
            // JBake exposes `content.uri` relative to the language tree: for a
            // non-default variant the leading `<lang>/` segment is the tree root.
            val uri = if (currentLang == "fr") normalised else normalised.removePrefix("$currentLang/")
            val dir = uri.substringBeforeLast('/', missingDelimiterValue = "")
            val rootpath = if (dir.isEmpty()) "" else "../".repeat(dir.split('/').count { it.isNotEmpty() })

            val context = org.thymeleaf.context.Context()
            context.setVariable(
                "supportedLanguages",
                listOf("fr", "en", "ar").map { mapOf("code" to it, "nativeName" to it, "rtl" to false) },
            )
            context.setVariable("config", mapOf("site_language" to currentLang))
            context.setVariable("content", mapOf("rootpath" to rootpath, "uri" to uri))

            val rendered = engine.process(template, context)
            return Regex("""data-lang="([a-z]{2})"[^>]*""").findAll(rendered).associate { match ->
                val lang = match.groupValues[1]
                val anchor = rendered.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
                lang to anchor.substringAfter("href=\"").substringBefore("\"")
            }
        }

        @Test
        fun `the shipped basic model menu resolves every language page-aware`() {
            val hrefs = renderMenu("site-basic/templates/menu.thyme", "en", "en/blog/foo.html")

            assertThat(hrefs["fr"]).describedAs("FR target").isEqualTo("../../blog/foo.html")
            assertThat(hrefs["en"]).describedAs("self target").isEqualTo("foo.html")
            assertThat(hrefs["ar"]).describedAs("AR target").isEqualTo("../../ar/blog/foo.html")
        }

        @Test
        fun `the shipped blog model menu resolves every language page-aware`() {
            val hrefs = renderMenu("site/templates/menu.thyme", "fr", "blog/foo.html")

            assertThat(hrefs["fr"]).describedAs("self target").isEqualTo("foo.html")
            assertThat(hrefs["en"]).describedAs("EN target").isEqualTo("../en/blog/foo.html")
        }
    }
}
