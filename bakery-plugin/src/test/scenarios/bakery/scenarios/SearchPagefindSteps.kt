package bakery.scenarios

import bakery.search.PagefindEntry
import bakery.search.PagefindIndexContract
import bakery.search.SearchBundle
import bakery.search.SearchIndexViolation
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

class SearchPagefindSteps {
    private var entryJson: String = ""
    private var violations: List<SearchIndexViolation> = emptyList()
    private var bundlePath: String = ""

    @Given("a Pagefind entry with indexes for {string} and {string}")
    fun aPagefindEntryWithIndexesFor(
        first: String,
        second: String,
    ) {
        entryJson = entry(mapOf(first to 10, second to 10))
    }

    @Given("a Pagefind entry with an index for {string}")
    fun aPagefindEntryWithAnIndexFor(language: String) {
        entryJson = entry(mapOf(language to 10))
    }

    @Given("a Pagefind entry with a zero-page index for {string}")
    fun aPagefindEntryWithAZeroPageIndexFor(language: String) {
        entryJson = entry(mapOf(language to 0))
    }

    @When("the index is verified against the staged languages {string} and {string}")
    fun theIndexIsVerifiedAgainstTwoLanguages(
        first: String,
        second: String,
    ) {
        verify(setOf(first, second))
    }

    @When("the index is verified against the staged language {string}")
    fun theIndexIsVerifiedAgainstOneLanguage(language: String) {
        verify(setOf(language))
    }

    @When("the bundle script is resolved from {string}")
    fun theBundleScriptIsResolvedFrom(rootpath: String) {
        bundlePath = SearchBundle.scriptPath(rootpath)
    }

    @Then("no search index violation is reported")
    fun noSearchIndexViolationIsReported() {
        assertThat(violations).isEmpty()
    }

    @Then("the search index reports {string} for {string}")
    fun theSearchIndexReports(
        code: String,
        language: String,
    ) {
        assertThat(violations.map { it.code }).contains(code)
        assertThat(violations.map { it.detail }).contains(language)
    }

    @Then("the bundle script path is {string}")
    fun theBundleScriptPathIs(expected: String) {
        assertThat(bundlePath).isEqualTo(expected)
    }

    private fun verify(languages: Set<String>) {
        val index = PagefindEntry.parse(entryJson).getOrNull()
        checkNotNull(index) { "the fixture must parse: $entryJson" }
        violations = PagefindIndexContract.verify(index, languages)
    }

    private fun entry(pageCounts: Map<String, Int>): String {
        val languages =
            pageCounts.entries.joinToString(",") { (code, pages) ->
                """"$code":{"hash":"${code}_hash","wasm":"$code","page_count":$pages}"""
            }
        return """{"version":"1.6.0","languages":{$languages},"include_characters":["_"]}"""
    }
}
