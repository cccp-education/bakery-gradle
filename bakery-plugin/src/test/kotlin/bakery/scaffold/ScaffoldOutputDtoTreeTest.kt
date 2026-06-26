package bakery.scaffold

import bakery.tree.SiteNode.Article
import bakery.tree.SiteNode.Section
import bakery.tree.SiteNode.Site
import bakery.tree.SiteNodeDto
import bakery.tree.toDomain
import bakery.tree.toDto
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests unitaires pour le parsing Jackson de `tree` dans [ScaffoldOutputDto].
 *
 * Baby-step TDD : le LLM peut retourner un arbre hierarchique JSON dans le
 * champ `tree`. Jackson polymorphic (SiteNodeDto) deserialise ce bloc.
 * Backward compat : `tree` absent = null (legacy templates liste plate).
 */
class ScaffoldOutputDtoTreeTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `DTO without tree field yields null tree`() {
        val json = """{"siteType":"blog","projectName":"mon-blog","templates":["blog.thyme"]}"""

        val dto = mapper.readValue(json, ScaffoldOutputDto::class.java)

        assertNull(dto.tree)
    }

    @Test
    fun `DTO with tree field deserializes to SiteNodeDto Site`() {
        val json = """
            {
              "siteType": "blog",
              "projectName": "mon-blog",
              "tree": {
                "type": "site",
                "path": "",
                "sections": [
                  {
                    "type": "section",
                    "path": "blog",
                    "articles": [
                      {"type": "article", "path": "blog/hello"}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val dto = mapper.readValue(json, ScaffoldOutputDto::class.java)

        val tree = dto.tree
        assertTrue(tree is SiteNodeDto.SiteDto)
        assertEquals("", tree.path)
        assertEquals(1, tree.sections.size)
        assertEquals("blog", tree.sections.first().path)
        assertEquals(1, tree.sections.first().articles.size)
        assertEquals("blog/hello", tree.sections.first().articles.first().path)
    }

    @Test
    fun `DTO tree with 3 levels round-trips to domain and back`() {
        val json = """
            {
              "siteType": "formation",
              "projectName": "ma-formation",
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
              }
            }
        """.trimIndent()

        val dto = mapper.readValue(json, ScaffoldOutputDto::class.java)
        val domain = dto.tree!!.toDomain()
        val backToDto = domain.toDto()

        assertTrue(domain is Site)
        assertEquals(2, domain.sections.size)
        val formations = domain.sections.first { it.path == "formations" }
        assertEquals(2, formations.articles.size)
        assertEquals("formations/ab-partition", formations.articles.first().path)

        assertEquals(dto.tree, backToDto)
    }

    @Test
    fun `DTO tree with empty sections yields site with no articles`() {
        val json = """
            {
              "siteType": "blog",
              "projectName": "empty-site",
              "tree": {
                "type": "site",
                "path": "",
                "sections": []
              }
            }
        """.trimIndent()

        val dto = mapper.readValue(json, ScaffoldOutputDto::class.java)
        val domain = dto.tree!!.toDomain()

        assertTrue(domain is Site)
        assertTrue(domain.sections.isEmpty())
    }
}