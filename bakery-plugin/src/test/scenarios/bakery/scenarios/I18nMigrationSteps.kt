package bakery.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import java.io.File

class I18nMigrationSteps(private val world: BakeryWorld) {

    @Given("a new Bakery project with site fully configured")
    fun createBakeryProjectWithSiteFullyConfigured() {
        world.createGradleProjectWithSiteConfigured(iaEnabled = true)
        assertThat(world.projectDir).exists()
    }

    @Given("a real site directory {string} with templates containing hardcoded French text")
    fun createRealSiteWithHardcodedText(name: String) {
        world.createGradleProjectWithSiteConfigured(iaEnabled = true)
        world.createRealSiteDirectory(name, withTemplates = true, alreadyI18n = false, emptyDivs = false)
        assertThat(world.realSiteDir).exists()
        assertThat(world.realSiteDir!!.resolve("templates")).exists().isDirectory
    }

    @Given("a real site directory {string} with NO templates directory")
    fun createRealSiteWithoutTemplates(name: String) {
        world.createGradleProjectWithSiteConfigured(iaEnabled = true)
        world.createRealSiteDirectory(name, withTemplates = false)
        assertThat(world.realSiteDir).exists()
        assertThat(world.realSiteDir!!.resolve("templates")).doesNotExist()
    }

    @Given("a real site directory {string} with templates already using th:text message keys")
    fun createRealSiteAlreadyI18n(name: String) {
        world.createGradleProjectWithSiteConfigured(iaEnabled = true)
        world.createRealSiteDirectory(name, withTemplates = true, alreadyI18n = true, emptyDivs = false)
        assertThat(world.realSiteDir).exists()
    }

    @Given("a real site directory {string} with templates containing only empty divs")
    fun createRealSiteWithEmptyDivs(name: String) {
        world.createGradleProjectWithSiteConfigured(iaEnabled = true)
        world.createRealSiteDirectory(name, withTemplates = true, alreadyI18n = false, emptyDivs = true)
        assertThat(world.realSiteDir).exists()
    }

    @And("i18n migration DSL configured with siteDir pointing to the real site and languages {string} and defaultLanguage {string} and dryRun {word}")
    fun configureI18nMigrationDslWithRealSite(
        languages: String,
        defaultLanguage: String,
        dryRun: String
    ) {
        val dryRunBool = dryRun.toBooleanStrict()
        val langList = languages.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val langListStr = langList.joinToString(", ") { "\"$it\"" }
        val siteDirPath = world.realSiteDir!!.absolutePath
        val i18nMigrationBlock = """
            i18nMigration {
                siteDir = "$siteDirPath"
                languages = listOf($langListStr)
                defaultLanguage = "$defaultLanguage"
                dryRun = $dryRunBool
            }
        """.trimIndent()
        val buildScriptContent = """
            bakery {
                configPath = file("site.yml").absolutePath
                $i18nMigrationBlock
            }
        """.trimIndent()
        world.projectDir!!.resolve("build.gradle.kts").writeText(
            """plugins { id("education.cccp.bakery") }
$buildScriptContent
"""
        )
    }

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

    @When("I am executing the task {string} with CLI options for the real site and languages {string} and dryRun {string}")
    fun runTaskWithCliOptionsForRealSite(taskName: String, languages: String, dryRun: String) = runBlocking {
        val siteDirPath = world.realSiteDir!!.absolutePath
        val args = listOf(
            taskName,
            "--i18nSite=$siteDirPath",
            "--i18nLangs=$languages",
            "--i18nDryRun=$dryRun"
        )
        world.executeGradle(*args.toTypedArray())
    }

    @When("I am executing the task {string} with arguments {string}")
    fun runTaskWithArguments(taskName: String, arguments: String) = runBlocking {
        val args = listOf(taskName) + arguments.split(" ").filter { it.isNotBlank() }
        world.executeGradle(*args.toTypedArray())
    }

    @And("no messages_{word}.properties file should exist in the templates directory")
    fun noMessagesFileShouldExist(lang: String) {
        val messagesFile = world.realSiteDir!!.resolve("templates/messages_$lang.properties")
        assertThat(messagesFile).doesNotExist()
    }

    @And("the templates should NOT be modified")
    fun templatesShouldNotBeModified() {
        val headerFile = world.realSiteDir!!.resolve("templates/header.thyme")
        val content = headerFile.readText()
        assertThat(content).doesNotContain("th:text=")
    }

    @And("messages_{word}.properties should exist in the templates directory")
    fun messagesFileShouldExist(lang: String) {
        val messagesFile = world.realSiteDir!!.resolve("templates/messages_$lang.properties")
        assertThat(messagesFile).exists().isFile
    }

    @And("the templates should contain th:text message keys")
    fun templatesShouldContainThText() {
        val headerFile = world.realSiteDir!!.resolve("templates/header.thyme")
        val content = headerFile.readText()
        assertThat(content).contains("th:text=")
    }

    @And("site.language={word} should be injected in jbake.properties")
    fun siteLanguageShouldBeInjected(lang: String) {
        val jbakeProps = world.realSiteDir!!.resolve("jbake.properties")
        val content = jbakeProps.readText()
        assertThat(content).contains("site.language=$lang")
    }

    @And("no new messages files should be created")
    fun noNewMessagesFilesShouldBeCreated() {
        val templatesDir = world.realSiteDir!!.resolve("templates")
        val messagesFiles = templatesDir.listFiles()?.filter { it.name.startsWith("messages_") && it.name.endsWith(".properties") } ?: emptyList()
        assertThat(messagesFiles).isEmpty()
    }
}
