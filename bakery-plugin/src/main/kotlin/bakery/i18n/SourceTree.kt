package bakery.i18n

import java.io.File

/**
 * CHE-I18N-UNIFY — a source-tree walk that actually *prunes* excluded subtrees.
 *
 * Kotlin's `FileTreeWalk` only reports an excluded directory entry: its
 * descendants are still visited. That is a data-loss amplifier when a task
 * writes its output inside the source tree — `migrateContentI18n` with source
 * `jbake/` and output `jbake/i18n/{lang}/` would copy the whole `i18n` subtree
 * back into every variant, recursively (the S-045 7.3 GB explosion).
 *
 * This object lists the entries in document order and never descends into a
 * directory whose path (relative to [root]) is excluded, nor returns an
 * excluded file.
 *
 * Pure domain: no Gradle, no I/O beyond the filesystem.
 */
object SourceTree {

    fun walk(
        root: File,
        excludePaths: Set<String>,
    ): Sequence<File> {
        val collected = mutableListOf<File>()
        if (root.exists()) collect(root, root, excludePaths, collected)
        return collected.asSequence()
    }

    private fun collect(
        directory: File,
        root: File,
        excludePaths: Set<String>,
        out: MutableList<File>,
    ) {
        directory.listFiles()?.sortedBy { it.name }?.forEach { child ->
            val relative = child.relativeTo(root).path
            if (ContentMigrationPlanner.isExcluded(relative, excludePaths)) return@forEach
            out.add(child)
            if (child.isDirectory) collect(child, root, excludePaths, out)
        }
    }
}
