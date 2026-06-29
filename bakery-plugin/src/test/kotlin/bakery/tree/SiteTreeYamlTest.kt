package bakery.tree

import bakery.tree.SiteNode.Article
import bakery.tree.SiteNode.Section
import bakery.tree.SiteNode.Site
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SiteTreeYamlTest {

    private val yaml = SiteTreeYaml

    @Test
    fun `round-trip empty site yields identical tree`() {
        val original = Site(path = "", sections = emptyList())

        val serialized = yaml.serialize(original)
        val parsed = yaml.parse(serialized)

        assertEquals(original, parsed)
    }

    @Test
    fun `round-trip three-level tree yields identical tree`() {
        val ab = Article(path = "formations/ab-partition")
        val cd = Article(path = "formations/cd-partition")
        val formations = Section(path = "formations", articles = listOf(ab, cd))
        val original = Site(path = "", sections = listOf(formations))

        val serialized = yaml.serialize(original)
        val parsed = yaml.parse(serialized)

        assertEquals(original, parsed)
    }

    @Test
    fun `round-trip site with multiple sections preserves order`() {
        val blog = Section(path = "blog", articles = listOf(Article(path = "blog/post-1")))
        val formations = Section(path = "formations", articles = listOf(Article(path = "formations/ab")))
        val original = Site(path = "", sections = listOf(formations, blog))

        val serialized = yaml.serialize(original)
        val parsed = yaml.parse(serialized)

        assertEquals(original, parsed)
        val parsedSite = parsed as Site
        assertEquals(listOf("formations", "blog"), parsedSite.sections.map { it.path })
    }

    @Test
    fun `serialize produces YAML with type discriminator site`() {
        val site = Site(path = "", sections = emptyList())

        val out = yaml.serialize(site)

        assertTrue(out.contains("type: site"))
    }

    @Test
    fun `serialize produces YAML with type discriminator section and article`() {
        val tree = Section(path = "s", articles = listOf(Article(path = "s/a")))

        val out = yaml.serialize(tree)

        assertTrue(out.contains("type: section"))
        assertTrue(out.contains("type: article"))
    }

    @Test
    fun `parse empty tree yaml returns site with no sections`() {
        val yamlText = """
            type: site
            path: ""
            sections: []
        """.trimIndent()

        val parsed = yaml.parse(yamlText)

        assertTrue(parsed is Site)
        assertEquals("", parsed.path)
        assertEquals(0, parsed.sections.size)
    }

    @Test
    fun `parse three-level yaml returns correct tree`() {
        val yamlText = """
            type: site
            path: ""
            sections:
              - type: section
                path: formations
                articles:
                  - type: article
                    path: formations/ab-partition
                  - type: article
                    path: formations/cd-partition
        """.trimIndent()

        val parsed = yaml.parse(yamlText)

        assertTrue(parsed is Site)
        assertEquals(1, parsed.sections.size)
        assertEquals("formations", parsed.sections[0].path)
        assertEquals(2, parsed.sections[0].articles.size)
        assertEquals("formations/ab-partition", parsed.sections[0].articles[0].path)
        assertEquals("formations/cd-partition", parsed.sections[0].articles[1].path)
    }

    @Test
    fun `parse null input returns null`() {
        assertNull(yaml.parseOrNull(null))
    }

    @Test
    fun `parseOrNull on invalid yaml returns null`() {
        val invalid = """
            type: unknown
            path: x
        """.trimIndent()

        assertNull(yaml.parseOrNull(invalid))
    }

    @Test
    fun `parse on invalid type throws IllegalArgumentException`() {
        val invalid = """
            type: not-a-type
            path: x
        """.trimIndent()

        assertThrows<Exception> { yaml.parse(invalid) }
    }

    @Test
    fun `parse on blank article path throws`() {
        val invalid = """
            type: site
            path: ""
            sections:
              - type: section
                path: s
                articles:
                  - type: article
                    path: ""
        """.trimIndent()

        assertThrows<Exception> { yaml.parse(invalid) }
    }

    @Test
    fun `parse empty string returns null via parseOrNull`() {
        assertNull(yaml.parseOrNull(""))
    }

    @Test
    fun `round-trip preserves content null on articles`() {
        val original = Article(path = "s/a")

        val serialized = yaml.serialize(original)
        val parsed = yaml.parse(serialized)

        assertTrue(parsed is Article)
        assertEquals("s/a", parsed.path)
        assertNull((parsed as Article).content)
    }

    @Test
    fun `round-trip article with page assets`() {
        val assets = PageAssets(
            css = listOf(AssetRef(path = "theme.css", integrity = "sha256-abc")),
            js = listOf(AssetRef(path = "app.js", defer = true))
        )
        val original = Article(
            path = "s/a",
            outputConfig = OutputConfig(assets = assets)
        )

        val serialized = yaml.serialize(original)
        val parsed = yaml.parse(serialized)

        assertTrue(parsed is Article)
        val config = (parsed as Article).outputConfig
        assertNotNull(config)
        val parsedAssets = config!!.assets
        assertNotNull(parsedAssets)
        assertEquals(1, parsedAssets.css?.size)
        assertEquals("theme.css", parsedAssets.css!![0].path)
        assertEquals("sha256-abc", parsedAssets.css!![0].integrity)
        assertEquals(1, parsedAssets.js?.size)
        assertEquals("app.js", parsedAssets.js!![0].path)
        assertEquals(true, parsedAssets.js!![0].defer)
    }

    @Test
    fun `round-trip site with assets on multiple levels`() {
        val siteAssets = PageAssets(
            css = listOf(AssetRef(path = "global.css")),
            js = listOf(AssetRef(path = "analytics.js", async = true))
        )
        val sectionCss = listOf(AssetRef(path = "section.css", integrity = "sha384-sec"))
        val original = Site(
            path = "",
            sections = listOf(
                Section(
                    path = "blog",
                    articles = listOf(Article(path = "blog/post-1")),
                    outputConfig = OutputConfig(assets = PageAssets(css = sectionCss))
                )
            ),
            outputConfig = OutputConfig(assets = siteAssets)
        )

        val serialized = yaml.serialize(original)
        val parsed = yaml.parse(serialized) as Site

        assertEquals(siteAssets, parsed.outputConfig?.assets)
        assertEquals(sectionCss, parsed.sections[0].outputConfig?.assets?.css)
    }
}