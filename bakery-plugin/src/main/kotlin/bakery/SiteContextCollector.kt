package bakery

import bakery.lens.AugmentedContextDsl
import bakery.lens.AugmentedContextResolver
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

object SiteContextCollector {

    private val logger = LoggerFactory.getLogger(SiteContextCollector::class.java)
    private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE

    fun collect(bakedDir: File, outputDir: File) {
        val articles = parseFeed(bakedDir)
        val sitemap = parseSitemap(bakedDir)
        val metadata = mapOf(
            "source" to "bakery",
            "type" to "site",
            "version" to "1.0",
            "model" to "jbake",
            "generatedAt" to Instant.now().toString(),
            "sessions" to articles.size,
            "dependencies" to listOf("jbake"),
            "articles" to articles,
            "sitemap" to sitemap,
            "feed" to "/feed.xml"
        )
        outputDir.mkdirs()
        val mapper = jacksonObjectMapper()
        mapper.writerWithDefaultPrettyPrinter()
            .writeValue(outputDir.resolve("metadata.json"), metadata)
    }

    /**
     * BKY-LENS-5 : Collecte le contexte du site baké + enrichit avec le composite-context.
     *
     * Lit le fichier composite-context.json (sortie de runner-gradle N3) via
     * [AugmentedContextResolver], extrait les canaux disponibles, et injecte
     * `augmentedEntries` dans metadata.json.
     *
     * @param bakedDir Répertoire du site baké
     * @param outputDir Répertoire de sortie pour metadata.json
     * @param augmentedContext Configuration du contexte augmenté (enabled, contextPath, maxArticles)
     */
    fun collectWithAugmentedContext(
        bakedDir: File,
        outputDir: File,
        augmentedContext: AugmentedContextDsl
    ) {
        // 1. Collecte standard
        collect(bakedDir, outputDir)

        // 2. Si disabled, ne pas enrichir
        if (!augmentedContext.enabled) return

        // 3. Résoudre le composite-context.json
        val resolver = AugmentedContextResolver()
        val channels = resolver.extractChannelsFromPath(augmentedContext.contextPath)

        if (channels.isEmpty()) {
            // Pas de composite-context → augmentedEntries vide (signale fonctionnalité active sans source N3)
            enrichMetadataWithAugmentedEntries(outputDir, mapOf("channels" to emptyList<Any>()))
            return
        }

        // 4. Construire les augmented entries
        val channelsList = channels.map { (type, content) ->
            mapOf("channel" to type.name, "content" to content)
        }

        // 5. Appliquer maxArticles (troncature)
        val truncated = if (augmentedContext.maxArticles > 0) {
            channelsList.take(augmentedContext.maxArticles)
        } else {
            channelsList
        }

        val augmentedEntries = mapOf("channels" to truncated)
        enrichMetadataWithAugmentedEntries(outputDir, augmentedEntries)
    }

    /**
     * Enrichit le metadata.json existant avec les augmentedEntries.
     */
    private fun enrichMetadataWithAugmentedEntries(
        outputDir: File,
        augmentedEntries: Map<String, Any>
    ) {
        val metadataFile = outputDir.resolve("metadata.json")
        if (!metadataFile.exists()) return

        val mapper = jacksonObjectMapper()
        val metadata: MutableMap<String, Any> = mapper.readValue(metadataFile, MutableMap::class.java)
            as MutableMap<String, Any>

        if (augmentedEntries.isNotEmpty()) {
            metadata["augmentedEntries"] = augmentedEntries
        }

        mapper.writerWithDefaultPrettyPrinter()
            .writeValue(metadataFile, metadata)
    }

    private fun parseFeed(bakedDir: File): List<Map<String, Any>> {
        val feedFile = bakedDir.resolve("feed.xml")
        if (!feedFile.exists()) return emptyList()

        return runCatching {
            SyndFeedInput().build(XmlReader(feedFile)).entries.map { entry ->
                val title: String = entry.title as? String ?: ""
                val url: String = (entry.link as? String)?.let { normalizeUrl(it) } ?: ""
                val date: String = (entry.publishedDate as? java.util.Date)?.let {
                    isoDate.format(it.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                } ?: ""
                val tags: List<String> = entry.categories.map { (it.name as? String)?.trim() ?: "" }.filter { it.isNotBlank() }
                val author: String = entry.author as? String ?: ""

                mapOf(
                    "title" to title,
                    "url" to url,
                    "date" to date,
                    "tags" to tags,
                    "author" to author
                )
            }
        }.onFailure { e ->
            logger.warn("Failed to parse feed.xml at {}: {}", feedFile.absolutePath, e.message)
        }.getOrDefault(emptyList())
    }

    private fun normalizeUrl(link: String): String =
        "/" + link.substringAfter("://")
            .substringAfter("/")
            .removePrefix("/")

    private fun parseSitemap(bakedDir: File): List<String> {
        val sitemapFile = bakedDir.resolve("sitemap.xml")
        if (!sitemapFile.exists()) return emptyList()

        return runCatching {
            val doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(sitemapFile)
            val locs = doc.getElementsByTagName("loc")
            (0 until locs.length).map { i ->
                val url = locs.item(i).textContent.trim()
                "/" + url.substringAfter("://")
                    .substringAfter("/")
                    .removePrefix("/")
            }.filter { it.isNotBlank() }
        }.onFailure { e ->
            logger.warn("Failed to parse sitemap.xml at {}: {}", sitemapFile.absolutePath, e.message)
        }.getOrDefault(emptyList())
    }
}
