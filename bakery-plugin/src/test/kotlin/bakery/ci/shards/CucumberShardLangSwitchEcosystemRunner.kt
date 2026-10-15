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
 * CI shard — language switch ecosystem: RTL direction injection (domain and
 * task), SEO injection, contact security, same-page language switch, variant
 * bake, template tree and DNS provisioning.
 *
 * One of six exhaustive, disjoint feature shards; guarded by
 * `bakery.ci.BakeryCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/52_rtl_direction_injection.feature")
@SelectClasspathResource("features/54_rtl_direction_injection_task.feature")
@SelectClasspathResource("features/59_seo_injection.feature")
@SelectClasspathResource("features/61_contact_sec.feature")
@SelectClasspathResource("features/66_same_page_language_switch.feature")
@SelectClasspathResource("features/67_variant_bake.feature")
@SelectClasspathResource("features/68_template_tree.feature")
@SelectClasspathResource("features/dns_provision.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/52_rtl_direction_injection.feature," +
        "src/test/features/54_rtl_direction_injection_task.feature,src/test/features/59_seo_injection.feature," +
        "src/test/features/61_contact_sec.feature,src/test/features/66_same_page_language_switch.feature," +
        "src/test/features/67_variant_bake.feature,src/test/features/68_template_tree.feature," +
        "src/test/features/dns_provision.feature",
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "bakery.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-lang-switch-ecosystem.html, json:build/reports/cucumber-shard-lang-switch-ecosystem.json",
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @personal")
class CucumberShardLangSwitchEcosystemRunner
