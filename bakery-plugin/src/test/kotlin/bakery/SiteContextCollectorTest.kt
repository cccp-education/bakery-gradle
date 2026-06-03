package bakery

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import bakery.lens.AugmentedContextDsl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SiteContextCollectorTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `should extract articles from feed xml`(@TempDir tempDir: File) {
        // GIVEN : un site baké avec feed.xml + sitemap.xml
        val bakedDir = tempDir.resolve("bake")
        bakedDir.mkdirs()
        bakedDir.resolve("feed.xml").writeText(
            """<?xml version="1.0"?>
<rss version="2.0">
<channel>
  <title>My Blog</title>
  <item>
    <title>First Post</title>
    <link>http://localhost:8820/blog/2024/first.html</link>
    <pubDate>Wed, 24 Jul 2024 00:00:00 +0200</pubDate>
    <guid isPermaLink="false">blog/2024/first.html</guid>
  </item>
  <item>
    <title>Second Post</title>
    <link>http://localhost:8820/blog/2024/second.html</link>
    <pubDate>Sun, 25 Aug 2024 00:00:00 +0200</pubDate>
    <guid isPermaLink="false">blog/2024/second.html</guid>
  </item>
</channel>
</rss>"""
        )
        bakedDir.resolve("sitemap.xml").writeText(
            """<?xml version="1.0"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url><loc>http://localhost:8820/about.html</loc></url>
  <url><loc>http://localhost:8820/blog.html</loc></url>
  <url><loc>http://localhost:8820/blog/2024/first.html</loc></url>
  <url><loc>http://localhost:8820/blog/2024/second.html</loc></url>
</urlset>"""
        )

        val outputDir = tempDir.resolve("output")

        // WHEN
        SiteContextCollector.collect(bakedDir, outputDir)

        // THEN : metadata.json produit
        val metadataFile = outputDir.resolve("metadata.json")
        assertThat(metadataFile).exists()

        val metadata: Map<String, Any> = mapper.readValue(metadataFile)
        assertThat(metadata["source"]).isEqualTo("bakery")
        assertThat(metadata["type"]).isEqualTo("site")
        assertThat(metadata["version"]).isEqualTo("1.0")
        assertThat(metadata["model"]).isEqualTo("jbake")
        assertThat(metadata["sessions"]).isEqualTo(2)
        assertThat(metadata["dependencies"]).isEqualTo(listOf("jbake"))
        assertThat(metadata["generatedAt"]).isNotNull()

        // articles
        @Suppress("UNCHECKED_CAST")
        val articles = metadata["articles"] as List<Map<String, String>>
        assertThat(articles).hasSize(2)
        assertThat(articles[0]["title"]).isEqualTo("First Post")
        assertThat(articles[0]["url"]).isEqualTo("/blog/2024/first.html")
        assertThat(articles[0]["date"]).isEqualTo("2024-07-24")
        assertThat(articles[1]["title"]).isEqualTo("Second Post")
        assertThat(articles[1]["url"]).isEqualTo("/blog/2024/second.html")

        // sitemap
        @Suppress("UNCHECKED_CAST")
        val sitemap = metadata["sitemap"] as List<String>
        assertThat(sitemap).contains("/about.html", "/blog.html")

        // feed
        assertThat(metadata["feed"]).isEqualTo("/feed.xml")
    }

    @Test
    fun `should return empty articles when no feed xml`(@TempDir tempDir: File) {
        val bakedDir = tempDir.resolve("bake")
        bakedDir.mkdirs()
        val outputDir = tempDir.resolve("output")

        SiteContextCollector.collect(bakedDir, outputDir)

        val metadataFile = outputDir.resolve("metadata.json")
        assertThat(metadataFile).exists()
        val metadata: Map<String, Any> = mapper.readValue(metadataFile)

        @Suppress("UNCHECKED_CAST")
        val articles = metadata["articles"] as List<*>
        assertThat(articles).isEmpty()
        assertThat(metadata["sessions"]).isEqualTo(0)
    }

    @Test
    fun `should handle empty feed with zero items`(@TempDir tempDir: File) {
        val bakedDir = tempDir.resolve("bake")
        bakedDir.mkdirs()
        bakedDir.resolve("feed.xml").writeText(
            """<?xml version="1.0"?>
<rss version="2.0">
<channel><title>Empty</title></channel>
</rss>"""
        )
        bakedDir.resolve("sitemap.xml").writeText(
            """<?xml version="1.0"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"></urlset>"""
        )

        val outputDir = tempDir.resolve("output")
        SiteContextCollector.collect(bakedDir, outputDir)

        val metadata: Map<String, Any> = mapper.readValue(outputDir.resolve("metadata.json"))
        assertThat(metadata["sessions"]).isEqualTo(0)

        @Suppress("UNCHECKED_CAST")
        val articles = metadata["articles"] as List<*>
        assertThat(articles).isEmpty()

        @Suppress("UNCHECKED_CAST")
        val sitemap = metadata["sitemap"] as List<*>
        assertThat(sitemap).isEmpty()
    }

    // ──────────────────────────────────────
    // BKY-LENS-5 : collectWithAugmentedContext()
    // ──────────────────────────────────────

    @Nested
    @DisplayName("BKY-LENS-5 : SiteContextCollector — collectWithAugmentedContext()")
    inner class CollectWithAugmentedContext {

        private fun minimalBakedSite(bakedDir: File) {
            bakedDir.mkdirs()
            bakedDir.resolve("feed.xml").writeText(
                """<?xml version="1.0"?>
<rss version="2.0">
<channel><title>Test</title></channel>
</rss>"""
            )
        }

        @Test
        @DisplayName("collectWithAugmentedContext ajoute augmentedEntries quand composite-context.json existe")
        fun `augmented entries added when composite context exists`(@TempDir tempDir: File) {
            // GIVEN : site baké + composite-context.json + augmentedContext enabled
            val bakedDir = tempDir.resolve("bake")
            minimalBakedSite(bakedDir)
            val outputDir = tempDir.resolve("output")

            val compositeContextJson = """{
                "eagerSection": "Eager governance content.",
                "ragSection": "RAG similarity scores.",
                "graphifySection": "Graphify structural relations.",
                "docsSection": "Docs corpus documentation.",
                "config": {
                    "totalTokenBudget": 8000,
                    "budgetEagerLazy": 0.40,
                    "budgetRag": 0.30,
                    "budgetGraphify": 0.20,
                    "budgetDocs": 0.10,
                    "budgetOverhead": 0.0
                }
            }""".trimIndent()
            val compositeContextFile = tempDir.resolve("composite-context.json")
            compositeContextFile.writeText(compositeContextJson)

            val dsl = AugmentedContextDsl()
            dsl.enabled = true
            dsl.contextPath = compositeContextFile.absolutePath
            dsl.maxArticles = 4

            // WHEN
            SiteContextCollector.collectWithAugmentedContext(bakedDir, outputDir, dsl)

            // THEN : metadata.json contient augmentedEntries
            val metadataFile = outputDir.resolve("metadata.json")
            assertThat(metadataFile).exists()
            val metadata: Map<String, Any> = mapper.readValue(metadataFile)

            assertThat(metadata).containsKey("augmentedEntries")
            @Suppress("UNCHECKED_CAST")
            val augmentedEntries = metadata["augmentedEntries"] as Map<String, Any>
            assertThat(augmentedEntries).isNotEmpty
            assertThat(augmentedEntries).containsKey("channels")
        }

        @Test
        @DisplayName("collectWithAugmentedContext sans composite-context.json → augmentedEntries = map vide")
        fun `no augmented entries when composite context absent`(@TempDir tempDir: File) {
            // GIVEN : site baké sans composite-context.json
            val bakedDir = tempDir.resolve("bake")
            minimalBakedSite(bakedDir)
            val outputDir = tempDir.resolve("output")

            val dsl = AugmentedContextDsl()
            dsl.enabled = true
            dsl.contextPath = "/nonexistent/composite-context.json"
            dsl.maxArticles = 4

            // WHEN
            SiteContextCollector.collectWithAugmentedContext(bakedDir, outputDir, dsl)

            // THEN : metadata.json contient augmentedEntries avec channels vide
            val metadata: Map<String, Any> = mapper.readValue(outputDir.resolve("metadata.json"))
            assertThat(metadata).containsKey("augmentedEntries")
            @Suppress("UNCHECKED_CAST")
            val augmentedEntries = metadata["augmentedEntries"] as Map<String, Any>
            assertThat(augmentedEntries).containsKey("channels")
            @Suppress("UNCHECKED_CAST")
            val channels = augmentedEntries["channels"] as List<*>
            assertThat(channels).isEmpty()
        }

        @Test
        @DisplayName("collectWithAugmentedContext disabled → pas d'augmentedEntries dans metadata")
        fun `no augmented entries when disabled`(@TempDir tempDir: File) {
            // GIVEN : site baké + composite-context.json mais augmentedContext disabled
            val bakedDir = tempDir.resolve("bake")
            minimalBakedSite(bakedDir)
            val outputDir = tempDir.resolve("output")

            val compositeContextFile = tempDir.resolve("composite-context.json")
            compositeContextFile.writeText("""{"eagerSection":"test","ragSection":"","graphifySection":"","docsSection":"","config":{"totalTokenBudget":8000,"budgetEagerLazy":0.4,"budgetRag":0.3,"budgetGraphify":0.2,"budgetDocs":0.1,"budgetOverhead":0.0}}""")

            val dsl = AugmentedContextDsl()
            dsl.enabled = false
            dsl.contextPath = compositeContextFile.absolutePath

            // WHEN
            SiteContextCollector.collectWithAugmentedContext(bakedDir, outputDir, dsl)

            // THEN : metadata.json NE contient PAS augmentedEntries
            val metadata: Map<String, Any> = mapper.readValue(outputDir.resolve("metadata.json"))
            assertThat(metadata).doesNotContainKey("augmentedEntries")
        }

        @Test
        @DisplayName("collectWithAugmentedContext maxArticles tronque les canaux")
        fun `maxArticles limits augmented channels`(@TempDir tempDir: File) {
            // GIVEN
            val bakedDir = tempDir.resolve("bake")
            minimalBakedSite(bakedDir)
            val outputDir = tempDir.resolve("output")

            val compositeContextFile = tempDir.resolve("composite-context.json")
            compositeContextFile.writeText("""{
                "eagerSection": "Eager content",
                "ragSection": "RAG content",
                "graphifySection": "Graphify content",
                "docsSection": "Docs content",
                "config": {"totalTokenBudget":8000,"budgetEagerLazy":0.4,"budgetRag":0.3,"budgetGraphify":0.2,"budgetDocs":0.1,"budgetOverhead":0.0}
            }""".trimIndent())

            val dsl = AugmentedContextDsl()
            dsl.enabled = true
            dsl.contextPath = compositeContextFile.absolutePath
            dsl.maxArticles = 2

            // WHEN
            SiteContextCollector.collectWithAugmentedContext(bakedDir, outputDir, dsl)

            // THEN : augmentedEntries.channels tronqué à maxArticles
            val metadata: Map<String, Any> = mapper.readValue(outputDir.resolve("metadata.json"))
            @Suppress("UNCHECKED_CAST")
            val augmentedEntries = metadata["augmentedEntries"] as Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            val channels = augmentedEntries["channels"] as List<*>
            assertThat(channels).hasSize(2)
        }

        @Test
        @DisplayName("collectWithAugmentedContext maxArticles=0 → pas de troncature (tous les canaux)")
        fun `maxArticles zero means all channels`(@TempDir tempDir: File) {
            // GIVEN
            val bakedDir = tempDir.resolve("bake")
            minimalBakedSite(bakedDir)
            val outputDir = tempDir.resolve("output")

            val compositeContextFile = tempDir.resolve("composite-context.json")
            compositeContextFile.writeText("""{
                "eagerSection": "Eager content",
                "ragSection": "RAG content",
                "graphifySection": "Graphify content",
                "docsSection": "Docs content",
                "config": {"totalTokenBudget":8000,"budgetEagerLazy":0.4,"budgetRag":0.3,"budgetGraphify":0.2,"budgetDocs":0.1,"budgetOverhead":0.0}
            }""".trimIndent())

            val dsl = AugmentedContextDsl()
            dsl.enabled = true
            dsl.contextPath = compositeContextFile.absolutePath
            dsl.maxArticles = 0

            // WHEN
            SiteContextCollector.collectWithAugmentedContext(bakedDir, outputDir, dsl)

            // THEN : tous les canaux (4 non vides)
            val metadata: Map<String, Any> = mapper.readValue(outputDir.resolve("metadata.json"))
            @Suppress("UNCHECKED_CAST")
            val augmentedEntries = metadata["augmentedEntries"] as Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            val channels = augmentedEntries["channels"] as List<*>
            assertThat(channels).hasSize(4)
        }
    }
}
