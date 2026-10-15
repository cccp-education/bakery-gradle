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
 * CI shard — language switch core: initial language switch, i18n migration,
 * page assets, tree bake config, AsciiDoc JBake-native parser, JBake-native
 * translation and the language-switch URL resolver.
 *
 * One of six exhaustive, disjoint feature shards; guarded by
 * `bakery.ci.BakeryCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/30_i18n_language_switch.feature")
@SelectClasspathResource("features/31_i18n_migration.feature")
@SelectClasspathResource("features/32_i18n_migration.feature")
@SelectClasspathResource("features/47_page_assets.feature")
@SelectClasspathResource("features/48_tree_bake_config.feature")
@SelectClasspathResource("features/49_asciidoc_parser_jbake_native.feature")
@SelectClasspathResource("features/50_jbake_native_translation.feature")
@SelectClasspathResource("features/51_lang_switch_url_resolver.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/30_i18n_language_switch.feature,src/test/features/31_i18n_migration.feature," +
        "src/test/features/32_i18n_migration.feature,src/test/features/47_page_assets.feature," +
        "src/test/features/48_tree_bake_config.feature,src/test/features/49_asciidoc_parser_jbake_native.feature," +
        "src/test/features/50_jbake_native_translation.feature,src/test/features/51_lang_switch_url_resolver.feature",
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "bakery.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-lang-switch-core.html, json:build/reports/cucumber-shard-lang-switch-core.json",
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @personal")
class CucumberShardLangSwitchCoreRunner
