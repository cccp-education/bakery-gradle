package bakery.i18n

import document.translation.delta.BlockChecksumEntry
import document.translation.delta.BlockTranslationStatus

/**
 * DOC-TRANSLATE-RESILIENCE (cross-borough, bakery side) — a file whose block
 * checksums contain a `PENDING` block must be re-scheduled even when its
 * source hash is unchanged.
 *
 * The file-level delta ([I18nDeltaApplier]) preserves any target file whose
 * source is byte-identical (`toPreserve`). Without this rule, a block whose LLM
 * call failed is stored `PENDING` by document-gradle but its file is never
 * re-entered — the silent source-language fallback is frozen at the file gate.
 * Forcing a retry lets the block delta re-attempt only the failed block.
 *
 * Pure: the caller supplies the block-checksum reader, so this is testable
 * without Gradle, files or an LLM.
 */
object PendingBlockRetry {
    fun filesWithPendingBlocks(
        existingTargetFiles: Set<String>,
        blockChecksumsOf: (String) -> Map<String, BlockChecksumEntry>,
    ): Set<String> =
        existingTargetFiles
            .filter { relPath ->
                blockChecksumsOf(relPath).values.any { it.status == BlockTranslationStatus.PENDING }
            }.toSet()
}
