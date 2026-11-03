package bakery.i18n

import document.translation.AsciiDocParser
import java.io.File

/**
 * GBL-010 P3 — plans the full reconstruction of structurally divergent variants.
 *
 * `migrateContentI18n` preserves a target file when its source is byte-unchanged.
 * A variant that diverged structurally (see [StructuralDivergence]) is therefore
 * frozen forever: no delta reaches it, and a full translation is never scheduled.
 * This planner names those files so the task can force a **full** rebuild and
 * reset their block-checksum store (Ink Economy: only the divergent leaves are
 * paid for again, the aligned ones are preserved).
 *
 * Read-only in [divergent]; the destructive [resetStores] is a separate, explicit
 * step so the dry-run and the report can share the same plan.
 */
object DivergentArticlePlanner {
    /**
     * Relative paths of the [existingTargetFiles] whose block structure differs
     * from the reference. Excluded subtrees are never considered.
     */
    fun divergent(
        sourceDir: File,
        variantDir: File,
        existingTargetFiles: Set<String>,
        excludePaths: Set<String> = emptySet(),
        parser: AsciiDocParser = AsciiDocParser(),
    ): List<String> {
        if (!sourceDir.isDirectory || !variantDir.isDirectory) return emptyList()
        return existingTargetFiles
            .asSequence()
            .filterNot { ContentMigrationPlanner.isExcluded(it, excludePaths) }
            .filter { relative ->
                val source = sourceDir.resolve(relative)
                val variant = variantDir.resolve(relative)
                if (!source.isFile || !variant.isFile) {
                    false
                } else {
                    val sourceArticle = parser.parse(source.readText())
                    val variantArticle = parser.parse(variant.readText())
                    !StructuralDivergence.isAligned(sourceArticle, variantArticle)
                }
            }.sorted()
            .toList()
    }

    /**
     * Deletes the block-checksum store of every divergent [relativePaths] entry —
     * the empty store forces `translateSingleFileWithBlockDelta` down the full
     * translation path even if the stale variant file is still on disk. Returns
     * the paths whose store was actually removed, sorted.
     */
    fun resetStores(
        blockStoreDir: File,
        relativePaths: List<String>,
    ): List<String> =
        relativePaths
            .filter { relative ->
                val store = blockStoreDir.resolve("$relative.checksums")
                store.isFile && store.delete()
            }.sorted()
}
