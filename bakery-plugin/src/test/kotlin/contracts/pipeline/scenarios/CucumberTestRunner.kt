package contracts.pipeline.scenarios

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "contracts.pipeline.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber.html, json:build/reports/cucumber.json"
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip and not @integration")
class CucumberTestRunner
