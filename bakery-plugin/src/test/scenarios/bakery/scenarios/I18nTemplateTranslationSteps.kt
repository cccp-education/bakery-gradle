package bakery.scenarios

import bakery.i18n.TemplateLanguageAttribute
import bakery.i18n.TemplateTextTranslator
import bakery.i18n.TemplateTranslationPlanner
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

/**
 * CHE-I18N-22 US-7 — BDD coverage of the template translation domain.
 *
 * No Gradle, no LLM: the fake service prefixes the target language so the
 * substitution, the plan and the ink-economy rules are observable. Every step is
 * prefixed with "template" so the shared glue namespace never collides (bug S-088).
 */
class I18nTemplateTranslationSteps {

    private val reference =
        linkedMapOf(
            "hero.thyme" to
                """
                <html lang="fr" data-bs-theme="light">
                <h1 class="display-5 fw-bold mb-5">
                    Développeur
                    <span class="text-primary">spécialisé en Ingénierie Pédagogique</span>
                </h1>
                <a data-lang="fr">Français</a>
                <link rel="alternate" hreflang="fr" href="x"/>
                <script>var label = "Développeur";</script>
            """.trimIndent(),
            "blog.thyme" to "<p>Derniers articles et ressources</p>",
        )

    private var target: Map<String, String> = emptyMap()
    private var output: String = ""
    private var aligned: String = ""
    private var plan: List<String> = emptyList()

    private class PrefixService : TranslationService {
        override fun translate(request: TranslationRequest): TranslationResult =
            TranslationResult.Success("${request.targetLanguage.uppercase()}:${request.sourceText}")
    }

    @Given("a template translation fixture with a French reference")
    fun fixture() {
        target = emptyMap()
        output = ""
        plan = emptyList()
    }

    @Given("a template translating service that prefixes the target language")
    fun service() {
        // The prefix service is instantiated per scenario below.
    }

    @When("I translate the template fixture into {string}")
    fun translate(language: String) {
        val translator = TemplateTextTranslator(PrefixService())
        output = translator.translate(reference.getValue("hero.thyme"), "fr", language).content
    }

    @Then("the translated template contains {string}")
    fun contains(fragment: String) {
        assertThat(output).contains(fragment)
    }

    @Then("the translated template preserves the class {string}")
    fun preservesClass(value: String) {
        assertThat(output).contains("class=\"$value\"")
    }

    @Then("the translated template preserves the attribute {string}")
    fun preservesAttribute(value: String) {
        assertThat(output).contains(value)
    }

    @Then("the translated template preserves the script body")
    fun preservesScript() {
        assertThat(output).contains("""var label = "Développeur";""")
    }

    @Given("a template variant with only the translated hero")
    fun variantPartial() {
        target = mapOf("hero.thyme" to "<h1>Entwickler</h1>")
    }

    @Given("a template variant with every template translated")
    fun variantComplete() {
        target = reference.mapValues { (_, content) -> "DE:$content" }
    }

    @When("I plan the templates for {string}")
    fun planFor(language: String) {
        plan = TemplateTranslationPlanner.plan(reference, target)
    }

    @When("I plan the templates for {string} forced")
    fun planForced(language: String) {
        plan = TemplateTranslationPlanner.plan(reference, target, force = true)
    }

    @Then("the template plan contains {string}")
    fun planContains(path: String) {
        assertThat(plan).contains(path)
    }

    @Then("the template plan does not contain {string}")
    fun planDoesNotContain(path: String) {
        assertThat(plan).doesNotContain(path)
    }

    @Then("the template plan is empty")
    fun planEmpty() {
        assertThat(plan).isEmpty()
    }

    @When("I align the html lang of the fixture into {string}")
    fun align(language: String) {
        aligned = TemplateLanguageAttribute.ensureLanguage(reference.getValue("hero.thyme"), language)
    }

    @Then("the aligned template declares the html lang {string}")
    fun declaresLang(language: String) {
        assertThat(aligned).contains("""<html lang="$language"""")
    }

    @Then("the aligned template preserves the attribute {string}")
    fun alignedPreserves(value: String) {
        assertThat(aligned).contains(value)
    }
}
