package bakery.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import java.io.File

class ContactSecSteps(
    private val world: BakeryWorld,
) {
    private var endpointUrl: String = "https://script.example.com/exec"
    private var siteKey: String = "0xTESTKEY"
    private val supportedLangs = mutableListOf("fr", "en")
    private var defaultLang = "fr"
    private var hasContactConfig = true

    private val footerThymeTemplate =
        """
        <html xmlns:th="http://www.thymeleaf.org">
        <body>
        <footer>Old footer</footer>
        </body>
        </html>
        """.trimIndent()

    @Given("a contact-sec fixture site with 2 languages {string} and {string}")
    fun createContactFixture2(
        lang1: String,
        lang2: String,
    ) {
        supportedLangs.clear()
        supportedLangs.addAll(listOf(lang1, lang2))
        defaultLang = lang1
        hasContactConfig = true
        createFixtureSite()
    }

    @Given("a contact-sec fixture site without a contact config")
    fun createContactFixtureNoConfig() {
        hasContactConfig = false
        createFixtureSite()
    }

    @Given("a contact config with endpoint {string} and siteKey {string}")
    fun setContactConfig(
        endpoint: String,
        key: String,
    ) {
        endpointUrl = endpoint
        siteKey = key
        writeSiteYml()
    }

    @When("I run scaffoldContactSec")
    fun runScaffoldContactSec() {
        runBlocking {
            try {
                world.executeGradle("scaffoldContactSec")
            } catch (_: Exception) {
                // capturé dans world.exception
            }
        }
    }

    @Then("the FR footer.thyme contains a contact form")
    fun frFooterContainsContactForm() {
        val footer = world.projectDir!!.resolve("site/templates/footer.thyme")
        assertThat(footer).exists()
        assertThat(footer.readText()).contains("contact-form")
    }

    @Then("the FR footer.thyme contains a honeypot field")
    fun frFooterContainsHoneypot() {
        val footer = world.projectDir!!.resolve("site/templates/footer.thyme")
        assertThat(footer.readText()).contains("hp_name")
    }

    @Then("the FR footer.thyme contains a session_token field")
    fun frFooterContainsSessionToken() {
        val footer = world.projectDir!!.resolve("site/templates/footer.thyme")
        assertThat(footer.readText()).contains("session_token")
    }

    @Then("the FR footer.thyme contains a pow_nonce field")
    fun frFooterContainsPowNonce() {
        val footer = world.projectDir!!.resolve("site/templates/footer.thyme")
        assertThat(footer.readText()).contains("pow_nonce")
    }

    @Then("the FR footer.thyme contains a Turnstile div with sitekey {string}")
    fun frFooterContainsTurnstileDiv(key: String) {
        val footer = world.projectDir!!.resolve("site/templates/footer.thyme")
        assertThat(footer.readText()).contains("cf-turnstile")
        assertThat(footer.readText()).contains("data-sitekey=\"$key\"")
    }

    @Then("the EN footer.thyme contains a contact form")
    fun enFooterContainsContactForm() {
        val footer = world.projectDir!!.resolve("site/en/templates/footer.thyme")
        assertThat(footer).exists()
        assertThat(footer.readText()).contains("contact-form")
    }

    @Then("the EN footer.thyme contains a honeypot field")
    fun enFooterContainsHoneypot() {
        val footer = world.projectDir!!.resolve("site/en/templates/footer.thyme")
        assertThat(footer.readText()).contains("hp_name")
    }

    @Then("the build succeeds")
    fun buildSucceeds() {
        assertThat(world.exception).isNull()
        assertThat(world.buildResult?.output).contains("BUILD SUCCESSFUL")
    }

    @Then("no contact form is scaffolded")
    fun noContactFormScaffolded() {
        val footer = world.projectDir!!.resolve("site/templates/footer.thyme")
        val content = if (footer.exists()) footer.readText() else ""
        assertThat(content).doesNotContain("contact-form")
        assertThat(world.buildResult?.output).contains("No contact config")
    }

    @Then("firestore.rules are generated at {string}")
    fun firestoreRulesGenerated(path: String) {
        val rules = world.projectDir!!.resolve(path)
        assertThat(rules).exists()
        assertThat(rules.readText()).contains("rules_version")
    }

    @Then("the firestore rules allow create only")
    fun firestoreRulesAllowCreateOnly() {
        val rules = world.projectDir!!.resolve("site/firestore.rules")
        val content = rules.readText()
        assertThat(content).contains("allow create:")
        assertThat(content).contains("allow read, update, delete: if false;")
    }

    @Then("the firestore rules enforce honeypot empty")
    fun firestoreRulesEnforceHoneypot() {
        val rules = world.projectDir!!.resolve("site/firestore.rules")
        assertThat(rules.readText()).contains("hp_name == ''")
    }

    @Then("the firestore rules enforce created_at equals request.time")
    fun firestoreRulesEnforceCreatedAt() {
        val rules = world.projectDir!!.resolve("site/firestore.rules")
        assertThat(rules.readText()).contains("created_at == request.time")
    }

    @Then("the firestore rules enforce whitelist of allowed fields")
    fun firestoreRulesEnforceWhitelist() {
        val rules = world.projectDir!!.resolve("site/firestore.rules")
        val content = rules.readText()
        assertThat(content).contains("name")
        assertThat(content).contains("email")
        assertThat(content).contains("subject")
        assertThat(content).contains("message")
        assertThat(content).contains("session_token")
    }

    private fun createFixtureSite() {
        val pluginId = "education.cccp.bakery"
        File
            .createTempFile("gradle-contact-", "")
            .apply {
                delete()
                mkdirs()
            }.run {
                resolve("settings.gradle.kts").writeText(
                    "pluginManagement.repositories.gradlePluginPortal()\n" +
                        "rootProject.name = \"${name}\"",
                )
                resolve("build.gradle.kts").writeText(
                    "plugins { id(\"$pluginId\") }\nbakery { configPath = \"site.yml\" }",
                )
                val siteDir = resolve("site")
                siteDir.resolve("templates").mkdirs()
                siteDir.resolve("content").mkdirs()
                siteDir.resolve("templates/footer.thyme").writeText(footerThymeTemplate)
                siteDir.resolve("content/index.html").writeText("<h1>Hello FR</h1>")
                for (lang in supportedLangs) {
                    if (lang == defaultLang) continue
                    val langDir = siteDir.resolve(lang)
                    langDir.resolve("templates").mkdirs()
                    langDir.resolve("content").mkdirs()
                    langDir.resolve("templates/footer.thyme").writeText(footerThymeTemplate)
                    langDir.resolve("content/index.html").writeText("<h1>Hello $lang</h1>")
                }
                world.projectDir = this
            }
        writeSiteYml()
    }

    private fun writeSiteYml() {
        val langsYaml = supportedLangs.joinToString(", ")
        val sb = StringBuilder()
        sb.append("bake:\n")
        sb.append("  srcPath: site\n")
        sb.append("  destDirPath: build/output\n")
        sb.append("language: $defaultLang\n")
        sb.append("supportedLanguages: [$langsYaml]\n")
        if (hasContactConfig) {
            sb.append("contact:\n")
            sb.append("  enabled: true\n")
            sb.append("  endpointUrl: \"$endpointUrl\"\n")
            sb.append("  firestoreCollection: contacts\n")
            sb.append("  turnstile:\n")
            sb.append("    siteKey: \"$siteKey\"\n")
            sb.append("  minRenderTimeMs: 2500\n")
            sb.append("  dailyGlobalCap: 50\n")
            sb.append("  rateLimit:\n")
            sb.append("    perHour: 3\n")
            sb.append("    perDay: 30\n")
        }
        world.projectDir!!.resolve("site.yml").writeText(sb.toString())
    }
}