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
 * CI shard — site tree: pivot YAML generation, site tree node, traversal,
 * content, YAML, theme, i18n subtree and scaffolding.
 *
 * One of seven exhaustive, disjoint feature shards; guarded by
 * `bakery.ci.BakeryCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/38_generate_pivot_yaml.feature")
@SelectClasspathResource("features/39_site_tree_node.feature")
@SelectClasspathResource("features/40_site_tree_traversal.feature")
@SelectClasspathResource("features/41_site_tree_content.feature")
@SelectClasspathResource("features/42_site_tree_yaml.feature")
@SelectClasspathResource("features/43_site_tree_theme.feature")
@SelectClasspathResource("features/44_site_tree_i18n_subtree.feature")
@SelectClasspathResource("features/45_scaffold_tree.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/38_generate_pivot_yaml.feature,src/test/features/39_site_tree_node.feature," +
        "src/test/features/40_site_tree_traversal.feature,src/test/features/41_site_tree_content.feature," +
        "src/test/features/42_site_tree_yaml.feature,src/test/features/43_site_tree_theme.feature," +
        "src/test/features/44_site_tree_i18n_subtree.feature,src/test/features/45_scaffold_tree.feature",
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "bakery.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-site-tree.html, json:build/reports/cucumber-shard-site-tree.json",
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @personal")
class CucumberShardSiteTreeRunner
