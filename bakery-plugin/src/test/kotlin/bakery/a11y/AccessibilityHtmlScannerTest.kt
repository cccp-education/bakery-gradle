package bakery.a11y

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AccessibilityHtmlScannerTest {

    @Test
    fun `skips element without inline colors`() {
        val html = "<div>Bonjour</div>"
        val findings = scanInlineColors(html)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `passes black text on white background`() {
        val html = "<p style=\"color: #000000; background-color: #FFFFFF;\">Text</p>"
        val findings = scanInlineColors(html)
        assertThat(findings).hasSize(1)
        assertThat(findings.first().pass).isTrue()
    }

    @Test
    fun `fails orange text on white background`() {
        val html = "<a style=\"color: #FFA500; background-color: #FFFFFF;\">Link</a>"
        val findings = scanInlineColors(html)
        assertThat(findings).hasSize(1)
        assertThat(findings.first().pass).isFalse()
        assertThat(findings.first().message).contains("< 4.5")
    }

    @Test
    fun `audits multiple elements`() {
        val html = """
            <div style="color: #000000; background-color: #FFFFFF;">OK</div>
            <span style="color: #777777; background-color: #FFFFFF;">Weak</span>
        """.trimIndent()
        val findings = scanInlineColors(html)
        assertThat(findings).hasSize(2)
        assertThat(findings[0].pass).isTrue()
        assertThat(findings[1].pass).isFalse()
    }

    @Test
    fun `flags img without alt`() {
        val html = "<img src=\"logo.png\">"
        val findings = scanStructural(html)
        assertThat(findings).hasSize(1)
        assertThat(findings.first().rule).isEqualTo("img-alt")
        assertThat(findings.first().pass).isFalse()
        assertThat(findings.first().selector).isEqualTo("img")
    }

    @Test
    fun `passes img with alt`() {
        val html = "<img src=\"logo.png\" alt=\"Logo\">"
        val findings = scanStructural(html)
        assertThat(findings).hasSize(1)
        assertThat(findings.first().rule).isEqualTo("img-alt")
        assertThat(findings.first().pass).isTrue()
    }

    @Test
    fun `flags link without aria-label when text is empty`() {
        val html = "<a href=\"#\"><i class=\"icon\"></i></a>"
        val findings = scanStructural(html)
        val aria = findings.firstOrNull { it.rule == "link-aria-label" }
        assertThat(aria).isNotNull()
        assertThat(aria!!.pass).isFalse()
        assertThat(aria.selector).isEqualTo("a")
    }

    @Test
    fun `passes link with aria-label`() {
        val html = "<a href=\"#\" aria-label=\"Accueil\"><i class=\"icon\"></i></a>"
        val findings = scanStructural(html)
        val aria = findings.firstOrNull { it.rule == "link-aria-label" }
        assertThat(aria).isNotNull()
        assertThat(aria!!.pass).isTrue()
    }

    @Test
    fun `flags heading skip from h1 to h3`() {
        val html = "<h1>Titre</h1><h3>Sous-titre</h3>"
        val findings = scanStructural(html)
        val headings = findings.filter { it.rule == "heading-order" }
        assertThat(headings).hasSize(1)
        assertThat(headings.first().pass).isFalse()
        assertThat(headings.first().message).contains("h1").contains("h3")
    }

    @Test
    fun `passes sequential headings h1 h2 h3`() {
        val html = "<h1>A</h1><h2>B</h2><h3>C</h3>"
        val findings = scanStructural(html)
        val headings = findings.filter { it.rule == "heading-order" }
        assertThat(headings).isEmpty()
    }

    @Test
    fun `scanStructural returns empty for plain text`() {
        val html = "<p>Bonjour</p>"
        val findings = scanStructural(html)
        assertThat(findings).isEmpty()
    }

}
