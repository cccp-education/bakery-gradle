package bakery.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CHE-I18N-22 US-4 — the content migration must honour `excludePaths` on the
 * AsciiDoc set and must actually translate in parallel.
 *
 * Two real defects measured on the cheroliv.com corpus:
 *
 *  1. `excludePaths` was only applied to the non-adoc copy loop. The delta still
 *     contained the 72 draft articles (39% of the metered budget) — the exclusion
 *     was silently ignored for the files that cost tokens.
 *  2. `parallelism` was accepted by the intention and forwarded to
 *     [document.translation.ContentTranslationService], but the task drove the
 *     translation file-by-file itself in a sequential loop — the option had no
 *     effect at all.
 */
class ContentMigrationExclusionParallelismTest {

    @Test
    fun `the delta excludes the requested subtrees`(
        @TempDir tempDir: File,
    ) {
        val source = createSource(tempDir)

        val plan = ContentMigrationPlanner.plan(
            sourceDir = source,
            storedChecksums = emptyMap(),
            excludePaths = setOf("draft", ".well-known"),
        )

        assertTrue(
            plan.filesToTranslate.none { it.startsWith("draft/") },
            "The draft subtree must be excluded: ${plan.filesToTranslate}",
        )
        assertTrue(
            plan.filesToTranslate.none { it.startsWith(".well-known/") },
            "The .well-known subtree must be excluded: ${plan.filesToTranslate}",
        )
        assertTrue(
            plan.filesToTranslate.any { it.startsWith("blog/") },
            "The blog subtree must be kept: ${plan.filesToTranslate}",
        )
    }

    @Test
    fun `without an exclusion every article is planned`(
        @TempDir tempDir: File,
    ) {
        val source = createSource(tempDir)

        val plan = ContentMigrationPlanner.plan(source, emptyMap(), emptySet())

        assertEquals(3, plan.filesToTranslate.size)
    }

    private fun createSource(root: File): File {
        val source = root.resolve("content")
        source.resolve("blog/2026").mkdirs()
        source.resolve("draft").mkdirs()
        source.resolve(".well-known").mkdirs()
        source.resolve("blog/2026/0001_post.adoc").writeText("= Article\n\nUn texte.\n")
        source.resolve("blog/2026/0002_post.adoc").writeText("= Article 2\n\nUn texte.\n")
        source.resolve("draft/brouillon.adoc").writeText("= Brouillon\n\nUn texte.\n")
        return source
    }
}
