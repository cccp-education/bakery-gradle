package bakery

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

class AnalyticsNewsletterInjectionTest {

    @TempDir
    lateinit var tempDir: File

    private val mapper = com.fasterxml.jackson.dataformat.yaml.YAMLFactory()
        .let { com.fasterxml.jackson.databind.ObjectMapper(it) }

    @Test
    fun `site yml with analytics and newsletter injects properties into jbake properties`() {
        val siteYml = tempDir.resolve("site.yml")
        siteYml.writeText("""
            bake:
              srcPath: site
              destDirPath: bake
            analytics:
              provider: "plausible"
              domain: "my-site.com"
              scriptSrc: "https://plausible.io/js/script.js"
            newsletter:
              enabled: true
              provider: "mailchimp"
              endpoint: "https://mailchimp.us1.list-manage.com/subscribe/post?u=xxx&id=yyy"
        """.trimIndent(), UTF_8)

        val siteDir = tempDir.resolve("site")
        siteDir.mkdirs()
        val jbakeProps = siteDir.resolve("jbake.properties")
        jbakeProps.writeText("template.index.file=index.thyme\nrender.feed=false\n", UTF_8)

        val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
        assertNotNull(config.analytics)
        assertNotNull(config.newsletter)

        config.analytics?.let { injectAnalyticsIntoJbakeProperties(jbakeProps, it) }
        config.newsletter?.let { injectNewsletterIntoJbakeProperties(jbakeProps, it) }

        val props = jbakeProps.readText(UTF_8)
        assertTrue(props.contains("analyticsProvider=plausible"), "must contain analyticsProvider")
        assertTrue(props.contains("analyticsDomain=my-site.com"), "must contain analyticsDomain")
        assertTrue(props.contains("analyticsScriptSrc=https://plausible.io/js/script.js"), "must contain analyticsScriptSrc")
        assertTrue(props.contains("newsletterEnabled=true"), "must contain newsletterEnabled")
        assertTrue(props.contains("newsletterProvider=mailchimp"), "must contain newsletterProvider")
        assertTrue(props.contains("newsletterEndpoint=https://mailchimp.us1.list-manage.com/subscribe/post?u=xxx&id=yyy"), "must contain newsletterEndpoint")
    }

    @Test
    fun `site yml without analytics and newsletter does not inject any analytics or newsletter properties`() {
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
        assertNull(config.analytics)
        assertNull(config.newsletter)

        config.analytics?.let { injectAnalyticsIntoJbakeProperties(jbakeProps, it) }
        config.newsletter?.let { injectNewsletterIntoJbakeProperties(jbakeProps, it) }

        val props = jbakeProps.readText(UTF_8)
        assertFalse(props.contains("analyticsProvider"), "must NOT contain analyticsProvider")
        assertFalse(props.contains("analyticsDomain"), "must NOT contain analyticsDomain")
        assertFalse(props.contains("newsletterEnabled"), "must NOT contain newsletterEnabled")
        assertFalse(props.contains("newsletterProvider"), "must NOT contain newsletterProvider")
    }

    @Test
    fun `site yml with analytics but newsletter disabled only injects analytics properties`() {
        val siteYml = tempDir.resolve("site.yml")
        siteYml.writeText("""
            bake:
              srcPath: site
              destDirPath: bake
            analytics:
              provider: "matomo"
              domain: "stats.example.com"
              scriptSrc: "https://stats.example.com/matomo.js"
            newsletter:
              enabled: false
              provider: "mailchimp"
        """.trimIndent(), UTF_8)

        val siteDir = tempDir.resolve("site")
        siteDir.mkdirs()
        val jbakeProps = siteDir.resolve("jbake.properties")
        jbakeProps.writeText("template.index.file=index.thyme\n", UTF_8)

        val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
        assertNotNull(config.analytics)
        assertNotNull(config.newsletter)

        config.analytics?.let { injectAnalyticsIntoJbakeProperties(jbakeProps, it) }
        config.newsletter?.let { injectNewsletterIntoJbakeProperties(jbakeProps, it) }

        val props = jbakeProps.readText(UTF_8)
        assertTrue(props.contains("analyticsProvider=matomo"), "must contain analyticsProvider")
        assertTrue(props.contains("analyticsDomain=stats.example.com"), "must contain analyticsDomain")
        assertTrue(props.contains("newsletterEnabled=false"), "must contain newsletterEnabled=false")
    }

    private fun injectAnalyticsIntoJbakeProperties(jbakeProps: File, analytics: AnalyticsConfig) {
        val lines = jbakeProps.readText(UTF_8).lines().toMutableList()
        fun updateProperty(key: String, value: String) {
            val idx = lines.indexOfFirst { it.startsWith("$key=") }
            if (idx >= 0) {
                lines[idx] = "$key=$value"
            } else {
                lines.add("$key=$value")
            }
        }
        updateProperty("analyticsProvider", analytics.provider)
        updateProperty("analyticsDomain", analytics.domain)
        updateProperty("analyticsScriptSrc", analytics.scriptSrc)
        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
    }

    private fun injectNewsletterIntoJbakeProperties(jbakeProps: File, newsletter: NewsletterConfig) {
        val lines = jbakeProps.readText(UTF_8).lines().toMutableList()
        fun updateProperty(key: String, value: String) {
            val idx = lines.indexOfFirst { it.startsWith("$key=") }
            if (idx >= 0) {
                lines[idx] = "$key=$value"
            } else {
                lines.add("$key=$value")
            }
        }
        updateProperty("newsletterEnabled", newsletter.enabled.toString())
        updateProperty("newsletterProvider", newsletter.provider)
        updateProperty("newsletterEndpoint", newsletter.endpoint)
        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
    }
}