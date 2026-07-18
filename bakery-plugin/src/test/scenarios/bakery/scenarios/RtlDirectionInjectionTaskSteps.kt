package bakery.scenarios

import bakery.i18n.rtl.RtlDirectionInjector
import bakery.pivot.AsciiDocParser
import bakery.pivot.JbakeNativeRenderer
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

/**
 * EPIC BKY-I18N-DEPLOY — US DEPLOY-4.
 *
 * BDD step definitions for the `injectRtlDirection` task pipeline
 * (parse → inject → render). The task itself is a thin Gradle wrapper
 * around [RtlDirectionInjector]; these steps validate the domain pipeline
 * that the task applies to each `.adoc` in `content-i18n/{lang}/`.
 */
class RtlDirectionInjectionTaskSteps {

    private val parser = AsciiDocParser()
    private val renderer = JbakeNativeRenderer()
    private val injector = RtlDirectionInjector()
    private lateinit var sourceAdoc: String
    private lateinit var rendered: String

    private val baseAdoc = """= Article de test
@CherOliv
2026-07-18
:jbake-title: Article de test
:jbake-type: post
:jbake-status: published
:jbake-date: 2026-07-18
:summary: un article de test

== Introduction

Ceci est un paragraphe de test.
"""

    @Given("a translated {string} article with no language directive")
    fun translatedArticleNoDirective(lang: String) {
        sourceAdoc = baseAdoc
    }

    @Given("a translated {string} article with {string} and {string}")
    fun translatedArticleWithDirective(lang: String, attr1: String, attr2: String) {
        sourceAdoc = baseAdoc.replace(
            ":summary: un article de test",
            ":summary: un article de test\n$attr1\n$attr2"
        )
    }

    @Given("a translated {string} article with jbake header attributes")
    fun translatedArticleWithJbakeHeader(lang: String) {
        sourceAdoc = baseAdoc
    }

    @Given("a translated {string} article with body content")
    fun translatedArticleWithBody(lang: String) {
        sourceAdoc = baseAdoc
    }

    @When("the RTL injection task processes language {string}")
    fun rtlInjectionTaskProcesses(lang: String) {
        val article = parser.parse(sourceAdoc)
        val injected = injector.inject(article.frontmatter, lang)
        val updated = article.copy(frontmatter = injected)
        rendered = renderer.render(updated)
    }

    @When("the RTL injection task processes language {string} twice")
    fun rtlInjectionTaskProcessesTwice(lang: String) {
        val article = parser.parse(sourceAdoc)
        val injected = injector.inject(article.frontmatter, lang)
        val updated = article.copy(frontmatter = injected)
        val firstPass = renderer.render(updated)
        val reparsed = parser.parse(firstPass)
        val reinjected = injector.inject(reparsed.frontmatter, lang)
        val reupdated = reparsed.copy(frontmatter = reinjected)
        rendered = renderer.render(reupdated)
    }

    @Then("the article should contain {string}")
    fun articleShouldContain(text: String) {
        assertThat(rendered).contains(text)
    }

    @Then("the article should not contain {string}")
    fun articleShouldNotContain(text: String) {
        assertThat(rendered).doesNotContain(text)
    }

    @Then("the article should contain {string} exactly once")
    fun articleShouldContainExactlyOnce(text: String) {
        val count = rendered.split(text).size - 1
        assertThat(count).isEqualTo(1)
    }
}