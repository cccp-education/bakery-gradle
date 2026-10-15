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
 * CI shard — i18n mass migration/validation, i18n client dictionary, template
 * translation, ci ollama config, config-mapping validation and the N0
 * pipeline-contracts suite.
 *
 * One of four exhaustive, disjoint feature shards; guarded by
 * `bakery.ci.BakeryCucumberShardCoverageTest`. This shard is the only one that
 * selects `36_pipeline_contracts.feature`, whose step definitions live in the
 * `contracts.pipeline.scenarios` package — hence the two-package glue.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/7_ci_cd_cheroliv_com_validation.feature")
@SelectClasspathResource("features/33_i18n_migration_magic_stick.feature")
@SelectClasspathResource("features/34_i18n_migration_prod_gen.feature")
@SelectClasspathResource("features/35_i18n_migration_auto_translation.feature")
@SelectClasspathResource("features/36_pipeline_contracts.feature")
@SelectClasspathResource("features/57_i18n_delta_wire.feature")
@SelectClasspathResource("features/57_ollama_config.feature")
@SelectClasspathResource("features/58_i18n_ci_ollama_device_key.feature")
@SelectClasspathResource("features/59_i18n_mass_validation.feature")
@SelectClasspathResource("features/60_i18n_mass_multi_lang.feature")
@SelectClasspathResource("features/62_i18n_mass_batch_annual.feature")
@SelectClasspathResource("features/63_i18n_source_path_resolution.feature")
@SelectClasspathResource("features/64_i18n_client_dictionary.feature")
@SelectClasspathResource("features/65_i18n_template_translation.feature")
@SelectClasspathResource("features/verify_configuration_mapping.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/7_ci_cd_cheroliv_com_validation.feature," +
        "src/test/features/33_i18n_migration_magic_stick.feature,src/test/features/34_i18n_migration_prod_gen.feature," +
        "src/test/features/35_i18n_migration_auto_translation.feature,src/test/features/36_pipeline_contracts.feature," +
        "src/test/features/57_i18n_delta_wire.feature,src/test/features/57_ollama_config.feature," +
        "src/test/features/58_i18n_ci_ollama_device_key.feature,src/test/features/59_i18n_mass_validation.feature," +
        "src/test/features/60_i18n_mass_multi_lang.feature,src/test/features/62_i18n_mass_batch_annual.feature," +
        "src/test/features/63_i18n_source_path_resolution.feature,src/test/features/64_i18n_client_dictionary.feature," +
        "src/test/features/65_i18n_template_translation.feature,src/test/features/verify_configuration_mapping.feature",
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "bakery.scenarios,contracts.pipeline.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-i18n-mass-pipeline.html, json:build/reports/cucumber-shard-i18n-mass-pipeline.json",
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @personal")
class CucumberShardI18nMassPipelineRunner
