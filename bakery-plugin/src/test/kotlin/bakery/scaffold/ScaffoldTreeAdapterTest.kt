package bakery.scaffold

import bakery.tree.SiteNode.Article
import bakery.tree.SiteNode.Section
import bakery.tree.SiteNode.Site
import bakery.tree.flattenTemplates
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests unitaires pour [flattenTemplates] — domaine pur, zero Gradle.
 *
 * Baby-step TDD : tree → liste plate de templates (backward compat scaffold legacy).
 * Convention : article path = template name (ex "blog/hello" → "blog/hello.thyme").
 */
class ScaffoldTreeAdapterTest {

    @Test
    fun `flattenTemplates of a 3-level tree yields one template per article`() {
        val ab = Article(path = "formations/ab-partition")
        val cd = Article(path = "formations/cd-partition")
        val hello = Article(path = "blog/hello")
        val formations = Section(path = "formations", articles = listOf(ab, cd))
        val blog = Section(path = "blog", articles = listOf(hello))
        val site = Site(path = "", sections = listOf(formations, blog))

        val templates = site.flattenTemplates()

        assertEquals(3, templates.size)
        assertTrue(templates.contains("formations/ab-partition.thyme"))
        assertTrue(templates.contains("formations/cd-partition.thyme"))
        assertTrue(templates.contains("blog/hello.thyme"))
    }

    @Test
    fun `flattenTemplates of empty tree yields empty list`() {
        val site = Site(path = "", sections = emptyList())

        val templates = site.flattenTemplates()

        assertTrue(templates.isEmpty())
    }

    @Test
    fun `flattenTemplates of a single article tree yields one template`() {
        val hello = Article(path = "blog/hello")
        val blog = Section(path = "blog", articles = listOf(hello))
        val site = Site(path = "", sections = listOf(blog))

        val templates = site.flattenTemplates()

        assertEquals(listOf("blog/hello.thyme"), templates)
    }

    @Test
    fun `flattenTemplates of a tree with empty sections yields empty list`() {
        val formations = Section(path = "formations", articles = emptyList())
        val blog = Section(path = "blog", articles = emptyList())
        val site = Site(path = "", sections = listOf(formations, blog))

        val templates = site.flattenTemplates()

        assertTrue(templates.isEmpty())
    }

    @Test
    fun `flattenTemplates preserves article order from tree walk`() {
        val first = Article(path = "section/first")
        val second = Article(path = "section/second")
        val third = Article(path = "section/third")
        val section = Section(path = "section", articles = listOf(first, second, third))
        val site = Site(path = "", sections = listOf(section))

        val templates = site.flattenTemplates()

        assertEquals(
            listOf("section/first.thyme", "section/second.thyme", "section/third.thyme"),
            templates
        )
    }

    @Test
    fun `flattenTemplates appends thyme extension to each article path`() {
        val article = Article(path = "docs/intro")
        val docs = Section(path = "docs", articles = listOf(article))
        val site = Site(path = "", sections = listOf(docs))

        val templates = site.flattenTemplates()

        assertEquals(listOf("docs/intro.thyme"), templates)
    }
}