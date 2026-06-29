package bakery.tree

import bakery.LayoutType
import bakery.tree.SiteNode.Article
import bakery.tree.SiteNode.Section
import bakery.tree.SiteNode.Site
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OutputConfigAssetsTest {

    @Test
    fun `output config with page assets`() {
        val assets = PageAssets(
            css = listOf(AssetRef(path = "theme.css", integrity = "sha256-abc")),
            js = listOf(AssetRef(path = "app.js", defer = true))
        )
        val config = OutputConfig(template = "page", assets = assets)

        assertEquals("page", config.template)
        assertEquals(assets, config.assets)
    }

    @Test
    fun `output config null assets by default`() {
        val config = OutputConfig()
        assertNull(config.assets)
    }

    @Test
    fun `resolve on article inherits assets from parent section`() {
        val sectionCss = listOf(AssetRef(path = "section.css", integrity = "sha256-section"))
        val section = Section(
            path = "formations",
            articles = listOf(Article(path = "formations/ab-partition")),
            outputConfig = OutputConfig(assets = PageAssets(css = sectionCss))
        )
        val tree = SiteTree(Site(path = "", sections = listOf(section)))
        val resolver = OutputConfigResolver(tree)
        val article = tree.findByPath("formations/ab-partition") as Article

        val config = resolver.effectiveConfig(article)

        assertEquals(sectionCss, config.assets?.css)
        assertNull(config.assets?.js)
    }

    @Test
    fun `resolve on article merges assets with site level`() {
        val siteJs = listOf(AssetRef(path = "site.js", defer = true))
        val articleCss = listOf(AssetRef(path = "article.css"))
        val site = Site(
            path = "",
            sections = listOf(
                Section(path = "blog", articles = listOf(
                    Article(
                        path = "blog/post1",
                        outputConfig = OutputConfig(assets = PageAssets(css = articleCss))
                    )
                ))
            ),
            outputConfig = OutputConfig(assets = PageAssets(js = siteJs))
        )
        val tree = SiteTree(site)
        val resolver = OutputConfigResolver(tree)
        val article = tree.findByPath("blog/post1") as Article

        val config = resolver.effectiveConfig(article)

        assertEquals(articleCss, config.assets?.css)
        assertEquals(siteJs, config.assets?.js)
    }

    @Test
    fun `resolve on article with no assets at any level returns null`() {
        val tree = SiteTree(Site(path = "", sections = listOf(
            Section(path = "s1", articles = listOf(Article(path = "s1/a1")))
        )))
        val resolver = OutputConfigResolver(tree)
        val article = tree.findByPath("s1/a1") as Article

        assertNull(resolver.effectiveConfig(article).assets)
    }

    @Test
    fun `resolveAll includes assets in config map`() {
        val assets = PageAssets(css = listOf(AssetRef(path = "global.css")))
        val tree = SiteTree(Site(
            path = "",
            sections = listOf(Section(path = "s1", articles = listOf(Article(path = "s1/a1")))),
            outputConfig = OutputConfig(assets = assets)
        ))
        val resolver = OutputConfigResolver(tree)
        val all = resolver.resolveAll()

        assertEquals(assets, all[""]?.assets)
        assertEquals(assets, all["s1/a1"]?.assets)
    }
}
