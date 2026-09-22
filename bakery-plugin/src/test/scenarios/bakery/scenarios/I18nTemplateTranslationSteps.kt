package bakery.scenarios

import bakery.i18n.TemplateAttributeRepair
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

    @Given("a contact form fixture with a French placeholder")
    fun contactForm() {
        output = ""
    }

    @When("I translate the contact form into {string}")
    fun translateContact(language: String) {
        val translator = TemplateTextTranslator(PrefixService())
        output = translator.translate(CONTACT_FORM, "fr", language).content
    }

    @Then("the translated contact form contains {string}")
    fun contactContains(fragment: String) {
        assertThat(output).contains(fragment)
    }

    @Then("the translated contact form preserves the attribute {string}")
    fun contactPreserves(value: String) {
        assertThat(output).contains(value)
    }

    @Given("a translated variant whose placeholder is still French")
    fun preservedVariantFrenchPlaceholder() {
        variant = PRESERVED_VARIANT
        pending = emptyList()
    }

    @Given("a translated variant whose placeholder is already translated")
    fun preservedVariantTranslatedPlaceholder() {
        variant = REPAIRED_VARIANT
        pending = emptyList()
    }

    @When("I repair the variant attributes into {string}")
    fun repairVariant(language: String) {
        val reference = CONTACT_FORM
        pending = TemplateAttributeRepair.pending(reference, variant)
        if (pending.isEmpty()) return
        val values = TemplateTextTranslator(PrefixService()).translateValues(pending, "fr", language)
        variant = TemplateAttributeRepair.repair(variant, values.replacements)
    }

    @When("I repair the variant attributes into {string} again")
    fun repairVariantAgain(language: String) {
        val reference = CONTACT_FORM
        val second = TemplateAttributeRepair.pending(reference, variant)
        val values = TemplateTextTranslator(PrefixService()).translateValues(second, "fr", language)
        variantAfterFirstRepair = variant
        variant = TemplateAttributeRepair.repair(variant, values.replacements)
    }

    @Then("the repaired variant contains {string}")
    fun repairedContains(fragment: String) {
        assertThat(variant).contains(fragment)
    }

    @Then("the repaired variant preserves the already translated {string}")
    fun repairedPreserves(fragment: String) {
        assertThat(variant).contains(fragment)
    }

    @Then("the repaired variant is unchanged by the second repair")
    fun repairedIdempotent() {
        assertThat(variant).isEqualTo(variantAfterFirstRepair)
    }

    @Then("the repaired variant reports nothing pending")
    fun repairedNothingPending() {
        assertThat(pending).isEmpty()
    }

    private var variant: String = ""
    private var variantAfterFirstRepair: String = ""
    private var pending: List<String> = emptyList()

    private companion object {
        val CONTACT_FORM =
            """
            <form id="contact-form" data-lang="fr">
                <input type="text" name="name" class="form-control" placeholder="Nom" required />
                <textarea name="message" rows="5" placeholder="Votre message" required></textarea>
                <a aria-label="Retour en haut de page" href="#"><i class="bi"></i></a>
            </form>
            """.trimIndent()

        val PRESERVED_VARIANT =
            """
            <form id="contact-form" data-lang="fr">
                <input type="text" name="name" class="form-control" placeholder="Nom" required />
                <textarea name="message" rows="5" placeholder="Your message" required></textarea>
            </form>
            """.trimIndent()

        val REPAIRED_VARIANT =
            """
            <form id="contact-form" data-lang="fr">
                <input type="text" name="name" class="form-control" placeholder="Name" required />
                <textarea name="message" rows="5" placeholder="Your message" required></textarea>
            </form>
            """.trimIndent()
    }
}
