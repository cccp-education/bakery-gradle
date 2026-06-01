package bakery

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ThemeConfigTest {

    private val mapper = ObjectMapper(YAMLFactory())

    @Test
    fun `parse site yml with theme returns config with values`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            theme:
              mode: "dark"
              primaryColor: "#e74c3c"
              secondaryColor: "#2ecc71"
              fontFamily: "Inter"
              logoUrl: "/img/logo.png"
              faviconUrl: "/img/favicon.ico"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.theme)
        assertEquals("dark", config.theme!!.mode)
        assertEquals("#e74c3c", config.theme!!.primaryColor)
        assertEquals("#2ecc71", config.theme!!.secondaryColor)
        assertEquals("Inter", config.theme!!.fontFamily)
        assertEquals("/img/logo.png", config.theme!!.logoUrl)
        assertEquals("/img/favicon.ico", config.theme!!.faviconUrl)
    }

    @Test
    fun `parse site yml without theme returns null`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNull(config.theme)
    }

    @Test
    fun `parse site yml with theme defaults returns auto mode and bootstrap colors`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            theme:
              mode: "auto"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.theme)
        assertEquals("auto", config.theme!!.mode)
        assertEquals("#0d6efd", config.theme!!.primaryColor)
        assertEquals("#6c757d", config.theme!!.secondaryColor)
        assertEquals("", config.theme!!.fontFamily)
        assertEquals("", config.theme!!.logoUrl)
        assertEquals("", config.theme!!.faviconUrl)
    }

    @Test
    fun `parse site yml with theme light mode`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            theme:
              mode: "light"
              primaryColor: "#ff6600"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.theme)
        assertEquals("light", config.theme!!.mode)
        assertEquals("#ff6600", config.theme!!.primaryColor)
    }

    @Test
    fun `parse site yml with theme dark mode and custom font`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            theme:
              mode: "dark"
              fontFamily: "Fira Code"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.theme)
        assertEquals("dark", config.theme!!.mode)
        assertEquals("Fira Code", config.theme!!.fontFamily)
    }

    @Test
    fun `parse site yml with theme logo and favicon only`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            theme:
              logoUrl: "/assets/brand.svg"
              faviconUrl: "/assets/icon.png"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.theme)
        assertEquals("auto", config.theme!!.mode)
        assertEquals("/assets/brand.svg", config.theme!!.logoUrl)
        assertEquals("/assets/icon.png", config.theme!!.faviconUrl)
    }

    @Test
    fun `parse site yml with theme and analytics together returns both`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            analytics:
              provider: "plausible"
              domain: "my-site.com"
              scriptSrc: "https://plausible.io/js/script.js"
            theme:
              mode: "dark"
              primaryColor: "#1a1a2e"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.analytics)
        assertNotNull(config.theme)
        assertEquals("dark", config.theme!!.mode)
        assertEquals("#1a1a2e", config.theme!!.primaryColor)
    }
}