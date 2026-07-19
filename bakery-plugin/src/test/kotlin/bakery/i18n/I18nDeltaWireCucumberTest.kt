package bakery.i18n

import io.cucumber.junit.platform.engine.Constants
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathResource
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import java.io.PrintWriter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class I18nDeltaWireCucumberTest {

    @Test
    fun `run feature 57 i18n delta wire`() {
        val request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectClasspathResource("features/57_i18n_delta_wire.feature"))
            .configurationParameter(Constants.GLUE_PROPERTY_NAME, "bakery.scenarios")
            .configurationParameter(Constants.FEATURES_PROPERTY_NAME, "src/test/features")
            .configurationParameter(Constants.FILTER_TAGS_PROPERTY_NAME, "@i18n-delta-wire")
            .build()

        val listener = SummaryGeneratingListener()
        LauncherFactory.create().execute(request, listener)
        val summary = listener.summary
        summary.printTo(PrintWriter(System.out))
        summary.printFailuresTo(PrintWriter(System.out))

        assertEquals(0L, summary.totalFailureCount, "Feature 57 should have no failures")
        assertTrue(summary.testsFoundCount > 0L, "Feature 57 should run at least one scenario")
    }
}
