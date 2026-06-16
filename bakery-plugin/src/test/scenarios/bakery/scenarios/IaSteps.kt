package bakery.scenarios

import io.cucumber.java.en.Given
import org.assertj.core.api.Assertions.assertThat
import java.io.File

class IaSteps(private val world: BakeryWorld) {

    @Given("a new Bakery project with IA block")
    fun createProjectWithIaBlock() {
        world.createGradleProjectWithIA()
        assertThat(world.projectDir).exists()
    }
}
