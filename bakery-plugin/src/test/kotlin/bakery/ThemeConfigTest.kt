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
              srcPath: site
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

    // BKY-IA-2 — Theme variant + extended properties

    @Test
    fun `parse site yml with theme variant returns variant name`() {
        val yaml = """
            bake:
              srcPath: site
              destDirPath: build
            theme:
              variant: "magazine"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.theme)
        assertEquals("magazine", config.theme!!.variant)
    }

    @Test
    fun `parse site yml with theme extended properties`() {
        val yaml = """
            bake:
              srcPath: site
              destDirPath: build
            theme:
              variant: "documentation"
              accentColor: "#8e44ad"
              backgroundColor: "#fafafa"
              textColor: "#333333"
              headingFont: "Inter"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.theme)
        assertEquals("documentation", config.theme!!.variant)
        assertEquals("#8e44ad", config.theme!!.accentColor)
        assertEquals("#fafafa", config.theme!!.backgroundColor)
        assertEquals("#333333", config.theme!!.textColor)
        assertEquals("Inter", config.theme!!.headingFont)
    }

    @Test
    fun `parse site yml with theme variant and override primary color`() {
        val yaml = """
            bake:
              srcPath: site
              destDirPath: build
            theme:
              variant: "magazine"
              primaryColor: "#ff6600"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.theme)
        assertEquals("magazine", config.theme!!.variant)
        assertEquals("#ff6600", config.theme!!.primaryColor)
    }

    @Test
    fun `parse site yml without variant returns empty variant`() {
        val yaml = """
            bake:
              srcPath: site
              destDirPath: build
            theme:
              mode: "dark"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.theme)
        assertEquals("", config.theme!!.variant)
    }
}