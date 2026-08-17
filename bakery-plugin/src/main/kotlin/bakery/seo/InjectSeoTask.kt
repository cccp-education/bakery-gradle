package bakery.seo

import bakery.BakeryConstants
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Template injection — file I/O, non-cacheable")
abstract class InjectSeoTask : DefaultTask() {
    @get:Internal
    var siteDir: File? = null

    @get:Internal
    var seoConfig: SeoConfig? = null

    @get:Internal
    var defaultLanguage: String = "fr"

    @get:Internal
    var supportedLanguages: List<String> = listOf("fr")

    @get:Internal
    var languages: List<String> = listOf("fr")

    init {
        group = BakeryConstants.TRANSFORM_GROUP
        description = "Injects SEO tags (canonical, hreflang, OG, Twitter, JSON-LD) into header.thyme for each configured language"
    }

    @TaskAction
    fun executeInjection() {
        val site =
            siteDir ?: run {
                logger.warn("[injectSeo] siteDir not configured — skipping")
                return
            }
        val config =
            seoConfig ?: run {
                logger.lifecycle("[injectSeo] No seo config — skipping (no-op)")
                return
            }
        if (!site.exists()) {
            logger.warn("[injectSeo] site directory does not exist: ${site.absolutePath}")
            return
        }

        val renderer = SeoThymeleafRenderer()
        val injector = SeoInjector()
        val sitemapBuilder = SitemapHreflangBuilder()
        var injectedCount = 0

        for (lang in languages) {
            val headerThyme = resolveHeaderThyme(site, lang) ?: continue
            val uri = resolvePageUri(site, lang)
            val meta = SeoPageMeta(uri = uri, title = config.siteName, description = config.siteName, type = SeoPageType.PAGE)
            val fragment = renderer.render(meta, config, defaultLanguage, supportedLanguages)
            val original = headerThyme.readText()
            val updated = injector.inject(original, fragment)
            if (updated != original) {
                headerThyme.writeText(updated)
                injectedCount++
                logger.lifecycle("[injectSeo] Injected SEO into header.thyme for '$lang'")
            } else {
                logger.lifecycle("[injectSeo] No change for '$lang' (already injected)")
            }
        }

        val sitemapFile = site.resolve("sitemap.xml")
        val siteHost = (config.websiteUrl ?: "").trimEnd('/')
        if (siteHost.isNotEmpty()) {
            val pageUris = collectPageUris(site)
            val sitemapXml = sitemapBuilder.build(siteHost, defaultLanguage, supportedLanguages, pageUris)
            sitemapFile.writeText(sitemapXml)
            logger.lifecycle("[injectSeo] Generated sitemap.xml with ${pageUris.size} url(s), ${supportedLanguages.size} language(s)")
        }

        logger.lifecycle("[injectSeo] Processed ${languages.size} language(s), injected into $injectedCount file(s)")
    }

    private fun resolveHeaderThyme(
        site: File,
        lang: String,
    ): File? {
        val templatesDir =
            if (lang == defaultLanguage) {
                site.resolve("templates")
            } else {
                site.resolve(lang).resolve("templates")
            }
        val headerThyme = templatesDir.resolve("header.thyme")
        return if (headerThyme.exists()) headerThyme else null
    }

    private fun resolvePageUri(
        site: File,
        lang: String,
    ): String = "index.html"

    private fun collectPageUris(site: File): List<String> {
        val uris = mutableListOf<String>()
        uris.add("")
        val contentDir = site.resolve("content")
        if (contentDir.exists()) {
            contentDir
                .walkTopDown()
                .filter { it.isFile && it.extension.equals("html", ignoreCase = true) }
                .forEach { file ->
                    val rel = file.relativeTo(contentDir).path.replace('\\', '/')
                    if (rel != "index.html") {
                        uris.add(rel)
                    }
                }
        }
        return uris
    }
}