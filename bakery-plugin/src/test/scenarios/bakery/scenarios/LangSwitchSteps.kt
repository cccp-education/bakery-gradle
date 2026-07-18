package bakery.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import java.io.File

class LangSwitchSteps(private val world: BakeryWorld) {

    private val supportedLangs = mutableListOf<String>()
    private var defaultLang = "fr"

    private val menuThymeTemplate = """
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
    fun createLangSwitchFixture2(lang1: String, lang2: String) {
        supportedLangs.clear()
        supportedLangs.addAll(listOf(lang1, lang2))
        createFixtureSite()
    }

    @Given("a lang-switch fixture site with 3 languages {string}, {string}, and {string}")
    fun createLangSwitchFixture3(lang1: String, lang2: String, lang3: String) {
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
    fun menuShouldContainLink(menuPath: String, expectedHref: String, lang: String) {
        val menuFile = world.projectDir!!.resolve(menuPath)
        assertThat(menuFile).exists()
        val content = menuFile.readText()
        val anchor = content.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
        assertThat(anchor)
            .describedAs("menu $menuPath should contain link $expectedHref for lang $lang")
            .contains(expectedHref)
    }

    @Then("the menu in {string} should not contain a self-loop for language {string}")
    fun menuShouldNotContainSelfLoop(menuPath: String, lang: String) {
        val menuFile = world.projectDir!!.resolve(menuPath)
        val content = menuFile.readText()
        val anchor = content.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
        assertThat(anchor)
            .describedAs("menu $menuPath should not contain self-loop for lang $lang")
            .doesNotContain("$lang/index.html")
    }

    @Then("the menu in {string} should not contain {string} anywhere in lang-switcher links")
    fun menuShouldNotContainAnywhere(menuPath: String, forbidden: String) {
        val menuFile = world.projectDir!!.resolve(menuPath)
        val content = menuFile.readText()
        val switcherBlock = content.substringAfter("lang-switcher-container").substringAfter("<ul")
        assertThat(switcherBlock)
            .describedAs("menu $menuPath lang-switcher should not contain $forbidden")
            .doesNotContain(forbidden)
    }

    @Then("the menu in {string} should not contain {string} for language {string}")
    fun menuShouldNotContainForLanguage(menuPath: String, forbidden: String, lang: String) {
        val menuFile = world.projectDir!!.resolve(menuPath)
        val content = menuFile.readText()
        val anchor = content.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
        assertThat(anchor)
            .describedAs("menu $menuPath should not contain $forbidden for lang $lang")
            .doesNotContain(forbidden)
    }

    @Then("the link for language {string} should resolve to {string}")
    fun linkShouldResolveTo(lang: String, expected: String) {
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
    fun langOptionShouldHaveClass(lang: String, className: String) {
        val menuFile = world.projectDir!!.resolve("site/templates/menu.thyme")
        val content = menuFile.readText()
        val anchor = content.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
        assertThat(anchor).contains(className)
    }

    @Then("the lang-option for language {string} should not have class {string}")
    fun langOptionShouldNotHaveClass(lang: String, className: String) {
        val menuFile = world.projectDir!!.resolve("site/templates/menu.thyme")
        val content = menuFile.readText()
        val anchor = content.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
        assertThat(anchor).doesNotContain(className)
    }

    private fun createFixtureSite() {
        val pluginId = "education.cccp.bakery"
        File.createTempFile("gradle-langswitch-", "").apply {
            delete()
            mkdirs()
        }.run {
            resolve("settings.gradle.kts").writeText(
                "pluginManagement.repositories.gradlePluginPortal()\n" +
                    "rootProject.name = \"${name}\""
            )
            resolve("build.gradle.kts").writeText(
                "plugins { id(\"$pluginId\") }\nbakery { configPath = \"site.yml\" }"
            )
            val siteDir = resolve("site")
            siteDir.resolve("templates").mkdirs()
            siteDir.resolve("content").mkdirs()
            siteDir.resolve("templates/menu.thyme").writeText(menuThymeTemplate)
            siteDir.resolve("content/index.html").writeText("<h1>Hello FR</h1>")
            for (lang in supportedLangs) {
                if (lang == "fr") continue
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
        world.projectDir!!.resolve("site.yml").writeText("""
            bake:
              srcPath: site
              destDirPath: build/output
            language: $defaultLang
            supportedLanguages: [$langsYaml]
        """.trimIndent())
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