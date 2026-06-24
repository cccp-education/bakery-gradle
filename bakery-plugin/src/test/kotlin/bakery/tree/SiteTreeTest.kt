package bakery.tree

import bakery.tree.SiteNode.Article
import bakery.tree.SiteNode.Section
import bakery.tree.SiteNode.Site
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SiteTreeTest {

    private fun sampleTree(): SiteTree {
        val ab = Article(path = "formations/ab-partition")
        val cd = Article(path = "formations/cd-partition")
        val formations = Section(path = "formations", articles = listOf(ab, cd))
        val blog = Section(path = "blog", articles = emptyList())
        return SiteTree(Site(path = "", sections = listOf(formations, blog)))
    }

    @Test
    fun `walk pre-order visits root then sections then articles`() {
        val tree = sampleTree()

        val paths = tree.walk().map { it.path }

        assertEquals(
            listOf("", "formations", "formations/ab-partition", "formations/cd-partition", "blog"),
            paths
        )
    }

    @Test
    fun `walk post-order visits children before parents`() {
        val tree = sampleTree()

        val paths = tree.walk(order = TraversalOrder.POST_ORDER).map { it.path }

        assertEquals(
            listOf("formations/ab-partition", "formations/cd-partition", "formations", "blog", ""),
            paths
        )
    }

    @Test
    fun `walk empty tree yields only root`() {
        val tree = SiteTree(Site(path = "", sections = emptyList()))

        val nodes = tree.walk()

        assertEquals(1, nodes.size)
        assertEquals("", nodes[0].path)
    }

    @Test
    fun `leaves returns only articles`() {
        val tree = sampleTree()

        val leaves = tree.leaves()

        assertEquals(2, leaves.size)
        assertTrue(leaves.all { it is Article })
        assertEquals(
            listOf("formations/ab-partition", "formations/cd-partition"),
            leaves.map { it.path }
        )
    }

    @Test
    fun `sections returns only sections including root site`() {
        val tree = sampleTree()

        val sections = tree.sections()

        assertEquals(3, sections.size)
        assertTrue(sections.all { it.isSection() })
        assertEquals(listOf("", "formations", "blog"), sections.map { it.path })
    }

    @Test
    fun `filter by type Article returns articles only`() {
        val tree = sampleTree()

        val articles = tree.filter { it is Article }

        assertEquals(2, articles.size)
        assertTrue(articles.all { it is Article })
    }

    @Test
    fun `findByPath locates an article by canonical path`() {
        val tree = sampleTree()

        val found = tree.findByPath("formations/ab-partition")

        assertTrue(found is Article)
        assertEquals("formations/ab-partition", found.path)
    }

    @Test
    fun `findByPath locates a section`() {
        val tree = sampleTree()

        val found = tree.findByPath("formations")

        assertTrue(found is Section)
        assertEquals("formations", found.path)
    }

    @Test
    fun `findByPath returns null for unknown path`() {
        val tree = sampleTree()

        assertNull(tree.findByPath("unknown/path"))
    }

    @Test
    fun `findByPath returns root for empty path`() {
        val tree = sampleTree()

        val found = tree.findByPath("")

        assertTrue(found is Site)
        assertEquals("", found.path)
    }

    @Test
    fun `findSubtree returns the subtree rooted at matching section`() {
        val tree = sampleTree()

        val subtree = tree.findSubtree("formations")

        assertTrue(subtree is SiteTree)
        val root = subtree.root
        assertTrue(root is Section)
        assertEquals("formations", root.path)
        assertEquals(2, subtree.leaves().size)
    }

    @Test
    fun `findSubtree returns null for unknown path`() {
        val tree = sampleTree()

        assertNull(tree.findSubtree("unknown"))
    }

    @Test
    fun `findSubtree on article path returns a leaf subtree`() {
        val tree = sampleTree()

        val subtree = tree.findSubtree("formations/ab-partition")

        assertTrue(subtree is SiteTree)
        assertTrue(subtree.root is Article)
        assertEquals("formations/ab-partition", subtree.root.path)
        assertEquals(1, subtree.walk().size)
    }

    @Test
    fun `visit applies transform to every node and collects results`() {
        val tree = sampleTree()

        val labels = tree.visit { node ->
            when (node) {
                is Site -> "site"
                is Section -> "section:${node.path}"
                is Article -> "article:${node.path}"
            }
        }

        assertEquals(
            listOf("site", "section:formations", "article:formations/ab-partition",
                "article:formations/cd-partition", "section:blog"),
            labels
        )
    }

    @Test
    fun `single level tree has root and one section no articles`() {
        val tree = SiteTree(Site(path = "", sections = listOf(Section(path = "blog", articles = emptyList()))))

        assertEquals(2, tree.walk().size)
        assertEquals(0, tree.leaves().size)
        assertEquals(2, tree.sections().size)
    }

    @Test
    fun `three level tree has correct counts`() {
        val tree = sampleTree()

        assertEquals(5, tree.walk().size)
        assertEquals(2, tree.leaves().size)
        assertEquals(3, tree.sections().size)
    }

    @Test
    fun `filter with predicate on path prefix`() {
        val tree = sampleTree()

        val formationsNodes = tree.filter { it.path.startsWith("formations") }

        assertEquals(3, formationsNodes.size)
        assertEquals(
            listOf("formations", "formations/ab-partition", "formations/cd-partition"),
            formationsNodes.map { it.path }
        )
    }
}