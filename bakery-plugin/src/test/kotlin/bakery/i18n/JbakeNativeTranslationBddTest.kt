package bakery.i18n

import bakery.pivot.AsciiDocRenderer
import bakery.pivot.JbakeNativeRenderer
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class JbakeNativeTranslationBddTest {

    @TempDir
    lateinit var tempDir: File

    private val fakeTranslator = object : TranslationService {
        override fun translate(request: TranslationRequest): TranslationResult =
            TranslationResult.Success("[EN] ${request.sourceText}")
    }

    @Test
    fun `scenario 1 - jbake native article translated with native renderer`() {
        val sourceContent = """
            = Article de Test
            @CherOliv
            2020-01-15
            :jbake-type: post
            :jbake-status: published

            Premier paragraphe en français.

            == Section

            Deuxième paragraphe.
        """.trimIndent()

        val translated = translateContent(sourceContent)

        assertTrue(translated.startsWith("= "), "Should start with JBake native title, got: ${translated.take(50)}")
        assertTrue(translated.contains("@CherOliv"), "Should preserve author")
        assertTrue(translated.contains(":jbake-type:"), "Should preserve jbake-type")
        assertTrue(translated.contains("[EN]"), "Should contain translated text")
    }

    @Test
    fun `scenario 2 - pivot format article still uses pivot renderer`() {
        val sourceContent = """
            title=Article de Test
            date=2020-01-15
            type=post
            status=published
            ~~~~~~

            Premier paragraphe en français.
        """.trimIndent()

        val translated = translateContent(sourceContent)

        assertTrue(translated.contains("title="), "Should use pivot format")
        assertTrue(translated.contains("~~~~~~"), "Should contain tilde separator")
        assertTrue(translated.contains("[EN]"), "Should contain translated text")
    }

    @Test
    fun `scenario 3 - jbake native with tags and summary preserves attributes`() {
        val sourceContent = """
            = Article Tagué
            @CherOliv
            2022-06-10
            :jbake-title: Article Tagué
            :jbake-tags: blog, kotlin, test
            :jbake-type: post
            :jbake-status: published
            :jbake-date: 2022-06-10
            :summary: Un article de test

            Corps de l'article en français.
        """.trimIndent()

        val translated = translateContent(sourceContent)

        assertTrue(translated.contains(":jbake-tags: blog, kotlin, test"),
            "Should preserve jbake-tags, got: ${translated.take(300)}")
        assertTrue(translated.contains(":summary: Un article de test"),
            "Should preserve summary, got: ${translated.take(300)}")
    }

    private fun translateContent(sourceContent: String): String {
        val sourceDir = File(tempDir, "fr").apply { mkdirs() }
        File(sourceDir, "article.adoc").writeText(sourceContent)
        val enDir = File(tempDir, "en").apply { mkdirs() }
        File(enDir, "article.adoc").writeText(sourceContent)

        val service = ContentTranslationService(
            fakeTranslator,
            renderer = AsciiDocRenderer(),
            jbakeRenderer = JbakeNativeRenderer()
        )
        service.translate(enDir, "fr", "en")
        return File(enDir, "article.adoc").readText()
    }
}
