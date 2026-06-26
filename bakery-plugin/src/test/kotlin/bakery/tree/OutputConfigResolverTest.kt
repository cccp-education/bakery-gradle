package bakery.tree

import bakery.LayoutType
import bakery.ThemeConfig
import bakery.tree.SiteNode.Article
import bakery.tree.SiteNode.Section
import bakery.tree.SiteNode.Site
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OutputConfigResolverTest {

    private val darkTheme = ThemeConfig(mode = "dark", primaryColor = "#222")
    private val lightTheme = ThemeConfig(mode = "light", primaryColor = "#fff")

    private fun sampleTree(): SiteTree {
        val formationsSection = Section(
            path = "formations",
            articles = listOf(
                Article(path = "formations/ab-partition"),
                Article(path = "formations/cd-partition")
            ),
            outputConfig = OutputConfig(
                layout = LayoutType.SIDEBAR_LEFT,
                cssFiles = listOf("formations.css"),
                theme = lightTheme
            )
        )
        val blog = Section(
            path = "blog",
            articles = emptyList(),
            outputConfig = OutputConfig(template = "blog-index")
        )
        return SiteTree(Site(
            path = "",
            sections = listOf(formationsSection, blog),
            outputConfig = OutputConfig(
                template = "default-page",
                jsFiles = listOf("analytics.js")
            )
        ))
    }

    @Test
    fun `resolve on site returns its own output config`() {
        val tree = sampleTree()
        val resolver = OutputConfigResolver(tree)
        val site = tree.findByPath("") as Site
        val config = resolver.effectiveConfig(site)
        assertEquals("default-page", config.template)
        assertEquals(listOf("analytics.js"), config.jsFiles)
        assertNull(config.layout)
    }

    @Test
    fun `resolve on section returns its own config`() {
        val tree = sampleTree()
        val resolver = OutputConfigResolver(tree)
        val section = tree.findByPath("formations") as Section
        val config = resolver.effectiveConfig(section)
        assertEquals(LayoutType.SIDEBAR_LEFT, config.layout)
        assertEquals(listOf("formations.css"), config.cssFiles)
        assertEquals(lightTheme, config.theme)
    }

    @Test
    fun `resolve on article inherits template from site`() {
        val tree = sampleTree()
        val resolver = OutputConfigResolver(tree)
        val article = tree.findByPath("formations/ab-partition") as Article
        val config = resolver.effectiveConfig(article)
        assertEquals("default-page", config.template)
        assertEquals(LayoutType.SIDEBAR_LEFT, config.layout)
        assertEquals(lightTheme, config.theme)
    }

    @Test
    fun `resolve on article with its own override wins`() {
        val article = Article(
            path = "formations/ab-partition",
            outputConfig = OutputConfig(template = "custom-article", layout = LayoutType.CENTERED)
        )
        val section = Section(
            path = "formations",
            articles = listOf(article),
            outputConfig = OutputConfig(layout = LayoutType.SIDEBAR_LEFT, theme = lightTheme)
        )
        val tree = SiteTree(Site(path = "", sections = listOf(section)))
        val resolver = OutputConfigResolver(tree)
        val config = resolver.effectiveConfig(article)
        assertEquals("custom-article", config.template)
        assertEquals(LayoutType.CENTERED, config.layout)
        assertEquals(lightTheme, config.theme)
    }

    @Test
    fun `resolve with no config at any level returns empty config`() {
        val tree = SiteTree(Site(path = "", sections = listOf(
            Section(path = "empty", articles = listOf(Article(path = "empty/page")))
        )))
        val resolver = OutputConfigResolver(tree)
        val article = tree.findByPath("empty/page") as Article
        val config = resolver.effectiveConfig(article)
        assertNull(config.template)
        assertNull(config.layout)
        assertNull(config.cssFiles)
        assertNull(config.jsFiles)
        assertNull(config.theme)
    }

    @Test
    fun `resolveAll returns config for every node`() {
        val tree = sampleTree()
        val resolver = OutputConfigResolver(tree)
        val all = resolver.resolveAll()
        assertEquals(5, all.size)
        assertEquals("default-page", all[""]?.template)
        assertEquals("default-page", all["formations/ab-partition"]?.template)
        assertEquals(LayoutType.SIDEBAR_LEFT, all["formations/ab-partition"]?.layout)
        assertEquals("blog-index", all["blog"]?.template)
        assertEquals(LayoutType.SIDEBAR_LEFT, all["formations"]?.layout)
    }
}
