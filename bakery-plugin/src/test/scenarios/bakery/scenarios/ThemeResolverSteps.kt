package bakery.scenarios

import bakery.ThemeConfig
import bakery.tree.ResolvedTheme
import bakery.tree.ThemeResolver
import bakery.tree.SiteNode as BakerySiteNode
import bakery.tree.SiteTree as BakerySiteTree
import document.translation.delta.ArticleModification
import document.translation.delta.I18nDelta
import document.translation.plan.SubtreeI18nPlanner
import document.translation.tree.SiteNode as DocSiteNode
import document.translation.tree.SiteTree as DocSiteTree
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows

class ThemeResolverSteps {

    private var tree: BakerySiteTree? = null
    private var flatArticles: List<BakerySiteNode.Article>? = null
    private var defaultTheme: ThemeConfig = ThemeConfig()
    private var overrides: MutableMap<String, ThemeConfig> = mutableMapOf()
    private var resolved: ResolvedTheme? = null
    private var creationError: Throwable? = null
    private var beforeChecksums: Map<String, String> = emptyMap()
    private var delta: I18nDelta? = null
    private var docTree: DocSiteTree? = null
    private var docFlatArticles: List<DocSiteNode.Article>? = null

    @Given("a site tree with sections {string} and {string}")
    fun aSiteTreeWithSections(s1: String, s2: String) {
        val formations = BakerySiteNode.Section(
            path = s1,
            articles = listOf(
                BakerySiteNode.Article(path = "$s1/ab-partition"),
                BakerySiteNode.Article(path = "$s1/cd-partition")
            )
        )
        val blog = BakerySiteNode.Section(path = s2, articles = listOf(BakerySiteNode.Article(path = "$s2/hello")))
        tree = BakerySiteTree(BakerySiteNode.Site(path = "", sections = listOf(formations, blog)))

        val docFormations = DocSiteNode.Section(
            path = s1,
            articles = listOf(
                DocSiteNode.Article(path = "$s1/ab-partition"),
                DocSiteNode.Article(path = "$s1/cd-partition")
            )
        )
        val docBlog = DocSiteNode.Section(
            path = s2,
            articles = listOf(DocSiteNode.Article(path = "$s2/hello"))
        )
        docTree = DocSiteTree(
            DocSiteNode.Site(path = "", sections = listOf(docFormations, docBlog))
        )
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
            BakerySiteNode.Article(path = "formations/ab-partition"),
            BakerySiteNode.Article(path = "formations/cd-partition"),
            BakerySiteNode.Article(path = "blog/hello")
        ).take(count)
        docFlatArticles = listOf(
            DocSiteNode.Article(path = "formations/ab-partition"),
            DocSiteNode.Article(path = "formations/cd-partition"),
            DocSiteNode.Article(path = "blog/hello")
        ).take(count)
    }

    @Given("before checksums for all articles")
    fun beforeChecksumsForAllArticles() {
        beforeChecksums = if (docTree != null) {
            docTree!!.leaves().associate { it.path to "hash-${it.path}-v1" }
        } else {
            docFlatArticles!!.associate { it.path to "hash-${it.path}-v1" }
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
        if (docTree != null) SubtreeI18nPlanner(docTree!!, beforeChecksums)
        else SubtreeI18nPlanner(docFlatArticles!!, beforeChecksums)
}
