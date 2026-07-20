package bakery.scenarios

import document.translation.plantuml.PlantUmlTranslationAdapter
import document.translation.PivotBlock
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

class PlantUmlTranslationSteps {

    private lateinit var adapter: PlantUmlTranslationAdapter
    private lateinit var block: PivotBlock.Source
    private lateinit var result: PivotBlock.Source

    private val labelAwareTranslator: TranslationService = object : TranslationService {
        override fun translate(request: TranslationRequest): TranslationResult {
            val translated = request.sourceText
                .replace("Utilisateur", "User")
                .replace("Service", "Service")
                .replace("Client", "Customer")
                .replace("Serveur", "Server")
                .replace("Requête", "Request")
                .replace("Demande", "Request")
                .replace("Réponse", "Response")
                .replace("Référentiel", "Referential")
                .replace("Évaluation", "Assessment")
            return TranslationResult.Success(translated)
        }
    }

    private val failingTranslator: TranslationService = object : TranslationService {
        override fun translate(request: TranslationRequest): TranslationResult =
            TranslationResult.Failure("boom")
    }

    @Given("a plantuml translation adapter with a label-aware fake translator")
    fun plantumlAdapterWithLabelAwareTranslator() {
        adapter = PlantUmlTranslationAdapter(labelAwareTranslator)
    }

    @Given("a plantuml translation adapter with a failing translator")
    fun plantumlAdapterWithFailingTranslator() {
        adapter = PlantUmlTranslationAdapter(failingTranslator)
    }

    @Given("a source block of language {string} with content {string}")
    fun sourceBlockOfLanguageWithContent(language: String, content: String) {
        block = PivotBlock.Source(language = language, content = content)
    }

    @Given("a plantuml source block with only technical syntax")
    fun plantumlSourceBlockWithOnlyTechnicalSyntax() {
        block = PivotBlock.Source(
            language = "plantuml",
            content = """
            @startuml
            class User
            class Service
            User --> Service
            @enduml
            """.trimIndent()
        )
    }

    @Given("a plantuml source block with translatable labels {string} and {string}")
    fun plantumlSourceBlockWithTranslatableLabels(label1: String, label2: String) {
        block = PivotBlock.Source(
            language = "plantuml",
            content = """
            @startuml
            class "$label1"
            class "$label2"
            "$label1" --> "$label2" : "Requête"
            @enduml
            """.trimIndent()
        )
    }

    @Given("a plantuml source block with borrowed vocabulary {word} and {word} and a translatable label {string}")
    fun plantumlSourceBlockWithBorrowedVocabularyAndLabel(term1: String, term2: String, label: String) {
        block = PivotBlock.Source(
            language = "plantuml",
            content = """
            @startuml
            class "$term1"
            class "$term2"
            "$term1" --> "$term2" : "$label"
            @enduml
            """.trimIndent()
        )
    }

    @Given("a plantuml source block with borrowed vocabulary {word} and a translatable label {string}")
    fun plantumlSourceBlockWithBorrowedVocabularyAndOneLabel(term: String, label: String) {
        block = PivotBlock.Source(
            language = "plantuml",
            content = """
            @startuml
            class "$term"
            class "$label"
            @enduml
            """.trimIndent()
        )
    }

    @Given("a plantuml source block with a code identifier {string} and a label {string}")
    fun plantumlSourceBlockWithCodeIdentifierAndLabel(identifier: String, label: String) {
        block = PivotBlock.Source(
            language = "plantuml",
            content = """
            @startuml
            class "$identifier"
            class "$label"
            @enduml
            """.trimIndent()
        )
    }

    @When("the plantuml adapter translates the block from {word} to {word}")
    fun plantumlAdapterTranslatesBlock(sourceLanguage: String, targetLanguage: String) {
        result = adapter.translate(block, sourceLanguage, targetLanguage)
    }

    @Then("the returned block content should be {string}")
    fun returnedBlockContentShouldBe(content: String) {
        assertThat(result.content).isEqualTo(content)
    }

    @Then("the returned block content should equal the original block content")
    fun returnedBlockContentShouldEqualOriginal() {
        assertThat(result.content).isEqualTo(block.content)
    }

    @Then("the returned block content should contain {string}")
    fun returnedBlockContentShouldContain(fragment: String) {
        assertThat(result.content).contains(fragment)
    }

    @Then("the returned block content should not contain {string}")
    fun returnedBlockContentShouldNotContain(fragment: String) {
        assertThat(result.content).doesNotContain(fragment)
    }

    @Then("the returned block language should be {string}")
    fun returnedBlockLanguageShouldBe(language: String) {
        assertThat(result.language).isEqualTo(language)
    }

    @And("the returned block content should equal the original block language")
    fun returnedBlockContentShouldEqualOriginalLanguage() {
        assertThat(result.language).isEqualTo(block.language)
    }
}