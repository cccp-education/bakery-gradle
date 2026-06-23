package bakery.a11y

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AccessibilityAuditorTest {

    @Test
    fun `empty audit is compliant`() {
        val report = audit(emptyList())
        assertThat(report.isCompliant).isTrue()
        assertThat(report.passedCount).isZero()
        assertThat(report.failedCount).isZero()
    }

    @Test
    fun `audit with one passing finding is compliant`() {
        val finding = AccessibilityFinding(
            selector = "body",
            rule = "color-contrast",
            pass = true,
            message = "Contrast OK"
        )
        val report = audit(listOf(finding))
        assertThat(report.isCompliant).isTrue()
        assertThat(report.passedCount).isEqualTo(1)
        assertThat(report.failedCount).isZero()
    }

    @Test
    fun `audit with one failing finding is not compliant`() {
        val finding = AccessibilityFinding(
            selector = "nav a",
            rule = "missing-aria-label",
            pass = false,
            message = "Link without accessible label"
        )
        val report = audit(listOf(finding))
        assertThat(report.isCompliant).isFalse()
        assertThat(report.passedCount).isZero()
        assertThat(report.failedCount).isEqualTo(1)
    }

    @Test
    fun `mixed audit counts pass and fail`() {
        val findings = listOf(
            AccessibilityFinding("header", "heading-order", true, "Heading OK"),
            AccessibilityFinding("img", "alt-text", false, "Missing alt attribute")
        )
        val report = audit(findings)
        assertThat(report.isCompliant).isFalse()
        assertThat(report.passedCount).isEqualTo(1)
        assertThat(report.failedCount).isEqualTo(1)
    }
}
