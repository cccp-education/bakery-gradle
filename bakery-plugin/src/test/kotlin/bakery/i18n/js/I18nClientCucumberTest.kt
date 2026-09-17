package bakery.i18n.js

import io.cucumber.junit.platform.engine.Constants
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathResource
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import java.io.PrintWriter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-3.
 *
 * Dedicated runner for feature 64 (`translateI18nClient`), pattern S-082.
 */
class I18nClientCucumberTest {
    @Test
    fun `run feature 64 i18n client dictionary`() {
        val request =
            LauncherDiscoveryRequestBuilder
                .request()
                .selectors(selectClasspathResource("features/64_i18n_client_dictionary.feature"))
                .configurationParameter(Constants.GLUE_PROPERTY_NAME, "bakery.scenarios")
                .configurationParameter(Constants.FEATURES_PROPERTY_NAME, "src/test/features")
                .configurationParameter(Constants.FILTER_TAGS_PROPERTY_NAME, "@i18n-client")
                .build()

        val listener = SummaryGeneratingListener()
        LauncherFactory.create().execute(request, listener)
        val summary = listener.summary
        summary.printTo(PrintWriter(System.out))
        summary.printFailuresTo(PrintWriter(System.out))

        assertEquals(0L, summary.totalFailureCount, "Feature 64 should have no failures")
        assertTrue(summary.testsFoundCount > 0L, "Feature 64 should run at least one scenario")
    }
}
