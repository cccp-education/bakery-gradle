package bakery.tree

import bakery.tree.SiteNode.Article
import bakery.tree.SiteNode.Section
import bakery.tree.SiteNode.Site
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SiteNodeTest {

    @Test
    fun `empty site has root path and no children`() {
        val site = Site(path = "", sections = emptyList())

        assertEquals("", site.path)
        assertEquals(0, site.sections.size)
        assertTrue(site.isSection())
        assertFalse(site.isLeaf())
    }

    @Test
    fun `site with one section exposes it as child`() {
        val section = Section(path = "formations", articles = emptyList())
        val site = Site(path = "", sections = listOf(section))

        assertEquals(1, site.sections.size)
        assertEquals("formations", site.sections[0].path)
    }

    @Test
    fun `section is section, article is leaf`() {
        val article = Article(path = "formations/ab-partition")
        val section = Section(path = "formations", articles = listOf(article))

        assertTrue(section.isSection())
        assertFalse(section.isLeaf())
        assertTrue(article.isLeaf())
        assertFalse(article.isSection())
    }

    @Test
    fun `three-level tree site section article`() {
        val article = Article(path = "formations/ab-partition")
        val section = Section(path = "formations", articles = listOf(article))
        val site = Site(path = "", sections = listOf(section))

        assertEquals(1, site.sections.size)
        assertEquals(1, site.sections[0].articles.size)
        assertEquals("formations/ab-partition", site.sections[0].articles[0].path)
    }

    @Test
    fun `structural equality holds for same path and children`() {
        val a1 = Article(path = "formations/ab-partition")
        val a2 = Article(path = "formations/ab-partition")
        val s1 = Section(path = "formations", articles = listOf(a1))
        val s2 = Section(path = "formations", articles = listOf(a2))

        assertEquals(a1, a2)
        assertEquals(s1, s2)
        assertEquals(Site(path = "", sections = listOf(s1)), Site(path = "", sections = listOf(s2)))
    }

    @Test
    fun `different path yields non-equality`() {
        assertNotEquals(Article(path = "a"), Article(path = "b"))
        assertNotEquals(Section(path = "x", articles = emptyList()), Section(path = "y", articles = emptyList()))
    }

    @Test
    fun `different children yield non-equality for same path`() {
        val withArticle = Section(path = "formations", articles = listOf(Article(path = "formations/ab-partition")))
        val empty = Section(path = "formations", articles = emptyList())

        assertNotEquals(withArticle, empty)
    }

    @Test
    fun `article path is canonical relative identifier`() {
        val article = Article(path = "formations/ab-partition")

        assertEquals("formations/ab-partition", article.path)
        assertFalse(article.path.startsWith("/"))
        assertFalse(article.path.endsWith("/"))
    }

    @Test
    fun `section path is canonical relative identifier`() {
        val section = Section(path = "formations", articles = emptyList())

        assertEquals("formations", section.path)
        assertFalse(section.path.startsWith("/"))
        assertFalse(section.path.endsWith("/"))
    }

    @Test
    fun `root site path is empty string`() {
        val site = Site(path = "", sections = emptyList())

        assertEquals("", site.path)
    }

    @Test
    fun `data classes are immutable - copy produces new instance`() {
        val original = Article(path = "formations/ab-partition")
        val copied = original.copy(path = "formations/cd-partition")

        assertNotEquals(original, copied)
        assertEquals("formations/ab-partition", original.path)
        assertEquals("formations/cd-partition", copied.path)
    }

    @Test
    fun `site copy preserves sections and overrides path`() {
        val section = Section(path = "formations", articles = emptyList())
        val site = Site(path = "", sections = listOf(section))
        val moved = site.copy(path = "mirror")

        assertEquals("mirror", moved.path)
        assertEquals(1, moved.sections.size)
        assertEquals(site.sections, moved.sections)
    }

    @Test
    fun `sealed interface exhausts to site section article`() {
        val nodes: List<SiteNode> = listOf(
            Site(path = "", sections = emptyList()),
            Section(path = "s", articles = emptyList()),
            Article(path = "s/a")
        )

        val labels = nodes.map {
            when (it) {
                is Site -> "site"
                is Section -> "section"
                is Article -> "article"
            }
        }

        assertEquals(listOf("site", "section", "article"), labels)
    }

    @Test
    fun `article requires non-blank path`() {
        assertThrows<IllegalArgumentException> { Article(path = "") }
    }

    @Test
    fun `section requires non-blank path`() {
        assertThrows<IllegalArgumentException> { Section(path = "", articles = emptyList()) }
    }
}