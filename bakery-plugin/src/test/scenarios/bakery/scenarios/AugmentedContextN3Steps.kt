package bakery.scenarios

import bakery.SiteContextCollector
import bakery.lens.AugmentedContextDsl
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import java.nio.file.Files

/**
 * Cucumber steps pour BKY-LENS-5 — N3 Composite Context.
 *
 * Teste le pipeline collectSiteContext → collectWithAugmentedContext → metadata.json enrichi.
 * Les steps manipulent directement SiteContextCollector (tests unitaires BDD),
 * sans passer par Gradle Task (trop lent pour Cucumber).
 */
class AugmentedContextN3Steps {

    private lateinit var bakedDir: File
    private lateinit var outputDir: File
    private lateinit var augmentedContextDsl: AugmentedContextDsl
    private lateinit var metadataFile: File
    private val mapper = jacksonObjectMapper()

    @Given("a minimal baked site directory")
    fun `a minimal baked site directory`() {
        val tempRoot = Files.createTempDirectory("bakery-lens5-test-").toFile()
        tempRoot.deleteOnExit()
        bakedDir = tempRoot.resolve("bake")
        bakedDir.mkdirs()
        // Minimal feed.xml (empty)
        bakedDir.resolve("feed.xml").writeText(
            """<?xml version="1.0"?>
<rss version="2.0">
<channel><title>Test</title></channel>
</rss>"""
        )
        outputDir = tempRoot.resolve("output")
    }

    @Given("a composite-context.json file with {int} non-empty channels")
    fun `a composite-context json file with non-empty channels`(channelCount: Int) {
        val tempRoot = bakedDir.parentFile
        val compositeFile = tempRoot.resolve("composite-context.json")
        val sections = mapOf(
            "eagerSection" to "Eager governance content.",
            "ragSection" to "RAG similarity scores.",
            "graphifySection" to "Graphify structural relations.",
            "docsSection" to "Docs corpus documentation."
        )
        // Select only the requested number of non-empty channels
        val selectedSections = sections.entries.take(channelCount)
        val emptySections = sections.entries.drop(channelCount)
        val jsonBuilder = StringBuilder("{\n")
        selectedSections.forEach { (key, value) ->
            jsonBuilder.append("  \"$key\": \"$value\",\n")
        }
        emptySections.forEach { (key, _) ->
            jsonBuilder.append("  \"$key\": \"\",\n")
        }
        jsonBuilder.append("  \"config\": {\n")
        jsonBuilder.append("    \"totalTokenBudget\": 8000,\n")
        jsonBuilder.append("    \"budgetEagerLazy\": 0.40,\n")
        jsonBuilder.append("    \"budgetRag\": 0.30,\n")
        jsonBuilder.append("    \"budgetGraphify\": 0.20,\n")
        jsonBuilder.append("    \"budgetDocs\": 0.10,\n")
        jsonBuilder.append("    \"budgetOverhead\": 0.0\n")
        jsonBuilder.append("  }\n}")
        compositeFile.writeText(jsonBuilder.toString())

        augmentedContextDsl = AugmentedContextDsl()
        augmentedContextDsl.enabled = true
        augmentedContextDsl.contextPath = compositeFile.absolutePath
        augmentedContextDsl.maxArticles = 4
    }

    @Given("the augmented context is enabled with contextPath set to the composite-context file")
    fun `the augmented context is enabled with contextPath set to composite-context file`() {
        // DSL already configured in the previous step
        assertThat(augmentedContextDsl.enabled).isTrue()
    }

    @Given("the augmented context is enabled with contextPath set to a non-existent file")
    fun `the augmented context is enabled with contextPath set to non-existent file`() {
        augmentedContextDsl = AugmentedContextDsl()
        augmentedContextDsl.enabled = true
        augmentedContextDsl.contextPath = "/nonexistent/composite-context.json"
        augmentedContextDsl.maxArticles = 4
    }

    @Given("the augmented context is disabled")
    fun `the augmented context is disabled`() {
        augmentedContextDsl = AugmentedContextDsl()
        augmentedContextDsl.enabled = false
        augmentedContextDsl.contextPath = "/nonexistent/composite-context.json"
    }

    @When("I collect site context with augmented context")
    fun `i collect site context with augmented context`() {
        SiteContextCollector.collectWithAugmentedContext(bakedDir, outputDir, augmentedContextDsl)
        metadataFile = outputDir.resolve("metadata.json")
    }

    @Then("the metadata.json should contain {string}")
    fun `the metadata json should contain`(key: String) {
        assertThat(metadataFile).exists()
        val metadata: Map<String, Any> = mapper.readValue(metadataFile)
        assertThat(metadata).containsKey(key)
    }

    @Then("the metadata.json should not contain {string}")
    fun `the metadata json should not contain`(key: String) {
        assertThat(metadataFile).exists()
        val metadata: Map<String, Any> = mapper.readValue(metadataFile)
        assertThat(metadata).doesNotContainKey(key)
    }

    @Then("the augmentedEntries channels should have {int} entries")
    fun `the augmentedEntries channels should have entries`(count: Int) {
        val metadata: Map<String, Any> = mapper.readValue(metadataFile)
        @Suppress("UNCHECKED_CAST")
        val augmentedEntries = metadata["augmentedEntries"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val channels = augmentedEntries["channels"] as List<*>
        assertThat(channels).hasSize(count)
    }

    @Then("the augmentedEntries channels should be empty")
    fun `the augmentedEntries channels should be empty`() {
        val metadata: Map<String, Any> = mapper.readValue(metadataFile)
        @Suppress("UNCHECKED_CAST")
        val augmentedEntries = metadata["augmentedEntries"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val channels = augmentedEntries["channels"] as List<*>
        assertThat(channels).isEmpty()
    }
}