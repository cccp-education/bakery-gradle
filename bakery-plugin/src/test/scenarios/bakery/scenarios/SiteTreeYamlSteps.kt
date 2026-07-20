package bakery.scenarios

import document.translation.PivotArticle
import document.translation.PivotBlock
import document.translation.PivotFrontmatter
import document.translation.PivotInline
import bakery.tree.Content
import bakery.tree.SiteNode
import bakery.tree.SiteNode.Article
import bakery.tree.SiteNode.Section
import bakery.tree.SiteNode.Site
import bakery.tree.SiteTreeYaml
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

class SiteTreeYamlSteps {

    private var original: SiteNode? = null
    private var serialized: String = ""
    private var reparsed: SiteNode? = null
    private var parsedOrNull: SiteNode? = null

    @Given("an empty tree")
    fun anEmptyTree() {
        original = Site(path = "", sections = emptyList())
    }

    @Given("a 3-level tree with 2 articles")
    fun aThreeLevelTreeWithTwoArticles() {
        val ab = Article(path = "formations/ab-partition")
        val cd = Article(path = "formations/cd-partition")
        val formations = Section(path = "formations", articles = listOf(ab, cd))
        original = Site(path = "", sections = listOf(formations))
    }

    @Given("an article {string} with content")
    fun anArticleWithContent(path: String) {
        val pivot = PivotArticle(
            frontmatter = PivotFrontmatter("T", "2026-01-01", "page", "published"),
            blocks = listOf(PivotBlock.Paragraph(inline = listOf(PivotInline.Text("text", translatable = true))))
        )
        original = Article(path = path, content = Content(pivot))
    }

    @When("I serialize and re-parse the tree")
    fun iSerializeAndReParseTheTree() {
        serialized = SiteTreeYaml.serialize(original!!)
        reparsed = SiteTreeYaml.parse(serialized)
    }

    @When("I serialize the tree")
    fun iSerializeTheTree() {
        serialized = SiteTreeYaml.serialize(original!!)
    }

    @When("I serialize the article")
    fun iSerializeTheArticle() {
        serialized = SiteTreeYaml.serialize(original!!)
    }

    @When("I parse an empty YAML")
    fun iParseAnEmptyYaml() {
        parsedOrNull = SiteTreeYaml.parseOrNull("")
    }

    @When("I parse a YAML with an unknown type")
    fun iParseAYamlWithAnUnknownType() {
        parsedOrNull = SiteTreeYaml.parseOrNull("type: unknown\npath: x\n")
    }

    @Then("the reparsed tree is identical to the original")
    fun theReparsedTreeIsIdenticalToTheOriginal() {
        assertThat(reparsed).isEqualTo(original)
    }

    @Then("the YAML contains {string}")
    fun theYamlContains(fragment: String) {
        assertThat(serialized).contains(fragment)
    }

    @Then("the YAML does not contain {string}")
    fun theYamlDoesNotContain(fragment: String) {
        assertThat(serialized).doesNotContain(fragment)
    }

    @Then("the result is null")
    fun theResultIsNull() {
        assertThat(parsedOrNull).isNull()
    }

    @Then("the parseOrNull result is null")
    fun theParseOrNullResultIsNull() {
        assertThat(parsedOrNull).isNull()
    }
}