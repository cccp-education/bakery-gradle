package bakery.scenarios

import bakery.pivot.PivotArticle
import bakery.pivot.PivotBlock
import bakery.pivot.PivotFrontmatter
import bakery.pivot.PivotInline
import bakery.tree.Content
import bakery.tree.SiteNode.Article
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import org.assertj.core.api.Assertions.assertThat

class SiteTreeContentSteps {

    private var article: Article? = null
    private var content: Content? = null

    private fun frontmatter() = PivotFrontmatter("T", "2026-01-01", "page", "published")

    @Given("an article {string} without content")
    fun anArticleWithoutContent(path: String) {
        article = Article(path = path)
    }

    @Given("an article {string} with a {int}-block content")
    fun anArticleWithBlockContent(path: String, blockCount: Int) {
        val blocks = when (blockCount) {
            3 -> listOf(
                PivotBlock.Heading(level = 2, text = "Title", translatable = true),
                PivotBlock.Paragraph(inline = listOf(PivotInline.Text("Text", translatable = true))),
                PivotBlock.Source(language = "bash", content = "echo hello")
            )
            else -> throw IllegalArgumentException("Unsupported block count: $blockCount")
        }
        content = Content(PivotArticle(frontmatter(), blocks))
        article = Article(path = path, content = content)
    }

    @Given("an article {string} with a {int}-segment paragraph")
    fun anArticleWithSegmentParagraph(path: String, segmentCount: Int) {
        val inlines = (1..segmentCount).map {
            PivotInline.Text("Segment $it", translatable = true)
        }
        content = Content(PivotArticle(frontmatter(), listOf(PivotBlock.Paragraph(inlines))))
        article = Article(path = path, content = content)
    }

    @Given("an article {string} with a mixed translatable and technical paragraph")
    fun anArticleWithMixedParagraph(path: String) {
        val inlines = listOf(
            PivotInline.Text("Laissez la cle USB inseree", translatable = true),
            PivotInline.Text("8 Go", translatable = false),
            PivotInline.Text("fr_FR.UTF-8", translatable = false)
        )
        content = Content(PivotArticle(frontmatter(), listOf(PivotBlock.Paragraph(inlines))))
        article = Article(path = path, content = content)
    }

    @Given("an article {string} with a {int}-column {int}-row table")
    fun anArticleWithTable(path: String, cols: Int, rows: Int) {
        val header = (1..cols).map { listOf(PivotInline.Text("H$it", translatable = true)) }
        val bodyRows = (1..rows).map { r ->
            (1..cols).map { c -> listOf(PivotInline.Text("R${r}C${c}", translatable = true)) }
        }
        content = Content(PivotArticle(frontmatter(), listOf(
            PivotBlock.Table(cols = null, header = header, rows = bodyRows)
        )))
        article = Article(path = path, content = content)
    }

    @Given("an article {string} with an empty content")
    fun anArticleWithEmptyContent(path: String) {
        content = Content(PivotArticle(frontmatter(), emptyList()))
        article = Article(path = path, content = content)
    }

    @Then("the article content is null")
    fun theArticleContentIsNull() {
        assertThat(article!!.content).isNull()
    }

    @Then("the content has {int} blocks")
    fun theContentHasBlocks(count: Int) {
        assertThat(article!!.content!!.blocs()).hasSize(count)
    }

    @Then("the first block is a heading")
    fun theFirstBlockIsAHeading() {
        val first = article!!.content!!.blocs()[0]
        assertThat(first).isInstanceOf(PivotBlock.Heading::class.java)
    }

    @Then("the content has {int} inline segments")
    fun theContentHasInlineSegments(count: Int) {
        assertThat(article!!.content!!.inlineTexts()).hasSize(count)
    }

    @Then("the content has {int} translatable segments")
    fun theContentHasTranslatableSegments(count: Int) {
        assertThat(article!!.content!!.translatableSegments()).hasSize(count)
    }

    @Then("the content has {int} translatable segment")
    fun theContentHasTranslatableSegment(count: Int) {
        assertThat(article!!.content!!.translatableSegments()).hasSize(count)
    }
}