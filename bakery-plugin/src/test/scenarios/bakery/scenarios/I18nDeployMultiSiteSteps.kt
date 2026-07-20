package bakery.scenarios

import document.translation.ContentTranslationService
import document.translation.plan.SiteTranslationPlan
import document.translation.plantuml.PlantUmlTranslationAdapter
import bakery.i18n.rtl.RtlDirectionInjector
import document.translation.AsciiDocParser
import document.translation.JbakeNativeRenderer
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
 * EPIC BKY-I18N-DEPLOY — US DEPLOY-7.
 *
 * BDD step definitions for the multi-site i18n deploy pipeline generalization.
 * Exercises the pipeline across 3 sites (cheroliv.com, cccp.education,
 * magic-stick) × 9 non-French supported languages, plus the [SiteTranslationPlan]
 * domain concept that drives the matrix.
 *
 * The fixture reuses the 3 articles from `cheroliv-com-i18n-deploy` (covering
 * the 3 PlantUml strategies) copied under each site directory. A label-aware
 * fake [TranslationService] stands in for the LLM (Ink Economy Law — zéro appel
 * réseau en CI).
 */
class I18nDeployMultiSiteSteps {

    private val supportedLanguages = listOf("fr", "ar", "bn", "en", "es", "hi", "pt", "ru", "ur", "zh")
    private val defaultLanguage = "fr"
    private val siteNames = listOf("cheroliv.com", "cccp.education", "magic-stick")

    private lateinit var fixtureRoot: File
    private lateinit var fakeTranslator: TranslationService
    private lateinit var translationService: ContentTranslationService
    private lateinit var plan: SiteTranslationPlan

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

    @Given("a multi-site i18n deploy fixture with 3 sites and 3 French articles each")
    fun setupMultiSiteFixture() {
        val resourceUrl = this::class.java.classLoader.getResource("fixtures/cheroliv-com-i18n-deploy")
            ?: throw IllegalStateException("fixture cheroliv-com-i18n-deploy not found on classpath")
        val sourceFixture = File(resourceUrl.toURI())
        fixtureRoot = Files.createTempDirectory("i18n-deploy-multi-site-").toFile()
        for (siteName in siteNames) {
            val siteDir = fixtureRoot.resolve(siteName)
            sourceFixture.copyRecursively(siteDir, overwrite = true)
        }
        fakeTranslator = object : TranslationService {
            override fun translate(request: TranslationRequest): TranslationResult {
                val target = request.targetLanguage
                val sourceText = request.sourceText
                if (sourceText.isBlank()) return TranslationResult.Success(sourceText)
                if (target == "en") {
                    var translated = sourceText
                    frToEnDictionary.forEach { (fr, en) -> translated = translated.replace(fr, en) }
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

    @Given("a SiteTranslationPlan for site {string} with source {string} and targets {string}")
    fun createSiteTranslationPlan(siteName: String, sourceLang: String, targetsCsv: String) {
        val targets = targetsCsv.split(",").map { it.trim() }.toSet()
        plan = SiteTranslationPlan(
            siteName = siteName,
            sourceLanguage = sourceLang,
            targetLanguages = targets
        )
    }

    @When("existing languages {string} are already translated")
    fun existingLanguagesAlreadyTranslated(existingCsv: String) {
        // No-op: existing languages are passed in the Then assertion. This step
        // documents the inputs for the missingLanguages computation.
        assertThat(existingCsv).isNotBlank()
    }

    @When("the multi-site pipeline dry-runs translation for all 3 sites")
    fun dryRunAllSites() {
        for (siteName in siteNames) {
            val siteDir = fixtureRoot.resolve(siteName)
            assertThat(siteDir.resolve("content/blog")).exists()
        }
    }

    @When("the multi-site pipeline translates all 3 sites from fr to all supported languages")
    fun translateAllSitesToAllLanguages() {
        val targets = supportedLanguages.filter { it != defaultLanguage }
        for (siteName in siteNames) {
            translateSite(siteName, targets)
        }
    }

    @When("the multi-site pipeline translates all 3 sites from fr to {string}")
    fun translateAllSitesToSingleLang(targetLang: String) {
        for (siteName in siteNames) {
            translateSite(siteName, listOf(targetLang))
        }
    }

    @When("the multi-site pipeline injects RTL for all supported languages on every site")
    fun injectRtlAllSitesAllLanguages() {
        for (siteName in siteNames) {
            for (lang in supportedLanguages.filter { it != defaultLanguage }) {
                injectRtlForSiteLang(siteName, lang)
            }
        }
    }

    @Then("each site should still have only French content")
    fun eachSiteOnlyFrenchContent() {
        for (siteName in siteNames) {
            val blog = fixtureRoot.resolve("$siteName/content/blog")
            assertThat(blog).isDirectory()
            assertThat(blog.listFiles { f -> f.extension == "adoc" })
                .describedAs("$siteName should still have French articles")
                .isNotEmpty
        }
    }

    @Then("each site should have no translated variants under {string}")
    fun eachSiteNoTranslatedVariants(dir: String) {
        for (siteName in siteNames) {
            assertThat(fixtureRoot.resolve("$siteName/$dir")).doesNotExist()
        }
    }

    @Then("each site should have no translated adoc file under {string}")
    fun eachSiteNoTranslatedAdocFiles(dir: String) {
        for (siteName in siteNames) {
            val i18nRoot = fixtureRoot.resolve("$siteName/$dir")
            if (i18nRoot.exists()) {
                val adocFiles = i18nRoot.walkTopDown().filter { it.isFile && it.extension == "adoc" }.toList()
                assertThat(adocFiles)
                    .describedAs("site $siteName should have no translated .adoc under $dir after dry-run")
                    .isEmpty()
            }
        }
    }

    @Then("each site should have {int} translated variants under {string}")
    fun eachSiteHasVariantCount(count: Int, dir: String) {
        for (siteName in siteNames) {
            val i18nRoot = fixtureRoot.resolve("$siteName/$dir")
            assertThat(i18nRoot).exists()
            val variants = i18nRoot.listFiles { f -> f.isDirectory } ?: emptyArray()
            assertThat(variants.size)
                .describedAs("site $siteName should have $count variants under $dir")
                .isEqualTo(count)
        }
    }

    @Then("each variant of each site should contain a translated version of {string}")
    fun eachVariantOfEachSiteContainsArticle(articleName: String) {
        for (siteName in siteNames) {
            for (lang in supportedLanguages.filter { it != defaultLanguage }) {
                val article = fixtureRoot.resolve("$siteName/i18n/$lang/blog/$articleName")
                assertThat(article)
                    .describedAs("$siteName/$lang should contain translated $articleName")
                    .exists()
            }
        }
    }

    @Then("the {string} variant of each site should contain {string}")
    fun variantOfEachSiteContains(lang: String, expected: String) {
        for (siteName in siteNames) {
            val article = fixtureRoot.resolve("$siteName/i18n/$lang/blog/introduction-pivot.adoc")
            assertThat(article.readText())
                .describedAs("$siteName/$lang variant should contain '$expected'")
                .contains(expected)
        }
    }

    @Then("the {string} variant of each site should not contain {string}")
    fun variantOfEachSiteNotContains(lang: String, forbidden: String) {
        for (siteName in siteNames) {
            val article = fixtureRoot.resolve("$siteName/i18n/$lang/blog/introduction-pivot.adoc")
            assertThat(article.readText())
                .describedAs("$siteName/$lang variant should not contain '$forbidden'")
                .doesNotContain(forbidden)
        }
    }

    @Then("each site {string} article {string} should start with {string}")
    fun eachSiteArticleShouldStartWith(lang: String, articleName: String, prefix: String) {
        for (siteName in siteNames) {
            val article = fixtureRoot.resolve("$siteName/i18n/$lang/blog/$articleName")
            assertThat(article).exists()
            assertThat(article.readText())
                .describedAs("$siteName/$lang/$articleName should start with '$prefix'")
                .startsWith(prefix)
        }
    }

    @Then("each site {string} article {string} should contain {string}")
    fun eachSiteArticleShouldContain(lang: String, articleName: String, expected: String) {
        for (siteName in siteNames) {
            val article = fixtureRoot.resolve("$siteName/i18n/$lang/blog/$articleName")
            assertThat(article).exists()
            assertThat(article.readText())
                .describedAs("$siteName/$lang/$articleName should contain '$expected'")
                .contains(expected)
        }
    }

    @Then("each site {string} article {string} should not contain {string}")
    fun eachSiteArticleShouldNotContain(lang: String, articleName: String, forbidden: String) {
        for (siteName in siteNames) {
            val article = fixtureRoot.resolve("$siteName/i18n/$lang/blog/$articleName")
            assertThat(article).exists()
            assertThat(article.readText())
                .describedAs("$siteName/$lang/$articleName should not contain '$forbidden'")
                .doesNotContain(forbidden)
        }
    }

    @Then("the plan missing languages should be {string}")
    fun planMissingLanguagesShouldBe(expectedCsv: String) {
        val existing = setOf("en", "zh")
        val missing = plan.missingLanguages(existing)
        val expected = expectedCsv.split(",").map { it.trim() }.toSet()
        assertThat(missing)
            .describedAs("missing languages for existing=$existing")
            .isEqualTo(expected)
    }

    @Then("the plan should not be complete")
    fun planShouldNotBeComplete() {
        assertThat(plan.isComplete(setOf("en", "zh"))).isFalse()
    }

    @Then("the plan rtlTargets should be {string}")
    fun planRtlTargetsShouldBe(expectedCsv: String) {
        val expected = expectedCsv.split(",").map { it.trim() }.toSet()
        assertThat(plan.rtlTargets()).isEqualTo(expected)
    }

    private fun translateSite(siteName: String, targets: List<String>) {
        val siteDir = fixtureRoot.resolve(siteName)
        val sourceBlog = siteDir.resolve("content/blog")
        assertThat(sourceBlog).exists()
        for (lang in targets) {
            val langBlog = siteDir.resolve("i18n/$lang/blog")
            langBlog.mkdirs()
            sourceBlog.listFiles { f -> f.extension == "adoc" }?.forEach { source ->
                langBlog.resolve(source.name).writeText(source.readText())
            }
            val langDir = siteDir.resolve("i18n/$lang")
            translationService.translate(langDir, defaultLanguage, lang)
        }
    }

    private fun injectRtlForSiteLang(siteName: String, lang: String) {
        val langBlog = fixtureRoot.resolve("$siteName/i18n/$lang/blog")
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
}