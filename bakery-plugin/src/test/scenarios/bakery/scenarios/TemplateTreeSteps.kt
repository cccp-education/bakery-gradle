package bakery.scenarios

import bakery.tree.TemplateTranslationStatus
import bakery.tree.TemplateTree
import bakery.tree.TemplateTreeAudit
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import java.nio.file.Files

/**
 * BDD steps for `68_template_tree.feature` (GBL-010 G-1).
 *
 * The glue package is `bakery.scenarios` (the runner's `GLUE_PROPERTY_NAME`), so
 * these names must not collide with another bakery steps class.
 */
class TemplateTreeSteps {

    private val root: File = Files.createTempDirectory("bakery-template-tree-bdd").toFile()
    private val reference: File = root.resolve("templates")
    private var tree: TemplateTree? = null

    @Given("a reference template {string} with the text {string}")
    fun referenceTemplate(
        name: String,
        text: String,
    ) {
        write(reference, name, text)
    }

    @Given("a variant {string} template {string} with the text {string}")
    fun variantTemplate(
        language: String,
        name: String,
        text: String,
    ) {
        write(root.resolve("i18n/$language/templates"), name, text)
    }

    @Given("a variant {string} with no {string} template")
    fun variantWithoutTemplate(
        language: String,
        name: String,
    ) {
        root.resolve("i18n/$language/templates").mkdirs()
    }

    @When("the template tree is audited for {string}")
    fun audit(language: String) {
        tree =
            TemplateTreeAudit.of(
                referenceTemplates = reference,
                variantTemplates = mapOf(language to root.resolve("i18n/$language/templates")),
            )
    }

    @Then("the template tree has {int} translated templates")
    fun hasTranslated(count: Int) {
        assertThat(tree!!.count(TemplateTranslationStatus.TRANSLATED)).isEqualTo(count)
    }

    @Then("the template tree has {int} untranslated templates")
    fun hasUntranslated(count: Int) {
        assertThat(tree!!.count(TemplateTranslationStatus.UNTRANSLATED)).isEqualTo(count)
    }

    @Then("the template tree has {int} missing templates")
    fun hasMissing(count: Int) {
        assertThat(tree!!.count(TemplateTranslationStatus.MISSING)).isEqualTo(count)
    }

    private fun write(
        dir: File,
        name: String,
        text: String,
    ) {
        dir.mkdirs()
        dir.resolve(name).writeText(text)
    }
}
