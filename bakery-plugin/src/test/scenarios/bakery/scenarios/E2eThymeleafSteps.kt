package bakery.scenarios

import bakery.ThymeleafRenderingTestFactory
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

/**
 * Cucumber step definitions for E2E Thymeleaf component rendering validation (BKY-JB-9 Phase B).
 *
 * Uses [ThymeleafRenderingTestFactory] to render templates with contextual variables
 * and then asserts on the resulting HTML output — proving that `th:if`/`th:unless` guards,
 * variable interpolation, and `th:each`/`th:text`/`th:replace` bindings
 * produce the expected markup in the final rendered page.
 *
 * Context is built incrementally across steps via the [context] map,
 * supporting both flat key/value pairs and nested maps.
 * Call [renderTemplate] to render with the accumulated context.
 */
class E2eThymeleafSteps {

    private val factory = ThymeleafRenderingTestFactory()
    private var renderedHtml: String = ""
    private var templateName: String = ""
    private var context: MutableMap<String, Any> = mutableMapOf()

    @Given("the template context has {string} = {string}")
    fun addContextEntry(key: String, value: String) {
        context[key] = value
    }

    @Given("the nested context {string} has {string} = {string}")
    fun addNestedContextEntry(parentKey: String, childKey: String, value: String) {
        @Suppress("UNCHECKED_CAST")
        val nestedMap = context.getOrPut(parentKey) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
        nestedMap[childKey] = value
    }

    @When("I render the template {string}")
    fun renderTemplate(name: String) {
        templateName = name
        renderedHtml = factory.render(name, context)
        assertThat(renderedHtml).describedAs("HTML should have been rendered for template '$name'").isNotEmpty()
    }

    @Then("the rendered HTML should contain {string}")
    fun renderedHtmlShouldContain(expected: String) {
        assertThat(renderedHtml)
            .describedAs("Rendered '$templateName' should contain '$expected'")
            .contains(expected)
    }

    @Then("the rendered HTML should not contain {string}")
    fun renderedHtmlShouldNotContain(expected: String) {
        assertThat(renderedHtml)
            .describedAs("Rendered '$templateName' should NOT contain '$expected'")
            .doesNotContain(expected)
    }
}