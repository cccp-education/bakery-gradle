package bakery.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat

class I18nMigrationSteps(private val world: BakeryWorld) {

    @Given("a new Bakery project with i18n migration intention configured with siteDir {string} and languages {string} and defaultLanguage {string} and dryRun {word}")
    fun createBakeryProjectWithI18nMigrationIntention(
        siteDir: String,
        languages: String,
        defaultLanguage: String,
        dryRun: String
    ) {
        val dryRunBool = dryRun.toBooleanStrict()
        val langList = languages.split(",").map { it.trim() }.filter { it.isNotBlank() }
        world.migrationSiteDir = siteDir
        world.createGradleProjectWithI18nMigrationIntention(
            siteDir = siteDir,
            languages = langList,
            defaultLanguage = defaultLanguage,
            dryRun = dryRunBool
        )
        assertThat(world.projectDir).exists()
    }

    @Given("a new Bakery project with site fully configured")
    fun createBakeryProjectWithSiteFullyConfigured() {
        world.createGradleProjectWithSiteConfigured(iaEnabled = true)
        assertThat(world.projectDir).exists()
    }

    @When("I am executing the task {string} with arguments {string}")
    fun runTaskWithArguments(taskName: String, arguments: String) = runBlocking {
        val args = listOf(taskName) + arguments.split(" ").filter { it.isNotBlank() }
        world.executeGradle(*args.toTypedArray())
    }
}
