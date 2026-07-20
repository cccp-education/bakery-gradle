package bakery.scenarios

import bakery.i18n.rtl.RtlDirectionInjector
import document.translation.PivotFrontmatter
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

class RtlDirectionSteps {

    private val injector = RtlDirectionInjector()
    private lateinit var frontmatter: PivotFrontmatter

    @Given("a pivot frontmatter with no language directive")
    fun pivotFrontmatterWithNoLanguageDirective() {
        frontmatter = PivotFrontmatter(
            title = "Test",
            date = "2026-07-18",
            type = "post",
            status = "published"
        )
    }

    @Given("a pivot frontmatter with {string} set to {string}")
    fun pivotFrontmatterWithAttribute(key: String, value: String) {
        frontmatter = if (key == "jbake-lang") {
            frontmatter.copy(jbakeAttributes = mapOf("lang" to value))
        } else {
            frontmatter.copy(asciidocAttributes = mapOf(key to value))
        }
    }

    @Given("a pivot frontmatter with {string} set to {string} and {string} set to {string}")
    fun pivotFrontmatterWithTwoAttributes(k1: String, v1: String, k2: String, v2: String) {
        val jbake = mutableMapOf<String, String>()
        val asciidoc = mutableMapOf<String, String>()
        if (k1 == "jbake-lang") jbake["lang"] = v1 else asciidoc[k1] = v1
        if (k2 == "jbake-lang") jbake["lang"] = v2 else asciidoc[k2] = v2
        frontmatter = frontmatter.copy(jbakeAttributes = jbake, asciidocAttributes = asciidoc)
    }

    @When("the RTL direction injector injects language {string}")
    fun rtlInjectorInjectsLanguage(lang: String) {
        frontmatter = injector.inject(frontmatter, lang)
    }

    @When("the RTL direction injector injects language {string} twice")
    fun rtlInjectorInjectsLanguageTwice(lang: String) {
        frontmatter = injector.inject(frontmatter, lang)
        frontmatter = injector.inject(frontmatter, lang)
    }

    @Then("the frontmatter asciidocAttributes should contain {string} with value {string}")
    fun asciidocShouldContain(key: String, value: String) {
        if (key == "jbake-lang") {
            assertThat(frontmatter.jbakeAttributes["lang"]).isEqualTo(value)
        } else {
            assertThat(frontmatter.asciidocAttributes[key]).isEqualTo(value)
        }
    }

    @Then("the frontmatter asciidocAttributes should not contain {string}")
    fun asciidocShouldNotContain(key: String) {
        if (key == "jbake-lang") {
            assertThat(frontmatter.jbakeAttributes).doesNotContainKey("lang")
        } else {
            assertThat(frontmatter.asciidocAttributes).doesNotContainKey(key)
        }
    }

    @Then("the frontmatter should be unchanged")
    fun frontmatterShouldBeUnchanged() {
        assertThat(frontmatter.jbakeAttributes).doesNotContainKey("lang")
        assertThat(frontmatter.asciidocAttributes).doesNotContainKey("lang")
    }
}