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
}
