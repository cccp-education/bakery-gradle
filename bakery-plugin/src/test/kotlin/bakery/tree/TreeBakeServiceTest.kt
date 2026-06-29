package bakery.tree

import bakery.LayoutType
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TreeBakeServiceTest {

    private val jsonMapper = ObjectMapper()

    @TempDir
    lateinit var tempDir: File

    private fun srcDir(): File = tempDir.resolve("site").also { it.mkdirs() }

    private fun writeJbakeProps(dir: File, content: String = "site.host=example.com\n") {
        dir.resolve("jbake.properties").writeText(content)
    }

    private fun readJbakeProps(dir: File): String =
        dir.resolve("jbake.properties").readText()

    @Test
    fun `injectTreeConfig adds bakeTreeConfig to jbake properties`() {
        val dir = srcDir()
        writeJbakeProps(dir)
        val dto = SiteNodeDto.SiteDto(
            path = "",
            sections = listOf(
                SiteNodeDto.SectionDto(
                    path = "docs",
                    articles = listOf(
                        SiteNodeDto.ArticleDto(path = "docs/guide")
                    )
                )
            )
        )

        TreeBakeService.injectTreeConfig(dto, dir)

        val props = readJbakeProps(dir)
        assertTrue(props.contains("bakeTreeConfig="))
        assertTrue(props.contains("site.host=example.com"))
    }

    @Test
    fun `injectTreeConfig does nothing when no jbake properties exists`() {
        val dir = srcDir()
        val dto = SiteNodeDto.SiteDto(
            path = "",
            sections = listOf(
                SiteNodeDto.SectionDto(
                    path = "docs",
                    articles = listOf(
                        SiteNodeDto.ArticleDto(path = "docs/guide")
                    )
                )
            )
        )

        TreeBakeService.injectTreeConfig(dto, dir)

        assertTrue(dir.resolve("jbake.properties").exists().not())
    }

    @Test
    fun `injected config contains resolved output config for each article`() {
        val dir = srcDir()
        writeJbakeProps(dir)
        val dto = SiteNodeDto.SiteDto(
            path = "",
            outputConfig = OutputConfig(
                template = "default-page",
                jsFiles = listOf("analytics.js")
            ),
            sections = listOf(
                SiteNodeDto.SectionDto(
                    path = "docs",
                    outputConfig = OutputConfig(
                        layout = LayoutType.SIDEBAR_LEFT,
                        cssFiles = listOf("docs.css")
                    ),
                    articles = listOf(
                        SiteNodeDto.ArticleDto(
                            path = "docs/guide",
                            outputConfig = OutputConfig(template = "guide.ftl")
                        ),
                        SiteNodeDto.ArticleDto(path = "docs/api")
                    )
                )
            )
        )

        TreeBakeService.injectTreeConfig(dto, dir)

        val props = readJbakeProps(dir)
        val prefix = "bakeTreeConfig="
        val jsonStart = props.indexOf(prefix) + prefix.length
        val json = props.substring(jsonStart)

        val parsed = jsonMapper.readValue<Map<String, Any?>>(json)
        assertEquals(true, parsed["present"])
        @Suppress("UNCHECKED_CAST")
        val nodes = parsed["nodes"] as Map<String, Map<String, Any?>>

        val guide = nodes["docs/guide"]
        assertNotNull(guide)
        assertEquals("guide.ftl", guide["template"])
        assertEquals("SIDEBAR_LEFT", guide["layout"])
        assertEquals(listOf("analytics.js"), guide["jsFiles"])
        assertEquals(listOf("docs.css"), guide["cssFiles"])

        val api = nodes["docs/api"]
        assertNotNull(api)
        assertEquals("default-page", api["template"])
        assertEquals("SIDEBAR_LEFT", api["layout"])
    }

    @Test
    fun `injected config includes asset references`() {
        val dir = srcDir()
        writeJbakeProps(dir)
        val assets = PageAssets(
            css = listOf(AssetRef(path = "theme.css", integrity = "sha256-abc")),
            js = listOf(AssetRef(path = "app.js", defer = true))
        )
        val dto = SiteNodeDto.SiteDto(
            path = "",
            sections = listOf(
                SiteNodeDto.SectionDto(
                    path = "blog",
                    articles = listOf(
                        SiteNodeDto.ArticleDto(
                            path = "blog/post-1",
                            outputConfig = OutputConfig(assets = assets)
                        )
                    )
                )
            )
        )

        TreeBakeService.injectTreeConfig(dto, dir)

        val props = readJbakeProps(dir)
        val prefix = "bakeTreeConfig="
        val jsonStart = props.indexOf(prefix) + prefix.length
        val json = props.substring(jsonStart)

        val parsed = jsonMapper.readValue<Map<String, Any?>>(json)
        @Suppress("UNCHECKED_CAST")
        val nodes = parsed["nodes"] as Map<String, Map<String, Any?>>
        @Suppress("UNCHECKED_CAST")
        val postAssets = nodes["blog/post-1"]?.get("assets") as Map<String, List<Map<String, Any?>>>

        assertEquals(1, postAssets["css"]?.size)
        assertEquals("theme.css", postAssets["css"]!![0]["path"])
        assertEquals("sha256-abc", postAssets["css"]!![0]["integrity"])

        assertEquals(1, postAssets["js"]?.size)
        assertEquals("app.js", postAssets["js"]!![0]["path"])
        assertEquals(true, postAssets["js"]!![0]["defer"])
    }

    @Test
    fun `injected config includes metadata`() {
        val dir = srcDir()
        writeJbakeProps(dir)
        val dto = SiteNodeDto.SiteDto(
            path = "",
            metadata = NodeMetadata(title = "My Site"),
            sections = listOf(
                SiteNodeDto.SectionDto(
                    path = "docs",
                    metadata = NodeMetadata(
                        description = "Documentation section",
                        tags = listOf("docs", "guide")
                    ),
                    articles = listOf(
                        SiteNodeDto.ArticleDto(
                            path = "docs/guide",
                            metadata = NodeMetadata(title = "User Guide")
                        )
                    )
                )
            )
        )

        TreeBakeService.injectTreeConfig(dto, dir)

        val props = readJbakeProps(dir)
        val prefix = "bakeTreeConfig="
        val jsonStart = props.indexOf(prefix) + prefix.length
        val json = props.substring(jsonStart)

        val parsed = jsonMapper.readValue<Map<String, Any?>>(json)
        @Suppress("UNCHECKED_CAST")
        val nodes = parsed["nodes"] as Map<String, Map<String, Any?>>

        val guide = nodes["docs/guide"]
        assertNotNull(guide)
        assertEquals("User Guide", guide["title"])
        assertEquals("Documentation section", guide["description"])
        assertEquals(listOf("docs", "guide"), guide["tags"])
    }
}
