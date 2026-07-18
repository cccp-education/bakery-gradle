package bakery.i18n.rtl

import bakery.pivot.AsciiDocParser
import bakery.pivot.JbakeNativeRenderer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-I18N-DEPLOY — US DEPLOY-4.
 *
 * Unit tests for the RTL injection pipeline logic (parse → inject → normalize
 * → render) used by [RtlDirectionInjectionTask]. The task itself is covered
 * by functional tests (GradleRunner); these UTs validate the domain pipeline
 * without forking a daemon.
 */
class RtlDirectionInjectionPipelineTest {

    private val parser = AsciiDocParser()
    private val renderer = JbakeNativeRenderer()
    private val injector = RtlDirectionInjector()

    private val sourceAdoc = """= Article de test
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

    @Test
    fun `pipeline injects jbake-lang and rtl for arabic`() {
        val rendered = injectAndRender(sourceAdoc, "ar")
        assertThat(rendered).contains(":jbake-lang: ar")
        assertThat(rendered).contains(":lang: rtl")
    }

    @Test
    fun `pipeline injects jbake-lang without rtl for french`() {
        val rendered = injectAndRender(sourceAdoc, "fr")
        assertThat(rendered).contains(":jbake-lang: fr")
        assertThat(rendered).doesNotContain(":lang: rtl")
    }

    @Test
    fun `pipeline is idempotent on second pass for arabic`() {
        val firstPass = injectAndRender(sourceAdoc, "ar")
        val secondPass = injectAndRender(firstPass, "ar")
        assertThat(secondPass).isEqualTo(firstPass)
    }

    @Test
    fun `pipeline drops rtl when translating rtl source to ltr target`() {
        val arabic = injectAndRender(sourceAdoc, "ar")
        val french = injectAndRender(arabic, "fr")
        assertThat(french).contains(":jbake-lang: fr")
        assertThat(french).doesNotContain(":lang: rtl")
    }

    @Test
    fun `pipeline does not duplicate jbake-lang on re-injection`() {
        val firstPass = injectAndRender(sourceAdoc, "ar")
        val secondPass = injectAndRender(firstPass, "ar")
        val jbakeLangCount = secondPass.split(":jbake-lang:").size - 1
        assertThat(jbakeLangCount).isEqualTo(1)
    }

    @Test
    fun `pipeline preserves body content after injection`() {
        val rendered = injectAndRender(sourceAdoc, "ar")
        assertThat(rendered).contains("== Introduction")
        assertThat(rendered).contains("Ceci est un paragraphe de test.")
    }

    @Test
    fun `pipeline preserves jbake header attributes after injection`() {
        val rendered = injectAndRender(sourceAdoc, "ar")
        assertThat(rendered).contains(":jbake-title: Article de test")
        assertThat(rendered).contains(":jbake-type: post")
        assertThat(rendered).contains(":jbake-status: published")
    }

    private fun injectAndRender(adoc: String, lang: String): String {
        val article = parser.parse(adoc)
        val injected = injector.inject(article.frontmatter, lang)
        val updated = article.copy(frontmatter = injected)
        return renderer.render(updated)
    }
}