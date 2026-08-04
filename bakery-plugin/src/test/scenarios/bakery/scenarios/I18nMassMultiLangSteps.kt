package bakery.scenarios

import bakery.i18n.rtl.RtlDirectionInjector
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import document.translation.AsciiDocParser
import document.translation.ContentTranslationService
import document.translation.JbakeNativeRenderer
import document.translation.plantuml.PlantUmlTranslationAdapter
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import java.nio.file.Files

class I18nMassMultiLangSteps {
    private val allTargetLanguages = listOf("en", "zh", "hi", "es", "ar", "bn", "pt", "ru", "ur")
    private val defaultLanguage = "fr"

    private lateinit var fixtureDir: File
    private lateinit var fakeTranslator: TranslationService
    private lateinit var translationService: ContentTranslationService

    @Given("a mass-multi-lang fixture with 3 pilot batch articles")
    fun setupFixture() {
        val resourceUrl =
            this::class.java.classLoader.getResource("fixtures/cheroliv-com-mass-multi-lang")
                ?: throw IllegalStateException("fixture cheroliv-com-mass-multi-lang not found on classpath")
        val sourceFixture = File(resourceUrl.toURI())
        fixtureDir = Files.createTempDirectory("i18n-mass-multi-lang-").toFile()
        sourceFixture.copyRecursively(fixtureDir, overwrite = true)

        fakeTranslator =
            object : TranslationService {
                override fun translate(request: TranslationRequest): TranslationResult {
                    val target = request.targetLanguage
                    val sourceText = request.sourceText
                    if (sourceText.isBlank()) return TranslationResult.Success(sourceText)
                    return TranslationResult.Success("[$target] $sourceText")
                }
            }
        val plantUmlAdapter = PlantUmlTranslationAdapter(fakeTranslator)
        translationService =
            ContentTranslationService(
                fakeTranslator,
                parser = AsciiDocParser(),
                renderer = JbakeNativeRenderer(),
                jbakeRenderer = JbakeNativeRenderer(),
                plantUmlAdapter = plantUmlAdapter,
            )
    }

    @When("the mass-multi-lang pipeline translates the fixture from fr to {string}")
    fun translateFixtureToSingleLang(targetLang: String) {
        translateFixtureTo(listOf(targetLang))
    }

    @When("the mass-multi-lang pipeline translates the fixture from fr to all 9 target languages")
    fun translateFixtureToAllLanguages() {
        translateFixtureTo(allTargetLanguages)
    }

    @When("the mass-multi-lang pipeline injects RTL for language {string}")
    fun rtlInjectionSingleLang(lang: String) {
        injectRtlForLang(lang)
    }

    @When("the mass-multi-lang pipeline injects RTL for all 9 target languages")
    fun rtlInjectionAllLanguages() {
        for (lang in allTargetLanguages) {
            injectRtlForLang(lang)
        }
    }

    @Then("the mass-multi-lang {string} article {string} should start with {string}")
    fun massMultiLangArticleShouldStartWith(
        lang: String,
        articleName: String,
        prefix: String,
    ) {
        val article = resolveTranslatedArticle(lang, articleName)
        assertThat(article).exists()
        assertThat(article.readText())
            .describedAs("$lang/$articleName should start with '$prefix'")
            .startsWith(prefix)
    }

    @Then("the mass-multi-lang {string} article {string} should contain {string}")
    fun massMultiLangArticleShouldContain(
        lang: String,
        articleName: String,
        expected: String,
    ) {
        val article = resolveTranslatedArticle(lang, articleName)
        assertThat(article).exists()
        assertThat(article.readText())
            .describedAs("$lang/$articleName should contain '$expected'")
            .contains(expected)
    }

    @Then("the mass-multi-lang {string} article {string} should not contain {string}")
    fun massMultiLangArticleShouldNotContain(
        lang: String,
        articleName: String,
        forbidden: String,
    ) {
        val article = resolveTranslatedArticle(lang, articleName)
        assertThat(article).exists()
        assertThat(article.readText())
            .describedAs("$lang/$articleName should not contain '$forbidden'")
            .doesNotContain(forbidden)
    }

    @Then("the mass-multi-lang {string} article {string} body should be in the target language")
    fun massMultiLangArticleBodyInTargetLang(
        lang: String,
        articleName: String,
    ) {
        val article = resolveTranslatedArticle(lang, articleName)
        assertThat(article).exists()
        assertThat(article.readText())
            .describedAs("$lang/$articleName body should bear the [$lang] translation marker")
            .contains("[$lang]")
    }

    @Then("the mass-multi-lang fixture should have {int} translated variants under {string}")
    fun massMultiLangFixtureShouldHaveVariantCount(
        count: Int,
        dir: String,
    ) {
        val i18nRoot = fixtureDir.resolve(dir)
        assertThat(i18nRoot).exists()
        val variants = i18nRoot.listFiles { f -> f.isDirectory } ?: emptyArray()
        assertThat(variants.size)
            .describedAs("expected $count translated variants under $dir")
            .isEqualTo(count)
    }

    @Then("each mass-multi-lang variant should contain a translated version of {string}")
    fun eachMassMultiLangVariantShouldContainArticle(articleName: String) {
        for (lang in allTargetLanguages) {
            val article = resolveTranslatedArticle(lang, articleName)
            assertThat(article)
                .describedAs("variant $lang should contain translated $articleName")
                .exists()
        }
    }

    @Then("the mass-multi-lang {string} variant should contain {string}")
    fun massMultiLangVariantShouldContain(
        lang: String,
        expected: String,
    ) {
        val article = resolveTranslatedArticle(lang, "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc")
        assertThat(article.readText())
            .describedAs("$lang variant should contain '$expected'")
            .contains(expected)
    }

    @Then("the mass-multi-lang {string} variant should not contain {string}")
    fun massMultiLangVariantShouldNotContain(
        lang: String,
        forbidden: String,
    ) {
        val article = resolveTranslatedArticle(lang, "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc")
        assertThat(article.readText())
            .describedAs("$lang variant should not contain '$forbidden'")
            .doesNotContain(forbidden)
    }

    private fun translateFixtureTo(targetLangs: List<String>) {
        val sourceBlog = fixtureDir.resolve("content/blog")
        assertThat(sourceBlog).exists()
        for (lang in targetLangs) {
            val langBlog = fixtureDir.resolve("i18n/$lang/blog")
            langBlog.mkdirs()
            sourceBlog.listFiles { f -> f.extension == "adoc" }?.forEach { source ->
                langBlog.resolve(source.name).writeText(source.readText())
            }
            val langDir = fixtureDir.resolve("i18n/$lang")
            translationService.translate(langDir, defaultLanguage, lang)
        }
    }

    private fun injectRtlForLang(lang: String) {
        val langBlog = fixtureDir.resolve("i18n/$lang/blog")
        if (!langBlog.exists()) return
        val parser = AsciiDocParser()
        val renderer = JbakeNativeRenderer()
        val injector = RtlDirectionInjector()
        langBlog
            .walkTopDown()
            .filter { it.isFile && it.extension == "adoc" }
            .forEach { file ->
                val article = parser.parse(file.readText())
                val injected = injector.inject(article.frontmatter, lang)
                val updated = article.copy(frontmatter = injected)
                file.writeText(renderer.render(updated))
            }
    }

    private fun resolveTranslatedArticle(
        lang: String,
        articleName: String,
    ): File = fixtureDir.resolve("i18n/$lang/blog/$articleName")
}
