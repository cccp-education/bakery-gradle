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

/**
 * EPIC BKY-I18N-DEPLOY — US DEPLOY-7.
 *
 * Standalone JUnit5 test that runs the Cucumber feature
 * `56_i18n_deploy_multi_site.feature` in isolation via the JUnit Platform
 * Launcher. Avoids launching the full Cucumber suite (slow) when iterating
 * on the multi-site deploy feature.
 */
class I18nDeployMultiSiteCucumberTest {

    @Test
    fun `run feature 56 i18n deploy multi site`() {
        val request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectClasspathResource("features/56_i18n_deploy_multi_site.feature"))
            .configurationParameter(Constants.GLUE_PROPERTY_NAME, "bakery.scenarios")
            .configurationParameter(Constants.FEATURES_PROPERTY_NAME, "src/test/features")
            .configurationParameter(Constants.FILTER_TAGS_PROPERTY_NAME, "@i18n-deploy-multi-site")
            .build()

        val listener = SummaryGeneratingListener()
        LauncherFactory.create().execute(request, listener)
        val summary = listener.summary
        summary.printTo(PrintWriter(System.out))
        summary.printFailuresTo(PrintWriter(System.out))

        assertEquals(0L, summary.totalFailureCount, "Feature 56 should have no failures")
        assertTrue(summary.testsFoundCount > 0L, "Feature 56 should run at least one scenario")
    }
}