package bakery.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentCopyServiceTest {

    private val service = ContentCopyService()

    @TempDir
    lateinit var tempDir: File

    private fun sourceDir(): File = tempDir.resolve("source").also { it.mkdirs() }
    private fun outputDir(): File = tempDir.resolve("output").also { it.mkdirs() }

    @Test
    fun `copies files to each language directory`() {
        val src = sourceDir()
        src.resolve("article.adoc").writeText("title=Test\n~~~~~~\n\nContent.")
        src.resolve("image.png").writeText("fake-png")
        src.resolve("sub").mkdirs()
        src.resolve("sub/data.txt").writeText("data")

        val out = outputDir()
        val result = service.copy(src, out, listOf("en", "es"), dryRun = false)

        assertTrue(out.resolve("en/article.adoc").exists())
        assertTrue(out.resolve("en/image.png").exists())
        assertTrue(out.resolve("en/sub/data.txt").exists())
        assertTrue(out.resolve("es/article.adoc").exists())
        assertTrue(out.resolve("es/image.png").exists())
        assertTrue(out.resolve("es/sub/data.txt").exists())
        assertEquals(6, result.filesCopied.size)
    }

    @Test
    fun `copied file content matches original`() {
        val src = sourceDir()
        val content = "title=Test\n~~~~~~\n\nHello world."
        src.resolve("hello.adoc").writeText(content)

        val out = outputDir()
        service.copy(src, out, listOf("en"), dryRun = false)

        assertEquals(content, out.resolve("en/hello.adoc").readText())
    }

    @Test
    fun `dryRun does not create any files`() {
        val src = sourceDir()
        src.resolve("article.adoc").writeText("title=A\n~~~~~~\n\nBody.")

        val out = outputDir()
        val result = service.copy(src, out, listOf("en"), dryRun = true)

        assertFalse(out.resolve("en").exists(), "Dry run should not create output")
        assertEquals(1, result.filesSkipped.size)
    }

    @Test
    fun `empty source directory produces empty result`() {
        val src = sourceDir()
        val out = outputDir()

        val result = service.copy(src, out, listOf("en"), dryRun = false)

        assertEquals(0, result.filesCopied.size)
    }

    @Test
    fun `multiple target languages all get copies`() {
        val src = sourceDir()
        src.resolve("doc.adoc").writeText("title=Doc\n~~~~~~\n\nText.")

        val out = outputDir()
        service.copy(src, out, listOf("en", "zh", "ar"), dryRun = false)

        assertTrue(out.resolve("en/doc.adoc").exists())
        assertTrue(out.resolve("zh/doc.adoc").exists())
        assertTrue(out.resolve("ar/doc.adoc").exists())
    }

    @Test
    fun `directory structure is preserved in output`() {
        val src = sourceDir()
        src.resolve("content").mkdirs()
        src.resolve("content/blog").mkdirs()
        src.resolve("content/blog/post-1.adoc").writeText("title=Post\n~~~~~~\n\nBody.")
        src.resolve("images").mkdirs()
        src.resolve("images/logo.svg").writeText("<svg></svg>")

        val out = outputDir()
        service.copy(src, out, listOf("en"), dryRun = false)

        assertTrue(out.resolve("en/content/blog/post-1.adoc").exists())
        assertTrue(out.resolve("en/images/logo.svg").exists())
    }
}
