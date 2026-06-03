package bakery.scaffold

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests unitaires pour [ScaffoldIntention] et [ScaffoldSiteType] — modele domaine DDD.
 *
 * Baby-step TDD : chaque test definit une regle metier avant l'implementation.
 * Pattern identique a [bakery.article.ArticleIntentionTest].
 */
class ScaffoldIntentionTest {

    // ── Construction valide ────────────────────────────────────────────

    @Test
    fun `minimal intention with description only has default values`() {
        val intention = ScaffoldIntention(description = "Un site de blog personnel")

        assertEquals("Un site de blog personnel", intention.description)
        assertEquals(ScaffoldSiteType.BLOG, intention.siteType)
        assertEquals("fr", intention.lang)
        assertEquals("", intention.projectName)
    }

    @Test
    fun `full intention with all parameters`() {
        val intention = ScaffoldIntention(
            description = "Portfolio professionnel Kotlin",
            siteType = ScaffoldSiteType.PORTFOLIO,
            lang = "en",
            projectName = "kotlin-dev-portfolio"
        )

        assertEquals("Portfolio professionnel Kotlin", intention.description)
        assertEquals(ScaffoldSiteType.PORTFOLIO, intention.siteType)
        assertEquals("en", intention.lang)
        assertEquals("kotlin-dev-portfolio", intention.projectName)
    }

    // ── Validation description obligatoire ──────────────────────────────

    @Test
    fun `intention requires non-blank description`() {
        assertThrows<IllegalArgumentException> {
            ScaffoldIntention(description = "")
        }
    }

    @Test
    fun `intention requires non-whitespace-only description`() {
        assertThrows<IllegalArgumentException> {
            ScaffoldIntention(description = "   ")
        }
    }

    // ── Enum ScaffoldSiteType ─────────────────────────────────────────

    @Test
    fun `ScaffoldSiteType has expected values`() {
        val types = ScaffoldSiteType.entries
        assertEquals(4, types.size)
        assertTrue(types.contains(ScaffoldSiteType.BLOG))
        assertTrue(types.contains(ScaffoldSiteType.PORTFOLIO))
        assertTrue(types.contains(ScaffoldSiteType.DOC))
        assertTrue(types.contains(ScaffoldSiteType.FORMATION))
    }

    @Test
    fun `ScaffoldSiteType has readable labels`() {
        assertEquals("blog", ScaffoldSiteType.BLOG.label)
        assertEquals("portfolio", ScaffoldSiteType.PORTFOLIO.label)
        assertEquals("documentation", ScaffoldSiteType.DOC.label)
        assertEquals("formation", ScaffoldSiteType.FORMATION.label)
    }

    @Test
    fun `ScaffoldSiteType fromStringOrDefault returns BLOG for null`() {
        assertEquals(ScaffoldSiteType.BLOG, ScaffoldSiteType.fromStringOrDefault(null))
    }

    @Test
    fun `ScaffoldSiteType fromStringOrDefault returns BLOG for blank`() {
        assertEquals(ScaffoldSiteType.BLOG, ScaffoldSiteType.fromStringOrDefault(""))
        assertEquals(ScaffoldSiteType.BLOG, ScaffoldSiteType.fromStringOrDefault("   "))
    }

    @Test
    fun `ScaffoldSiteType fromStringOrDefault resolves valid types case insensitive`() {
        assertEquals(ScaffoldSiteType.PORTFOLIO, ScaffoldSiteType.fromStringOrDefault("portfolio"))
        assertEquals(ScaffoldSiteType.PORTFOLIO, ScaffoldSiteType.fromStringOrDefault("PORTFOLIO"))
        assertEquals(ScaffoldSiteType.PORTFOLIO, ScaffoldSiteType.fromStringOrDefault("Portfolio"))
        assertEquals(ScaffoldSiteType.DOC, ScaffoldSiteType.fromStringOrDefault("doc"))
        assertEquals(ScaffoldSiteType.FORMATION, ScaffoldSiteType.fromStringOrDefault("formation"))
    }

    @Test
    fun `ScaffoldSiteType fromStringOrDefault returns BLOG for unknown type`() {
        assertEquals(ScaffoldSiteType.BLOG, ScaffoldSiteType.fromStringOrDefault("unknown"))
    }

    // ── Lang validation ──────────────────────────────────────────────

    @Test
    fun `lang defaults to fr`() {
        val intention = ScaffoldIntention(description = "Test")
        assertEquals("fr", intention.lang)
    }

    @Test
    fun `lang accepts en and fr only`() {
        val frIntention = ScaffoldIntention(description = "Test", lang = "fr")
        val enIntention = ScaffoldIntention(description = "Test", lang = "en")
        assertEquals("fr", frIntention.lang)
        assertEquals("en", enIntention.lang)
    }

    @Test
    fun `lang rejects unsupported language`() {
        assertThrows<IllegalArgumentException> {
            ScaffoldIntention(description = "Test", lang = "de")
        }
    }

    // ── toPromptContext ────────────────────────────────────────────────

    @Test
    fun `toPromptContext includes description and site type`() {
        val intention = ScaffoldIntention(
            description = "Site de documentation technique",
            siteType = ScaffoldSiteType.DOC,
            lang = "en",
            projectName = "kotlin-docs"
        )
        val context = intention.toPromptContext()

        assertTrue(context.contains("Site de documentation technique"), "Must contain description")
        assertTrue(context.contains("documentation"), "Must contain site type label")
        assertTrue(context.contains("en"), "Must contain language")
        assertTrue(context.contains("kotlin-docs"), "Must contain project name")
    }

    @Test
    fun `toPromptContext with minimal intention omits project name`() {
        val intention = ScaffoldIntention(description = "Un simple blog")
        val context = intention.toPromptContext()

        assertTrue(context.contains("Un simple blog"))
        assertTrue(context.contains("blog"))
        assertTrue(context.contains("fr"))
    }
}