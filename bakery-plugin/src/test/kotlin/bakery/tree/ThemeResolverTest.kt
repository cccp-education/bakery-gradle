package bakery.tree

import bakery.ThemeConfig
import bakery.tree.SiteNode.Article
import bakery.tree.SiteNode.Section
import bakery.tree.SiteNode.Site
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ThemeResolverTest {

    private fun sampleTree(): SiteTree {
        val ab = Article(path = "formations/ab-partition")
        val cd = Article(path = "formations/cd-partition")
        val formations = Section(path = "formations", articles = listOf(ab, cd))
        val blog = Section(path = "blog", articles = listOf(Article(path = "blog/hello")))
        return SiteTree(Site(path = "", sections = listOf(formations, blog)))
    }

    @Test
    fun `root node without override resolves to default theme`() {
        val tree = sampleTree()
        val resolver = ThemeResolver(tree, overrides = emptyMap(), default = ThemeConfig(primaryColor = "#abc"))

        val resolved = resolver.effectiveTheme(tree.root)

        assertEquals("#abc", resolved.theme.primaryColor)
        assertEquals("", resolved.resolvedAtPath)
    }

    @Test
    fun `section inherits site default when no override`() {
        val tree = sampleTree()
        val resolver = ThemeResolver(tree, overrides = emptyMap(), default = ThemeConfig(primaryColor = "#abc"))

        val section = tree.findByPath("formations")!!
        val resolved = resolver.effectiveTheme(section)

        assertEquals("#abc", resolved.theme.primaryColor)
        assertEquals("", resolved.resolvedAtPath)
    }

    @Test
    fun `article inherits site default when no override`() {
        val tree = sampleTree()
        val resolver = ThemeResolver(tree, overrides = emptyMap(), default = ThemeConfig(primaryColor = "#abc"))

        val article = tree.findByPath("formations/ab-partition")!!
        val resolved = resolver.effectiveTheme(article)

        assertEquals("#abc", resolved.theme.primaryColor)
        assertEquals("", resolved.resolvedAtPath)
    }

    @Test
    fun `section override replaces inherited theme completely`() {
        val tree = sampleTree()
        val overrides = mapOf("formations" to ThemeConfig(primaryColor = "#f00", mode = "dark"))

        val resolver = ThemeResolver(tree, overrides, default = ThemeConfig(primaryColor = "#abc", mode = "auto"))

        val section = tree.findByPath("formations")!!
        val resolved = resolver.effectiveTheme(section)

        assertEquals("#f00", resolved.theme.primaryColor)
        assertEquals("dark", resolved.theme.mode)
        assertEquals("formations", resolved.resolvedAtPath)
    }

    @Test
    fun `article under overridden section inherits section override`() {
        val tree = sampleTree()
        val overrides = mapOf("formations" to ThemeConfig(primaryColor = "#f00", mode = "dark"))

        val resolver = ThemeResolver(tree, overrides, default = ThemeConfig(primaryColor = "#abc", mode = "auto"))

        val article = tree.findByPath("formations/ab-partition")!!
        val resolved = resolver.effectiveTheme(article)

        assertEquals("#f00", resolved.theme.primaryColor)
        assertEquals("dark", resolved.theme.mode)
        assertEquals("formations", resolved.resolvedAtPath)
    }

    @Test
    fun `article own override takes precedence over section override`() {
        val tree = sampleTree()
        val overrides = mapOf(
            "formations" to ThemeConfig(primaryColor = "#f00", mode = "dark"),
            "formations/ab-partition" to ThemeConfig(primaryColor = "#00f", mode = "light")
        )

        val resolver = ThemeResolver(tree, overrides, default = ThemeConfig(primaryColor = "#abc", mode = "auto"))

        val article = tree.findByPath("formations/ab-partition")!!
        val resolved = resolver.effectiveTheme(article)

        assertEquals("#00f", resolved.theme.primaryColor)
        assertEquals("light", resolved.theme.mode)
        assertEquals("formations/ab-partition", resolved.resolvedAtPath)
    }

    @Test
    fun `sibling article without own override inherits section override`() {
        val tree = sampleTree()
        val overrides = mapOf(
            "formations" to ThemeConfig(primaryColor = "#f00", mode = "dark"),
            "formations/ab-partition" to ThemeConfig(primaryColor = "#00f", mode = "light")
        )

        val resolver = ThemeResolver(tree, overrides, default = ThemeConfig(primaryColor = "#abc", mode = "auto"))

        val article = tree.findByPath("formations/cd-partition")!!
        val resolved = resolver.effectiveTheme(article)

        assertEquals("#f00", resolved.theme.primaryColor)
        assertEquals("dark", resolved.theme.mode)
        assertEquals("formations", resolved.resolvedAtPath)
    }

    @Test
    fun `article in non-overridden section falls back to site default`() {
        val tree = sampleTree()
        val overrides = mapOf("formations" to ThemeConfig(primaryColor = "#f00", mode = "dark"))

        val resolver = ThemeResolver(tree, overrides, default = ThemeConfig(primaryColor = "#abc", mode = "auto"))

        val article = tree.findByPath("blog/hello")!!
        val resolved = resolver.effectiveTheme(article)

        assertEquals("#abc", resolved.theme.primaryColor)
        assertEquals("auto", resolved.theme.mode)
        assertEquals("", resolved.resolvedAtPath)
    }

    @Test
    fun `override does not merge with inherited - replacement is total`() {
        val tree = sampleTree()
        val overrides = mapOf(
            "formations" to ThemeConfig(primaryColor = "#f00")
        )

        val resolver = ThemeResolver(tree, overrides, default = ThemeConfig(
            primaryColor = "#abc", mode = "dark", fontFamily = "Inter"
        ))

        val section = tree.findByPath("formations")!!
        val resolved = resolver.effectiveTheme(section)

        assertEquals("#f00", resolved.theme.primaryColor)
        assertEquals("auto", resolved.theme.mode)
        assertEquals("", resolved.theme.fontFamily)
    }

    @Test
    fun `orphan override path not in tree throws`() {
        val tree = sampleTree()

        assertThrows<IllegalArgumentException> {
            ThemeResolver(tree, mapOf("unknown/path" to ThemeConfig(primaryColor = "#f00")), ThemeConfig())
        }
    }

    @Test
    fun `resolveAll maps every node path to its resolved theme`() {
        val tree = sampleTree()
        val overrides = mapOf("formations" to ThemeConfig(primaryColor = "#f00", mode = "dark"))

        val resolver = ThemeResolver(tree, overrides, default = ThemeConfig(primaryColor = "#abc", mode = "auto"))

        val all = resolver.resolveAll()

        assertEquals(tree.walk().size, all.size)
        assertEquals("#f00", all["formations"]!!.theme.primaryColor)
        assertEquals("dark", all["formations"]!!.theme.mode)
        assertEquals("#f00", all["formations/ab-partition"]!!.theme.primaryColor)
        assertEquals("#abc", all["blog"]!!.theme.primaryColor)
        assertEquals("auto", all["blog"]!!.theme.mode)
        assertEquals("#abc", all["blog/hello"]!!.theme.primaryColor)
    }

    @Test
    fun `root without default uses ThemeConfig defaults`() {
        val tree = sampleTree()
        val resolver = ThemeResolver(tree, overrides = emptyMap())

        val resolved = resolver.effectiveTheme(tree.root)

        assertEquals(ThemeConfig(), resolved.theme)
        assertEquals(ThemeConfig().primaryColor, resolved.theme.primaryColor)
    }
}