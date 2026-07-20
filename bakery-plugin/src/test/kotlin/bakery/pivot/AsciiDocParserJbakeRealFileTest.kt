package bakery.pivot

import document.translation.AsciiDocParser
import document.translation.AsciiDocRenderer
import document.translation.PivotBlock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AsciiDocParserJbakeRealFileTest {

    private val parser = AsciiDocParser()
    private val renderer = AsciiDocRenderer()

    @Test
    fun `parses real cheroliv com 2019 article with jbake native header`() {
        val adoc = """
= Groovy: Caractères ASCII
@CherOliv
2019-07-10
:jbake-title: Groovy: Caractères ASCII
:jbake-tags: blog, Groovy, ASCII, string, char
:jbake-type: post
:jbake-status: published
:jbake-date: 2019-07-10
:summary: du groovy, des boucles et de la manipulation de code ascii

Voici un bout de code fonctionnel en Groovy, qui génère un fichier texte,
avec les 256 premiers caractères lisibles du tableau ASCII +
[source,groovy]
----
#!/usr/bin/env groovy
import java.nio.charset.StandardCharsets

List<Character> chars = new ArrayList<>()
----

j'ai nommé le fichier spe_char.groovy,
depuis le dossier ou est le fichier +
ouvrir un terminal et copier coller pour exécuter le script +
[source,bash]
----
${'$'} groovy spe_char.groovy
----

résultat:
----
  0  1  2  3  4  5  6  7  8  9
----
""".trimIndent()

        val article = parser.parse(adoc)

        assertEquals("Groovy: Caractères ASCII", article.frontmatter.title)
        assertEquals("2019-07-10", article.frontmatter.date)
        assertEquals("post", article.frontmatter.type)
        assertEquals("published", article.frontmatter.status)

        assertTrue(article.blocks.isNotEmpty(), "Blocks should not be empty")
        val firstBlock = article.blocks[0]
        assertTrue(firstBlock is PivotBlock.Paragraph,
            "First block should be a paragraph, got $firstBlock")

        val sourceBlocks = article.blocks.filterIsInstance<PivotBlock.Source>()
        assertTrue(sourceBlocks.size >= 2,
            "Should have at least 2 source blocks, got ${sourceBlocks.size}")
        assertEquals("groovy", sourceBlocks[0].language)
        assertEquals("bash", sourceBlocks[1].language)
    }

    @Test
    fun `roundtrip real jbake article preserves title and block count`() {
        val adoc = """
= Test Real Article
@CherOliv
2020-06-15
:jbake-type: post
:jbake-status: published

== Introduction

Premier paragraphe d'introduction.

[source,kotlin]
----
fun main() {
    println("Hello")
}
----

== Conclusion

Dernier paragraphe.
""".trimIndent()

        val article = parser.parse(adoc)
        val rendered = renderer.render(article)
        val reparsed = parser.parse(rendered)

        assertEquals(article.frontmatter.title, reparsed.frontmatter.title)
        assertEquals(article.blocks.size, reparsed.blocks.size)
    }
}