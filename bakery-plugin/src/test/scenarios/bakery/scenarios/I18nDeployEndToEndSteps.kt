package bakery.scenarios

import bakery.i18n.ContentTranslationService
import bakery.i18n.plantuml.PlantUmlTranslationAdapter
import bakery.i18n.rtl.RtlDirectionInjector
import bakery.langswitch.LangSwitchInjector
import bakery.langswitch.LangSwitchMenu
import bakery.langswitch.LangSwitchThymeleafRenderer
import bakery.pivot.AsciiDocParser
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

/**
 * EPIC BKY-I18N-DEPLOY — US DEPLOY-6.
 *
 * BDD step definitions for the end-to-end i18n deploy pipeline on the
 * `cheroliv-com-i18n-deploy` fixture (3 French articles × 10 languages).
 *
 * The pipeline exercised is:
 *   1. `ContentTranslationService.translate` (with PlantUmlAdapter) — parse →
 *      translate bloc by bloc → rewrite in JBake native format.
 *   2. `RtlDirectionInjector` — walk translated `.adoc` and inject
 *      `:jbake-lang:` + `:lang: rtl` for RTL languages (ar, ur).
 *   3. `LangSwitchInjector` — inject the language switcher fragment into
 *      `menu.thyme` for the FR root and each variant.
 *
 * A label-aware fake [TranslationService] stands in for the LLM so the
 * scenario runs in CI without network or metered cost (Ink Economy Law).
 * The fake applies a FR→EN dictionary for the PlantUml labels assertions
 * and prefixes every other translation with `[lang]` to mark the target
 * language unambiguously.
 */
class I18nDeployEndToEndSteps {

    private val supportedLanguages = listOf("fr", "ar", "bn", "en", "es", "hi", "pt", "ru", "ur", "zh")
    private val defaultLanguage = "fr"

    private lateinit var fixtureDir: File
    private lateinit var fakeTranslator: TranslationService
    private lateinit var translationService: ContentTranslationService

    private val frToEnDictionary = mapOf(
        "Utilisateur" to "User",
        "Administrateur" to "Administrator",
        "Visiteur" to "Visitor",
        "Formateur" to "Trainer",
        "Stagiaire" to "Trainee",
        "Se connecter" to "Log in",
        "Gérer les utilisateurs" to "Manage users",
        "Consulter le catalogue" to "Browse the catalog",
        "Valider les" to "Validate the",
        "Compléter le" to "Complete the",
        "Publier le" to "Publish the",
        "Diagramme de cas d'utilisation" to "Use case diagram",
        "Texte associé" to "Associated text",
        "Diagramme avec labels traduisibles" to "Diagram with translatable labels",
        "Ceci est un paragraphe en français. Il contient du texte traduisible que le service de traduction doit transformer dans la langue cible." to "This is a paragraph in French. It contains translatable text that the translation service must transform into the target language.",
        "Les acteurs Utilisateur, Administrateur et Visiteur apparaissent dans le diagramme. La traduction doit transformer ces labels tout en préservant la structure PlantUML (startuml, enduml, flèches)." to "The actors User, Administrator and Visitor appear in the diagram. The translation must transform these labels while preserving the PlantUML structure (startuml, enduml, arrows)."
    )

    @Given("a cheroliv-com-i18n-deploy fixture with 3 French articles and 10 supported languages")
    fun setupFixture() {
        val resourceUrl = this::class.java.classLoader.getResource("fixtures/cheroliv-com-i18n-deploy")
            ?: throw IllegalStateException("fixture cheroliv-com-i18n-deploy not found on classpath")
        val sourceFixture = File(resourceUrl.toURI())
        fixtureDir = Files.createTempDirectory("i18n-deploy-e2e-").toFile()
        sourceFixture.copyRecursively(fixtureDir, overwrite = true)

        fakeTranslator = object : TranslationService {
            override fun translate(request: TranslationRequest): TranslationResult {
                val target = request.targetLanguage
                val sourceText = request.sourceText
                if (sourceText.isBlank()) return TranslationResult.Success(sourceText)
                if (target == "en") {
                    var translated = sourceText
                    frToEnDictionary.forEach { (fr, en) ->
                        translated = translated.replace(fr, en)
                    }
                    return TranslationResult.Success(translated)
                }
                return TranslationResult.Success("[$target] $sourceText")
            }
        }
        val plantUmlAdapter = PlantUmlTranslationAdapter(fakeTranslator)
        translationService = ContentTranslationService(
            fakeTranslator,
            parser = AsciiDocParser(),
            renderer = JbakeNativeRenderer(),
            jbakeRenderer = JbakeNativeRenderer(),
            plantUmlAdapter = plantUmlAdapter
        )
    }

    @When("the deploy pipeline translates the fixture from fr to {string}")
    fun translateFixtureToSingleLang(targetLang: String) {
        translateFixtureTo(listOf(targetLang))
    }

    @When("the deploy pipeline translates the fixture from fr to all supported languages")
    fun translateFixtureToAllLanguages() {
        translateFixtureTo(supportedLanguages.filter { it != defaultLanguage })
    }

    @When("the deploy pipeline injects RTL for language {string}")
    fun rtlInjectionSingleLang(lang: String) {
        injectRtlForLang(lang)
    }

    @When("the deploy pipeline injects RTL for all supported languages")
    fun rtlInjectionAllLanguages() {
        for (lang in supportedLanguages.filter { it != defaultLanguage }) {
            injectRtlForLang(lang)
        }
    }

    @When("the language switcher is injected into the FR menu")
    fun injectLangSwitchIntoFrMenu() {
        val menuThyme = fixtureDir.resolve("templates/menu.thyme")
        assertThat(menuThyme).exists()
        val injector = LangSwitchInjector()
        val links = LangSwitchMenu(supportedLanguages, defaultLanguage, defaultLanguage, "").generateLinks()
        val labels = mapOf(
            "fr" to "Fran\u00e7ais", "en" to "English", "ar" to "\u0627\u0644\u0639\u0631\u0628\u064a\u0629",
            "zh" to "\u4e2d\u6587", "hi" to "\u0939\u093f\u0928\u094d\u0926\u0940", "es" to "Espa\u00f1ol",
            "bn" to "\u09ac\u09be\u0982\u09b2\u09be", "pt" to "Portugu\u00eas",
            "ru" to "\u0420\u0443\u0441\u0441\u043a\u0438\u0439", "ur" to "\u0627\u0631\u062f\u0648"
        )
        val fragment = LangSwitchThymeleafRenderer(labels).render(links)
        val original = menuThyme.readText()
        val updated = injector.inject(original, fragment)
        menuThyme.writeText(updated)
    }

    @Then("the translated {string} article {string} should start with {string}")
    fun translatedArticleShouldStartWith(lang: String, articleName: String, prefix: String) {
        val article = resolveTranslatedArticle(lang, articleName)
        assertThat(article).exists()
        val content = article.readText()
        assertThat(content)
            .describedAs("$lang/$articleName should start with '$prefix'")
            .startsWith(prefix)
    }

    @Then("the translated {string} article {string} should contain {string}")
    fun translatedArticleShouldContain(lang: String, articleName: String, expected: String) {
        val article = resolveTranslatedArticle(lang, articleName)
        assertThat(article).exists()
        assertThat(article.readText())
            .describedAs("$lang/$articleName should contain '$expected'")
            .contains(expected)
    }

    @Then("the translated {string} article {string} should not contain {string}")
    fun translatedArticleShouldNotContain(lang: String, articleName: String, forbidden: String) {
        val article = resolveTranslatedArticle(lang, articleName)
        assertThat(article).exists()
        assertThat(article.readText())
            .describedAs("$lang/$articleName should not contain '$forbidden'")
            .doesNotContain(forbidden)
    }

    @Then("the translated {string} article {string} body should be in the target language")
    fun translatedArticleBodyInTargetLang(lang: String, articleName: String) {
        val article = resolveTranslatedArticle(lang, articleName)
        assertThat(article).exists()
        val content = article.readText()
        assertThat(content)
            .describedAs("$lang/$articleName body should bear the [$lang] translation marker")
            .contains("[$lang]")
    }

    @Then("the fixture should have {int} translated variants under {string}")
    fun fixtureShouldHaveVariantCount(count: Int, dir: String) {
        val i18nRoot = fixtureDir.resolve(dir)
        assertThat(i18nRoot).exists()
        val variants = i18nRoot.listFiles { f -> f.isDirectory } ?: emptyArray()
        assertThat(variants.size)
            .describedAs("expected $count translated variants under $dir")
            .isEqualTo(count)
    }

    @Then("each variant should contain a translated version of {string}")
    fun eachVariantShouldContainArticle(articleName: String) {
        for (lang in supportedLanguages.filter { it != defaultLanguage }) {
            val article = resolveTranslatedArticle(lang, articleName)
            assertThat(article)
                .describedAs("variant $lang should contain translated $articleName")
                .exists()
        }
    }

    @Then("the {string} variant should contain {string}")
    fun variantShouldContain(lang: String, expected: String) {
        val article = resolveTranslatedArticle(lang, "introduction-pivot.adoc")
        assertThat(article.readText())
            .describedAs("$lang variant should contain '$expected'")
            .contains(expected)
    }

    @Then("the {string} variant should not contain {string}")
    fun variantShouldNotContain(lang: String, forbidden: String) {
        val article = resolveTranslatedArticle(lang, "introduction-pivot.adoc")
        assertThat(article.readText())
            .describedAs("$lang variant should not contain '$forbidden'")
            .doesNotContain(forbidden)
    }

    @Then("the FR menu should contain a link to {string} for language {string}")
    fun frMenuShouldContainLink(href: String, lang: String) {
        val menu = fixtureDir.resolve("templates/menu.thyme")
        assertThat(menu).exists()
        val content = menu.readText()
        val anchor = content.substringBefore("data-lang=\"$lang\"").substringAfterLast("<a ")
        assertThat(anchor)
            .describedAs("FR menu should contain link $href for language $lang")
            .contains(href)
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
        langBlog.walkTopDown()
            .filter { it.isFile && it.extension == "adoc" }
            .forEach { file ->
                val article = parser.parse(file.readText())
                val injected = injector.inject(article.frontmatter, lang)
                val updated = article.copy(frontmatter = injected)
                file.writeText(renderer.render(updated))
            }
    }

    private fun resolveTranslatedArticle(lang: String, articleName: String): File =
        fixtureDir.resolve("i18n/$lang/blog/$articleName")
}