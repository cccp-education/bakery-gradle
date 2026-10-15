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
 * CI shard — jbake core components: article generation, Thymeleaf components,
 * Google Forms embed, Firebase auth/comments, layouts and the theme system.
 *
 * One of eight exhaustive, disjoint feature shards; guarded by
 * `bakery.ci.BakeryCucumberShardCoverageTest`. Partition by feature file, all
 * scenarios preserved (`not @personal`, the aggregate exclusion).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/9_generate_article.feature")
@SelectClasspathResource("features/10_thymeleaf_components.feature")
@SelectClasspathResource("features/11_google_forms_embed.feature")
@SelectClasspathResource("features/12_firebase_auth_comments.feature")
@SelectClasspathResource("features/13_layouts.feature")
@SelectClasspathResource("features/14_theme_system.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/9_generate_article.feature,src/test/features/10_thymeleaf_components.feature," +
        "src/test/features/11_google_forms_embed.feature,src/test/features/12_firebase_auth_comments.feature," +
        "src/test/features/13_layouts.feature,src/test/features/14_theme_system.feature",
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "bakery.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-jbake-core-components.html, json:build/reports/cucumber-shard-jbake-core-components.json",
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @personal")
class CucumberShardJbakeCoreComponentsRunner
