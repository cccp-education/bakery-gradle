package bakery

import bakery.lens.AugmentedContextDsl
import bakery.lens.AugmentedContextResolver
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

object SiteContextCollector {

    private val rssDateFormats = listOf(
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    )
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
            val doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(feedFile)
            val items = doc.getElementsByTagName("item")
            (0 until items.length).map { i ->
                val item = items.item(i)
                mapOf(
                    "title" to item.childNodes.let { nodes ->
                        (0 until nodes.length).firstNotNullOfOrNull { j ->
                            if (nodes.item(j).nodeName == "title") nodes.item(j).textContent else null
                        } ?: ""
                    },
                    "url" to item.childNodes.let { nodes ->
                        val link = (0 until nodes.length).firstNotNullOfOrNull { j ->
                            if (nodes.item(j).nodeName == "link") nodes.item(j).textContent.trim() else null
                        } ?: ""
                        "/" + link.substringAfter("://")
                            .substringAfter("/")
                            .removePrefix("/")
                    },
                    "date" to item.childNodes.let { nodes ->
                        val raw = (0 until nodes.length).firstNotNullOfOrNull { j ->
                            if (nodes.item(j).nodeName == "pubDate") nodes.item(j).textContent else null
                        } ?: ""
                        parseRssDate(raw)
                    },
                    "tags" to item.childNodes.let { nodes ->
                        (0 until nodes.length).filter { j ->
                            nodes.item(j).nodeName == "category"
                        }.map { j -> nodes.item(j).textContent.trim() }
                    },
                    "author" to ""
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun parseRssDate(raw: String): String {
        if (raw.isBlank()) return ""
        for (fmt in rssDateFormats) {
            try {
                val temporal = fmt.parseBest(raw, LocalDate::from, { it })
                return isoDate.format(temporal as? LocalDate ?: return "")
            } catch (_: DateTimeParseException) { continue }
        }
        val isoMatch = Regex("""\d{4}-\d{2}-\d{2}""").find(raw)
        return isoMatch?.value ?: ""
    }

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
        }.getOrDefault(emptyList())
    }
}
