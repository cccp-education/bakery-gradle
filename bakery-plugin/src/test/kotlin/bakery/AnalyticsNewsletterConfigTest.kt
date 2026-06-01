package bakery

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AnalyticsNewsletterConfigTest {

    private val mapper = ObjectMapper(YAMLFactory())

    @Test
    fun `parse site yml with analytics returns config with values`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            analytics:
              provider: "plausible"
              domain: "my-site.com"
              scriptSrc: "https://plausible.io/js/script.js"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.analytics)
        assertEquals("plausible", config.analytics!!.provider)
        assertEquals("my-site.com", config.analytics!!.domain)
        assertEquals("https://plausible.io/js/script.js", config.analytics!!.scriptSrc)
    }

    @Test
    fun `parse site yml without analytics returns null`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNull(config.analytics)
    }

    @Test
    fun `parse site yml with newsletter returns config with values`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            newsletter:
              enabled: true
              provider: "mailchimp"
              endpoint: "https://mailchimp.us1.list-manage.com/subscribe/post?u=xxx&id=yyy"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.newsletter)
        assertTrue(config.newsletter!!.enabled)
        assertEquals("mailchimp", config.newsletter!!.provider)
        assertEquals("https://mailchimp.us1.list-manage.com/subscribe/post?u=xxx&id=yyy", config.newsletter!!.endpoint)
    }

    @Test
    fun `parse site yml without newsletter returns null`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNull(config.newsletter)
    }

    @Test
    fun `parse site yml with analytics and newsletter together returns both`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            analytics:
              provider: "matomo"
              domain: "stats.example.com"
              scriptSrc: "https://stats.example.com/matomo.js"
            newsletter:
              enabled: true
              provider: "mailchimp"
              endpoint: "https://mailchimp.example.com/subscribe"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.analytics)
        assertNotNull(config.newsletter)
        assertEquals("matomo", config.analytics!!.provider)
        assertTrue(config.newsletter!!.enabled)
    }

    @Test
    fun `parse site yml with analytics defaults returns empty strings`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            analytics:
              provider: "plausible"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.analytics)
        assertEquals("plausible", config.analytics!!.provider)
        assertEquals("", config.analytics!!.domain)
        assertEquals("", config.analytics!!.scriptSrc)
    }

    @Test
    fun `parse site yml with newsletter disabled by default`() {
        val yaml = """
            bake:
              srcPath: src
              destDirPath: build
            newsletter:
              provider: "mailerlite"
              endpoint: "https://mailerlite.com/subscribe"
        """.trimIndent()
        val config = mapper.readValue(yaml, SiteConfiguration::class.java)
        assertNotNull(config.newsletter)
        assertFalse(config.newsletter!!.enabled)
        assertEquals("mailerlite", config.newsletter!!.provider)
    }
}