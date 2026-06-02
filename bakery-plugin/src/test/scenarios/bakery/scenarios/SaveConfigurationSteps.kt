package bakery.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.text.Charsets.UTF_8

/**
 * Cucumber steps for BKY-CONV-1.7: saveConfiguration persistence
 *
 * Tests that collectSiteConfig with -P flags correctly persists
 * push credentials into site.yml via File.saveConfiguration().
 */
class SaveConfigurationSteps(private val world: BakeryWorld) {

    @Given("a new Bakery project with fully configured site for collectSiteConfig")
    fun createFullyConfiguredProject() {
        world.createGradleProjectWithSiteConfigured(iaEnabled = false)
    }

    @When("I execute task {string} with github username {string} and repository {string} and token {string}")
    fun executeCollectSiteConfigWithCredentials(
        taskName: String,
        username: String,
        repository: String,
        token: String
    ) {
        val projectDir = world.projectDir
            ?: throw IllegalStateException("Project dir not initialized")

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                taskName,
                "-PgithubUsername=$username",
                "-PgithubRepo=$repository",
                "-PgithubToken=$token"
            )
            .build()

        world.buildResult = result
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }
}