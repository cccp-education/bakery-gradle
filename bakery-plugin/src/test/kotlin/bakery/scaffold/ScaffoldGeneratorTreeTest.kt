package bakery.scaffold

import bakery.llm.FakeLlmService
import bakery.tree.SiteNode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests unitaires TREE-7 — ScaffoldGenerator parse un arbre hierarchique
 * quand le LLM retourne `tree`, fallback `templates` sinon.
 *
 * Baby-step TDD : chaque test definit une regle metier.
 */
class ScaffoldGeneratorTreeTest {

    private val sampleTreeJson = """
        {
          "siteType": "formation",
          "projectName": "ma-formation",
          "description": "Formation FPA",
          "tree": {
            "type": "site",
            "path": "",
            "sections": [
              {
                "type": "section",
                "path": "formations",
                "articles": [
                  {"type": "article", "path": "formations/ab-partition"},
                  {"type": "article", "path": "formations/cd-partition"}
                ]
              },
              {
                "type": "section",
                "path": "blog",
                "articles": [
                  {"type": "article", "path": "blog/hello"}
                ]
              }
            ]
          },
          "metadata": {
            "title": "Ma Formation",
            "description": "Formation FPA",
            "tags": ["fpa", "formation"],
            "layout": "page",
            "language": "fr"
          }
        }
    """.trimIndent()

    @Test
    fun `generate parses tree when present in LLM response`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleTreeJson)
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(description = "Formation FPA", siteType = ScaffoldSiteType.FORMATION)
        val output = generator.generate(intention, fakeLlm)

        assertNotNull(output.tree)
        assertTrue(output.tree is SiteNode.Site)
        val site = output.tree as SiteNode.Site
        assertEquals(2, site.sections.size)
        assertEquals("formations", site.sections.first().path)
    }

    @Test
    fun `generate derives templates from tree via flatten when tree present`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleTreeJson)
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(description = "Formation FPA", siteType = ScaffoldSiteType.FORMATION)
        val output = generator.generate(intention, fakeLlm)

        assertTrue(output.templates.contains("formations/ab-partition.thyme"))
        assertTrue(output.templates.contains("formations/cd-partition.thyme"))
        assertTrue(output.templates.contains("blog/hello.thyme"))
        assertEquals(3, output.templates.size)
    }

    @Test
    fun `generate falls back to templates list when tree absent`() = runBlocking {
        val json = """
            {
              "siteType": "blog",
              "projectName": "mon-blog",
              "description": "Blog",
              "templates": ["blog.thyme", "post.thyme"],
              "metadata": {"title": "Blog", "description": "Blog", "tags": ["x"], "layout": "post", "language": "fr"}
            }
        """.trimIndent()
        val fakeLlm = FakeLlmService(json)
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(description = "Blog")
        val output = generator.generate(intention, fakeLlm)

        assertNull(output.tree)
        assertEquals(listOf("blog.thyme", "post.thyme"), output.templates)
    }

    @Test
    fun `generate tree present but templates absent uses flatten only`() = runBlocking {
        val json = """
            {
              "siteType": "blog",
              "projectName": "mon-blog",
              "description": "Blog",
              "tree": {
                "type": "site",
                "path": "",
                "sections": [
                  {"type": "section", "path": "blog", "articles": [{"type": "article", "path": "blog/hello"}]}
                ]
              }
            }
        """.trimIndent()
        val fakeLlm = FakeLlmService(json)
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(description = "Blog")
        val output = generator.generate(intention, fakeLlm)

        assertNotNull(output.tree)
        assertEquals(listOf("blog/hello.thyme"), output.templates)
    }

    @Test
    fun `generate prefers tree over templates when both present`() = runBlocking {
        val json = """
            {
              "siteType": "blog",
              "projectName": "mon-blog",
              "description": "Blog",
              "templates": ["legacy.thyme"],
              "tree": {
                "type": "site",
                "path": "",
                "sections": [
                  {"type": "section", "path": "docs", "articles": [{"type": "article", "path": "docs/intro"}]}
                ]
              }
            }
        """.trimIndent()
        val fakeLlm = FakeLlmService(json)
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(description = "Blog")
        val output = generator.generate(intention, fakeLlm)

        assertNotNull(output.tree)
        assertEquals(listOf("docs/intro.thyme"), output.templates)
    }

    @Test
    fun `buildPrompt requests hierarchical tree structure`() {
        val generator = ScaffoldGenerator()
        val intention = ScaffoldIntention(description = "Formation", siteType = ScaffoldSiteType.FORMATION)
        val prompt = generator.buildPrompt(intention)

        assertTrue(prompt.contains("tree"), "Prompt must request tree structure")
        assertTrue(prompt.contains("section"), "Prompt must mention sections")
        assertTrue(prompt.contains("article"), "Prompt must mention articles")
    }

    @Test
    fun `buildPrompt shows tree JSON example with type discriminator`() {
        val generator = ScaffoldGenerator()
        val intention = ScaffoldIntention(description = "Test")
        val prompt = generator.buildPrompt(intention)

        assertTrue(prompt.contains("\"type\""), "Prompt must show type discriminator")
        assertTrue(prompt.contains("\"site\""), "Prompt must show site type")
    }

    @Test
    fun `fallback output has null tree`() = runBlocking {
        val fakeLlm = FakeLlmService("")
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(description = "Mon site", projectName = "mon-site")
        val output = generator.generate(intention, fakeLlm)

        assertNull(output.tree)
        assertTrue(output.templates.isNotEmpty())
    }
}