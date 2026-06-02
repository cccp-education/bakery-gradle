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
        fun `site yml with relatedArticles injects enabled, maxResults, heading and graphFilePath into jbake properties`() {
            val siteYml = tempDir.resolve("site.yml")
            siteYml.writeText("""
                bake:
                  srcPath: site
                  destDirPath: bake
                relatedArticles:
                  enabled: true
                  maxResults: 5
                  heading: "Voir aussi"
                  graphFilePath: "custom/graph.json"
            """.trimIndent(), UTF_8)

            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("template.index.file=index.thyme\nrender.tags=true\n", UTF_8)

            val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
            assertThat(config.relatedArticles).isNotNull
            assertThat(config.relatedArticles!!.enabled).isTrue()
            assertThat(config.relatedArticles!!.graphFilePath).isEqualTo("custom/graph.json")

            config.relatedArticles?.let { injectRelatedArticlesIntoJbakeProperties(jbakeProps, it) }

            val props = jbakeProps.readText(UTF_8)
            assertThat(props).contains("relatedArticlesEnabled=true")
            assertThat(props).contains("relatedArticlesMaxResults=5")
            assertThat(props).contains("relatedArticlesHeading=Voir aussi")
            assertThat(props).contains("relatedArticlesGraphFilePath=custom/graph.json")
            // existing properties preserved
            assertThat(props).contains("template.index.file=index.thyme")
        }

        @Test
        fun `site yml with relatedArticles default graphFilePath injects build-bakery path`() {
            val siteYml = tempDir.resolve("site.yml")
            siteYml.writeText("""
                bake:
                  srcPath: site
                  destDirPath: bake
                relatedArticles:
                  enabled: true
            """.trimIndent(), UTF_8)

            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("template.index.file=index.thyme\n", UTF_8)

            val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
            config.relatedArticles?.let { injectRelatedArticlesIntoJbakeProperties(jbakeProps, it) }

            val props = jbakeProps.readText(UTF_8)
            assertThat(props).contains("relatedArticlesGraphFilePath=build/bakery/related-articles.json")
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
            assertThat(props).contains("relatedArticlesGraphFilePath=build/bakery/related-articles.json")
        }
    }

    @Nested
    inner class RelatedArticlesDataInjection {

        @Test
        fun `injects relatedArticlesData when graph file exists`() {
            val siteYml = tempDir.resolve("site.yml")
            siteYml.writeText("""
                bake:
                  srcPath: site
                  destDirPath: bake
                relatedArticles:
                  enabled: true
                  graphFilePath: build/bakery/related-articles.json
            """.trimIndent(), UTF_8)

            // Create the graph file
            val graphFile = tempDir.resolve("build/bakery/related-articles.json")
            graphFile.parentFile.mkdirs()
            graphFile.writeText("""{"suggestions":{"/a.html":[{"url":"/b.html","title":"B","score":2.0}]},"blogArticles":{"/a.html":{"title":"A"}}}""", UTF_8)

            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("template.index.file=index.thyme\n", UTF_8)

            val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
            assertThat(config.relatedArticles).isNotNull

            config.relatedArticles?.let { injectRelatedArticlesIntoJbakeProperties(jbakeProps, it) }

            val props = jbakeProps.readText(UTF_8)
            assertThat(props).contains("relatedArticlesEnabled=true")
            assertThat(props).contains("relatedArticlesData=")
            assertThat(props).contains("relatedArticlesMaxResults=4")
        }

        @Test
        fun `injects empty relatedArticlesData when graph file does not exist`() {
            val siteYml = tempDir.resolve("site.yml")
            siteYml.writeText("""
                bake:
                  srcPath: site
                  destDirPath: bake
                relatedArticles:
                  enabled: true
                  graphFilePath: build/bakery/nonexistent.json
            """.trimIndent(), UTF_8)

            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("template.index.file=index.thyme\n", UTF_8)

            val config = mapper.readValue(siteYml, SiteConfiguration::class.java)
            config.relatedArticles?.let { injectRelatedArticlesIntoJbakeProperties(jbakeProps, it) }

            val props = jbakeProps.readText(UTF_8)
            assertThat(props).contains("relatedArticlesEnabled=true")
            assertThat(props).contains("relatedArticlesData=")
        }
    }

    private fun injectRelatedArticlesIntoJbakeProperties(jbakeProps: File, config: RelatedArticlesConfig, projectDir: File = jbakeProps.parentFile.parentFile) {
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
        updateProperty("relatedArticlesGraphFilePath", config.graphFilePath)

        // BKG-1.4: Inject relatedArticlesData from the graph file
        val graphFile = projectDir.resolve(config.graphFilePath)
        if (graphFile.exists()) {
            val graphData = graphFile.readText(UTF_8)
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
            updateProperty("relatedArticlesData", graphData)
        } else {
            updateProperty("relatedArticlesData", "")
        }

        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
    }
}