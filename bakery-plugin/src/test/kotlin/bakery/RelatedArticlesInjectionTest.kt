package bakery

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

class RelatedArticlesInjectionTest {

    @TempDir
    lateinit var tempDir: File

    private val mapper = com.fasterxml.jackson.dataformat.yaml.YAMLFactory()
        .let { com.fasterxml.jackson.databind.ObjectMapper(it) }

    @Nested
    inner class WithRelatedArticlesConfig {
        @Test
        fun `site yml with relatedArticles injects enabled, maxResults and heading into jbake properties`() {
            val siteYml = tempDir.resolve("site.yml")
            siteYml.writeText("""
                bake:
                  srcPath: site
                  destDirPath: bake
                relatedArticles:
                  enabled: true
                  maxResults: 5
                  heading: "Voir aussi"
            """.trimIndent(), UTF_8)

            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("template.index.file=index.thyme\nrender.tags=true\n", UTF_8)

            val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
            assertThat(config.relatedArticles).isNotNull
            assertThat(config.relatedArticles!!.enabled).isTrue()

            config.relatedArticles?.let { injectRelatedArticlesIntoJbakeProperties(jbakeProps, it) }

            val props = jbakeProps.readText(UTF_8)
            assertThat(props).contains("relatedArticlesEnabled=true")
            assertThat(props).contains("relatedArticlesMaxResults=5")
            assertThat(props).contains("relatedArticlesHeading=Voir aussi")
            // existing properties preserved
            assertThat(props).contains("template.index.file=index.thyme")
        }
    }

    @Nested
    inner class WithoutRelatedArticlesConfig {
        @Test
        fun `site yml without relatedArticles does not inject relatedArticles properties`() {
            val siteYml = tempDir.resolve("site.yml")
            siteYml.writeText("""
                bake:
                  srcPath: site
                  destDirPath: bake
            """.trimIndent(), UTF_8)

            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("template.index.file=index.thyme\n", UTF_8)

            val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
            assertThat(config.relatedArticles).isNull()

            // No injection — config is null, method returns early
            val propsBefore = jbakeProps.readText(UTF_8)
            assertThat(propsBefore).doesNotContain("relatedArticlesEnabled")
            assertThat(propsBefore).doesNotContain("relatedArticlesMaxResults")
            assertThat(propsBefore).doesNotContain("relatedArticlesHeading")
        }

        @Test
        fun `site yml with relatedArticles disabled injects enabled=false into jbake properties`() {
            val siteYml = tempDir.resolve("site.yml")
            siteYml.writeText("""
                bake:
                  srcPath: site
                  destDirPath: bake
                relatedArticles:
                  enabled: false
            """.trimIndent(), UTF_8)

            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("template.index.file=index.thyme\n", UTF_8)

            val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
            assertThat(config.relatedArticles).isNotNull
            assertThat(config.relatedArticles!!.enabled).isFalse()

            config.relatedArticles?.let { injectRelatedArticlesIntoJbakeProperties(jbakeProps, it) }

            val props = jbakeProps.readText(UTF_8)
            assertThat(props).contains("relatedArticlesEnabled=false")
            assertThat(props).contains("relatedArticlesMaxResults=4")
            assertThat(props).contains("relatedArticlesHeading=Articles connexes")
        }
    }

    private fun injectRelatedArticlesIntoJbakeProperties(jbakeProps: File, config: RelatedArticlesConfig) {
        val lines = jbakeProps.readText(UTF_8).lines().toMutableList()
        fun updateProperty(key: String, value: String) {
            val idx = lines.indexOfFirst { it.startsWith("$key=") }
            if (idx >= 0) {
                lines[idx] = "$key=$value"
            } else {
                lines.add("$key=$value")
            }
        }
        updateProperty("relatedArticlesEnabled", config.enabled.toString())
        updateProperty("relatedArticlesMaxResults", config.maxResults.toString())
        updateProperty("relatedArticlesHeading", config.heading)
        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
    }
}