package bakery.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.io.File

/**
 * US-61a — Cucumber steps pour verifyConfigurationMapping.
 *
 * Valide le mapping YAML sécurisé : masquage des secrets et gestion
 * des erreurs de parsing.
 */
class VerifyConfigurationMappingSteps(private val world: BakeryWorld) {

    @Given("a valid site.yml with secrets")
    fun validSiteYmlWithSecrets() {
        val projectDir = world.projectDir ?: world.createGradleProjectWithSiteConfigured()
        projectDir.resolve("site.yml").writeText("""
            bake:
              srcPath: "site"
              destDirPath: "build/bake"
              cname: "test.example.com"
            pushPage:
              from: "site"
              to: "cvs"
              repo:
                name: "test-site"
                repository: "https://github.com/user/repo.git"
                credentials:
                  username: "user"
                  password: "secret-token-42"
              branch: "main"
              message: "Deploy test"
            pushMaquette:
              from: "maquette"
              to: "cvs"
              repo:
                name: "test-maquette"
                repository: "https://github.com/user/maquette.git"
                credentials:
                  username: "user"
                  password: "another-secret"
              branch: "main"
              message: "Deploy maquette"
            firebase:
              project:
                projectId: "test-project"
                apiKey: "AIzaSy-test-api-key"
              firestore:
                contacts:
                  name: "contacts"
                  fields:
                    - name: "name"
                      type: "string"
                  rulesEnabled: false
                messages:
                  name: "messages"
                  fields:
                    - name: "body"
                      type: "string"
                  rulesEnabled: false
              callable:
                name: "handleContact"
                params:
                  - name: "p_name"
                    type: "string"
        """.trimIndent())
    }

    @Given("a malformed site.yml")
    fun malformedSiteYml() {
        val projectDir = world.projectDir ?: world.createGradleProjectWithSiteConfigured()
        projectDir.resolve("site.yml").writeText("this is not: valid yaml: : :")
    }

    @Given("the site.yml is missing")
    fun siteYmlIsMissing() {
        val projectDir = world.projectDir ?: world.createGradleProjectWithSiteConfigured()
        projectDir.resolve("site.yml").delete()
    }

    @When("I run verifyConfigurationMapping")
    fun runVerifyConfigurationMapping() {
        if (world.exception == null) {
            world.executeGradleExpectingFailure("verifyConfigurationMapping")
        }
    }

    @Then("the task succeeds and secrets are masked")
    fun taskSucceedsAndSecretsAreMasked() {
        assertThat(world.buildResult).isNotNull
        assertThat(world.buildResult!!.output).contains("Configuration OK")
        assertThat(world.buildResult!!.output).contains("credentials.password=***")
        assertThat(world.buildResult!!.output).contains("firebase.apiKey=***")
        assertThat(world.buildResult!!.output).doesNotContain("secret-token-42")
        assertThat(world.buildResult!!.output).doesNotContain("AIzaSy-test-api-key")
    }

    @Then("the task fails with parsing error")
    fun taskFailsWithParsingError() {
        assertThat(world.exception).isNotNull
        // Le plugin plante dans afterEvaluate quand le YAML est malformé,
        // avant même que verifyConfigurationMapping ne s'exécute.
        assertThat(world.exception!!.message).contains("mapping values are not allowed here")
    }

    @Then("the task fails with missing file error")
    fun taskFailsWithMissingFileError() {
        assertThat(world.exception).isNotNull
        assertThat(world.exception!!.message).contains("Configuration file not found")
    }
}
