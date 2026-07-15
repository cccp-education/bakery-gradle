package bakery.i18n

import bakery.pivot.AsciiDocParser
import bakery.pivot.ArticleRenderer
import bakery.pivot.AsciiDocRenderer
import bakery.pivot.JbakeNativeRenderer
import bakery.pivot.PivotArticle
import bakery.pivot.PivotBlock
import bakery.pivot.PivotFrontmatter
import bakery.pivot.PivotInline
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import org.slf4j.LoggerFactory
import java.io.File

class ContentTranslationService(
    private val translationService: TranslationService,
    private val parser: AsciiDocParser = AsciiDocParser(),
    private val renderer: ArticleRenderer = AsciiDocRenderer(),
    private val jbakeRenderer: ArticleRenderer = JbakeNativeRenderer()
) {
    private val log = LoggerFactory.getLogger(ContentTranslationService::class.java)

    fun translate(
        langDir: File,
        sourceLanguage: String,
        targetLanguage: String
    ): ContentTranslationResult {
        val adocFiles = langDir.walkTopDown()
            .filter { it.isFile && it.extension == "adoc" }
            .toList()

        log.info("[translate] Traduction $targetLanguage — ${adocFiles.size} fichiers .adoc dans {}", langDir.name)

        val translated = mutableListOf<String>()
        val errors = mutableListOf<String>()

        for ((idx, file) in adocFiles.withIndex()) {
            val relPath = file.relativeTo(langDir).path
            log.info("[translate] [{}] {}/{} Traduction de : {}", targetLanguage, idx + 1, adocFiles.size, relPath)
            try {
                val original = file.readText()
                val article = parser.parse(original)
                val translatedArticle = translateArticle(article, sourceLanguage, targetLanguage)
                val outputRenderer = if (article.frontmatter.isJbakeNative) jbakeRenderer else renderer
                val rendered = outputRenderer.render(translatedArticle)
                file.writeText(rendered)
                translated.add(relPath)
                log.info("[translate] [{}] OK : {}", targetLanguage, relPath)
            } catch (e: Exception) {
                val msg = "${relPath}: ${e.message}"
                errors.add(msg)
                log.warn("[translate] [{}] ERREUR : {}", targetLanguage, msg)
            }
        }

        log.info("[translate] [{}] Terminé — {} traduits, {} erreurs",
            targetLanguage, translated.size, errors.size)
        return ContentTranslationResult(translated, errors)
    }

    internal fun translateArticle(
        article: PivotArticle,
        sourceLanguage: String,
        targetLanguage: String
    ): PivotArticle {
        val translatedFrontmatter = translateFrontmatter(article.frontmatter, sourceLanguage, targetLanguage)
        val translatedBlocks = article.blocks.map { translateBlock(it, sourceLanguage, targetLanguage) }
        return PivotArticle(translatedFrontmatter, translatedBlocks)
    }

    private fun translateFrontmatter(
        fm: PivotFrontmatter,
        sourceLanguage: String,
        targetLanguage: String
    ): PivotFrontmatter {
        val translatedTitle = doTranslate(fm.title, sourceLanguage, targetLanguage)
        return fm.copy(title = translatedTitle)
    }

    private fun translateBlock(
        block: PivotBlock,
        sourceLanguage: String,
        targetLanguage: String
    ): PivotBlock = when (block) {
        is PivotBlock.Heading -> {
            val translated = doTranslate(block.text, sourceLanguage, targetLanguage)
            block.copy(text = translated)
        }
        is PivotBlock.Paragraph -> {
            block.copy(inline = translateInlines(block.inline, sourceLanguage, targetLanguage))
        }
        is PivotBlock.ListBlock -> {
            block.copy(
                items = block.items.map { items ->
                    translateInlines(items, sourceLanguage, targetLanguage)
                }
            )
        }
        is PivotBlock.Table -> {
            block.copy(
                header = block.header.map { cells ->
                    translateInlines(cells, sourceLanguage, targetLanguage)
                },
                rows = block.rows.map { row ->
                    row.map { cells ->
                        translateInlines(cells, sourceLanguage, targetLanguage)
                    }
                }
            )
        }
        is PivotBlock.Admonition -> {
            block.copy(
                blocks = block.blocks.map { translateBlock(it, sourceLanguage, targetLanguage) }
            )
        }
        is PivotBlock.Source -> block
        is PivotBlock.Hr -> block
    }

    private fun translateInlines(
        inlines: List<PivotInline>,
        sourceLanguage: String,
        targetLanguage: String
    ): List<PivotInline> = inlines.map { translateInline(it, sourceLanguage, targetLanguage) }

    private fun translateInline(
        inline: PivotInline,
        sourceLanguage: String,
        targetLanguage: String
    ): PivotInline = when (inline) {
        is PivotInline.Text -> {
            if (inline.translatable) {
                inline.copy(text = doTranslate(inline.text, sourceLanguage, targetLanguage))
            } else inline
        }
        is PivotInline.Bold -> {
            if (inline.translatable) {
                inline.copy(text = doTranslate(inline.text, sourceLanguage, targetLanguage))
            } else inline
        }
        is PivotInline.Code -> inline
        is PivotInline.Link -> {
            if (inline.translatable) {
                inline.copy(label = doTranslate(inline.label, sourceLanguage, targetLanguage))
            } else inline
        }
    }

    private fun doTranslate(text: String, sourceLanguage: String, targetLanguage: String): String {
        if (text.isBlank()) return text
        val request = TranslationRequest(text, sourceLanguage, targetLanguage)
        return when (val result = translationService.translate(request)) {
            is TranslationResult.Success -> result.translatedText
            is TranslationResult.Failure -> text
        }
    }
}

data class ContentTranslationResult(
    val filesTranslated: List<String>,
    val errors: List<String>
) {
    val success: Boolean get() = errors.isEmpty()
}
