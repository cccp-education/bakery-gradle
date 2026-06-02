package bakery.scenarios

import bakery.kgraph.ArticleNode
import bakery.kgraph.BlogArticleSummary
import bakery.kgraph.RelatedArticlesService
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

/**
 * Cucumber steps pour BKG-1 — Consommation du graphe d'articles connexes.
 *
 * Scénarios 1-2 : injection jbake.properties (via BakeryWorld + site.yml).
 * Scénario 3 : logique pure du service (blogArticles dans l'output).
 */
class GraphConsumptionSteps(private val world: BakeryWorld) {

    private val service = RelatedArticlesService()
    private var articles: List<ArticleNode> = emptyList()
    private var output: bakery.kgraph.RelatedArticlesOutput? = null

    @Given("the site configuration contains a relatedArticles block with enabled=true and graphFilePath {string}")
    fun `site config with relatedArticles enabled and custom graphFilePath`(graphFilePath: String) {
        val siteYml = world.projectDir!!.resolve("site.yml")
        val existingContent = siteYml.readText(kotlin.text.Charsets.UTF_8)
        val newContent = existingContent + """
            |
            |relatedArticles:
            |  enabled: true
            |  maxResults: 4
            |  heading: "Articles connexes"
            |  graphFilePath: "$graphFilePath"
        """.trimMargin()
        siteYml.writeText(newContent)
    }

    @Given("the site configuration contains a relatedArticles block with enabled=true")
    fun `site config with relatedArticles enabled defaults`() {
        val siteYml = world.projectDir!!.resolve("site.yml")
        val existingContent = siteYml.readText(kotlin.text.Charsets.UTF_8)
        val newContent = existingContent + """
            |
            |relatedArticles:
            |  enabled: true
        """.trimMargin()
        siteYml.writeText(newContent)
    }

    // Scénario 3 : Tests de logique pure

    @Given("4 articles with overlapping tags for graph consumption")
    fun `4 articles with overlapping tags for graph consumption`() {
        articles = listOf(
            ArticleNode("/a.html", "Kotlin Gradle Tutorial", tags = listOf("kotlin", "gradle"), description = "Guide Kotlin Gradle"),
            ArticleNode("/b.html", "Kotlin Maven Guide", tags = listOf("kotlin", "maven", "gradle"), description = "Tutorial Kotlin Maven"),
            ArticleNode("/c.html", "Java Spring Boot", tags = listOf("java", "spring"), description = "Introduction Spring Boot"),
            ArticleNode("/d.html", "Gradle Kotlin DSL", tags = listOf("kotlin", "gradle"), description = "DSL Gradle Kotlin")
        )
    }

    @When("the knowledge graph is built and suggestions generated")
    fun `build knowledge graph and generate suggestions`() {
        val graph = service.buildGraph(articles)
        output = service.toSuggestions(graph)
    }

    @Then("the output should contain blogArticles for all {int} articles")
    fun `output contains blogArticles for all articles`(expectedCount: Int) {
        assertThat(output).isNotNull
        assertThat(output!!.blogArticles).hasSize(expectedCount)
    }

    @Then("each blogArticle should have a description matching the input")
    fun `each blogArticle matches input description`() {
        assertThat(output).isNotNull
        for ((url, summary) in output!!.blogArticles) {
            val inputArticle = articles.find { it.url == url }
            assertThat(inputArticle).isNotNull
            assertThat(summary.title).isEqualTo(inputArticle!!.title)
            assertThat(summary.tags).isEqualTo(inputArticle.tags)
            assertThat(summary.description).isEqualTo(inputArticle.description)
        }
    }
}