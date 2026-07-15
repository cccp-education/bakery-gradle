package bakery.scenarios

import bakery.i18n.ContentTranslationService
import bakery.pivot.AsciiDocRenderer
import bakery.pivot.JbakeNativeRenderer
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import java.nio.file.Files

class JbakeNativeTranslationSteps {

    private lateinit var articleContent: String
    private lateinit var translatedContent: String
    private val tempDir: File = Files.createTempDirectory("jbake-i18n-test").toFile()

    @Given("a JBake native AsciiDoc article with header and body")
    fun jbakeNativeArticleWithHeaderAndBody() {
        articleContent = """
            = Article de Test
            @CherOliv
            2020-01-15
            :jbake-type: post
            :jbake-status: published

            Premier paragraphe en français.

            == Section

            Deuxième paragraphe.
        """.trimIndent()
    }

    @Given("a pivot format AsciiDoc article with title and body")
    fun pivotFormatArticleWithTitleAndBody() {
        articleContent = """
            title=Article de Test
            date=2020-01-15
            type=post
            status=published
            ~~~~~~

            Premier paragraphe en français.

            == Section

            Deuxième paragraphe.
        """.trimIndent()
    }

    @Given("a JBake native AsciiDoc article with tags and summary attributes")
    fun jbakeNativeArticleWithTagsAndSummary() {
        articleContent = """
            = Article Tagué
            @CherOliv
            2022-06-10
            :jbake-title: Article Tagué
            :jbake-tags: blog, kotlin, test
            :jbake-type: post
            :jbake-status: published
            :jbake-date: 2022-06-10
            :summary: Un article de test

            Corps de l'article en français.
        """.trimIndent()
    }

    @When("the content translation service translates it from fr to en")
    fun contentTranslationServiceTranslatesIt() {
        val sourceDir = File(tempDir, "fr").apply { mkdirs() }
        val sourceFile = File(sourceDir, "article.adoc")
        sourceFile.writeText(articleContent)

        val enDir = File(tempDir, "en").apply { mkdirs() }
        val enFile = File(enDir, "article.adoc")
        enFile.writeText(articleContent)

        val fakeTranslator = object : TranslationService {
            override fun translate(request: TranslationRequest): TranslationResult =
                TranslationResult.Success("[EN] ${request.sourceText}")
        }
        val service = ContentTranslationService(
            fakeTranslator,
            renderer = AsciiDocRenderer(),
            jbakeRenderer = JbakeNativeRenderer()
        )
        service.translate(enDir, "fr", "en")
        translatedContent = enFile.readText()
    }

    @Then("the translated article should use JBake native format")
    fun translatedArticleShouldUseJbakeNativeFormat() {
        assertThat(translatedContent).startsWith("= ")
        assertThat(translatedContent).contains("@CherOliv")
        assertThat(translatedContent).contains(":jbake-type:")
    }

    @Then("the translated header should start with the document title")
    fun translatedHeaderShouldStartWithDocumentTitle() {
        assertThat(translatedContent).startsWith("= ")
    }

    @Then("the translated body should be in the target language")
    fun translatedBodyShouldBeInTargetLanguage() {
        assertThat(translatedContent).contains("[EN]")
    }

    @Then("the translated article should use pivot format")
    fun translatedArticleShouldUsePivotFormat() {
        assertThat(translatedContent).contains("title=")
        assertThat(translatedContent).contains("~~~~~~")
    }

    @Then("the translated header should contain the tilde separator")
    fun translatedHeaderShouldContainTildeSeparator() {
        assertThat(translatedContent).contains("~~~~~~")
    }

    @Then("the translated article should preserve jbake tags attribute")
    fun translatedArticleShouldPreserveJbakeTagsAttribute() {
        assertThat(translatedContent).contains(":jbake-tags: blog, kotlin, test")
    }

    @Then("the translated article should preserve summary attribute")
    fun translatedArticleShouldPreserveSummaryAttribute() {
        assertThat(translatedContent).contains(":summary: Un article de test")
    }
}