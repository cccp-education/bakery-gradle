package bakery.scenarios

import bakery.tree.SiteNode.Article
import bakery.tree.SiteNode.Section
import bakery.tree.SiteNode.Site
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows

class SiteTreeSteps {

    private var site: Site? = null
    private var section: Section? = null
    private var article: Article? = null
    private var secondSection: Section? = null
    private var firstArticle: Article? = null
    private var secondArticle: Article? = null
    private var creationError: Throwable? = null

    @Given("an empty site")
    fun anEmptySite() {
        site = Site(path = "", sections = emptyList())
    }

    @Then("the site path is empty")
    fun theSitePathIsEmpty() {
        assertThat(site!!.path).isEmpty()
    }

    @Then("the site has {int} sections")
    fun theSiteHasSections(count: Int) {
        assertThat(site!!.sections).hasSize(count)
    }

    @Then("the site is a section node")
    fun theSiteIsASectionNode() {
        assertThat(site!!.isSection()).isTrue()
    }

    @Then("the site is not a leaf")
    fun theSiteIsNotALeaf() {
        assertThat(site!!.isLeaf()).isFalse()
    }

    @Given("a section {string} with an article {string}")
    fun aSectionWithAnArticle(sectionPath: String, articlePath: String) {
        article = Article(path = articlePath)
        section = Section(path = sectionPath, articles = listOf(article!!))
    }

    @Given("an empty section {string}")
    fun anEmptySection(sectionPath: String) {
        secondSection = Section(path = sectionPath, articles = emptyList())
    }

    @When("I build a site with this section")
    fun iBuildASiteWithThisSection() {
        site = Site(path = "", sections = listOf(section!!))
    }

    @Then("the section {string} has {int} article")
    fun theSectionHasArticle(sectionPath: String, count: Int) {
        val found = site!!.sections.first { it.path == sectionPath }
        assertThat(found.articles).hasSize(count)
    }

    @Then("the article {string} is a leaf")
    fun theArticleIsALeaf(articlePath: String) {
        val found = site!!.sections
            .flatMap { it.articles }
            .first { it.path == articlePath }
        assertThat(found.isLeaf()).isTrue()
        assertThat(found.isSection()).isFalse()
    }

    @Given("an article {string}")
    fun anArticle(articlePath: String) {
        article = Article(path = articlePath)
    }

    @Then("the node is a leaf")
    fun theNodeIsALeaf() {
        assertThat(article!!.isLeaf()).isTrue()
    }

    @Then("the node is not a section")
    fun theNodeIsNotASection() {
        assertThat(article!!.isSection()).isFalse()
    }

    @Given("two articles with path {string}")
    fun twoArticlesWithPath(articlePath: String) {
        firstArticle = Article(path = articlePath)
        secondArticle = Article(path = articlePath)
    }

    @Then("the two articles are equal")
    fun theTwoArticlesAreEqual() {
        assertThat(firstArticle).isEqualTo(secondArticle)
    }

    @Then("the two sections are not equal")
    fun theTwoSectionsAreNotEqual() {
        assertThat(section).isNotEqualTo(secondSection)
    }

    @When("I create an article with an empty path")
    fun iCreateAnArticleWithAnEmptyPath() {
        creationError = assertThrows<IllegalArgumentException> { Article(path = "") }
    }

    @Then("the creation fails with a blank path error")
    fun theCreationFailsWithABlankPathError() {
        assertThat(creationError).isNotNull()
        assertThat(creationError).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Then("the path {string} is canonical")
    fun thePathIsCanonical(expected: String) {
        assertThat(article!!.path).isEqualTo(expected)
        assertThat(article!!.path).doesNotStartWith("/")
        assertThat(article!!.path).doesNotEndWith("/")
    }
}