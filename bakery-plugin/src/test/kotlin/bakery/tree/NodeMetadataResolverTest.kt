package bakery.tree

import bakery.LayoutType
import bakery.ThemeConfig
import bakery.tree.SiteNode.Article
import bakery.tree.SiteNode.Section
import bakery.tree.SiteNode.Site
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NodeMetadataResolverTest {

    private fun sampleTree(): SiteTree {
        val formationsSection = Section(
            path = "formations",
            articles = listOf(
                Article(path = "formations/ab-partition"),
                Article(path = "formations/cd-partition")
            ),
            metadata = NodeMetadata(
                title = "Formations",
                description = "Toutes nos formations",
                layout = LayoutType.SIDEBAR_LEFT
            )
        )
        val blog = Section(
            path = "blog",
            articles = emptyList(),
            metadata = NodeMetadata(title = "Blog")
        )
        return SiteTree(Site(
            path = "",
            sections = listOf(formationsSection, blog),
            metadata = NodeMetadata(title = "Mon Site", tags = listOf("education"))
        ))
    }

    @Test
    fun `resolve metadata on site returns site metadata`() {
        val tree = sampleTree()
        val resolver = NodeMetadataResolver(tree)
        val site = tree.findByPath("") as Site
        val meta = resolver.effectiveMetadata(site)
        assertEquals("Mon Site", meta.title)
        assertNull(meta.description)
        assertEquals(listOf("education"), meta.tags)
    }

    @Test
    fun `resolve metadata on section returns its own metadata`() {
        val tree = sampleTree()
        val resolver = NodeMetadataResolver(tree)
        val section = tree.findByPath("formations") as Section
        val meta = resolver.effectiveMetadata(section)
        assertEquals("Formations", meta.title)
        assertEquals("Toutes nos formations", meta.description)
        assertEquals(LayoutType.SIDEBAR_LEFT, meta.layout)
    }

    @Test
    fun `resolve metadata on article inherits from parent section`() {
        val tree = sampleTree()
        val resolver = NodeMetadataResolver(tree)
        val article = tree.findByPath("formations/ab-partition") as Article
        val meta = resolver.effectiveMetadata(article)
        assertEquals("Formations", meta.title)
        assertEquals("Toutes nos formations", meta.description)
        assertEquals(LayoutType.SIDEBAR_LEFT, meta.layout)
    }

    @Test
    fun `resolve metadata on article with own override wins`() {
        val article = Article(
            path = "formations/ab-partition",
            metadata = NodeMetadata(title = "AB Partition Custom", layout = LayoutType.CENTERED)
        )
        val section = Section(
            path = "formations",
            articles = listOf(article),
            metadata = NodeMetadata(title = "Formations", layout = LayoutType.SIDEBAR_LEFT)
        )
        val tree = SiteTree(Site(path = "", sections = listOf(section)))
        val resolver = NodeMetadataResolver(tree)
        val resolved = resolver.effectiveMetadata(article)
        assertEquals("AB Partition Custom", resolved.title)
        assertEquals(LayoutType.CENTERED, resolved.layout)
    }

    @Test
    fun `resolveAll returns metadata for every node`() {
        val tree = sampleTree()
        val resolver = NodeMetadataResolver(tree)
        val all = resolver.resolveAll()
        assertEquals(5, all.size)
        assertEquals("Mon Site", all[""]?.title)
        assertEquals("Formations", all["formations"]?.title)
        assertEquals("Formations", all["formations/ab-partition"]?.title)
        assertEquals("Formations", all["formations/cd-partition"]?.title)
        assertEquals("Blog", all["blog"]?.title)
    }

    @Test
    fun `site metadata with no children uses defaults for non-set fields`() {
        val tree = SiteTree(Site(path = "", sections = emptyList()))
        val resolver = NodeMetadataResolver(tree)
        val site = tree.findByPath("") as Site
        val meta = resolver.effectiveMetadata(site)
        assertNull(meta.title)
        assertNull(meta.description)
        assertNull(meta.tags)
        assertNull(meta.layout)
    }
}
