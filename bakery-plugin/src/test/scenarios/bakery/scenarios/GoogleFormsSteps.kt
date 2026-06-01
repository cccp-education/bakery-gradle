package bakery.scenarios

import io.cucumber.java.en.Then
import org.assertj.core.api.Assertions.assertThat
import kotlin.text.Charsets.UTF_8

class GoogleFormsSteps(private val world: BakeryWorld) {

    @Then("the file {string} should not contain {string}")
    fun checkFileDoesNotContain(filePath: String, unexpectedContent: String) {
        val file = world.projectDir!!.resolve(filePath)
        assertThat(file)
            .describedAs("File $filePath should exist")
            .exists()
            .isFile
        assertThat(file.readText(UTF_8))
            .describedAs("File $filePath should NOT contain '$unexpectedContent'")
            .doesNotContain(unexpectedContent)
    }
}