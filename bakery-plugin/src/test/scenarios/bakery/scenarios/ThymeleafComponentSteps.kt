package bakery.scenarios

import io.cucumber.java.en.Then
import org.assertj.core.api.Assertions.assertThat
import kotlin.text.Charsets.UTF_8

class ThymeleafComponentSteps(private val world: BakeryWorld) {

    @Then("the project should have a file named {string} in the site templates directory")
    fun checkTemplateFileExists(fileName: String) {
        val templateFile = world.projectDir!!.resolve("site/templates/$fileName")
        assertThat(templateFile)
            .describedAs("site/templates/$fileName should exist")
            .exists()
            .isFile
    }

    @Then("the file {string} should contain {string}")
    fun checkFileContains(filePath: String, expectedContent: String) {
        val file = world.projectDir!!.resolve(filePath)
        assertThat(file)
            .describedAs("File $filePath should exist")
            .exists()
            .isFile
        assertThat(file.readText(UTF_8))
            .describedAs("File $filePath should contain '$expectedContent'")
            .contains(expectedContent)
    }
}