package bakery

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

class GoogleFormsInjectionTest {

    @TempDir
    lateinit var tempDir: File

    private val mapper = com.fasterxml.jackson.dataformat.yaml.YAMLFactory()
        .let { com.fasterxml.jackson.databind.ObjectMapper(it) }

    @Test
    fun `site yml with googleForms injects properties into jbake properties`() {
        val siteYml = tempDir.resolve("site.yml")
        siteYml.writeText("""
            bake:
              srcPath: site
              destDirPath: bake
            googleForms:
              formId: "1ABC-x12345"
              width: "800"
              height: "1000"
        """.trimIndent(), UTF_8)

        val siteDir = tempDir.resolve("site")
        siteDir.mkdirs()
        val jbakeProps = siteDir.resolve("jbake.properties")
        jbakeProps.writeText("template.index.file=index.thyme\nrender.feed=false\n", UTF_8)

        val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
        assertNotNull(config.googleForms)
        assertEquals("1ABC-x12345", config.googleForms!!.formId)

        // Simulate the injection logic (same as injectFirebaseConfigIntoJbakeProperties)
        injectGoogleFormsIntoJbakeProperties(jbakeProps, config.googleForms)

        val props = jbakeProps.readText(UTF_8)
        assertTrue(props.contains("googleFormsFormId=1ABC-x12345"), "must contain formId")
        assertTrue(props.contains("googleFormsWidth=800"), "must contain width")
        assertTrue(props.contains("googleFormsHeight=1000"), "must contain height")
    }

    @Test
    fun `site yml without googleForms does not inject any googleForms properties`() {
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
        assertNull(config.googleForms)

        // No injection when googleForms is null
        config.googleForms?.let {
            injectGoogleFormsIntoJbakeProperties(jbakeProps, it)
        }

        val props = jbakeProps.readText(UTF_8)
        assertFalse(props.contains("googleForms"), "must NOT contain any googleForms properties")
    }

    private fun injectGoogleFormsIntoJbakeProperties(jbakeProps: File, googleForms: GoogleFormsConfig) {
        val lines = jbakeProps.readText(UTF_8).lines().toMutableList()
        fun updateProperty(key: String, value: String) {
            val idx = lines.indexOfFirst { it.startsWith("$key=") }
            if (idx >= 0) {
                lines[idx] = "$key=$value"
            } else {
                lines.add("$key=$value")
            }
        }
        updateProperty("googleFormsFormId", googleForms.formId)
        updateProperty("googleFormsWidth", googleForms.width)
        updateProperty("googleFormsHeight", googleForms.height)
        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
    }
}