package bakery.scenarios

import bakery.i18n.variant.VariantBakePlan
import bakery.i18n.variant.VariantBaker
import bakery.i18n.variant.VariantLayout
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import java.nio.file.Files

/**
 * BKY-LANG-NAV-8 — BDD steps for feature 67 (`bakeVariants`).
 *
 * The JBake render is replaced by a deterministic fake bake: the production
 * orchestration ([VariantBaker]) is exercised in full (assembled root, wiped
 * output, cleanup), but the render itself is a marker write. The pure plan
 * ([VariantBakePlan]) and the deployability guard are driven exactly as the task
 * drives them, so the rollout contract is proven without spawning JBake.
 */
class VariantBakeSteps {
    private lateinit var bakeRoot: File
    private lateinit var dest: File
    private lateinit var layout: VariantLayout
    private var plan: List<String> = emptyList()
    private val assembledHadSharedAssets = linkedMapOf<String, Boolean>()

    @Given("a variant bake fixture site with languages {string} and {string}")
    fun createFixture(
        first: String,
        second: String,
    ) {
        val temp = Files.createTempDirectory("variant-bake-").toFile()
        bakeRoot = temp.resolve("jbake")
        dest = temp.resolve("build/bake")
        layout = VariantLayout(bakeRoot)

        layout.referenceTemplates.mkdirs()
        layout.referenceTemplates.resolve("menu.thyme").writeText("<nav>fr</nav>")
        layout.sharedAssets.mkdirs()
        layout.sharedAssets.resolve("styles.css").writeText("body{}")
        layout.jbakeProperties.writeText("site.host=https://example.org")

        listOf(first, second).forEach { language ->
            layout.templates(language).mkdirs()
            layout.templates(language).resolve("menu.thyme").writeText("<nav>$language</nav>")
            layout.content(language).mkdirs()
            layout.content(language).resolve("index.adoc").writeText("= $language")
        }
    }

    @Given("the {string} variant is missing the reference template {string}")
    fun removeReferenceTemplate(
        language: String,
        template: String,
    ) {
        layout.templates(language).resolve(template).delete()
    }

    @Given("the {string} variant has no content tree")
    fun removeVariantContent(language: String) {
        layout.content(language).deleteRecursively()
    }

    @When("the variant bake is planned for languages {string} with reference {string}")
    fun planBake(
        languages: String,
        reference: String,
    ) {
        plan = VariantBakePlan.languagesToBake(layout, languages.split(","), reference)
    }

    @When("the variants are baked")
    fun bakeVariants() {
        VariantBaker(layout) { sourceRoot, outputDir ->
            assembledHadSharedAssets[outputDir.name] = carriesSharedBakeRoot(sourceRoot)
            outputDir.mkdirs()
            outputDir.resolve("index.html").writeText("<html>${sourceRoot.name}</html>")
        }.bakeAll(plan, dest)
    }

    private fun carriesSharedBakeRoot(sourceRoot: File): Boolean =
        sourceRoot.resolve("assets/styles.css").exists() &&
            sourceRoot.resolve("jbake.properties").exists()

    @Then("the bake plan is {string}")
    fun assertPlan(expected: String) {
        assertThat(plan).containsExactlyElementsOf(expected.split(","))
    }

    @Then("the bake plan does not contain {string}")
    fun assertPlanExcludes(language: String) {
        assertThat(plan).doesNotContain(language)
    }

    @Then("the {string} variant tree contains a baked {string}")
    fun assertVariantBaked(
        language: String,
        page: String,
    ) {
        assertThat(dest.resolve("$language/$page")).exists()
    }

    @Then("the {string} variant tree is absent")
    fun assertVariantAbsent(language: String) {
        assertThat(dest.resolve(language)).doesNotExist()
    }

    @Then("the assembled {string} bake root carried the shared assets")
    fun assertSharedAssets(language: String) {
        assertThat(assembledHadSharedAssets.getValue(language))
            .describedAs("the assembled root must overlay the shared assets and jbake.properties")
            .isTrue()
    }

    @Then("no assembled bake root remains")
    fun assertAssembledRemoved() {
        assertThat(dest.parentFile.resolve("i18n-assembled")).doesNotExist()
    }

    @Then("the {string} variant content tree is empty")
    fun assertEmptyContent(language: String) {
        assertThat(dest.resolve("$language/index.html")).exists()
        assertThat(dest.resolve("$language/content")).doesNotExist()
    }
}
