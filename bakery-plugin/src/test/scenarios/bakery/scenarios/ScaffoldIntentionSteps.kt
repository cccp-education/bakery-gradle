package bakery.scenarios

import io.cucumber.java.en.Given
import org.assertj.core.api.Assertions.assertThat

class ScaffoldIntentionSteps(private val world: BakeryWorld) {

    @Given("a new Bakery project with scaffold intention configured with description {string} and siteType {string}")
    fun createBakeryProjectWithScaffoldIntention(description: String, siteType: String) {
        world.createGradleProjectWithScaffoldIntention(description = description, siteType = siteType)
        assertThat(world.projectDir).exists()
    }

    @Given("a new Bakery project with minimal scaffolding configuration")
    fun createBakeryProjectWithMinimalScaffolding() {
        world.createGradleProjectWithScaffoldIntention(description = null, siteType = null)
        assertThat(world.projectDir).exists()
    }
}
