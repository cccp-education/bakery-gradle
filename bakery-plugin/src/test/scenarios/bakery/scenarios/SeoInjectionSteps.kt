package bakery.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import java.io.File

class SeoInjectionSteps(
    private val world: BakeryWorld,
) {
    private var siteName: String = "Example"
    private var websiteUrl: String = "https://example.com"
    private val supportedLangs = mutableListOf("fr", "en")
    private var defaultLang = "fr"

    private val headerThymeTemplate =
        """
        <html xmlns:th="http://www.thymeleaf.org">
        <head>
        <!-- SEO: bakery -->
        <old-seo />
        <!-- /SEO: bakery -->
        <title>Test</title>
        </head>
        <body></body>
        </html>
        """.trimIndent()

    @Given("a seo fixture site with 2 languages {string} and {string}")
    fun createSeoFixture2(
        lang1: String,
        lang2: String,
    ) {
        supportedLangs.clear()
        supportedLangs.addAll(listOf(lang1, lang2))
        defaultLang = lang1
        createFixtureSite()
    }

    @Given("a seo config with siteName {string} and websiteUrl {string}")
    fun setSeoConfig(
        name: String,
        url: String,
    ) {
        siteName = name
        websiteUrl = url
        writeSiteYml()
    }

    @When("I run injectSeo")
    fun runInjectSeo() {
        runBlocking {
            try {
                world.executeGradle("injectSeo")
            } catch (_: Exception) {
                // capturé dans world.exception
            }
        }
    }

    @Then("the FR header.thyme contains a canonical link")
    fun frHeaderContainsCanonical() {
        val header = world.projectDir!!.resolve("site/templates/header.thyme")
        assertThat(header).exists()
        assertThat(header.readText()).contains("<link rel=\"canonical\"")
    }

    @Then("the FR header.thyme contains hreflang {string}")
    fun frHeaderContainsHreflang(lang: String) {
        val header = world.projectDir!!.resolve("site/templates/header.thyme")
        assertThat(header.readText()).contains("hreflang=\"$lang\"")
    }

    @Then("the EN header.thyme contains a canonical link")
    fun enHeaderContainsCanonical() {
        val header = world.projectDir!!.resolve("site/en/templates/header.thyme")
        assertThat(header).exists()
        assertThat(header.readText()).contains("<link rel=\"canonical\"")
    }

    @Then("the EN header.thyme contains hreflang {string}")
    fun enHeaderContainsHreflang(lang: String) {
        val header = world.projectDir!!.resolve("site/en/templates/header.thyme")
        assertThat(header.readText()).contains("hreflang=\"$lang\"")
    }

    @Then("the FR header.thyme contains og:image with the default image")
    fun frHeaderContainsOgImageDefault() {
        val header = world.projectDir!!.resolve("site/templates/header.thyme")
        assertThat(header.readText()).contains("og:image")
        assertThat(header.readText()).contains("example-default.png")
    }

    @Then("the FR header.thyme contains og:site_name {string}")
    fun frHeaderContainsOgSiteName(name: String) {
        val header = world.projectDir!!.resolve("site/templates/header.thyme")
        assertThat(header.readText()).contains("og:site_name")
        assertThat(header.readText()).contains(name)
    }

    @Then("the FR header.thyme contains a JSON-LD script")
    fun frHeaderContainsJsonLdScript() {
        val header = world.projectDir!!.resolve("site/templates/header.thyme")
        assertThat(header.readText()).contains("application/ld+json")
    }

    @Then("the JSON-LD contains a WebSite node with name {string}")
    fun jsonLdContainsWebSiteNode(name: String) {
        val header = world.projectDir!!.resolve("site/templates/header.thyme")
        val content = header.readText()
        assertThat(content).contains("WebSite")
        assertThat(content).contains(name)
    }

    @Then("the FR header.thyme contains a meta description tag")
    fun frHeaderContainsMetaDescription() {
        val header = world.projectDir!!.resolve("site/templates/header.thyme")
        assertThat(header.readText()).contains("<meta name=\"description\"")
    }

    @Then("a sitemap.xml is generated at {string}")
    fun sitemapIsGenerated(path: String) {
        val sitemap = world.projectDir!!.resolve(path)
        assertThat(sitemap).exists()
        assertThat(sitemap.readText()).contains("<urlset")
    }

    @Then("the sitemap contains a root url with loc {string}")
    fun sitemapContainsRootUrl(loc: String) {
        val sitemap = world.projectDir!!.resolve("site/sitemap.xml")
        assertThat(sitemap.readText()).contains("<loc>$loc</loc>")
    }

    @Then("the sitemap contains hreflang {string} pointing to {string}")
    fun sitemapContainsHreflang(
        lang: String,
        href: String,
    ) {
        val sitemap = world.projectDir!!.resolve("site/sitemap.xml")
        val content = sitemap.readText()
        assertThat(content).contains("hreflang=\"$lang\"")
        assertThat(content).contains("href=\"$href\"")
    }

    private fun createFixtureSite() {
        val pluginId = "education.cccp.bakery"
        File
            .createTempFile("gradle-seo-", "")
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
                siteDir.resolve("templates/header.thyme").writeText(headerThymeTemplate)
                siteDir.resolve("content/index.html").writeText("<h1>Hello FR</h1>")
                for (lang in supportedLangs) {
                    if (lang == defaultLang) continue
                    val langDir = siteDir.resolve(lang)
                    langDir.resolve("templates").mkdirs()
                    langDir.resolve("content").mkdirs()
                    langDir.resolve("templates/header.thyme").writeText(headerThymeTemplate)
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
            seo:
              siteName: $siteName
              brand: $siteName
              defaultOgImage: example-default.png
              websiteUrl: "$websiteUrl"
              inLanguage: $defaultLang
            """.trimIndent(),
        )
    }
}