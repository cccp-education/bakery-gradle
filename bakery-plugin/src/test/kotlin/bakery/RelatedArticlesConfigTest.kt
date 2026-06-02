package bakery

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RelatedArticlesConfigTest {

    private val mapper = ObjectMapper(YAMLFactory())

    @Nested
    inner class DataClassDefaults {
        @Test
        fun `RelatedArticlesConfig defaults are disabled with 4 results`() {
            val config = RelatedArticlesConfig()
            assertThat(config.enabled).isFalse()
            assertThat(config.maxResults).isEqualTo(4)
            assertThat(config.heading).isEqualTo("Articles connexes")
            assertThat(config.graphFilePath).isEqualTo("build/bakery/related-articles.json")
        }

        @Test
        fun `RelatedArticlesConfig can be created with custom values`() {
            val config = RelatedArticlesConfig(
                enabled = true,
                maxResults = 6,
                heading = "Lire aussi",
                graphFilePath = "custom/path/related-articles.json"
            )
            assertThat(config.enabled).isTrue()
            assertThat(config.maxResults).isEqualTo(6)
            assertThat(config.heading).isEqualTo("Lire aussi")
            assertThat(config.graphFilePath).isEqualTo("custom/path/related-articles.json")
        }
    }

    @Nested
    inner class YamlParsing {
        @Test
        fun `parse site yml with relatedArticles returns config with values`() {
            val yaml = """
                bake:
                  srcPath: src
                  destDirPath: build
                relatedArticles:
                  enabled: true
                  maxResults: 5
                  heading: "Voir aussi"
            """.trimIndent()
            val config = mapper.readValue(yaml, SiteConfiguration::class.java)
            assertThat(config.relatedArticles).isNotNull
            assertThat(config.relatedArticles!!.enabled).isTrue()
            assertThat(config.relatedArticles!!.maxResults).isEqualTo(5)
            assertThat(config.relatedArticles!!.heading).isEqualTo("Voir aussi")
        }

        @Test
        fun `parse site yml with relatedArticles including graphFilePath`() {
            val yaml = """
                bake:
                  srcPath: src
                  destDirPath: build
                relatedArticles:
                  enabled: true
                  graphFilePath: "custom/graph.json"
            """.trimIndent()
            val config = mapper.readValue(yaml, SiteConfiguration::class.java)
            assertThat(config.relatedArticles).isNotNull
            assertThat(config.relatedArticles!!.enabled).isTrue()
            assertThat(config.relatedArticles!!.graphFilePath).isEqualTo("custom/graph.json")
        }

        @Test
        fun `parse site yml without relatedArticles returns null`() {
            val yaml = """
                bake:
                  srcPath: src
                  destDirPath: build
            """.trimIndent()
            val config = mapper.readValue(yaml, SiteConfiguration::class.java)
            assertThat(config.relatedArticles).isNull()
        }

        @Test
        fun `parse site yml with relatedArticles defaults returns enabled=false and maxResults=4`() {
            val yaml = """
                bake:
                  srcPath: src
                  destDirPath: build
                relatedArticles:
                  enabled: true
            """.trimIndent()
            val config = mapper.readValue(yaml, SiteConfiguration::class.java)
            assertThat(config.relatedArticles).isNotNull
            assertThat(config.relatedArticles!!.enabled).isTrue()
            assertThat(config.relatedArticles!!.maxResults).isEqualTo(4)
            assertThat(config.relatedArticles!!.heading).isEqualTo("Articles connexes")
        }
    }

    @Nested
    inner class DslConfiguration {
        @Test
        fun `RelatedArticlesDsl defaults match config defaults`() {
            val dsl = RelatedArticlesDsl()
            assertThat(dsl.enabled).isFalse()
            assertThat(dsl.maxResults).isEqualTo(4)
            assertThat(dsl.heading).isEqualTo("Articles connexes")
            assertThat(dsl.graphFilePath).isEqualTo("build/bakery/related-articles.json")
        }

        @Test
        fun `RelatedArticlesDsl can be configured via properties`() {
            val dsl = RelatedArticlesDsl()
            dsl.enabled = true
            dsl.maxResults = 5
            dsl.heading = "Voir aussi"
            dsl.graphFilePath = "custom/path/related-articles.json"

            assertThat(dsl.enabled).isTrue()
            assertThat(dsl.maxResults).isEqualTo(5)
            assertThat(dsl.heading).isEqualTo("Voir aussi")
            assertThat(dsl.graphFilePath).isEqualTo("custom/path/related-articles.json")
        }
    }
}