package bakery.ci.shards

import io.cucumber.junit.platform.engine.Constants.FEATURES_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * CI shard — augmented context (lens, N3, articles template, analytics),
 * intention scaffolds (theme/scaffold), config resolution cascade, Firebase
 * config validation and the accessibility audit.
 *
 * One of seven exhaustive, disjoint feature shards; guarded by
 * `bakery.ci.BakeryCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/13_analytics_newsletter.feature")
@SelectClasspathResource("features/22_augmented_context_lens.feature")
@SelectClasspathResource("features/23_augmented_context_n3.feature")
@SelectClasspathResource("features/24_augmented_articles_template.feature")
@SelectClasspathResource("features/25_scaffold_intention_ia.feature")
@SelectClasspathResource("features/27_theme_intention.feature")
@SelectClasspathResource("features/28_validate_firebase_config.feature")
@SelectClasspathResource("features/29_config_resolution_cascade.feature")
@SelectClasspathResource("features/37_accessibility_audit.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/13_analytics_newsletter.feature,src/test/features/22_augmented_context_lens.feature," +
        "src/test/features/23_augmented_context_n3.feature,src/test/features/24_augmented_articles_template.feature," +
        "src/test/features/25_scaffold_intention_ia.feature,src/test/features/27_theme_intention.feature," +
        "src/test/features/28_validate_firebase_config.feature,src/test/features/29_config_resolution_cascade.feature," +
        "src/test/features/37_accessibility_audit.feature",
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "bakery.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-lens-tree.html, json:build/reports/cucumber-shard-lens-tree.json",
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @personal")
class CucumberShardLensTreeRunner
