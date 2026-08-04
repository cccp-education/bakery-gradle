package bakery.i18n.mass

import io.cucumber.junit.platform.engine.Constants
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathResource
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import java.io.PrintWriter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class I18nMassMultiLangCucumberTest {
    @Test
    fun `run feature 60 i18n mass multi lang`() {
        val request =
            LauncherDiscoveryRequestBuilder
                .request()
                .selectors(selectClasspathResource("features/60_i18n_mass_multi_lang.feature"))
                .configurationParameter(Constants.GLUE_PROPERTY_NAME, "bakery.scenarios")
                .configurationParameter(Constants.FEATURES_PROPERTY_NAME, "src/test/features")
                .configurationParameter(Constants.FILTER_TAGS_PROPERTY_NAME, "@multi-lang")
                .build()

        val listener = SummaryGeneratingListener()
        LauncherFactory.create().execute(request, listener)
        val summary = listener.summary
        summary.printTo(PrintWriter(System.out))
        summary.printFailuresTo(PrintWriter(System.out))

        assertEquals(0L, summary.totalFailureCount, "Feature 60 should have no failures")
        assertTrue(summary.testsFoundCount > 0L, "Feature 60 should run at least one scenario")
    }
}
