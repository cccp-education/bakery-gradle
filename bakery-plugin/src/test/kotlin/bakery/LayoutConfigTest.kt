package bakery

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LayoutConfigTest {

    private val mapper = ObjectMapper(YAMLFactory())

    @Test
    fun `parse site yml with layout FULL_WIDTH returns config with FULL_WIDTH`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            layout:
              layoutType: "FULL_WIDTH"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.layout)
        assertEquals(LayoutType.FULL_WIDTH, config.layout!!.layoutType)
    }

    @Test
    fun `parse site yml with layout SIDEBAR_LEFT returns config with SIDEBAR_LEFT`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            layout:
              layoutType: "SIDEBAR_LEFT"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.layout)
        assertEquals(LayoutType.SIDEBAR_LEFT, config.layout!!.layoutType)
    }

    @Test
    fun `parse site yml with layout SIDEBAR_RIGHT returns config with SIDEBAR_RIGHT`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            layout:
              layoutType: "SIDEBAR_RIGHT"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.layout)
        assertEquals(LayoutType.SIDEBAR_RIGHT, config.layout!!.layoutType)
    }

    @Test
    fun `parse site yml with layout CENTERED returns config with CENTERED`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            layout:
              layoutType: "CENTERED"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.layout)
        assertEquals(LayoutType.CENTERED, config.layout!!.layoutType)
    }

    @Test
    fun `parse site yml without layout returns null`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNull(config.layout)
    }

    @Test
    fun `parse site yml with layout and theme together returns both`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            layout:
              layoutType: "CENTERED"
            theme:
              mode: "dark"
              primaryColor: "#1a1a2e"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.layout)
        assertNotNull(config.theme)
        assertEquals(LayoutType.CENTERED, config.layout!!.layoutType)
        assertEquals("dark", config.theme!!.mode)
    }
}