package bakery.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.text.Charsets.UTF_8

class CiCdCherolivSteps(private val world: BakeryWorld) {

    private val jbakeSourcePath = System.getenv("OFFICE_PATH")
        ?.let { "$it/sites/cheroliv.com/jbake" }
        ?: "/home/cheroliv/workspace/office/sites/cheroliv.com/jbake"

    @Given("the cheroliv.com jbake source in the engine-style project")
    fun copyCherolivJbakeSource() {
        world.createGradleProject()
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")

        val sourceJbake = File(jbakeSourcePath)
        assertThat(sourceJbake)
            .describedAs("Source jbake directory must exist at $jbakeSourcePath")
            .exists()
            .isDirectory

        val targetJbake = projectDir.resolve("jbake")
        sourceJbake.copyRecursively(targetJbake, overwrite = true)
        assertThat(targetJbake.resolve("jbake.properties"))
            .describedAs("jbake.properties must be copied")
            .exists()
        assertThat(targetJbake.resolve("templates"))
            .describedAs("templates must be copied")
            .exists()
        assertThat(targetJbake.resolve("content"))
            .describedAs("content must be copied")
            .exists()
    }

    @And("a {string} with bake config and empty credentials")
    fun writeSiteYmlWithEmptyCredentials(fileName: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val siteYml = projectDir.resolve(fileName)
        siteYml.writeText(
            """
            bake:
              srcPath: "jbake"
              destDirPath: "bake"
              cname: "cheroliv.com"
            pushPage:
              from: "bake"
              to: "cvs"
              repo:
                name: "cheroliv.github.io"
                repository: "https://github.com/cheroliv/cheroliv.github.io.git"
                credentials:
                  username: ""
                  password: ""
              branch: "main"
              message: "cheroliv.com"
            """.trimIndent(),
            UTF_8
        )
        assertThat(siteYml.readText(UTF_8))
            .describedAs("site.yml must contain credentials block")
            .contains("credentials")
    }

    @When("I run the {string} task for cheroliv.com")
    fun runTaskForCheroliv(taskName: String) = runBlocking {
        world.executeGradle(taskName)
    }

    @Then("the baked output should contain {string}")
    fun checkBakedOutputContains(fileName: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val bakedFile = projectDir.resolve("build/bake").resolve(fileName)
        assertThat(bakedFile)
            .describedAs("Baked file $fileName must exist")
            .exists()
    }

    @Then("the task {string} should be listed in the output")
    fun checkTaskListed(taskName: String) {
        val output = world.buildResult?.output ?: ""
        assertThat(output)
            .describedAs("Output must contain '$taskName'")
            .contains(taskName)
    }

    @Then("the baked article {string} should contain {string}")
    fun checkArticleContains(path: String, expected: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val article = projectDir.resolve("build/bake").resolve(path)
        assertThat(article)
            .describedAs("Article $path must exist")
            .exists()
        assertThat(article.readText(UTF_8))
            .describedAs("Article $path must contain '$expected'")
            .contains(expected)
    }
}
