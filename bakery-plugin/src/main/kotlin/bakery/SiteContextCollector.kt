package bakery

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
                        link.substringAfter("://")
                            .substringAfter("/")
                            .let { it }  // déjà préfixé par / via substringAfter
                    },
                    "date" to item.childNodes.let { nodes ->
                        val raw = (0 until nodes.length).firstNotNullOfOrNull { j ->
                            if (nodes.item(j).nodeName == "pubDate") nodes.item(j).textContent else null
                        } ?: ""
                        parseRssDate(raw)
                    },
                    "tags" to emptyList<String>(),
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
                url.substringAfter("://")
                    .substringAfter("/")
                    .let { it }  // déjà préfixé par / via substringAfter
            }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}
