package bakery.i18n.deploy

import io.cucumber.junit.platform.engine.Constants
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathResource
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import java.io.PrintWriter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class I18nDeployEndToEndCucumberTest {
    @Test
    fun `run feature 55 i18n deploy end to end`() {
        val request =
            LauncherDiscoveryRequestBuilder
                .request()
                .selectors(selectClasspathResource("features/55_i18n_deploy_end_to_end.feature"))
                .configurationParameter(Constants.GLUE_PROPERTY_NAME, "bakery.scenarios")
                .configurationParameter(Constants.FEATURES_PROPERTY_NAME, "src/test/features")
                .configurationParameter(Constants.FILTER_TAGS_PROPERTY_NAME, "@i18n-deploy-e2e")
                .build()

        val listener = SummaryGeneratingListener()
        LauncherFactory.create().execute(request, listener)
        val summary = listener.summary
        summary.printTo(PrintWriter(System.out))
        summary.printFailuresTo(PrintWriter(System.out))

        assertEquals(0L, summary.totalFailureCount, "Feature 55 should have no failures")
        assertTrue(summary.testsFoundCount > 0L, "Feature 55 should run at least one scenario")
    }
}
