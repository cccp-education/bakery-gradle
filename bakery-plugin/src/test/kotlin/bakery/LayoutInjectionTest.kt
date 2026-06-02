package bakery

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

class LayoutInjectionTest {

    @TempDir
    lateinit var tempDir: File

    private val mapper = com.fasterxml.jackson.dataformat.yaml.YAMLFactory()
        .let { com.fasterxml.jackson.databind.ObjectMapper(it) }

    @Test
    fun `site yml with layout injects layoutType into jbake properties`() {
        val siteYml = tempDir.resolve("site.yml")
        siteYml.writeText("""
            bake:
              srcPath: site
              destDirPath: bake
            layout:
              layoutType: "SIDEBAR_LEFT"
        """.trimIndent(), UTF_8)

        val siteDir = tempDir.resolve("site")
        siteDir.mkdirs()
        val jbakeProps = siteDir.resolve("jbake.properties")
        jbakeProps.writeText("template.index.file=index.thyme\nrender.feed=false\n", UTF_8)

        val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
        assertNotNull(config.layout)

        config.layout?.let { injectLayoutIntoJbakeProperties(jbakeProps, it) }

        val props = jbakeProps.readText(UTF_8)
        assertTrue(props.contains("layoutType=SIDEBAR_LEFT"), "must contain layoutType=SIDEBAR_LEFT")
    }

    @Test
    fun `site yml without layout does not inject layout properties but defaults to FULL_WIDTH`() {
        val siteYml = tempDir.resolve("site.yml")
        siteYml.writeText("""
            bake:
              srcPath: site
              destDirPath: bake
        """.trimIndent(), UTF_8)

        val siteDir = tempDir.resolve("site")
        siteDir.mkdirs()
        val jbakeProps = siteDir.resolve("jbake.properties")
        jbakeProps.writeText("template.index.file=index.thyme\nrender.feed=false\n", UTF_8)

        val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
        assertNull(config.layout)

        val defaultLayout = config.layout ?: LayoutConfig()
        injectLayoutIntoJbakeProperties(jbakeProps, defaultLayout)

        val props = jbakeProps.readText(UTF_8)
        assertTrue(props.contains("layoutType=FULL_WIDTH"), "must contain default layoutType=FULL_WIDTH")
    }

    @Test
    fun `site yml with layout CENTERED injects layoutType into jbake properties`() {
        val siteYml = tempDir.resolve("site.yml")
        siteYml.writeText("""
            bake:
              srcPath: site
              destDirPath: bake
            layout:
              layoutType: "CENTERED"
        """.trimIndent(), UTF_8)

        val siteDir = tempDir.resolve("site")
        siteDir.mkdirs()
        val jbakeProps = siteDir.resolve("jbake.properties")
        jbakeProps.writeText("template.index.file=index.thyme\n", UTF_8)

        val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
        assertNotNull(config.layout)

        config.layout?.let { injectLayoutIntoJbakeProperties(jbakeProps, it) }

        val props = jbakeProps.readText(UTF_8)
        assertTrue(props.contains("layoutType=CENTERED"), "must contain layoutType=CENTERED")
    }

    private fun injectLayoutIntoJbakeProperties(jbakeProps: File, layout: LayoutConfig) {
        val lines = jbakeProps.readText(UTF_8).lines().toMutableList()
        fun updateProperty(key: String, value: String) {
            val idx = lines.indexOfFirst { it.startsWith("$key=") }
            if (idx >= 0) {
                lines[idx] = "$key=$value"
            } else {
                lines.add("$key=$value")
            }
        }
        updateProperty("layoutType", layout.layoutType.name)
        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
    }
}