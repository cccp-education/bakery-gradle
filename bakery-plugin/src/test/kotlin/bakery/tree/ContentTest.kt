package bakery.tree

import bakery.pivot.PivotArticle
import bakery.pivot.PivotBlock
import bakery.pivot.PivotFrontmatter
import bakery.pivot.PivotInline
import bakery.tree.SiteNode.Article
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContentTest {

    private fun samplePivotArticle(): PivotArticle {
        val frontmatter = PivotFrontmatter(
            title = "Partitionnement AB",
            date = "2026-06-25",
            type = "page",
            status = "published"
        )
        val blocks = listOf(
            PivotBlock.Heading(level = 2, text = "Guide de demarrage", translatable = true),
            PivotBlock.Paragraph(inline = listOf(
                PivotInline.Text("Laissez la cle USB inseree", translatable = true),
                PivotInline.Bold("balenaEtcher", translatable = true)
            )),
            PivotBlock.Source(language = "bash", content = "sudo dd if=img.iso of=/dev/sdX bs=4M")
        )
        return PivotArticle(frontmatter = frontmatter, blocks = blocks)
    }

    @Test
    fun `content exposes its pivot article`() {
        val pivot = samplePivotArticle()
        val content = Content(pivot)

        assertEquals(pivot, content.pivot)
    }

    @Test
    fun `blocs returns the pivot blocks in order`() {
        val content = Content(samplePivotArticle())

        val blocs = content.blocs()

        assertEquals(3, blocs.size)
        assertTrue(blocs[0] is PivotBlock.Heading)
        assertTrue(blocs[1] is PivotBlock.Paragraph)
        assertTrue(blocs[2] is PivotBlock.Source)
    }

    @Test
    fun `inlineTexts collects all inline segments from paragraphs`() {
        val content = Content(samplePivotArticle())

        val inlines = content.inlineTexts()

        assertEquals(2, inlines.size)
        assertTrue(inlines[0] is PivotInline.Text)
        assertTrue(inlines[1] is PivotInline.Bold)
        assertEquals("Laissez la cle USB inseree", (inlines[0] as PivotInline.Text).text)
    }

    @Test
    fun `inlineTexts collects inline from list blocks`() {
        val pivot = PivotArticle(
            frontmatter = PivotFrontmatter("T", "2026-01-01", "page", "published"),
            blocks = listOf(
                PivotBlock.ListBlock(ordered = false, items = listOf(
                    listOf(PivotInline.Text("Premier item", translatable = true)),
                    listOf(PivotInline.Text("Deuxieme item", translatable = true))
                ))
            )
        )
        val content = Content(pivot)

        val inlines = content.inlineTexts()

        assertEquals(2, inlines.size)
        assertEquals("Premier item", (inlines[0] as PivotInline.Text).text)
        assertEquals("Deuxieme item", (inlines[1] as PivotInline.Text).text)
    }

    @Test
    fun `inlineTexts collects inline from table blocks`() {
        val pivot = PivotArticle(
            frontmatter = PivotFrontmatter("T", "2026-01-01", "page", "published"),
            blocks = listOf(
                PivotBlock.Table(
                    cols = null,
                    header = listOf(listOf(PivotInline.Text("Colonne A", translatable = true))),
                    rows = listOf(listOf(listOf(PivotInline.Text("Cellule 1", translatable = true))))
                )
            )
        )
        val content = Content(pivot)

        val inlines = content.inlineTexts()

        assertEquals(2, inlines.size)
        assertEquals("Colonne A", (inlines[0] as PivotInline.Text).text)
        assertEquals("Cellule 1", (inlines[1] as PivotInline.Text).text)
    }

    @Test
    fun `inlineTexts collects inline from nested admonition blocks`() {
        val pivot = PivotArticle(
            frontmatter = PivotFrontmatter("T", "2026-01-01", "page", "published"),
            blocks = listOf(
                PivotBlock.Admonition(
                    kind = "note",
                    blocks = listOf(
                        PivotBlock.Paragraph(inline = listOf(
                            PivotInline.Text("Note interne", translatable = true)
                        ))
                    )
                )
            )
        )
        val content = Content(pivot)

        val inlines = content.inlineTexts()

        assertEquals(1, inlines.size)
        assertEquals("Note interne", (inlines[0] as PivotInline.Text).text)
    }

    @Test
    fun `inlineTexts skips source and hr blocks`() {
        val pivot = PivotArticle(
            frontmatter = PivotFrontmatter("T", "2026-01-01", "page", "published"),
            blocks = listOf(
                PivotBlock.Source(language = "bash", content = "echo hello"),
                PivotBlock.Hr
            )
        )
        val content = Content(pivot)

        val inlines = content.inlineTexts()

        assertEquals(0, inlines.size)
    }

    @Test
    fun `translatableSegments filters inline by TextTranslatableClassifier`() {
        val pivot = PivotArticle(
            frontmatter = PivotFrontmatter("T", "2026-01-01", "page", "published"),
            blocks = listOf(
                PivotBlock.Paragraph(inline = listOf(
                    PivotInline.Text("Laissez la cle USB inseree", translatable = true),
                    PivotInline.Text("8 Go", translatable = false),
                    PivotInline.Text("fr_FR.UTF-8", translatable = false)
                ))
            )
        )
        val content = Content(pivot)

        val segments = content.translatableSegments()

        assertEquals(1, segments.size)
        assertEquals("Laissez la cle USB inseree", (segments[0] as PivotInline.Text).text)
    }

    @Test
    fun `content of empty article has no blocs and no inlines`() {
        val pivot = PivotArticle(
            frontmatter = PivotFrontmatter("T", "2026-01-01", "page", "published"),
            blocks = emptyList()
        )
        val content = Content(pivot)

        assertEquals(0, content.blocs().size)
        assertEquals(0, content.inlineTexts().size)
        assertEquals(0, content.translatableSegments().size)
    }

    @Test
    fun `article without content has null content`() {
        val article = Article(path = "formations/ab-partition")

        assertNull(article.content)
    }

    @Test
    fun `article with content exposes its AST`() {
        val pivot = samplePivotArticle()
        val article = Article(path = "formations/ab-partition", content = Content(pivot))

        assertTrue(article.content != null)
        assertEquals(pivot, article.content!!.pivot)
        assertEquals(3, article.content!!.blocs().size)
    }

    @Test
    fun `article with content translatable segments are extracted`() {
        val pivot = samplePivotArticle()
        val article = Article(path = "formations/ab-partition", content = Content(pivot))

        val segments = article.content!!.translatableSegments()

        assertTrue(segments.isNotEmpty())
        assertTrue(segments.all { it.translatable })
    }

    @Test
    fun `table with nested rows yields all inline in order`() {
        val pivot = PivotArticle(
            frontmatter = PivotFrontmatter("T", "2026-01-01", "page", "published"),
            blocks = listOf(
                PivotBlock.Table(
                    cols = null,
                    header = listOf(
                        listOf(PivotInline.Text("H1", translatable = true)),
                        listOf(PivotInline.Text("H2", translatable = true))
                    ),
                    rows = listOf(
                        listOf(
                            listOf(PivotInline.Text("R1C1", translatable = true)),
                            listOf(PivotInline.Text("R1C2", translatable = true))
                        )
                    )
                )
            )
        )
        val content = Content(pivot)

        val inlines = content.inlineTexts()

        assertEquals(4, inlines.size)
        assertEquals("H1", (inlines[0] as PivotInline.Text).text)
        assertEquals("H2", (inlines[1] as PivotInline.Text).text)
        assertEquals("R1C1", (inlines[2] as PivotInline.Text).text)
        assertEquals("R1C2", (inlines[3] as PivotInline.Text).text)
    }
}