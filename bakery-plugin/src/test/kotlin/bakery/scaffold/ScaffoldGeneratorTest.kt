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
 * Tests unitaires pour [ScaffoldGenerator] — domaine metier pur, zero Gradle.
 *
 * Baby-step TDD : chaque test definit une regle metier.
 * Pattern identique a [bakery.article.ArticleGeneratorTest].
 */
class ScaffoldGeneratorTest {

    // ── Sample JSON response from LLM ────────────────────────────────────

    private val sampleBlogJson = """
        {
          "siteType": "blog",
          "projectName": "mon-blog-tech",
          "description": "Blog technique sur Kotlin et Gradle",
          "templates": ["blog.thyme", "post.thyme", "page.thyme", "archive.thyme", "tags.thyme"],
          "metadata": {
            "title": "Mon Blog Tech",
            "description": "Blog technique sur Kotlin et Gradle",
            "tags": ["kotlin", "gradle", "plugin", "tutoriel"],
            "layout": "post",
            "language": "fr"
          }
        }
    """.trimIndent()

    private val samplePortfolioJson = """
        {
          "siteType": "portfolio",
          "projectName": "kotlin-dev-portfolio",
          "description": "Portfolio developpeur Kotlin",
          "templates": ["page.thyme", "project-list.thyme", "project-detail.thyme"],
          "metadata": {
            "title": "Kotlin Dev Portfolio",
            "description": "Portfolio professionnel Kotlin",
            "tags": ["portfolio", "kotlin", "developpement"],
            "layout": "page",
            "language": "en"
          }
        }
    """.trimIndent()

    @Test
    fun `generate produces ScaffoldOutput with parsed siteType`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleBlogJson)
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(description = "Un blog tech")
        val output = generator.generate(intention, fakeLlm)

        assertNotNull(output)
        assertEquals(ScaffoldSiteType.BLOG, output.siteType)
    }

    @Test
    fun `generate produces ScaffoldOutput with parsed projectName`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleBlogJson)
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(description = "Un blog tech")
        val output = generator.generate(intention, fakeLlm)

        assertEquals("mon-blog-tech", output.projectName)
    }

    @Test
    fun `generate produces ScaffoldOutput with parsed description`() = runBlocking {
        val fakeLlm = FakeLlmService(samplePortfolioJson)
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(description = "Portfolio Kotlin", siteType = ScaffoldSiteType.PORTFOLIO)
        val output = generator.generate(intention, fakeLlm)

        assertEquals("Portfolio developpeur Kotlin", output.description)
    }

    @Test
    fun `generate produces ScaffoldOutput with parsed templates`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleBlogJson)
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(description = "Blog")
        val output = generator.generate(intention, fakeLlm)

        assertTrue(output.templates.contains("blog.thyme"), "Must contain blog.thyme")
        assertTrue(output.templates.contains("post.thyme"), "Must contain post.thyme")
    }

    @Test
    fun `generate produces ScaffoldOutput with parsed metadata`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleBlogJson)
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(description = "Blog Kotlin")
        val output = generator.generate(intention, fakeLlm)

        assertEquals("Mon Blog Tech", output.metadata.title)
        assertEquals("Blog technique sur Kotlin et Gradle", output.metadata.description)
        assertEquals(listOf("kotlin", "gradle", "plugin", "tutoriel"), output.metadata.tags)
        assertEquals("post", output.metadata.layout)
        assertEquals("fr", output.metadata.language)
    }

    @Test
    fun `generate sends prompt containing the description to LLM`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleBlogJson)
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(description = "Site de documentation API")
        generator.generate(intention, fakeLlm)

        assertTrue(fakeLlm.promptsReceived.isNotEmpty())
        assertTrue(fakeLlm.promptsReceived.first().contains("Site de documentation API"))
    }

    @Test
    fun `generate sends prompt containing the site type to LLM`() = runBlocking {
        val fakeLlm = FakeLlmService(samplePortfolioJson)
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(
            description = "Portfolio",
            siteType = ScaffoldSiteType.PORTFOLIO
        )
        generator.generate(intention, fakeLlm)

        val prompt = fakeLlm.promptsReceived.first()
        assertTrue(prompt.contains("portfolio"), "Prompt must contain site type")
    }

    @Test
    fun `generate sends prompt requesting JSON format`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleBlogJson)
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(description = "Test")
        generator.generate(intention, fakeLlm)

        val prompt = fakeLlm.promptsReceived.first()
        assertTrue(prompt.contains("JSON"), "Prompt must request JSON format")
        assertTrue(prompt.contains("siteType"), "Prompt must mention siteType field")
        assertTrue(prompt.contains("templates"), "Prompt must mention templates field")
    }

    @Test
    fun `generate handles empty response gracefully`() = runBlocking {
        val fakeLlm = FakeLlmService("") // reponse vide
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(
            description = "Mon site",
            siteType = ScaffoldSiteType.BLOG,
            projectName = "mon-site"
        )
        val output = generator.generate(intention, fakeLlm)

        assertNotNull(output)
        assertEquals(ScaffoldSiteType.BLOG, output.siteType, "Fallback must use intention siteType")
        assertEquals("mon-site", output.projectName, "Fallback must use intention projectName")
        assertTrue(output.templates.isNotEmpty(), "Fallback must have default templates")
    }

    @Test
    fun `generate handles invalid JSON gracefully`() = runBlocking {
        val fakeLlm = FakeLlmService("This is not JSON at all!") // reponse invalide
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(
            description = "Blog Kotlin",
            siteType = ScaffoldSiteType.BLOG
        )
        val output = generator.generate(intention, fakeLlm)

        assertNotNull(output)
        assertEquals(ScaffoldSiteType.BLOG, output.siteType, "Fallback must use BLOG")
        assertTrue(output.templates.isNotEmpty(), "Fallback must have default templates")
    }

    @Test
    fun `generate handles JSON with missing fields using intention defaults`() = runBlocking {
        val partialJson = """
            {
              "siteType": "doc",
              "projectName": "my-docs"
            }
        """.trimIndent()
        val fakeLlm = FakeLlmService(partialJson)
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(
            description = "Documentation technique",
            siteType = ScaffoldSiteType.DOC
        )
        val output = generator.generate(intention, fakeLlm)

        assertEquals(ScaffoldSiteType.DOC, output.siteType)
        assertEquals("my-docs", output.projectName)
        assertTrue(output.templates.isNotEmpty(), "Must have default templates for DOC")
    }

    // ── buildPrompt ──────────────────────────────────────────────────────

    @Test
    fun `buildPrompt includes description and siteType`() {
        val generator = ScaffoldGenerator()
        val intention = ScaffoldIntention(
            description = "Site de formation Kotlin",
            siteType = ScaffoldSiteType.FORMATION,
            lang = "fr",
            projectName = "kotlin-training"
        )
        val prompt = generator.buildPrompt(intention)

        assertTrue(prompt.contains("Site de formation Kotlin"))
        assertTrue(prompt.contains("formation"))
        assertTrue(prompt.contains("fr"))
        assertTrue(prompt.contains("kotlin-training"))
    }

    @Test
    fun `buildPrompt lists available templates per site type`() {
        val generator = ScaffoldGenerator()
        val intention = ScaffoldIntention(description = "Test", siteType = ScaffoldSiteType.BLOG)
        val prompt = generator.buildPrompt(intention)

        assertTrue(prompt.contains("blog.thyme"), "Must list blog templates")
        assertTrue(prompt.contains("post.thyme"), "Must list post template")
    }

    @Test
    fun `buildPrompt requests tree hierarchy with type discriminator`() {
        val generator = ScaffoldGenerator()
        val intention = ScaffoldIntention(
            description = "Formation FPA",
            siteType = ScaffoldSiteType.FORMATION
        )
        val prompt = generator.buildPrompt(intention)

        assertTrue(prompt.contains("\"tree\""), "Prompt must mention tree field")
        assertTrue(prompt.contains("\"type\""), "Prompt must mention type discriminator")
        assertTrue(prompt.contains("\"site\""), "Prompt must mention site type")
        assertTrue(prompt.contains("\"section\""), "Prompt must mention section type")
        assertTrue(prompt.contains("\"article\""), "Prompt must mention article type")
        assertTrue(prompt.contains("Site→Section→Article"), "Prompt must explain tree hierarchy")
    }

    // ── extractJson ──────────────────────────────────────────────────────

    @Test
    fun `extractJson extracts JSON object from response with surrounding text`() {
        val generator = ScaffoldGenerator()
        val response = """
            Voici la structure de site demandee :
            ${sampleBlogJson}
            N'hesitez pas a me contacter pour des modifications.
        """.trimIndent()

        val json = generator.extractJson(response)
        assertTrue(json.startsWith("{"), "Must start with {")
        assertTrue(json.endsWith("}"), "Must end with }")
        assertTrue(json.contains("siteType"), "Must contain siteType key")
    }

    @Test
    fun `extractJson throws when no JSON found`() {
        val generator = ScaffoldGenerator()

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            generator.extractJson("No JSON here, just plain text.")
        }
    }

    // ── Fallback defaults per site type ──────────────────────────────────

    @Test
    fun `fallback for BLOG type provides blog templates`() = runBlocking {
        val fakeLlm = FakeLlmService("") // empty response triggers fallback
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(
            description = "Mon blog",
            siteType = ScaffoldSiteType.BLOG,
            projectName = "mon-blog"
        )
        val output = generator.generate(intention, fakeLlm)

        assertEquals(listOf("blog.thyme", "post.thyme", "page.thyme"), output.templates)
    }

    @Test
    fun `fallback for DOC type provides documentation templates`() = runBlocking {
        val fakeLlm = FakeLlmService("")
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(
            description = "Ma doc",
            siteType = ScaffoldSiteType.DOC,
            projectName = "ma-doc"
        )
        val output = generator.generate(intention, fakeLlm)

        assertEquals(listOf("page.thyme", "section.thyme", "search.thyme"), output.templates)
    }

    @Test
    fun `fallback for FORMATION type provides formation templates`() = runBlocking {
        val fakeLlm = FakeLlmService("")
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(
            description = "Ma formation",
            siteType = ScaffoldSiteType.FORMATION,
            projectName = "ma-formation"
        )
        val output = generator.generate(intention, fakeLlm)

        assertEquals(listOf("page.thyme", "session.thyme", "module.thyme"), output.templates)
    }

    @Test
    fun `fallback for PORTFOLIO type provides portfolio templates`() = runBlocking {
        val fakeLlm = FakeLlmService("")
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(
            description = "Mon portfolio",
            siteType = ScaffoldSiteType.PORTFOLIO,
            projectName = "mon-portfolio"
        )
        val output = generator.generate(intention, fakeLlm)

        assertEquals(listOf("page.thyme", "project-list.thyme", "project-detail.thyme"), output.templates)
    }

    @Test
    fun `fallback uses description as projectName when projectName is blank`() = runBlocking {
        val fakeLlm = FakeLlmService("")
        val generator = ScaffoldGenerator()

        val intention = ScaffoldIntention(
            description = "Mon Super Blog",
            siteType = ScaffoldSiteType.BLOG,
            projectName = ""
        )
        val output = generator.generate(intention, fakeLlm)

        assertEquals("mon-super-blog", output.projectName, "Must slugify description as fallback projectName")
    }

    // ── TREE-7: Scaffold IA returns tree ─────────────────────────────────

    @Test
    fun `generate produces ScaffoldOutput with tree when LLM returns tree`() = runBlocking {
        val json = """
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
                    "path": "modules",
                    "articles": [
                      {"type": "article", "path": "modules/intro"},
                      {"type": "article", "path": "modules/avance"}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()
        val fakeLlm = FakeLlmService(json)
        val generator = ScaffoldGenerator()
        val intention = ScaffoldIntention(description = "Formation", siteType = ScaffoldSiteType.FORMATION)

        val output = generator.generate(intention, fakeLlm)

        assertNotNull(output.tree)
        assertTrue(output.tree is SiteNode.Site)
        val site = output.tree as SiteNode.Site
        assertEquals(1, site.sections.size)
        assertEquals("modules", site.sections.first().path)
        assertEquals(2, site.sections.first().articles.size)
    }

    @Test
    fun `generate derives templates from tree when tree is present`() = runBlocking {
        val json = """
            {
              "siteType": "blog",
              "projectName": "mon-blog",
              "description": "Blog",
              "tree": {
                "type": "site",
                "path": "",
                "sections": [
                  {
                    "type": "section",
                    "path": "articles",
                    "articles": [
                      {"type": "article", "path": "articles/a"},
                      {"type": "article", "path": "articles/b"}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()
        val fakeLlm = FakeLlmService(json)
        val generator = ScaffoldGenerator()
        val intention = ScaffoldIntention(description = "Blog", siteType = ScaffoldSiteType.BLOG)

        val output = generator.generate(intention, fakeLlm)

        assertEquals(listOf("articles/a.thyme", "articles/b.thyme"), output.templates)
    }

    @Test
    fun `generate sets tree to null when LLM response has no tree`() = runBlocking {
        val json = """
            {
              "siteType": "blog",
              "projectName": "mon-blog",
              "description": "Blog",
              "templates": ["blog.thyme", "post.thyme"]
            }
        """.trimIndent()
        val fakeLlm = FakeLlmService(json)
        val generator = ScaffoldGenerator()
        val intention = ScaffoldIntention(description = "Blog", siteType = ScaffoldSiteType.BLOG)

        val output = generator.generate(intention, fakeLlm)

        assertNull(output.tree)
    }

    @Test
    fun `generate uses tree over legacy templates when both are present`() = runBlocking {
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
                  {
                    "type": "section",
                    "path": "docs",
                    "articles": [
                      {"type": "article", "path": "docs/intro"}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()
        val fakeLlm = FakeLlmService(json)
        val generator = ScaffoldGenerator()
        val intention = ScaffoldIntention(description = "Blog", siteType = ScaffoldSiteType.BLOG)

        val output = generator.generate(intention, fakeLlm)

        assertNotNull(output.tree)
        assertEquals(listOf("docs/intro.thyme"), output.templates)
    }

    @Test
    fun `parseResponse handles compact JSON without spaces`() {
        val generator = ScaffoldGenerator()
        val compactJson = """{"siteType":"blog","projectName":"mon-blog","description":"Mon blog technique","templates":["blog.thyme","post.thyme"],"metadata":{"title":"Mon Blog","description":"Blog technique","tags":["kotlin"],"layout":"post","language":"fr"}}"""
        val intention = ScaffoldIntention(description = "test", projectName = "fallback")

        val output = generator.parseResponse(compactJson, intention)

        assertEquals(ScaffoldSiteType.BLOG, output.siteType)
        assertEquals("mon-blog", output.projectName)
        assertEquals("Mon blog technique", output.description)
        assertEquals(listOf("blog.thyme", "post.thyme"), output.templates)
        assertEquals("Mon Blog", output.metadata.title)
        assertEquals(listOf("kotlin"), output.metadata.tags)
    }
}