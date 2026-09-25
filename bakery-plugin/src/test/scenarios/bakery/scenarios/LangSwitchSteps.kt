package bakery.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import java.io.File

class LangSwitchSteps(
    private val world: BakeryWorld,
) {
    private val supportedLangs = mutableListOf<String>()
    private var defaultLang = "fr"
    private var materializeUnderI18n = false

    private val menuThymeTemplate =
        """
        <html xmlns:th="http://www.thymeleaf.org">
        <body>
        <nav class="navbar">
            <div class="container">
                <a class="navbar-brand" href="#">Test</a>
                <div class="lang-switcher-container">
                </div>
            </div>
        </nav>
        </body>
        </html>
        """.trimIndent()

    @Given("a lang-switch fixture site with 2 languages {string} and {string}")
    fun createLangSwitchFixture2(
        lang1: String,
        lang2: String,
    ) {
        supportedLangs.clear()
        supportedLangs.addAll(listOf(lang1, lang2))
        materializeUnderI18n = false
        createFixtureSite()
    }

    /**
     * BKY-LANG-NAV-8 — the non-default variant is materialised under
     * `i18n/{lang}/templates/` (the frozen-bundle layout), not `{lang}/`.
     */
    @Given("a lang-switch fixture site with 2 languages {string} and {string} materialized under i18n")
    fun createLangSwitchFixture2Materialized(
        lang1: String,
        lang2: String,
    ) {
        supportedLangs.clear()
        supportedLangs.addAll(listOf(lang1, lang2))
        materializeUnderI18n = true
        createFixtureSite()
    }

    @Given("a lang-switch fixture site with 3 languages {string}, {string}, and {string}")
    fun createLangSwitchFixture3(
        lang1: String,
        lang2: String,
        lang3: String,
    ) {
        supportedLangs.clear()
        supportedLangs.addAll(listOf(lang1, lang2, lang3))
        createFixtureSite()
    }

    @Given("the default language is {string}")
    fun setDefaultLanguage(lang: String) {
        defaultLang = lang
        writeSiteYml()
    }

    @When("I inject the language switcher into the FR site")
    fun injectLanguageSwitcherFR() {
        runGradleInject()
    }

    @When("I inject the language switcher into the EN site")
    fun injectLanguageSwitcherEN() {
        runGradleInject()
    }

    @When("I inject the language switcher into the AR site")
    fun injectLanguageSwitcherAR() {
        runGradleInject()
    }

    @Then("the menu in {string} should contain a link to {string} for language {string}")
    fun menuShouldContainLink(
        menuPath: String,
        expectedHref: String,
        lang: String,
    ) {
        val menuFile = world.projectDir!!.resolve(menuPath)
        assertThat(menuFile).exists()
        val content = menuFile.readText()
        val anchor = content.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
        assertThat(anchor)
            .describedAs("menu $menuPath should contain link $expectedHref for lang $lang")
            .contains(expectedHref)
    }

    @Then("the menu in {string} should contain a page-aware link to language {string}")
    fun menuShouldContainPageAwareLink(
        menuPath: String,
        lang: String,
    ) {
        val menuFile = world.projectDir!!.resolve(menuPath)
        assertThat(menuFile).exists()
        val content = menuFile.readText()
        val anchor = content.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
        assertThat(anchor)
            .describedAs("menu $menuPath should carry a page-aware link for lang $lang")
            .contains("content.uri")
        assertIndexFallbackGuarded(anchor, menuPath, lang)
    }

    /**
     * The only `index.html` a page-aware link may carry is the D8 degradation
     * branch, reached when `content.uri` is null (synthetic pages: archive, tags,
     * master index). An unguarded index href would be the page-blind bug (S-228).
     */
    private fun assertIndexFallbackGuarded(
        anchor: String,
        menuPath: String,
        lang: String,
    ) {
        if (!anchor.contains("index.html")) return
        assertThat(anchor)
            .describedAs("menu $menuPath link for lang $lang: index.html must be null-guarded")
            .contains("content.uri != null")
        assertThat(anchor)
            .describedAs("menu $menuPath link for lang $lang must not hardcode a page-blind href")
            .doesNotContain("th:href=\"'index.html'\"")
            .doesNotContain("th:href=\"index.html\"")
    }

    @Then("the lang-option for language {string} should point at the current page")
    fun langOptionShouldPointAtCurrentPage(lang: String) {
        val menuFile = world.projectDir!!.resolve("site/en/templates/menu.thyme")
        val content = menuFile.readText()
        val anchor = content.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
        assertThat(anchor)
            .describedAs("lang $lang self-option should resolve to the current page")
            .contains("content.uri.lastIndexOf")
    }

    @Then("the menu in {string} should not contain a self-loop for language {string}")
    fun menuShouldNotContainSelfLoop(
        menuPath: String,
        lang: String,
    ) {
        val menuFile = world.projectDir!!.resolve(menuPath)
        val content = menuFile.readText()
        val anchor = content.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
        assertThat(anchor)
            .describedAs("menu $menuPath should not contain self-loop for lang $lang")
            .doesNotContain("$lang/index.html")
    }

    @Then("the menu in {string} should not contain {string} anywhere in lang-switcher links")
    fun menuShouldNotContainAnywhere(
        menuPath: String,
        forbidden: String,
    ) {
        val menuFile = world.projectDir!!.resolve(menuPath)
        val content = menuFile.readText()
        val switcherBlock = content.substringAfter("lang-switcher-container").substringAfter("<ul")
        assertNotPageBlindIndex(switcherBlock, menuPath, forbidden)
    }

    @Then("the menu in {string} should not contain {string} for language {string}")
    fun menuShouldNotContainForLanguage(
        menuPath: String,
        forbidden: String,
        lang: String,
    ) {
        val menuFile = world.projectDir!!.resolve(menuPath)
        val content = menuFile.readText()
        val anchor = content.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
        assertNotPageBlindIndex(anchor, menuPath, forbidden)
    }

    /**
     * D8 — an `index.html` fallback is legitimate only inside the
     * `content.uri != null` guard (synthetic pages: archive, tags, master index).
     * A page-blind href would be the S-228 bug: the link must always keep the
     * page-aware branch and never hardcode a static index target.
     */
    private fun assertNotPageBlindIndex(
        block: String,
        menuPath: String,
        forbidden: String,
    ) {
        if (!forbidden.contains("index.html")) {
            assertThat(block)
                .describedAs("menu $menuPath should not contain $forbidden")
                .doesNotContain(forbidden)
            return
        }
        assertThat(block)
            .describedAs("menu $menuPath must keep a page-aware branch ($forbidden)")
            .contains("content.uri")
        assertThat(block)
            .describedAs("menu $menuPath: the '$forbidden' fallback must be null-guarded")
            .contains("content.uri != null")
        assertThat(block)
            .describedAs("menu $menuPath must not hardcode a page-blind href to $forbidden")
            .doesNotContain("th:href=\"$forbidden\"")
            .doesNotContain("th:href='$forbidden'")
    }

    @Then("the link for language {string} should resolve to {string}")
    fun linkShouldResolveTo(
        lang: String,
        expected: String,
    ) {
        val enMenu = world.projectDir!!.resolve("site/en/templates/menu.thyme")
        val content = enMenu.readText()
        val anchor = content.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
        assertThat(anchor).contains(expected)
    }

    @Then("the active language should be {string}")
    fun activeLanguageShouldBe(lang: String) {
        val currentPath = if (lang == defaultLang) "site/templates/menu.thyme" else "site/$lang/templates/menu.thyme"
        val menuFile = world.projectDir!!.resolve(currentPath)
        val content = menuFile.readText()
        val anchor = content.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
        assertThat(anchor)
            .describedAs("lang $lang should have active class")
            .contains("active")
    }

    @Then("the lang-option for language {string} should have class {string}")
    fun langOptionShouldHaveClass(
        lang: String,
        className: String,
    ) {
        val menuFile = world.projectDir!!.resolve("site/templates/menu.thyme")
        val content = menuFile.readText()
        val anchor = content.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
        assertThat(anchor).contains(className)
    }

    @Then("the lang-option for language {string} should not have class {string}")
    fun langOptionShouldNotHaveClass(
        lang: String,
        className: String,
    ) {
        val menuFile = world.projectDir!!.resolve("site/templates/menu.thyme")
        val content = menuFile.readText()
        val anchor = content.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
        assertThat(anchor).doesNotContain(className)
    }

    private fun createFixtureSite() {
        val pluginId = "education.cccp.bakery"
        File
            .createTempFile("gradle-langswitch-", "")
            .apply {
                delete()
                mkdirs()
            }.run {
                resolve("settings.gradle.kts").writeText(
                    "pluginManagement.repositories.gradlePluginPortal()\n" +
                        "rootProject.name = \"${name}\"",
                )
                resolve("build.gradle.kts").writeText(
                    "plugins { id(\"$pluginId\") }\nbakery { configPath = \"site.yml\" }",
                )
                val siteDir = resolve("site")
                siteDir.resolve("templates").mkdirs()
                siteDir.resolve("content").mkdirs()
                siteDir.resolve("templates/menu.thyme").writeText(menuThymeTemplate)
                siteDir.resolve("content/index.html").writeText("<h1>Hello FR</h1>")
                for (lang in supportedLangs) {
                    if (lang == "fr") continue
                    if (materializeUnderI18n) {
                        val i18nDir = siteDir.resolve("i18n").resolve(lang)
                        i18nDir.resolve("templates").mkdirs()
                        i18nDir.resolve("content").mkdirs()
                        i18nDir.resolve("templates/menu.thyme").writeText(menuThymeTemplate)
                        i18nDir.resolve("content/index.html").writeText("<h1>Hello $lang</h1>")
                        continue
                    }
                    val langDir = siteDir.resolve(lang)
                    langDir.resolve("templates").mkdirs()
                    langDir.resolve("content").mkdirs()
                    langDir.resolve("templates/menu.thyme").writeText(menuThymeTemplate)
                    langDir.resolve("content/index.html").writeText("<h1>Hello $lang</h1>")
                }
                world.projectDir = this
            }
        writeSiteYml()
    }

    private fun writeSiteYml() {
        val langsYaml = supportedLangs.joinToString(", ")
        world.projectDir!!.resolve("site.yml").writeText(
            """
            bake:
              srcPath: site
              destDirPath: build/output
            language: $defaultLang
            supportedLanguages: [$langsYaml]
            """.trimIndent(),
        )
    }

    private fun runGradleInject() {
        runBlocking {
            try {
                world.executeGradle("injectLangSwitch")
            } catch (_: Exception) {
                // capturé dans world.exception
            }
        }
    }
}
