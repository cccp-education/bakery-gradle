package bakery.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import kotlin.text.Charsets.UTF_8

class AccessibilityAuditSteps(
    private val world: BakeryWorld,
) {
    @When("I list the tasks in group {string}")
    fun listTasksInGroup(group: String) =
        runBlocking {
            world.executeGradle("tasks", "--group", group)
        }

    @Then("the task {string} should be registered")
    fun assertTaskRegistered(taskName: String) {
        val output = world.buildResult?.output ?: ""
        assertThat(output).describedAs("Task list should contain '$taskName'").contains(taskName)
    }

    @Given("a new Bakery project with a11y failOnNonCompliant enabled")
    fun createBakeryProjectWithA11yGateEnabled() {
        world.createGradleProjectWithSiteConfigured(iaEnabled = false)
        val buildFile = world.projectDir!!.resolve("build.gradle.kts")
        val existing = buildFile.readText(UTF_8)
        buildFile.writeText(
            existing + "\nbakery { a11y { failOnNonCompliant = true }\n}\n",
            UTF_8,
        )
        assertThat(world.projectDir).exists()
    }

    @Given("the baked directory contains {string} with inline contrast {string}")
    fun createBakedHtmlWithInlineContrast(
        fileName: String,
        contrastSpec: String,
    ) {
        val (fg, bg) = contrastSpec.split(" on ").map { it.trim() }
        val bakedDir = bakedDir().apply { mkdirs() }
        bakedDir.resolve(fileName).writeText(
            """
            <p style="color: $fg; background-color: $bg;">Sample text</p>
            """.trimIndent(),
            UTF_8,
        )
    }

    @Given("the baked directory contains {string} with img without alt")
    fun createBakedHtmlWithImgWithoutAlt(fileName: String) {
        val bakedDir = bakedDir().apply { mkdirs() }
        bakedDir.resolve(fileName).writeText(
            """<img src="logo.png">""".trimIndent(),
            UTF_8,
        )
    }

    @Given("the baked directory contains {string} with heading skip from h1 to h3")
    fun createBakedHtmlWithHeadingSkip(fileName: String) {
        val bakedDir = bakedDir().apply { mkdirs() }
        bakedDir.resolve(fileName).writeText(
            """<h1>Titre</h1><h3>Sous-titre</h3>""".trimIndent(),
            UTF_8,
        )
    }

    @Given("the baked directory is empty")
    fun createEmptyBakedDir() {
        bakedDir().apply { mkdirs() }
    }

    /**
     * BKY-A11Y-2 — Le répertoire baké est la sortie de `bake`, soit
     * `buildDirectory/<destDirPath>`. La fixture `createGradleProjectWithSiteConfigured`
     * déclare `destDirPath: build/bake` (convention non-canonique, le scaffold bakery
     * officiel utilise `destDirPath: bake`), donc la sortie réelle est `build/build/bake`.
     * On résout dynamiquement pour ne pas coder en dur un chemin dépendant de la fixture.
     */
    private fun bakedDir(): java.io.File {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val destDirPath =
            projectDir
                .resolve("site.yml")
                .readText(UTF_8)
                .lineSequence()
                .firstOrNull { it.trimStart().startsWith("destDirPath:") }
                ?.substringAfter("destDirPath:")
                ?.trim()
                ?.trim('"')
                ?: "bake"
        return projectDir.resolve("build").resolve(destDirPath)
    }

    @When("I am executing the a11y task {string}")
    fun runA11yTaskByName(taskName: String) =
        runBlocking {
            world.executeGradle(taskName)
        }

    @When("I am executing the a11y task {string} expecting failure")
    fun runA11yTaskByNameExpectingFailure(taskName: String) {
        world.executeGradleExpectingFailure(taskName)
    }

    @Then("the report {string} should contain {string}")
    fun assertReportContains(
        relativePath: String,
        expected: String,
    ) {
        val reportFile = world.projectDir!!.resolve(relativePath)
        assertThat(reportFile)
            .describedAs("Report $relativePath should exist")
            .exists()
            .isFile
        assertThat(reportFile.readText(UTF_8))
            .describedAs("Report content should contain '$expected'")
            .contains(expected)
    }
}
