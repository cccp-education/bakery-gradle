package bakery.scenarios

import bakery.langswitch.LangSwitchInjector
import bakery.langswitch.LangSwitchMenu
import bakery.langswitch.LangSwitchPath
import bakery.langswitch.LangSwitchThymeleafRenderer
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.StringTemplateResolver
import java.io.File
import java.nio.file.Files
import kotlin.text.Charsets.UTF_8

/**
 * BKY-LANG-NAV-5 — the end-to-end BDD close of the BKY-LANG-NAV core.
 *
 * A nested page (`blog/foo.html` in FR, `en/blog/foo.html` in EN) is resolved by
 * **both hosts** of the single same-page rule (decision D3):
 *
 *   - the **injected** host (NAV-2): `LangSwitchMenu` → `LangSwitchThymeleafRenderer`
 *     → `LangSwitchInjector`, the exact production chain of `injectLangSwitch`;
 *   - the **model** host (NAV-4): the shipped `site-basic/templates/menu.thyme`
 *     selector, projected on a `th:each`.
 *
 * Both host outputs are evaluated through the real Thymeleaf engine per page and
 * asserted against `LangSwitchPath.resolveSamePage` — the same rule the JS host
 * (NAV-3) already replays. A divergence on any host turns this feature red.
 *
 * The fixture bakes each language tree from its own root (FR at the site root,
 * EN under `en/`): `content.uri` is the page path within the tree and
 * `content.rootpath` ascends to that tree root — exactly the NAV-2 assumption.
 */
class SamePageLanguageSwitchSteps {
    private val defaultLanguage = "fr"
    private val supportedLanguages = listOf("fr", "en")

    private lateinit var siteDir: File
    private val injectedAnchors: MutableMap<String, Map<String, String>> = mutableMapOf()

    private val engine =
        TemplateEngine().apply {
            setTemplateResolver(
                StringTemplateResolver().apply {
                    templateMode = TemplateMode.HTML
                    isCacheable = false
                },
            )
        }

    @Given("a same-page fixture site with a French page {string} and its English twin")
    fun createFixture(frenchPage: String) {
        siteDir =
            Files.createTempDirectory("bakery-same-page-").toFile().apply {
                deleteOnExit()
            }

        val menuTemplate =
            """
            <html xmlns:th="http://www.thymeleaf.org">
            <body>
            <nav class="navbar">
                <div class="lang-switcher-container">
                </div>
            </nav>
            </body>
            </html>
            """.trimIndent()

        // FR tree — default language lives at the site root.
        siteDir.resolve("templates").mkdirs()
        siteDir.resolve("templates/menu.thyme").writeText(menuTemplate, UTF_8)
        siteDir.resolve(frenchPage).also { it.parentFile.mkdirs() }.writeText("<h1>Bonjour</h1>", UTF_8)

        // EN tree — non-default language lives under its own directory.
        val enRoot = siteDir.resolve("en")
        enRoot.resolve("templates").mkdirs()
        enRoot.resolve("templates/menu.thyme").writeText(menuTemplate, UTF_8)
        enRoot.resolve("blog").mkdirs()
        enRoot.resolve("blog/foo.html").writeText("<h1>Hello</h1>", UTF_8)
    }

    @When("the same-page switcher is injected into both language trees")
    fun injectBothTrees() {
        val injector = LangSwitchInjector()
        val labels = mapOf("fr" to "Français", "en" to "English")

        for (lang in supportedLanguages) {
            val currentPath = if (lang == defaultLanguage) "" else "$lang/"
            val menuThyme = resolveMenuThyme(lang)
            val links = LangSwitchMenu(supportedLanguages, defaultLanguage, lang, currentPath).generateLinks()
            val fragment = LangSwitchThymeleafRenderer(labels).render(links)
            menuThyme.writeText(injector.inject(menuThyme.readText(UTF_8), fragment), UTF_8)
        }
    }

    @Then("the {string} tree link for page {string} to language {string} resolves to {string}")
    fun treeLinkResolvesTo(
        tree: String,
        page: String,
        targetLang: String,
        expected: String,
    ) {
        assertThat(resolveInjected(tree, page)[targetLang])
            .describedAs("$tree tree link for $page to $targetLang")
            .isEqualTo(expected)
    }

    @Then("the {string} tree link for page {string} to language {string} does not fall back to home")
    fun treeLinkDoesNotFallBackToHome(
        tree: String,
        page: String,
        targetLang: String,
    ) {
        val href = resolveInjected(tree, page)[targetLang]
        assertThat(href)
            .describedAs("$tree tree link for $page to $targetLang must keep the page")
            .isNotNull
        assertThat(href!!.endsWith("index.html"))
            .describedAs("$tree tree link for $page to $targetLang fell back to home: $href")
            .isFalse()
        assertThat(href)
            .describedAs("$tree tree link for $page to $targetLang must not be page-blind")
            .doesNotContain("'index.html'")
    }

    @Then("the {string} tree self link for page {string} resolves to the file name {string}")
    fun treeSelfLinkResolvesToFileName(
        tree: String,
        page: String,
        fileName: String,
    ) {
        val currentLang = if (tree == defaultLanguage) defaultLanguage else tree
        assertThat(resolveInjected(tree, page)[currentLang])
            .describedAs("$tree tree self link for $page")
            .isEqualTo(fileName)
    }

    @Then("the active language of the {string} tree is {string}")
    fun activeLanguageOfTree(
        tree: String,
        lang: String,
    ) {
        val anchor = injectedAnchors.getValue(tree).getValue(lang)
        assertThat(anchor)
            .describedAs("$tree tree anchor for $lang should carry the active class")
            .contains("active")
    }

    @Then("every link of the {string} tree for page {string} matches the shared same-page rule")
    fun everyLinkMatchesSharedRule(
        tree: String,
        page: String,
    ) {
        val currentLang = if (tree == defaultLanguage) defaultLanguage else tree
        val hrefs = resolveInjected(tree, page)
        for (targetLang in supportedLanguages) {
            val expected = LangSwitchPath.resolveSamePage(page, currentLang, targetLang, defaultLanguage)
            assertThat(hrefs[targetLang])
                .describedAs("$tree tree vector $page $currentLang->$targetLang vs shared rule")
                .isEqualTo(expected)
        }
    }

    @Then("the shipped model menu for page {string} and language {string} resolves language {string} to {string}")
    fun shippedModelResolvesTo(
        page: String,
        currentLang: String,
        targetLang: String,
        expected: String,
    ) {
        assertThat(resolveModel(page, currentLang)[targetLang])
            .describedAs("shipped model menu $page ($currentLang) -> $targetLang")
            .isEqualTo(expected)
    }

    @Then("the shipped model menu for page {string} and language {string} matches the shared same-page rule")
    fun shippedModelMatchesSharedRule(
        page: String,
        currentLang: String,
    ) {
        val hrefs = resolveModel(page, currentLang)
        for (targetLang in supportedLanguages) {
            val expected = LangSwitchPath.resolveSamePage(page, currentLang, targetLang, defaultLanguage)
            assertThat(hrefs[targetLang])
                .describedAs("shipped model menu $page ($currentLang) -> $targetLang vs shared rule")
                .isEqualTo(expected)
        }
    }

    @Then("the injected {string} host and the shipped model menu agree for page {string}")
    fun injectedAndModelAgree(
        tree: String,
        page: String,
    ) {
        val hrefs = resolveInjected(tree, page)
        val model = resolveModel(page, tree)
        for (targetLang in supportedLanguages) {
            assertThat(hrefs[targetLang])
                .describedAs("injected $tree host vs shipped model menu for $page -> $targetLang")
                .isEqualTo(model[targetLang])
        }
    }

    private fun resolveMenuThyme(lang: String): File =
        if (lang == defaultLanguage) {
            siteDir.resolve("templates/menu.thyme")
        } else {
            siteDir.resolve(lang).resolve("templates/menu.thyme")
        }

    /**
     * Renders the *injected* menu (the real output of `injectLangSwitch`) through
     * the Thymeleaf engine for one page and returns the resolved href per language.
     */
    private fun resolveInjected(
        tree: String,
        page: String,
    ): Map<String, String> {
        val menu = resolveMenuThyme(tree).readText(UTF_8)
        val (rootpath, uri) = thymeleafContext(page, tree)
        val rendered = engine.process(menu, pageContext(tree, rootpath, uri))
        val result = anchorsByLanguage(rendered)
        injectedAnchors[tree] = result
        return result.mapValues { it.value.substringAfter("href=\"").substringBefore("\"") }
    }

    /**
     * Renders the shipped ex-nihilo model selector (`site-basic/templates/menu.thyme`)
     * for one page and returns the resolved href per language.
     */
    private fun resolveModel(
        page: String,
        currentLang: String,
    ): Map<String, String> {
        val menu =
            javaClass.classLoader
                .getResourceAsStream(MODEL_MENU_RESOURCE)!!
                .use { it.readBytes().toString(UTF_8) }
        val (rootpath, uri) = thymeleafContext(page, currentLang)
        val context =
            pageContext(currentLang, rootpath, uri).apply {
                setVariable(
                    "supportedLanguages",
                    supportedLanguages.map { mapOf("code" to it, "nativeName" to it, "rtl" to false) },
                )
            }
        val rendered = engine.process(selectorBlock(menu), context)
        return anchorsByLanguage(rendered).mapValues { it.value.substringAfter("href=\"").substringBefore("\"") }
    }

    private fun pageContext(
        currentLang: String,
        rootpath: String,
        uri: String,
    ): Context =
        Context().apply {
            setVariable("config", mapOf("site_language" to currentLang))
            setVariable("content", mapOf("rootpath" to rootpath, "uri" to uri))
        }

    /**
     * Maps a page URI to the pair a JBake page exposes to Thymeleaf:
     * `content.uri` relative to the language tree, and `content.rootpath`
     * ascending from the page directory to that tree root.
     */
    private fun thymeleafContext(
        page: String,
        currentLang: String,
    ): Pair<String, String> {
        val normalised = page.trim().trimStart('/')
        val withinTree =
            if (currentLang == defaultLanguage) normalised else normalised.removePrefix("$currentLang/")
        val dir = withinTree.substringBeforeLast('/', missingDelimiterValue = "")
        val rootpath = if (dir.isEmpty()) "" else "../".repeat(dir.split('/').count { it.isNotEmpty() })
        return rootpath to withinTree
    }

    private fun anchorsByLanguage(rendered: String): Map<String, String> =
        Regex("""data-lang="([a-z]{2})"[^>]*""").findAll(rendered).associate { match ->
            val lang = match.groupValues[1]
            val anchor = rendered.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
            lang to anchor
        }

    private fun selectorBlock(menu: String): String {
        val container = menu.indexOf("language-switcher-container")
        val divStart = menu.lastIndexOf("<div", container)
        val ulEnd = menu.indexOf("</ul>", container)
        val divEnd = menu.indexOf("</div>", ulEnd)
        return menu.substring(divStart, divEnd + "</div>".length)
    }

    private companion object {
        const val MODEL_MENU_RESOURCE = "site-basic/templates/menu.thyme"
    }
}
