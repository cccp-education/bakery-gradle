package bakery.i18n

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class DivergentRebuildReportTest {
    @Test
    fun `renders languages and articles deterministically`() {
        val report =
            DivergentRebuildReport.render(
                mapOf(
                    "vi" to listOf("z.adoc", "a.adoc"),
                    "en" to listOf("c.adoc"),
                ),
            )

        assertTrue(report.indexOf("| en |") < report.indexOf("| vi |"))
        assertTrue(report.contains("| vi | a.adoc, z.adoc"))
    }

    @Test
    fun `an empty plan says there is nothing to rebuild`() {
        assertTrue(DivergentRebuildReport.render(emptyMap()).contains("No structurally divergent variant"))
    }
}
