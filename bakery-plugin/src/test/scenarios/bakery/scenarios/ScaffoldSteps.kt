package bakery.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner.create
import java.io.File
import kotlin.text.Charsets.UTF_8

class ScaffoldSteps(private val world: BakeryWorld) {

    var lastFailedOutput: String? = null
    private val props = linkedMapOf("configPath" to "file(\"site.yml\").absolutePath")

    @And("the bakery extension defines {string} as {string}")
    fun defineExtensionProperty(key: String, value: String) {
        props[key] = when (key) {
            "sitesBaseDir" -> "file(\"$value\").absolutePath"
            "siteName" -> "\"$value\""
            else -> "file(\"$value\").absolutePath"
        }
        rebuildBuildFile()
    }

    @And("the bakery extension does not define {string}")
    fun ensureExtensionPropertyNotDefined(key: String) {
        props.remove(key)
        rebuildBuildFile()
    }

    @When("I am executing the task {string} expecting failure")
    fun runTaskExpectingFailure(taskName: String) {
        val result = create()
            .withProjectDir(world.projectDir!!)
            .withArguments(taskName, "--stacktrace")
            .withPluginClasspath()
            .buildAndFail()
        world.buildResult = result
        lastFailedOutput = result.output
    }

    @Then("the build output should contain {string}")
    fun checkBuildOutputContains(expected: String) {
        val output = lastFailedOutput ?: world.buildResult?.output ?: ""
        assertThat(output).describedAs("Build output should contain '$expected'").contains(expected)
    }

    @Then("the project directory {string} should exist")
    fun checkProjectDirectoryExists(relativePath: String) {
        assertThat(world.projectDir!!.resolve(relativePath)).exists().isDirectory
    }

    @Then("the directory {string} should have a {string} file for site configuration")
    fun checkSiteConfigInDirectory(dirPath: String, configFileName: String) {
        assertThat(world.projectDir!!.resolve(dirPath).resolve(configFileName)).exists().isFile
    }

    @Then("the directory {string} should have a directory named {string} who contains {string} file")
    fun checkSiteFolderInDirectory(dirPath: String, siteDirName: String, fileName: String) {
        assertThat(world.projectDir!!.resolve(dirPath).resolve(siteDirName).resolve(fileName)).exists().isFile
    }

    @Then("the directory {string} should have a file named {string} who contains {string}, {string}, {string} and {string}")
    fun checkGitIgnoreInDirectory(
        dirPath: String, gitIgnoreFileName: String,
        config: String, dotGradle: String, buildDir: String, dotKotlin: String,
    ) {
        val file = world.projectDir!!.resolve(dirPath).resolve(gitIgnoreFileName)
        assertThat(file).exists().isFile
        assertThat(file.readText(UTF_8)).contains(config, dotGradle, buildDir, dotKotlin)
    }

    @Then("the directory {string} should have a file named {string} who contains {string} and {string}")
    fun checkGitAttributesInDirectory(dirPath: String, name: String, eol: String, crlf: String) {
        val file = world.projectDir!!.resolve(dirPath).resolve(name)
        assertThat(file).exists().isFile
        assertThat(file.readText(UTF_8)).contains(eol, crlf)
    }

    @And("the directory {string} already exists")
    fun createExistingDirectory(dirPath: String) {
        world.projectDir!!.resolve(dirPath).mkdirs()
        assertThat(world.projectDir!!.resolve(dirPath)).exists().isDirectory
    }

    private fun rebuildBuildFile() {
        val dslBody = props.entries.joinToString("\n") { (k, v) -> "    $k = $v" }
        world.projectDir!!.resolve("build.gradle.kts").writeText(
            """plugins { id("com.cheroliv.bakery") }
bakery {
$dslBody
}
""", UTF_8
        )
    }
}
