package bakery.scenarios

import bakery.tree.SiteNode
import bakery.tree.SiteNode.Article
import bakery.tree.SiteNode.Section
import bakery.tree.SiteNode.Site
import bakery.tree.SiteTree
import bakery.tree.TraversalOrder
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

class SiteTreeTraversalSteps(private val world: BakeryWorld) {

    private fun sampleTree(): SiteTree {
        val ab = Article(path = "formations/ab-partition")
        val cd = Article(path = "formations/cd-partition")
        val formations = Section(path = "formations", articles = listOf(ab, cd))
        val blog = Section(path = "blog", articles = emptyList())
        return SiteTree(Site(path = "", sections = listOf(formations, blog)))
    }

    private var walkedPaths: List<String> = emptyList()
    private var foundNode: SiteNode? = null
    private var foundSubtree: SiteTree? = null
    private var filteredNodes: List<SiteNode> = emptyList()
    private var leavesCount: Int = 0
    private var leavesPaths: List<String> = emptyList()
    private var sectionsCount: Int = 0
    private var sectionsPaths: List<String> = emptyList()
    private var labels: List<String> = emptyList()

    @Given("a tree with two sections {string} and {string} and two articles")
    fun aTreeWithTwoSectionsAndTwoArticles(s1: String, s2: String) {
        world.siteTree = sampleTree()
        assertThat(world.siteTree).isNotNull()
    }

    @When("I walk the tree in pre-order")
    fun iWalkTheTreeInPreOrder() {
        walkedPaths = world.siteTree!!.walk(TraversalOrder.PRE_ORDER).map { it.path }
    }

    @When("I walk the tree in post-order")
    fun iWalkTheTreeInPostOrder() {
        walkedPaths = world.siteTree!!.walk(TraversalOrder.POST_ORDER).map { it.path }
    }

    @Then("the path {string} comes before {string} before {string}")
    fun thePathComesBeforeBefore(p1: String, p2: String, p3: String) {
        val i1 = walkedPaths.indexOf(p1)
        val i2 = walkedPaths.indexOf(p2)
        val i3 = walkedPaths.indexOf(p3)
        assertThat(i1).isLessThan(i2)
        assertThat(i2).isLessThan(i3)
    }

    @Then("the path {string} is visited last")
    fun thePathIsVisitedLast(path: String) {
        assertThat(walkedPaths.last()).isEqualTo(path)
    }

    @When("I collect the leaves")
    fun iCollectTheLeaves() {
        val leaves = world.siteTree!!.leaves()
        leavesCount = leaves.size
        leavesPaths = leaves.map { it.path }
    }

    @Then("there are {int} leaves")
    fun thereAreLeaves(count: Int) {
        assertThat(leavesCount).isEqualTo(count)
    }

    @Then("the leaves have paths {string} and {string}")
    fun theLeavesHavePathsAnd(p1: String, p2: String) {
        assertThat(leavesPaths).containsExactlyInAnyOrder(p1, p2)
    }

    @When("I collect the sections")
    fun iCollectTheSections() {
        val sections = world.siteTree!!.sections()
        sectionsCount = sections.size
        sectionsPaths = sections.map { it.path }
    }

    @Then("there are {int} sections")
    fun thereAreSections(count: Int) {
        assertThat(sectionsCount).isEqualTo(count)
    }

    @Then("the sections have paths {string} {string} and {string}")
    fun theSectionsHavePaths(p1: String, p2: String, p3: String) {
        assertThat(sectionsPaths).containsExactlyInAnyOrder(p1, p2, p3)
    }

    @When("I search for node {string}")
    fun iSearchForNode(path: String) {
        foundNode = world.siteTree!!.findByPath(path)
    }

    @Then("the found node is a leaf")
    fun theFoundNodeIsALeaf() {
        assertThat(foundNode).isNotNull()
        assertThat(foundNode!!.isLeaf()).isTrue()
    }

    @Then("no node is found")
    fun noNodeIsFound() {
        assertThat(foundNode).isNull()
    }

    @When("I search for subtree {string}")
    fun iSearchForSubtree(path: String) {
        foundSubtree = world.siteTree!!.findSubtree(path)
    }

    @Then("the subtree root is a section")
    fun theSubtreeRootIsASection() {
        assertThat(foundSubtree).isNotNull()
        assertThat(foundSubtree!!.root.isSection()).isTrue()
    }

    @Then("the subtree contains {int} leaves")
    fun theSubtreeContainsLeaves(count: Int) {
        assertThat(foundSubtree!!.leaves().size).isEqualTo(count)
    }

    @When("I filter nodes of type article")
    fun iFilterNodesOfTypeArticle() {
        filteredNodes = world.siteTree!!.filter { it is Article }
    }

    @Then("there are {int} filtered nodes")
    fun thereAreFilteredNodes(count: Int) {
        assertThat(filteredNodes).hasSize(count)
    }

    @Then("all filtered nodes are leaves")
    fun allFilteredNodesAreLeaves() {
        assertThat(filteredNodes).allMatch { it.isLeaf() }
    }

    @When("I visit the tree labelling each node by its type")
    fun iVisitTheTreeLabellingEachNodeByItsType() {
        labels = world.siteTree!!.visit { node ->
            when (node) {
                is Site -> "site"
                is Section -> "section"
                is Article -> "article"
            }
        }
    }

    @Then("I get {int} labels")
    fun iGetLabels(count: Int) {
        assertThat(labels).hasSize(count)
    }

    @Then("the first label is {string}")
    fun theFirstLabelIs(label: String) {
        assertThat(labels.first()).isEqualTo(label)
    }

    @Then("the last label is {string}")
    fun theLastLabelIs(label: String) {
        assertThat(labels.last()).isEqualTo(label)
    }
}