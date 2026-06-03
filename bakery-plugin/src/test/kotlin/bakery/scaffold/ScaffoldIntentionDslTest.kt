package bakery.scaffold

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests unitaires pour [ScaffoldIntentionDsl] — bridge Gradle → domaine.
 *
 * Baby-step TDD : chaque test valide que la DSL produit une [ScaffoldIntention] correcte.
 */
class ScaffoldIntentionDslTest {

    @Test
    fun `toIntention converts DSL values to domain model`() {
        val dsl = ScaffoldIntentionDsl().apply {
            description = "Mon site de portfolio"
            siteType = "portfolio"
            lang = "en"
            projectName = "my-portfolio"
        }

        val intention = dsl.toIntention()

        assertEquals("Mon site de portfolio", intention.description)
        assertEquals(ScaffoldSiteType.PORTFOLIO, intention.siteType)
        assertEquals("en", intention.lang)
        assertEquals("my-portfolio", intention.projectName)
    }

    @Test
    fun `toIntention uses defaults for unspecified values`() {
        val dsl = ScaffoldIntentionDsl().apply {
            description = "Un blog"
        }

        val intention = dsl.toIntention()

        assertEquals("Un blog", intention.description)
        assertEquals(ScaffoldSiteType.BLOG, intention.siteType)
        assertEquals("fr", intention.lang)
        assertEquals("", intention.projectName)
    }

    @Test
    fun `toIntention throws when description is blank`() {
        val dsl = ScaffoldIntentionDsl()

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            dsl.toIntention()
        }
    }

    @Test
    fun `toIntention resolves site type case insensitive`() {
        val dsl = ScaffoldIntentionDsl().apply {
            description = "Test"
            siteType = "FORMATION"
        }

        val intention = dsl.toIntention()
        assertEquals(ScaffoldSiteType.FORMATION, intention.siteType)
    }

    @Test
    fun `toIntention falls back to BLOG for unknown site type`() {
        val dsl = ScaffoldIntentionDsl().apply {
            description = "Test"
            siteType = "unknown-type"
        }

        val intention = dsl.toIntention()
        assertEquals(ScaffoldSiteType.BLOG, intention.siteType)
    }
}