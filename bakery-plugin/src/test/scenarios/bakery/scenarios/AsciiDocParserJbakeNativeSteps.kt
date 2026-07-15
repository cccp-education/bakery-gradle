package bakery.scenarios

import bakery.pivot.AsciiDocParser
import bakery.pivot.AsciiDocRenderer
import bakery.pivot.PivotArticle
import bakery.pivot.PivotBlock
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

class AsciiDocParserJbakeNativeSteps {

    private val parser = AsciiDocParser()
    private val renderer = AsciiDocRenderer()
    private var document: String = ""
    private var article: PivotArticle? = null
    private var rendered: String = ""

    @Given("a JBake native AsciiDoc document with title and jbake attributes")
    fun jbakeNativeDocumentWithTitleAndAttributes() {
        document = """
            = Groovy: Caractères ASCII
            @CherOliv
            2019-07-10
            :jbake-title: Groovy: Caractères ASCII
            :jbake-tags: blog, Groovy, ASCII
            :jbake-type: post
            :jbake-status: published
            :jbake-date: 2019-07-10
            :summary: du groovy et de l'ascii

            Voici un bout de code.
        """.trimIndent()
    }

    @Given("a JBake native AsciiDoc document with heading and source block")
    fun jbakeNativeDocumentWithHeadingAndSource() {
        document = """
            = Test Article
            @Author
            2020-01-15
            :jbake-type: post
            :jbake-status: published

            == First Section

            First paragraph text.

            [source,bash]
            ----
            echo hello
            ----

            Second paragraph.
        """.trimIndent()
    }

    @Given("a JBake native AsciiDoc document with heading and paragraph")
    fun jbakeNativeDocumentWithHeadingAndParagraph() {
        document = """
            = Roundtrip Test
            @CherOliv
            2021-03-20
            :jbake-type: post
            :jbake-status: published

            == Heading

            Paragraph with **bold** text.
        """.trimIndent()
    }

    @When("the parser parses the document")
    fun parserParsesDocument() {
        article = parser.parse(document)
    }

    @When("the parser parses and the renderer renders the article")
    fun parserParsesAndRendererRenders() {
        article = parser.parse(document)
        rendered = renderer.render(article!!)
    }

    @Then("the frontmatter title should be the document title")
    fun frontmatterTitleShouldBeDocumentTitle() {
        assertThat(article!!.frontmatter.title).isEqualTo("Groovy: Caractères ASCII")
    }

    @Then("the frontmatter date should be the jbake date")
    fun frontmatterDateShouldBeJbakeDate() {
        assertThat(article!!.frontmatter.date).isEqualTo("2019-07-10")
    }

    @Then("the frontmatter type should be the jbake type")
    fun frontmatterTypeShouldBeJbakeType() {
        assertThat(article!!.frontmatter.type).isEqualTo("post")
    }

    @Then("the frontmatter status should be the jbake status")
    fun frontmatterStatusShouldBeJbakeStatus() {
        assertThat(article!!.frontmatter.status).isEqualTo("published")
    }

    @Then("the first block should be a heading at level {int}")
    fun firstBlockShouldBeHeadingAtLevel(level: Int) {
        val block = article!!.blocks[0]
        assertThat(block).isInstanceOf(PivotBlock.Heading::class.java)
        assertThat((block as PivotBlock.Heading).level).isEqualTo(level)
    }

    @Then("the second block should be a paragraph")
    fun secondBlockShouldBeParagraph() {
        assertThat(article!!.blocks[1]).isInstanceOf(PivotBlock.Paragraph::class.java)
    }

    @Then("the third block should be a source block with language {string}")
    fun thirdBlockShouldBeSourceWithLanguage(lang: String) {
        val block = article!!.blocks[2]
        assertThat(block).isInstanceOf(PivotBlock.Source::class.java)
        assertThat((block as PivotBlock.Source).language).isEqualTo(lang)
    }

    @Then("the reparsed article should have the same frontmatter title")
    fun reparsedArticleSameFrontmatterTitle() {
        val reparsed = parser.parse(rendered)
        assertThat(reparsed.frontmatter.title).isEqualTo(article!!.frontmatter.title)
    }

    @Then("the reparsed article should have the same number of blocks")
    fun reparsedArticleSameNumberOfBlocks() {
        val reparsed = parser.parse(rendered)
        assertThat(reparsed.blocks.size).isEqualTo(article!!.blocks.size)
    }
}