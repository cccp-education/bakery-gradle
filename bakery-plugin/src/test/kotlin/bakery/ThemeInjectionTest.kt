package bakery

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

class ThemeInjectionTest {

    @TempDir
    lateinit var tempDir: File

    private val mapper = com.fasterxml.jackson.dataformat.yaml.YAMLFactory()
        .let { com.fasterxml.jackson.databind.ObjectMapper(it) }

    @Test
    fun `site yml with theme injects all theme properties into jbake properties`() {
        val siteYml = tempDir.resolve("site.yml")
        siteYml.writeText("""
            bake:
              srcPath: site
              destDirPath: bake
            theme:
              mode: "dark"
              primaryColor: "#e74c3c"
              secondaryColor: "#2ecc71"
              fontFamily: "Inter"
              logoUrl: "/img/logo.png"
              faviconUrl: "/img/favicon.ico"
        """.trimIndent(), UTF_8)

        val siteDir = tempDir.resolve("site")
        siteDir.mkdirs()
        val jbakeProps = siteDir.resolve("jbake.properties")
        jbakeProps.writeText("template.index.file=index.thyme\nrender.feed=false\n", UTF_8)

        val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
        assertNotNull(config.theme)

        config.theme?.let { injectThemeIntoJbakeProperties(jbakeProps, it) }

        val props = jbakeProps.readText(UTF_8)
        assertTrue(props.contains("themeMode=dark"), "must contain themeMode")
        assertTrue(props.contains("themePrimaryColor=#e74c3c"), "must contain themePrimaryColor")
        assertTrue(props.contains("themeSecondaryColor=#2ecc71"), "must contain themeSecondaryColor")
        assertTrue(props.contains("themeFontFamily=Inter"), "must contain themeFontFamily")
        assertTrue(props.contains("themeLogoUrl=/img/logo.png"), "must contain themeLogoUrl")
        assertTrue(props.contains("themeFaviconUrl=/img/favicon.ico"), "must contain themeFaviconUrl")
    }

    @Test
    fun `site yml without theme does not inject any theme properties`() {
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
        assertNull(config.theme)

        config.theme?.let { injectThemeIntoJbakeProperties(jbakeProps, it) }

        val props = jbakeProps.readText(UTF_8)
        assertFalse(props.contains("themeMode"), "must NOT contain themeMode")
        assertFalse(props.contains("themePrimaryColor"), "must NOT contain themePrimaryColor")
        assertFalse(props.contains("themeFontFamily"), "must NOT contain themeFontFamily")
        assertFalse(props.contains("themeLogoUrl"), "must NOT contain themeLogoUrl")
    }

    @Test
    fun `site yml with theme minimal config injects mode and colors`() {
        val siteYml = tempDir.resolve("site.yml")
        siteYml.writeText("""
            bake:
              srcPath: site
              destDirPath: bake
            theme:
              mode: "light"
              primaryColor: "#ff6600"
        """.trimIndent(), UTF_8)

        val siteDir = tempDir.resolve("site")
        siteDir.mkdirs()
        val jbakeProps = siteDir.resolve("jbake.properties")
        jbakeProps.writeText("template.index.file=index.thyme\n", UTF_8)

        val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
        assertNotNull(config.theme)

        config.theme?.let { injectThemeIntoJbakeProperties(jbakeProps, it) }

        val props = jbakeProps.readText(UTF_8)
        assertTrue(props.contains("themeMode=light"), "must contain themeMode=light")
        assertTrue(props.contains("themePrimaryColor=#ff6600"), "must contain themePrimaryColor")
        assertTrue(props.contains("themeSecondaryColor=#6c757d"), "must contain default themeSecondaryColor")
        assertTrue(props.contains("themeFontFamily="), "must contain empty themeFontFamily")
        assertTrue(props.contains("themeLogoUrl="), "must contain empty themeLogoUrl")
    }

    private fun injectThemeIntoJbakeProperties(jbakeProps: File, theme: ThemeConfig) {
        val lines = jbakeProps.readText(UTF_8).lines().toMutableList()
        fun updateProperty(key: String, value: String) {
            val idx = lines.indexOfFirst { it.startsWith("$key=") }
            if (idx >= 0) {
                lines[idx] = "$key=$value"
            } else {
                lines.add("$key=$value")
            }
        }
        updateProperty("themeMode", theme.mode)
        updateProperty("themePrimaryColor", theme.primaryColor)
        updateProperty("themeSecondaryColor", theme.secondaryColor)
        updateProperty("themeFontFamily", theme.fontFamily)
        updateProperty("themeLogoUrl", theme.logoUrl)
        updateProperty("themeFaviconUrl", theme.faviconUrl)
        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
    }
}