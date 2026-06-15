package bakery.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import java.io.File
import kotlin.text.Charsets.UTF_8

class I18nSteps(private val world: BakeryWorld) {

    @Given("the site configuration contains language {string}")
    fun siteConfigContainsLanguage(lang: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val siteYml = projectDir.resolve("site.yml")
        val base = if (siteYml.exists() && siteYml.readText(UTF_8).isNotBlank()) {
            siteYml.readText(UTF_8)
        } else {
            """
            bake:
              srcPath: "site"
              destDirPath: "build/bake"
            pushPage:
              from: "site"
              to: "cvs"
              repo:
                name: "test-site"
                repository: "https://github.com/user/repo.git"
                credentials:
                  username: "user"
                  password: "token"
              branch: "main"
              message: "Deploy test"
            pushMaquette:
              from: "maquette"
              to: "cvs"
            """.trimIndent()
        }
        val updatedContent = if (base.contains("language:")) {
            base.replace(Regex("language:\\s*\"?[^\"]*\"?"), "language: \"$lang\"")
        } else {
            base.trimEnd() + "\nlanguage: \"$lang\"\n"
        }
        siteYml.writeText(updatedContent, UTF_8)
    }

    @And("the bakery DSL defines language {string}")
    fun bakeryDslDefinesLanguage(lang: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val buildFile = projectDir.resolve("build.gradle.kts")
        val content = buildFile.readText(UTF_8)
        val updatedContent = content.replace(
            Regex("(bakery\\s*\\{\\s*)(configPath)"),
            "$1language = \"$lang\"\n    $2"
        )
        buildFile.writeText(updatedContent, UTF_8)
    }

    @Given("a new Bakery project with scaffold intention configured with description {string} and siteType {string} and lang {string}")
    fun createBakeryProjectWithScaffoldIntentionAndLang(description: String, siteType: String, lang: String) {
        world.createGradleProjectWithScaffoldIntention(
            description = description,
            siteType = siteType,
            lang = lang
        )
    }
}
