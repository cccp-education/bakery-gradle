package bakery.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import java.io.File

class GeneratePivotYamlSteps(private val world: BakeryWorld) {

    @Given("a new Bakery project without site.yml")
    fun createBakeryProjectWithoutSiteYml() {
        val pluginId = "education.cccp.bakery"
        File.createTempFile("gradle-pivot-", "").apply {
            delete()
            mkdirs()
        }.run {
            resolve("settings.gradle.kts").writeText(
                "pluginManagement.repositories.gradlePluginPortal()\n" +
                    "rootProject.name = \"${name}\""
            )
            resolve("build.gradle.kts").writeText(
                "plugins { id(\"$pluginId\") }\n"
            )
            world.projectDir = this
        }
        assertThat(world.projectDir).exists()
    }

    @Given("an AsciiDoc input file {string} with frontmatter and heading")
    fun createAsciiDocInputFile(fileName: String) {
        val dir = world.projectDir!!
        dir.resolve(fileName).writeText(
            """
            title=Test pivot BDD
            date=2026-06-24
            type=page
            status=published
            ~~~~~~

            == Titre principal BDD

            Paragraphe de contenu pour test BDD.
            """.trimIndent()
        )
        assertThat(dir.resolve(fileName)).exists()
    }

    @When("I execute generatePivotYaml with input {string} and output {string}")
    fun executeGeneratePivotYaml(input: String, output: String) = runBlocking {
        try {
            world.executeGradle("generatePivotYaml", "-Pinput=$input", "-Poutput=$output")
        } catch (_: Exception) {
            // capturé dans world.exception
        }
    }

    @Then("the build should succeed")
    fun buildShouldSucceed() {
        assertThat(world.exception)
            .describedAs("Build should have succeeded (no exception expected)")
            .isNull()
    }

    @Then("the output file {string} should contain {string}")
    fun outputFileShouldContain(fileName: String, expected: String) {
        val dir = world.projectDir!!
        val file = dir.resolve(fileName)
        assertThat(file).exists()
        assertThat(file.readText())
            .describedAs("Output file '$fileName' should contain '$expected'")
            .contains(expected)
    }

    @Then("the build should fail with missing input {string}")
    fun buildShouldFailWithMissingInput(fileName: String) {
        assertThat(world.exception)
            .describedAs("Build should have failed for missing input '$fileName'")
            .isNotNull()
    }
}