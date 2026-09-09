package bakery.i18n.path

import io.cucumber.junit.platform.engine.Constants
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathResource
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import java.io.PrintWriter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class I18nSourcePathCucumberTest {
    @Test
    fun `run feature 63 i18n source path resolution`() {
        val request =
            LauncherDiscoveryRequestBuilder
                .request()
                .selectors(selectClasspathResource("features/63_i18n_source_path_resolution.feature"))
                .configurationParameter(Constants.GLUE_PROPERTY_NAME, "bakery.scenarios")
                .configurationParameter(Constants.FEATURES_PROPERTY_NAME, "src/test/features")
                .configurationParameter(Constants.FILTER_TAGS_PROPERTY_NAME, "@source-path")
                .build()

        val listener = SummaryGeneratingListener()
        LauncherFactory.create().execute(request, listener)
        val summary = listener.summary
        summary.printTo(PrintWriter(System.out))
        summary.printFailuresTo(PrintWriter(System.out))

        assertEquals(0L, summary.totalFailureCount, "Feature 63 should have no failures")
        assertTrue(summary.testsFoundCount > 0L, "Feature 63 should run at least one scenario")
    }
}