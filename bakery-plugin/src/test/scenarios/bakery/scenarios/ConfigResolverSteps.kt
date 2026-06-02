package bakery.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import kotlin.text.Charsets.UTF_8

/**
 * Cucumber steps for BKY-CONV-1: ConfigResolver 4-layer cascade
 * CLI (-P) > gradle.properties > DSL > site.yml > defaults
 *
 * Tests use generateSite task which creates the site scaffold and
 * reads the YAML config to inject resolved properties into jbake.properties.
 */
class ConfigResolverSteps(private val world: BakeryWorld) {

    @Given("a new Bakery project with site configured")
    fun createBakeryProjectWithSiteConfigured() {
        // Use the standard project creation (no IA, no site/maquette dirs)
        // This will take the scaffolding branch in BakeryPlugin
        world.createGradleProject()
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")

        // Add minimal site.yml content that includes googleForms block
        // This site.yml will be overwritten by generateSite's createAndConfigureSiteYml
        // but for these tests, we need it to exist BEFORE generateSite runs
        // because the scaffolding branch only runs if configFile does NOT exist.
        // So here we need to set up differently:
        // We need site.yml to exist with the config, AND site/ directories to NOT exist
        // so that generateSite will scaffold AND inject the resolved config.
        // But that's contradictory: if site.yml exists AND site/ doesn't, the plugin takes
        // the scaffolding branch because site/ doesn't exist.
        // Actually looking at the condition: !configDir.resolve(bake.srcPath).exists()
        // So if site.yml exists but "site" dir doesn't, it takes the scaffolding branch. ✅
    }

    @Given("the site configuration contains a googleForms block with formId {string}")
    fun siteConfigContainsGoogleForms(formId: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val siteYml = projectDir.resolve("site.yml")
        val content = if (siteYml.exists()) siteYml.readText(UTF_8) else defaultSiteYml()
        // Replace or append googleForms block
        val updatedContent = if (content.contains("googleForms:")) {
            content.replace(Regex("googleForms:[\\s\\S]*?(?=\\n[a-zA-Z]|$)"), "googleForms:\n  formId: \"$formId\"\n  width: \"640\"\n  height: \"800\"")
        } else {
            content.trimEnd() + "\ngoogleForms:\n  formId: \"$formId\"\n  width: \"640\"\n  height: \"800\"\n"
        }
        siteYml.writeText(updatedContent, UTF_8)
    }

    @Given("the bakery DSL defines googleForms.formId as {string}")
    fun bakeryDslDefinesGoogleFormsFormId(formId: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val buildFile = projectDir.resolve("build.gradle.kts")
        val content = buildFile.readText(UTF_8)
        // Add googleForms DSL block inside bakery { }
        val updatedContent = content.replace(
            Regex("(bakery\\s*\\{)"),
            "$1\n    googleForms {\n        formId = \"$formId\"\n    }"
        )
        buildFile.writeText(updatedContent, UTF_8)
    }

    @Given("gradle.properties with bakery.googleForms.formId = {string}")
    fun gradlePropertiesWithConfig(formId: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val gradleProps = projectDir.resolve("gradle.properties")
        val existing = if (gradleProps.exists()) gradleProps.readText(UTF_8) else ""
        gradleProps.writeText(existing + "\nbakery.googleForms.formId=$formId\n", UTF_8)
    }

    @Then("the file {string} should contain {string}")
    fun fileShouldContain(filePath: String, expectedContent: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val file = projectDir.resolve(filePath)
        assertThat(file)
            .describedAs("File $filePath should exist")
            .exists()
            .isFile
        val content = file.readText(UTF_8)
        assertThat(content)
            .describedAs("File $filePath should contain '$expectedContent'")
            .contains(expectedContent)
    }

    private fun defaultSiteYml(): String = """
        |bake:
        |  srcPath: "site"
        |  destDirPath: "build/bake"
        |pushPage:
        |  from: "site"
        |  to: "cvs"
        |  repo:
        |    name: "test-site"
        |    repository: "https://github.com/user/repo.git"
        |    credentials:
        |      username: "user"
        |      password: "token"
        |  branch: "main"
        |  message: "Deploy test"
        |pushMaquette:
        |  from: "maquette"
        |  to: "cvs"
        |  repo:
        |    name: "test-maquette"
        |    repository: "https://github.com/user/maquette.git"
        |    credentials:
        |      username: "user"
        |      password: "token"
        |  branch: "main"
        |  message: "Deploy maquette"
    """.trimMargin()
}