package bakery

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GoogleFormsConfigTest {

    private val mapper = ObjectMapper(YAMLFactory())

    @Test
    fun `parse site yml with googleForms returns config with formId and defaults`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            googleForms:
              formId: "1ABCDEF-x1234567890"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.googleForms)
        assertEquals("1ABCDEF-x1234567890", config.googleForms!!.formId)
        assertEquals("640", config.googleForms!!.width)
        assertEquals("800", config.googleForms!!.height)
        assertFalse(config.googleForms!!.allowMultiple)
    }

    @Test
    fun `parse site yml without googleForms returns null`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNull(config.googleForms)
    }
}
