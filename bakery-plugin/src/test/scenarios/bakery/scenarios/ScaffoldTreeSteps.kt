package bakery.scenarios

import bakery.llm.FakeLlmService
import bakery.scaffold.ScaffoldGenerator
import bakery.scaffold.ScaffoldIntention
import bakery.scaffold.ScaffoldSiteType
import bakery.tree.SiteNode
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat

class ScaffoldTreeSteps {

    private var llmResponse: String = ""
    private var intention: ScaffoldIntention =
        ScaffoldIntention(description = "Formation FPA", siteType = ScaffoldSiteType.FORMATION)
    private var generator: ScaffoldGenerator = ScaffoldGenerator()
    private var prompt: String = ""

    @Given("an LLM response with a 3-level tree JSON")
    fun anLlmResponseWith3LevelTreeJson() {
        llmResponse = """
            {
              "siteType": "formation",
              "projectName": "ma-formation",
              "description": "Formation FPA",
              "tree": {
                "type": "site",
                "path": "",
                "sections": [
                  {
                    "type": "section",
                    "path": "formations",
                    "articles": [
                      {"type": "article", "path": "formations/ab-partition"},
                      {"type": "article", "path": "formations/cd-partition"}
                    ]
                  },
                  {
                    "type": "section",
                    "path": "blog",
                    "articles": [
                      {"type": "article", "path": "blog/hello"}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()
    }

    @Given("an LLM response with a tree containing 3 articles")
    fun anLlmResponseWithTreeContaining3Articles() {
        llmResponse = """
            {
              "siteType": "blog",
              "projectName": "mon-blog",
              "description": "Blog",
              "tree": {
                "type": "site",
                "path": "",
                "sections": [
                  {
                    "type": "section",
                    "path": "blog",
                    "articles": [
                      {"type": "article", "path": "blog/a"},
                      {"type": "article", "path": "blog/b"},
                      {"type": "article", "path": "blog/c"}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()
    }

    @Given("an LLM response with a legacy templates list and no tree")
    fun anLlmResponseWithLegacyTemplatesList() {
        llmResponse = """
            {
              "siteType": "blog",
              "projectName": "mon-blog",
              "description": "Blog",
              "templates": ["blog.thyme", "post.thyme", "page.thyme"]
            }
        """.trimIndent()
    }

    @Given("an LLM response with both tree and legacy templates")
    fun anLlmResponseWithBothTreeAndLegacyTemplates() {
        llmResponse = """
            {
              "siteType": "blog",
              "projectName": "mon-blog",
              "description": "Blog",
              "templates": ["legacy.thyme", "old.thyme"],
              "tree": {
                "type": "site",
                "path": "",
                "sections": [
                  {
                    "type": "section",
                    "path": "docs",
                    "articles": [
                      {"type": "article", "path": "docs/intro"}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()
    }

    @Given("a scaffold intention for a formation site")
    fun aScaffoldIntentionForFormationSite() {
        intention = ScaffoldIntention(
            description = "Formation FPA complete",
            siteType = ScaffoldSiteType.FORMATION,
            lang = "fr",
            projectName = "ma-formation"
        )
    }

    @When("I generate the scaffold output")
    fun iGenerateTheScaffoldOutput() {
        generator = ScaffoldGenerator()
    }

    @When("I build the prompt")
    fun iBuildThePrompt() {
        prompt = generator.buildPrompt(intention)
    }

    @Then("the output tree is a Site with {int} sections")
    fun theOutputTreeIsASiteWithSections(expected: Int) {
        val output = runBlocking { generator.generate(intention, FakeLlmService(llmResponse)) }
        assertThat(output.tree).isInstanceOf(SiteNode.Site::class.java)
        val site = output.tree as SiteNode.Site
        assertThat(site.sections).hasSize(expected)
    }

    @Then("the output templates are derived from the tree leaves")
    fun theOutputTemplatesAreDerivedFromTreeLeaves() {
        val output = runBlocking { generator.generate(intention, FakeLlmService(llmResponse)) }
        assertThat(output.templates).contains(
            "formations/ab-partition.thyme",
            "formations/cd-partition.thyme",
            "blog/hello.thyme"
        )
    }

    @Then("the output templates contains {int} entries")
    fun theOutputTemplatesContainsEntries(expected: Int) {
        val output = runBlocking { generator.generate(intention, FakeLlmService(llmResponse)) }
        assertThat(output.templates).hasSize(expected)
    }

    @Then("each template ends with {string}")
    fun eachTemplateEndsWith(suffix: String) {
        val output = runBlocking { generator.generate(intention, FakeLlmService(llmResponse)) }
        assertThat(output.templates).allSatisfy { t ->
            assertThat(t).endsWith(suffix)
        }
    }

    @Then("the output tree is null")
    fun theOutputTreeIsNull() {
        val output = runBlocking { generator.generate(intention, FakeLlmService(llmResponse)) }
        assertThat(output.tree).isNull()
    }

    @Then("the output templates equals the legacy list")
    fun theOutputTemplatesEqualsTheLegacyList() {
        val output = runBlocking { generator.generate(intention, FakeLlmService(llmResponse)) }
        assertThat(output.templates).containsExactly("blog.thyme", "post.thyme", "page.thyme")
    }

    @Then("the prompt contains {string}")
    fun thePromptContains(fragment: String) {
        assertThat(prompt).contains(fragment)
    }

    @Then("the prompt contains a type discriminator example")
    fun thePromptContainsTypeDiscriminatorExample() {
        assertThat(prompt).contains("\"type\"")
        assertThat(prompt).contains("\"site\"")
        assertThat(prompt).contains("\"section\"")
        assertThat(prompt).contains("\"article\"")
    }

    @Then("the output templates are derived from the tree")
    fun theOutputTemplatesAreDerivedFromTheTree() {
        val output = runBlocking { generator.generate(intention, FakeLlmService(llmResponse)) }
        assertThat(output.templates).containsExactly("docs/intro.thyme")
    }

    @Then("the output templates do not contain legacy entries")
    fun theOutputTemplatesDoNotContainLegacyEntries() {
        val output = runBlocking { generator.generate(intention, FakeLlmService(llmResponse)) }
        assertThat(output.templates).doesNotContain("legacy.thyme", "old.thyme")
    }
}