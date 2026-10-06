package bakery.i18n

import document.translation.delta.BlockChecksumEntry
import document.translation.delta.BlockTranslationStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * DOC-TRANSLATE-RESILIENCE (cross-borough, bakery side) — a target file whose
 * block checksums contain a PENDING block must be re-scheduled even when its
 * source hash is unchanged: otherwise the block stored PENDING by document-gradle
 * is never re-entered because the file-level delta preserves the byte-identical
 * file.
 */
class PendingBlockRetryTest {
    private fun translated(hash: String) = BlockChecksumEntry(hash, BlockTranslationStatus.TRANSLATED)

    private fun pending(hash: String) = BlockChecksumEntry(hash, BlockTranslationStatus.PENDING)

    @Test
    fun `a file with a pending block is re-scheduled`() {
        val checksums =
            mapOf(
                "intro.adoc" to mapOf("0" to translated("h0"), "1" to pending("h1")),
                "blog/ok.adoc" to mapOf("0" to translated("h0")),
            )

        val retry =
            PendingBlockRetry.filesWithPendingBlocks(
                setOf("intro.adoc", "blog/ok.adoc"),
            ) { checksums.getValue(it) }

        assertEquals(setOf("intro.adoc"), retry)
    }

    @Test
    fun `a file with only translated blocks is not re-scheduled`() {
        val checksums =
            mapOf("intro.adoc" to mapOf("0" to translated("h0"), "1" to translated("h1")))

        val retry =
            PendingBlockRetry.filesWithPendingBlocks(
                setOf("intro.adoc"),
            ) { checksums.getValue(it) }

        assertTrue(retry.isEmpty())
    }

    @Test
    fun `a missing checksum file is never re-scheduled`() {
        val retry =
            PendingBlockRetry.filesWithPendingBlocks(
                setOf("fresh.adoc"),
            ) { emptyMap() }

        assertTrue(retry.isEmpty())
    }

    @Test
    fun `an empty target set yields no retry`() {
        val retry =
            PendingBlockRetry.filesWithPendingBlocks(
                emptySet(),
            ) { emptyMap() }

        assertTrue(retry.isEmpty())
    }
}
