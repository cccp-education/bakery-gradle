package bakery.pivot

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AsciiDocParserTest {

    private val parser = AsciiDocParser()

    @Test
    fun `parses frontmatter header before tilde separator`() {
        val adoc = """
            title=Guide de demarrage rapide
            date=2026-04-26
            type=page
            status=published
            ~~~~~~

            == Guide de demarrage rapide
        """.trimIndent()

        val article = parser.parse(adoc)

        assertEquals("Guide de demarrage rapide", article.frontmatter.title)
        assertEquals("2026-04-26", article.frontmatter.date)
        assertEquals("page", article.frontmatter.type)
        assertEquals("published", article.frontmatter.status)
    }

    @Test
    fun `parses level 2 heading`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            == Guide de demarrage rapide
        """.trimIndent()

        val article = parser.parse(adoc)

        assertEquals(1, article.blocks.size)
        val heading = article.blocks[0] as PivotBlock.Heading
        assertEquals(2, heading.level)
        assertEquals("Guide de demarrage rapide", heading.text)
        assertTrue(heading.translatable)
    }

    @Test
    fun `parses level 3 and level 4 headings`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            == Title 2

            === Title 3

            ==== Title 4
        """.trimIndent()

        val article = parser.parse(adoc)

        assertEquals(3, article.blocks.size)
        assertEquals(2, (article.blocks[0] as PivotBlock.Heading).level)
        assertEquals(3, (article.blocks[1] as PivotBlock.Heading).level)
        assertEquals(4, (article.blocks[2] as PivotBlock.Heading).level)
    }

    @Test
    fun `parses simple paragraph as single text inline`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            Ce guide vous permet d'etre operationnel avec Magic Stick en quelques minutes.
        """.trimIndent()

        val article = parser.parse(adoc)

        val para = article.blocks[0] as PivotBlock.Paragraph
        assertEquals(1, para.inline.size)
        val text = para.inline[0] as PivotInline.Text
        assertEquals("Ce guide vous permet d'etre operationnel avec Magic Stick en quelques minutes.", text.text)
        assertTrue(text.translatable)
    }

    @Test
    fun `parses paragraph with bold segment`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            avec **balenaEtcher** (recommande) :
        """.trimIndent()

        val article = parser.parse(adoc)

        val para = article.blocks[0] as PivotBlock.Paragraph
        assertEquals(3, para.inline.size)
        assertEquals("avec ", (para.inline[0] as PivotInline.Text).text)
        assertEquals("balenaEtcher", (para.inline[1] as PivotInline.Bold).text)
        assertEquals(" (recommande) :", (para.inline[2] as PivotInline.Text).text)
    }

    @Test
    fun `parses paragraph with inline code`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            Verifiez le device avec `lsblk` avant.
        """.trimIndent()

        val article = parser.parse(adoc)

        val para = article.blocks[0] as PivotBlock.Paragraph
        val segments = para.inline
        assertEquals(3, segments.size)
        assertEquals("Verifiez le device avec ", (segments[0] as PivotInline.Text).text)
        assertEquals("lsblk", (segments[1] as PivotInline.Code).text)
        assertEquals(" avant.", (segments[2] as PivotInline.Text).text)
    }

    @Test
    fun `parses paragraph with link inline`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            Pour le guide complet avec balenaEtcher et dd, consultez link:flash-guide.html[Guide d'installation et flashage USB].
        """.trimIndent()

        val article = parser.parse(adoc)

        val para = article.blocks[0] as PivotBlock.Paragraph
        assertEquals(3, para.inline.size)
        assertEquals("Pour le guide complet avec balenaEtcher et dd, consultez ", (para.inline[0] as PivotInline.Text).text)
        val link = para.inline[1] as PivotInline.Link
        assertEquals("flash-guide.html", link.url)
        assertEquals("Guide d'installation et flashage USB", link.label)
        assertEquals(".", (para.inline[2] as PivotInline.Text).text)
    }

    @Test
    fun `parses unordered list with single link item`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            * https://sourceforge.net/projects/magic-stick/files/[magic-stick sur SourceForge]
        """.trimIndent()

        val article = parser.parse(adoc)

        val list = article.blocks[0] as PivotBlock.ListBlock
        assertEquals(false, list.ordered)
        assertEquals(1, list.items.size)
        val link = list.items[0][0] as PivotInline.Link
        assertEquals("https://sourceforge.net/projects/magic-stick/files/", link.url)
        assertEquals("magic-stick sur SourceForge", link.label)
    }

    @Test
    fun `parses ordered list with multiple items`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            . Telechargez https://balena.io/etcher[balenaEtcher]
            . **Flash from file** → selectionnez le fichier ISO `magic-stick_{version}.iso`
            . **Select target** → votre cle USB
            . **Flash!**
        """.trimIndent()

        val article = parser.parse(adoc)

        val list = article.blocks[0] as PivotBlock.ListBlock
        assertEquals(true, list.ordered)
        assertEquals(4, list.items.size)
        assertEquals(2, list.items[0].size)
        assertEquals("Telechargez ", (list.items[0][0] as PivotInline.Text).text)
        assertEquals("balenaEtcher", (list.items[0][1] as PivotInline.Link).label)
        assertEquals(3, list.items[1].size)
        assertEquals("Flash from file", (list.items[1][0] as PivotInline.Bold).text)
    }

    @Test
    fun `parses source block with language`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            [source,bash]
            ----
            sudo dd if=magic-stick_{version}.iso of=/dev/sdX bs=4M status=progress && sync
            ----
        """.trimIndent()

        val article = parser.parse(adoc)

        val src = article.blocks[0] as PivotBlock.Source
        assertEquals("bash", src.language)
        assertTrue(src.content.contains("sudo dd if=magic-stick_{version}.iso"))
    }

    @Test
    fun `parses source block without language`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            [source]
            ----
            /unicorn/home
            /unicorn/usr
            ----
        """.trimIndent()

        val article = parser.parse(adoc)

        val src = article.blocks[0] as PivotBlock.Source
        assertEquals("", src.language)
        assertTrue(src.content.contains("/unicorn/home"))
    }

    @Test
    fun `parses admonition with kind and paragraph`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            [WARNING]
            ====
            Cette operation **efface toutes les donnees** sur la cle USB. Verifiez le device avec `lsblk` avant.
            ====
        """.trimIndent()

        val article = parser.parse(adoc)

        val adm = article.blocks[0] as PivotBlock.Admonition
        assertEquals("WARNING", adm.kind)
        assertEquals(1, adm.blocks.size)
        val para = adm.blocks[0] as PivotBlock.Paragraph
        assertEquals(5, para.inline.size)
        assertEquals("efface toutes les donnees", (para.inline[1] as PivotInline.Bold).text)
    }

    @Test
    fun `parses hr separator`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            ---
        """.trimIndent()

        val article = parser.parse(adoc)

        assertEquals(PivotBlock.Hr, article.blocks[0])
    }

    @Test
    fun `parses table with cols spec header and rows`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            [cols="1,2,3"]
            |===
            | Partition | Taille | Role

            | System A (lecture seule)
            | ~8 Go
            | Systeme live Xubuntu actif (boote par defaut)
            |===
        """.trimIndent()

        val article = parser.parse(adoc)

        val table = article.blocks[0] as PivotBlock.Table
        assertEquals("1,2,3", table.cols)
        assertEquals(3, table.header.size)
        assertEquals("Partition", (table.header[0][0] as PivotInline.Text).text)
        assertEquals(1, table.rows.size)
        assertEquals(3, table.rows[0].size)
        assertEquals("System A (lecture seule)", (table.rows[0][0][0] as PivotInline.Text).text)
        assertEquals("~8 Go", (table.rows[0][1][0] as PivotInline.Text).text)
    }

    @Test
    fun `parses table without cols spec as null cols`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            |===
            | Partition | Device | Taille

            | System A
            | `/dev/sdX1`
            | ~8 Go
            |===
        """.trimIndent()

        val article = parser.parse(adoc)

        val table = article.blocks[0] as PivotBlock.Table
        assertEquals(null, table.cols)
        assertEquals(3, table.header.size)
        assertEquals(1, table.rows.size)
        val code = table.rows[0][1][0] as PivotInline.Code
        assertEquals("/dev/sdX1", code.text)
    }

    @Test
    fun `parses table cell with inline code and text`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            [cols="1,3"]
            |===
            | Option | Description

            | `-c`, `--clean`
            | Nettoyer le repertoire de build avant de construire
            |===
        """.trimIndent()

        val article = parser.parse(adoc)

        val table = article.blocks[0] as PivotBlock.Table
        val cell = table.rows[0][0]
        assertEquals(3, cell.size)
        assertEquals("-c", (cell[0] as PivotInline.Code).text)
        assertEquals(", ", (cell[1] as PivotInline.Text).text)
        assertEquals("--clean", (cell[2] as PivotInline.Code).text)
    }

    @Test
    fun `parses numbered ordered list with digit prefix`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            1. Laissez la cle USB inseree
            2. Redemarrez le PC
            3. Accedez au BIOS/UEFI
        """.trimIndent()

        val article = parser.parse(adoc)

        val list = article.blocks[0] as PivotBlock.ListBlock
        assertEquals(true, list.ordered)
        assertEquals(3, list.items.size)
        assertEquals("Laissez la cle USB inseree", (list.items[0][0] as PivotInline.Text).text)
        assertEquals("Redemarrez le PC", (list.items[1][0] as PivotInline.Text).text)
        assertEquals("Accedez au BIOS/UEFI", (list.items[2][0] as PivotInline.Text).text)
    }

    @Test
    fun `parses multiple blocks in sequence`() {
        val adoc = """
            title=Test
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            == Premier titre

            Premier paragraphe.

            === Deuxieme titre

            * item un
            * item deux
        """.trimIndent()

        val article = parser.parse(adoc)

        assertEquals(4, article.blocks.size)
        assertEquals("Premier titre", (article.blocks[0] as PivotBlock.Heading).text)
        assertTrue(article.blocks[1] is PivotBlock.Paragraph)
        assertEquals("Deuxieme titre", (article.blocks[2] as PivotBlock.Heading).text)
        val list = article.blocks[3] as PivotBlock.ListBlock
        assertEquals(2, list.items.size)
    }
}