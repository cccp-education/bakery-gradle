package bakery.scenarios

import bakery.seo.LocalizedSeoHead
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

/**
 * BKY-OUT-1 — Cucumber steps for the localised SEO head.
 *
 * Steps are prefixed with "locale head" / "the head" so they never collide with
 * the shared SEO-injection glue (the S-088 lesson). The domain is exercised
 * directly: the Gradle task wiring is owned by `LocalizeSeoHeadFunctionalTest`.
 */
class SeoHeadLocaleSteps {
    private var head: String = ""
    private var rewritten: String = ""
    private var beforeRewrite: String = ""

    @Given("a locale head fixture for the language {string}")
    fun aLocaleHeadFixtureForTheLanguage(language: String) {
        head = HEAD.replace("LANG", language)
        rewritten = head
    }

    @Given("a head without canonical, og url, og locale and og image")
    fun aHeadWithoutSeoFields() {
        head = "<html xmlns:th=\"http://www.thymeleaf.org\">\n<head>\n<title>T</title>\n</head>\n</html>\n"
        rewritten = head
    }

    @When("the head is localised for {string} with og images for {string}")
    fun theHeadIsLocalisedFor(
        language: String,
        ogImageLanguage: String,
    ) {
        beforeRewrite = rewritten
        rewritten = LocalizedSeoHead.rewrite(rewritten, language, "fr", ogImageLanguage)
        rewritten = bakery.i18n.DocumentDirection.apply(rewritten, language)
    }

    @Then("the head canonical is prefixed with {string}")
    fun theHeadCanonicalIsPrefixedWith(prefix: String) {
        assertThat(rewritten).contains("""th:href="${'$'}{config.site_host} + '$prefix' + ${'$'}{canonUri}"""")
    }

    @Then("the head og url is prefixed with {string}")
    fun theHeadOgUrlIsPrefixedWith(prefix: String) {
        assertThat(rewritten)
            .contains("""property="og:url" th:content="${'$'}{config.site_host} + '$prefix' + ${'$'}{canonUri}"""")
    }

    @Then("the head declares og locale {string}")
    fun theHeadDeclaresOgLocale(locale: String) {
        assertThat(rewritten).contains("""<meta property="og:locale" content="$locale"/>""")
    }

    @Then("the head og image reads {string}")
    fun theHeadOgImageReads(directory: String) {
        assertThat(rewritten).contains(directory)
    }

    @Then("the head og image does not read {string}")
    fun theHeadOgImageDoesNotRead(directory: String) {
        assertThat(rewritten).doesNotContain(directory)
    }

    @Then("the head canonical is not prefixed")
    fun theHeadCanonicalIsNotPrefixed() {
        assertThat(rewritten).contains("""th:href="${'$'}{config.site_host} + ${'$'}{canonUri}"""")
        assertThat(rewritten).doesNotContain("""+ 'fr/' + ${'$'}{canonUri}""")
    }

    @Then("the head is unchanged")
    fun theHeadIsUnchanged() {
        assertThat(rewritten).isEqualTo(beforeRewrite)
    }

    @Then("the head declares the direction {string}")
    fun theHeadDeclaresTheDirection(direction: String) {
        assertThat(rewritten).contains("dir=\"$direction\"")
    }

    @Then("the head carries no direction")
    fun theHeadCarriesNoDirection() {
        assertThat(rewritten).doesNotContain("dir=")
    }

    private companion object {
        val HEAD =
            """
            <html xmlns:th="http://www.thymeleaf.org" lang="LANG">
            <head>
                <th:block th:with="canonUri=${'$'}{content.uri},
                                   ogImage=${'$'}{'og-images/fr/' + #strings.replace(content.uri, '.html', '.png')}">
                    <link rel="canonical" th:href="${'$'}{config.site_host} + ${'$'}{canonUri}"/>
                    <meta property="og:url" th:content="${'$'}{config.site_host} + ${'$'}{canonUri}"/>
                    <meta property="og:locale" content="fr_FR"/>
                    <meta property="og:image" th:content="${'$'}{config.site_host} + ${'$'}{ogImage}"/>
                </th:block>
            </head>
            </html>
            """.trimIndent()
    }
}
