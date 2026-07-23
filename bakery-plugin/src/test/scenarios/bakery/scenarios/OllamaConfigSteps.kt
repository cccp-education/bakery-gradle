package bakery.scenarios

import bakery.FileSystemManager
import bakery.SecretField
import bakery.maskSecret
import contracts.i18n.OllamaDeviceKey
import com.fasterxml.jackson.module.kotlin.readValue
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

class OllamaConfigSteps {

    private var yamlString: String? = null
    private var ollamaConfig: contracts.i18n.OllamaConfig? = null
    private var siteParseResult: bakery.SiteConfiguration? = null
    private var deviceKey: OllamaDeviceKey? = null
    private var toStringOutput: String? = null
    private var maskedOutput: String? = null

    @Given("a site.yml without ollama section")
    fun aSiteYmlWithoutOllamaSection() {
        yamlString = """
            |bake:
            |  srcPath: "site"
            |  destDirPath: "build/bake"
        """.trimMargin()
    }

    @Given("a site.yml with 29 ollama device keys for ports 11437 to 11465")
    fun aSiteYmlWith29OllamaDeviceKeys() {
        val keysYaml = (11437..11465).joinToString("\n") { port ->
            "|        - keyName: ollama-$port\n" +
            "|          privateKey: ssh-ed25519-fake-key-$port"
        }
        yamlString = """
            |bake:
            |  srcPath: "site"
            |  destDirPath: "build/bake"
            |ollama:
            |  model: "gemma4:31b-cloud"
            |  portStart: 11437
            |  portEnd: 11465
            |  timeoutSeconds: 300
            |  deviceKeys:
$keysYaml
        """.trimMargin()
    }

    @Given("an Ollama device key with private key {string}")
    fun anOllamaDeviceKey(privateKey: String) {
        deviceKey = OllamaDeviceKey(keyName = "ollama-11437", privateKey = privateKey)
    }

    @When("I parse the site configuration for ollama")
    fun iParseSiteConfigurationForOllama() {
        val config = FileSystemManager.yamlMapper.readValue<bakery.SiteConfiguration>(yamlString!!)
        siteParseResult = config
        ollamaConfig = config.ollama
    }

    @When("I call toString on the device key")
    fun iCallToStringOnDeviceKey() {
        toStringOutput = deviceKey!!.toString()
    }

    @When("I mask the private key as a SecretField.DeviceKey")
    fun iMaskPrivateKeyAsDeviceKey() {
        maskedOutput = maskSecret(SecretField.DeviceKey(deviceKey!!.privateKey))
    }

    @Then("the ollama config is null")
    fun theOllamaConfigIsNull() {
        assertThat(ollamaConfig).isNull()
    }

    @Then("isConfigured returns false by default")
    fun isConfiguredReturnsFalseByDefault() {
        assertThat(contracts.i18n.OllamaConfig().isConfigured).isFalse()
    }

    @Then("the ollama config is present")
    fun theOllamaConfigIsPresent() {
        assertThat(ollamaConfig).isNotNull()
    }

    @Then("isConfigured returns true")
    fun isConfiguredReturnsTrue() {
        assertThat(ollamaConfig!!.isConfigured).isTrue()
    }

    @Then("the device keys list has {int} entries")
    fun theDeviceKeysCount(count: Int) {
        assertThat(ollamaConfig!!.deviceKeys).hasSize(count)
    }

    @Then("the model is {string}")
    fun theModelIs(model: String) {
        assertThat(ollamaConfig!!.model).isEqualTo(model)
    }

    @Then("portStart is {int}")
    fun portStartIs(port: Int) {
        assertThat(ollamaConfig!!.portStart).isEqualTo(port)
    }

    @Then("portEnd is {int}")
    fun portEndIs(port: Int) {
        assertThat(ollamaConfig!!.portEnd).isEqualTo(port)
    }

    @Then("the first device key name is {string}")
    fun theFirstDeviceKeyNameIs(name: String) {
        assertThat(ollamaConfig!!.deviceKeys.first().keyName).isEqualTo(name)
    }

    @Then("the last device key name is {string}")
    fun theLastDeviceKeyNameIs(name: String) {
        assertThat(ollamaConfig!!.deviceKeys.last().keyName).isEqualTo(name)
    }

    @Then("the output contains {string}")
    fun outputContains(text: String) {
        assertThat(toStringOutput).contains(text)
    }

    @Then("the output does not contain {string}")
    fun outputDoesNotContain(text: String) {
        assertThat(toStringOutput).doesNotContain(text)
    }

    @Then("the masked value starts with {string}")
    fun maskedValueStartsWith(prefix: String) {
        assertThat(maskedOutput).startsWith(prefix)
    }

    @Then("the masked value does not contain {string}")
    fun maskedValueDoesNotContain(text: String) {
        assertThat(maskedOutput).doesNotContain(text)
    }
}
