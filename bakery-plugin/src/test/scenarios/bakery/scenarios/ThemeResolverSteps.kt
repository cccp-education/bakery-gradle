package bakery.scenarios

import bakery.ThemeConfig
import bakery.tree.ArticleModification
import bakery.tree.I18nDelta
import bakery.tree.ResolvedTheme
import bakery.tree.SiteNode.Article
import bakery.tree.SiteNode.Section
import bakery.tree.SiteNode.Site
import bakery.tree.SiteTree
import bakery.tree.SubtreeI18nPlanner
import bakery.tree.ThemeResolver
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows

class ThemeResolverSteps {

    private var tree: SiteTree? = null
    private var flatArticles: List<Article>? = null
    private var defaultTheme: ThemeConfig = ThemeConfig()
    private var overrides: MutableMap<String, ThemeConfig> = mutableMapOf()
    private var resolved: ResolvedTheme? = null
    private var creationError: Throwable? = null
    private var beforeChecksums: Map<String, String> = emptyMap()
    private var delta: I18nDelta? = null

    @Given("a site tree with sections {string} and {string}")
    fun aSiteTreeWithSections(s1: String, s2: String) {
        val formations = Section(
            path = s1,
            articles = listOf(
                Article(path = "$s1/ab-partition"),
                Article(path = "$s1/cd-partition")
            )
        )
        val blog = Section(path = s2, articles = listOf(Article(path = "$s2/hello")))
        tree = SiteTree(Site(path = "", sections = listOf(formations, blog)))
    }

    @Given("a default theme with primaryColor {string}")
    fun aDefaultThemeWithPrimaryColor(primaryColor: String) {
        defaultTheme = ThemeConfig(primaryColor = primaryColor)
    }

    @Given("a default theme with primaryColor {string} and mode {string}")
    fun aDefaultThemeWithPrimaryColorAndMode(primaryColor: String, mode: String) {
        defaultTheme = ThemeConfig(primaryColor = primaryColor, mode = mode)
    }

    @Given("a default theme with primaryColor {string} and mode {string} and fontFamily {string}")
    fun aDefaultThemeWithPrimaryColorModeAndFontFamily(primaryColor: String, mode: String, fontFamily: String) {
        defaultTheme = ThemeConfig(primaryColor = primaryColor, mode = mode, fontFamily = fontFamily)
    }

    @Given("an override at {string} with primaryColor {string}")
    fun anOverrideAtWithPrimaryColor(path: String, primaryColor: String) {
        overrides[path] = ThemeConfig(primaryColor = primaryColor)
    }

    @Given("an override at {string} with primaryColor {string} and mode {string}")
    fun anOverrideAtWithPrimaryColorAndMode(path: String, primaryColor: String, mode: String) {
        overrides[path] = ThemeConfig(primaryColor = primaryColor, mode = mode)
    }

    @When("I resolve the theme of the root node")
    fun iResolveTheThemeOfTheRootNode() {
        val resolver = ThemeResolver(tree!!, overrides.toMap(), defaultTheme)
        resolved = resolver.effectiveTheme(tree!!.root)
    }

    @When("I resolve the theme of node {string}")
    fun iResolveTheThemeOfNode(path: String) {
        val resolver = ThemeResolver(tree!!, overrides.toMap(), defaultTheme)
        val node = tree!!.findByPath(path)!!
        resolved = resolver.effectiveTheme(node)
    }

    @When("I create the theme resolver")
    fun iCreateTheThemeResolver() {
        creationError = assertThrows<IllegalArgumentException> {
            ThemeResolver(tree!!, overrides.toMap(), defaultTheme)
        }
    }

    @Then("the resolved primaryColor is {string}")
    fun theResolvedPrimaryColorIs(expected: String) {
        assertThat(resolved!!.theme.primaryColor).isEqualTo(expected)
    }

    @Then("the resolved mode is {string}")
    fun theResolvedModeIs(expected: String) {
        assertThat(resolved!!.theme.mode).isEqualTo(expected)
    }

    @Then("the resolved fontFamily is {string}")
    fun theResolvedFontFamilyIs(expected: String) {
        assertThat(resolved!!.theme.fontFamily).isEqualTo(expected)
    }

    @Then("the theme was resolved at path {string}")
    fun theThemeWasResolvedAtPath(expected: String) {
        assertThat(resolved!!.resolvedAtPath).isEqualTo(expected)
    }

    @Then("the creation fails")
    fun theCreationFails() {
        assertThat(creationError).isNotNull()
        assertThat(creationError).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Given("a flat article list with {int} articles")
    fun aFlatArticleListWith(count: Int) {
        flatArticles = listOf(
            Article(path = "formations/ab-partition"),
            Article(path = "formations/cd-partition"),
            Article(path = "blog/hello")
        ).take(count)
    }

    @Given("before checksums for all articles")
    fun beforeChecksumsForAllArticles() {
        beforeChecksums = if (tree != null) {
            tree!!.leaves().associate { it.path to "hash-${it.path}-v1" }
        } else {
            flatArticles!!.associate { it.path to "hash-${it.path}-v1" }
        }
    }

    @When("I compute the delta with the same checksums")
    fun iComputeTheDeltaWithTheSameChecksums() {
        delta = createPlanner().computeDelta(beforeChecksums)
    }

    @When("I compute the delta with {string} changed")
    fun iComputeTheDeltaWithChanged(path: String) {
        val after = beforeChecksums.toMutableMap().apply { this[path] = "hash-${path}-v2" }
        delta = createPlanner().computeDelta(after)
    }

    @When("I compute the delta for subtree {string} with {string} and {string} changed")
    fun iComputeTheDeltaForSubtreeWithAndChanged(subtree: String, p1: String, p2: String) {
        val after = beforeChecksums.toMutableMap().apply {
            this[p1] = "hash-${p1}-v2"
            this[p2] = "hash-${p2}-v2"
        }
        delta = createPlanner().computeDelta(after, subtreePath = subtree)
    }

    @When("I compute the delta for subtree {string} with {string} changed")
    fun iComputeTheDeltaForSubtreeWithChanged(subtree: String, path: String) {
        val after = beforeChecksums.toMutableMap().apply { this[path] = "hash-${path}-v2" }
        delta = createPlanner().computeDelta(after, subtreePath = subtree)
    }

    @When("I recompute the delta with the updated checksums")
    fun iRecomputeTheDeltaWithTheUpdatedChecksums() {
        val replanner = createPlanner(beforeChecksums = delta!!.updatedChecksums)
        delta = replanner.computeDelta(delta!!.updatedChecksums)
    }

    @Then("the delta has {int} modified article")
    fun theDeltaHasModifiedArticle(count: Int) {
        assertThat(delta!!.modifiedArticles).hasSize(count)
    }

    @Then("the delta has {int} modified articles")
    fun theDeltaHasModifiedArticles(count: Int) {
        assertThat(delta!!.modifiedArticles).hasSize(count)
    }

    @Then("the delta has {int} untouched article")
    fun theDeltaHasUntouchedArticle(count: Int) {
        assertThat(delta!!.untouchedArticles).hasSize(count)
    }

    @Then("the delta has {int} untouched articles")
    fun theDeltaHasUntouchedArticles(count: Int) {
        assertThat(delta!!.untouchedArticles).hasSize(count)
    }

    @Then("the modified article is {string}")
    fun theModifiedArticleIs(path: String) {
        assertThat(delta!!.modifiedArticles).hasSize(1)
        assertThat(delta!!.modifiedArticles[0].path).isEqualTo(path)
    }

    private fun createPlanner(beforeChecksums: Map<String, String> = this.beforeChecksums): SubtreeI18nPlanner =
        if (tree != null) SubtreeI18nPlanner(tree!!, beforeChecksums)
        else SubtreeI18nPlanner(flatArticles!!, beforeChecksums)
}