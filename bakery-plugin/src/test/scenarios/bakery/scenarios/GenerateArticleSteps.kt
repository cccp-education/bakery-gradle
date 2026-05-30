package bakery.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat

class GenerateArticleSteps(private val world: BakeryWorld) {

    @Given("a new Bakery project with site configured")
    fun createBakeryProjectWithSiteConfigured() {
        world.createGradleProjectWithSiteConfigured()
        assertThat(world.projectDir).exists()
    }

    @When("I check for task {string}")
    fun checkForTask(taskName: String) = runBlocking {
        world.executeGradle("tasks", "--all")
    }

    @When("I execute task {string} with topic {string}")
    fun executeTaskWithTopic(taskName: String, topic: String) = runBlocking {
        try {
            world.executeGradle(taskName, "-Ptopic=$topic")
        } catch (_: Exception) {
            // L'erreur est capturée dans world.exception
        }
    }

    @Then("task {string} should be registered")
    fun taskShouldBeRegistered(taskName: String) {
        val output = world.buildResult?.output
        assertThat(output)
            .describedAs("Task '$taskName' should appear in 'tasks' output")
            .contains(taskName)
    }

    @Then("task {string} should be in group {string}")
    fun taskShouldBeInGroup(taskName: String, group: String) {
        val output = world.buildResult?.output
        assertThat(output)
            .describedAs("Task '$taskName' should be in group '$group'")
            .contains(group)
    }

    @Then("the build should fail")
    fun buildShouldFail() {
        // world.exception est set si executeGradle a lancé une exception
        // ou buildResult peut contenir un échec
        assertThat(world.exception)
            .describedAs("Build should have failed")
            .isNotNull
    }

    @Then("the output should contain {string}")
    fun outputShouldContain(text: String) {
        val output = world.exception?.message ?: world.buildResult?.output
        assertThat(output)
            .describedAs("Output should contain '$text'")
            .contains(text)
    }
}
