package bakery.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/**
 * CHE-I18N-22 US-4 — the content migration parallelism must be drivable from the
 * CLI (`--contentI18nParallelism`), so a consumer can run the translation on two
 * Ollama providers at once — never three articles in parallel (pilot decision:
 * two providers, bounded concurrency).
 *
 * The parallelism lives on [ContentMigrationIntention], wired into
 * [ContentTranslationService]. Before this US the CLI could not reach it: the
 * option was absent and the value always fell back to 1.
 */
class ContentMigrationParallelismTest {

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
    fun `a parallelism above two is rejected`() {
        assertThrows<IllegalArgumentException> { intention(parallelism = 3) }
    }

    private fun intention(parallelism: Int = 1) =
        ContentMigrationIntention(
            sourceDir = "jbake/content",
            outputDir = "content-i18n",
            targetLanguages = listOf("en"),
            parallelism = parallelism,
        )
}
