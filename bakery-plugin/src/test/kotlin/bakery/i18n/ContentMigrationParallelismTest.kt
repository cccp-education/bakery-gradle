package bakery.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/**
 * CHE-I18N-22 US-4/US-8 — the content migration parallelism must be drivable from
 * the CLI (`--contentI18nParallelism`), so a consumer can run the translation on
 * several Ollama providers at once.
 *
 * Pilot decision S-044: the ceiling is the number of ports the pool can serve at
 * once — **twenty-five** healthy instances on 11437-11465 (29 ports, minus three
 * without a container and one whose account hit its monthly quota). This
 * replaces the earlier two-provider bound. The default stays 1, so an
 * unconfigured consumer keeps the historical sequential behaviour.
 *
 * The parallelism lives on [ContentMigrationIntention], wired into
 * [ContentTranslationService]. Before US-4 the CLI could not reach it: the
 * option was absent and the value always fell back to 1.
 */
class ContentMigrationParallelismTest {

    @Test
    fun `an intention accepts the full-pool ceiling`() {
        val intention = intention(parallelism = 25)

        assertEquals(25, intention.parallelism)
    }

    @Test
    fun `an intention accepts a parallelism of five`() {
        val intention = intention(parallelism = 5)

        assertEquals(5, intention.parallelism)
    }

    @Test
    fun `an intention accepts a parallelism of two`() {
        val intention = intention(parallelism = 2)

        assertEquals(2, intention.parallelism)
    }

    @Test
    fun `the default parallelism is one`() {
        val intention = intention()

        assertEquals(1, intention.parallelism)
    }

    @Test
    fun `a parallelism below one is rejected`() {
        assertThrows<IllegalArgumentException> { intention(parallelism = 0) }
    }

    @Test
    fun `a parallelism above the full pool is rejected`() {
        assertThrows<IllegalArgumentException> { intention(parallelism = 26) }
    }

    @Test
    fun `the ceiling is the number of healthy pool ports`() {
        assertEquals(25, ContentMigrationIntention.MAX_PARALLELISM)
    }

    private fun intention(parallelism: Int = 1) =
        ContentMigrationIntention(
            sourceDir = "jbake/content",
            outputDir = "content-i18n",
            targetLanguages = listOf("en"),
            parallelism = parallelism,
        )
}
