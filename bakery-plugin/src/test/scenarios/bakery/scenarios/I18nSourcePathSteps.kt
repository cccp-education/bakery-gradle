package bakery.scenarios

import bakery.i18n.path.ContentSourcePathResolver
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import java.nio.file.Files

class I18nSourcePathSteps {
    private lateinit var projectDir: File
    private var srcPath: String = ""
    private lateinit var lastResolved: File

    @Given("a content root {string} in the project dir")
    fun givenContentRoot(contentRoot: String) {
        projectDir = Files.createTempDirectory("i18n-source-path-").toFile()
        srcPath = contentRoot
    }

    @When("the source path {string} is resolved")
    fun whenSourcePathResolved(sourceDir: String) {
        lastResolved = ContentSourcePathResolver.resolve(projectDir, srcPath, sourceDir)
    }

    @Then("the resolved path equals the project dir joined with {string}")
    fun thenResolvedUnderProjectDir(relativePath: String) {
        assertThat(lastResolved).isEqualTo(projectDir.resolve(relativePath))
    }

    @Then("the resolved path equals {string}")
    fun thenResolvedEquals(path: String) {
        assertThat(lastResolved).isEqualTo(File(path))
    }
}