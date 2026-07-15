package bakery.i18n

import bakery.pivot.AsciiDocRenderer
import bakery.pivot.JbakeNativeRenderer
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentTranslationServiceParallelTest {

    @TempDir
    lateinit var tempDir: File

    private val renderer = AsciiDocRenderer()
    private val jbakeRenderer = JbakeNativeRenderer()

    @Test
    fun `parallel translate produces same results as sequential`() {
        val files = listOf(
            "article1.adoc" to """title=Article Un
date=2020-01-01
type=post
status=published
~~~~~~

Premier paragraphe en français.

== Section

Deuxième paragraphe.""",
            "article2.adoc" to """title=Article Deux
date=2020-02-02
type=post
status=published
~~~~~~

Texte en français.""",
            "article3.adoc" to """= Article Trois
@CherOliv
2020-03-03
:jbake-type: post
:jbake-status: published

Paragraphe jbake natif."""
        )

        val translator = SuffixTranslator("[EN]")

        val seqDir = File(tempDir, "seq").apply { mkdirs() }
        files.forEach { (name, content) -> File(seqDir, name).writeText(content) }
        val seqService = ContentTranslationService(translator, renderer = renderer, jbakeRenderer = jbakeRenderer)
        val seqResult = seqService.translate(seqDir, "fr", "en")

        val parDir = File(tempDir, "par").apply { mkdirs() }
        files.forEach { (name, content) -> File(parDir, name).writeText(content) }
        val parService = ContentTranslationService(translator, renderer = renderer, jbakeRenderer = jbakeRenderer, parallelism = 3)
        val parResult = parService.translate(parDir, "fr", "en")

        assertEquals(seqResult.filesTranslated.size, parResult.filesTranslated.size)
        assertEquals(seqResult.errors.size, parResult.errors.size)
        files.forEach { (name, _) ->
            val seqContent = File(seqDir, name).readText()
            val parContent = File(parDir, name).readText()
            assertEquals(seqContent, parContent, "Parallel output differs for $name")
        }
    }

    @Test
    fun `parallel translate processes files concurrently`() {
        val concurrencyTracker = ConcurrencyTracker()
        val translator = concurrencyTracker.asTranslator("[EN]")

        val parDir = File(tempDir, "par").apply { mkdirs() }
        repeat(6) { i ->
            File(parDir, "article$i.adoc").writeText("""title=Article $i
date=2020-01-01
type=post
status=published
~~~~~~

Texte $i.""")
        }

        val service = ContentTranslationService(translator, renderer = renderer, jbakeRenderer = jbakeRenderer, parallelism = 4)
        service.translate(parDir, "fr", "en")

        assertTrue(concurrencyTracker.maxConcurrent.get() >= 2,
            "Expected at least 2 concurrent translations, got max=${concurrencyTracker.maxConcurrent.get()}")
    }

    @Test
    fun `parallel translate collects all errors without losing any`() {
        val translator = FailingAfterTwoTranslator()
        val parDir = File(tempDir, "err").apply { mkdirs() }
        repeat(5) { i ->
            File(parDir, "article$i.adoc").writeText("""title=Article $i
date=2020-01-01
type=post
status=published
~~~~~~

Texte $i.""")
        }

        val service = ContentTranslationService(translator, renderer = renderer, jbakeRenderer = jbakeRenderer, parallelism = 3)
        val result = service.translate(parDir, "fr", "en")

        assertEquals(5, result.filesTranslated.size + result.errors.size,
            "All files should be accounted for (translated or errored)")
    }

    @Test
    fun `parallelism defaults to 1 preserves sequential behavior`() {
        val translator = SuffixTranslator("[EN]")
        val dir = File(tempDir, "default").apply { mkdirs() }
        File(dir, "a.adoc").writeText("""title=A
date=2020-01-01
type=post
status=published
~~~~~~

Texte A.""")

        val service = ContentTranslationService(translator, renderer = renderer, jbakeRenderer = jbakeRenderer)
        val result = service.translate(dir, "fr", "en")

        assertTrue(result.success)
        assertEquals(1, result.filesTranslated.size)
    }

    private class SuffixTranslator(private val suffix: String) : TranslationService {
        override fun translate(request: TranslationRequest): TranslationResult =
            TranslationResult.Success("$suffix ${request.sourceText}")
    }

    private class ConcurrencyTracker {
        val current = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)

        fun asTranslator(suffix: String): TranslationService = object : TranslationService {
            override fun translate(request: TranslationRequest): TranslationResult {
                val cur = current.incrementAndGet()
                maxConcurrent.updateAndGet { maxOf(it, cur) }
                Thread.sleep(200)
                current.decrementAndGet()
                return TranslationResult.Success("$suffix ${request.sourceText}")
            }
        }
    }

    private class FailingAfterTwoTranslator : TranslationService {
        val count = AtomicInteger(0)
        override fun translate(request: TranslationRequest): TranslationResult {
            val n = count.incrementAndGet()
            return if (n > 2) {
                TranslationResult.Failure("Failed on request $n")
            } else {
                TranslationResult.Success("[EN] ${request.sourceText}")
            }
        }
    }
}