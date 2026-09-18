package bakery.i18n

import java.io.File

/**
 * CHE-I18N-22 US-4 — the pure planning step of the content migration.
 *
 * Extracted so the exclusion rule is testable without a Gradle task or an LLM,
 * and so the task and its delta agree on the same file set.
 *
 * [excludePaths] are paths relative to the source root. A file is excluded when
 * it lives in an excluded directory (`draft/…`) — the exact rule the non-adoc
 * copy loop already applied.
 */
object ContentMigrationPlanner {

    data class Plan(
        val filesToTranslate: List<String>,
        val checksums: Map<String, String>,
    )

    fun plan(
        sourceDir: File,
        storedChecksums: Map<String, String>,
        excludePaths: Set<String>,
    ): Plan {
        val checksums = checksumOf(sourceDir, excludePaths)
        val toTranslate =
            checksums
                .filter { (path, hash) -> storedChecksums[path] != hash }
                .keys
                .sorted()
        return Plan(filesToTranslate = toTranslate, checksums = checksums)
    }

    fun checksumOf(
        sourceDir: File,
        excludePaths: Set<String>,
    ): Map<String, String> {
        if (!sourceDir.exists()) return emptyMap()
        // CHE-I18N-UNIFY — prune excluded subtrees so the output tree, when it
        // lives inside the source root, is never re-scanned as input.
        return SourceTree
            .walk(sourceDir, excludePaths)
            .filter { it.isFile && it.extension == "adoc" }
            .associate { it.relativeTo(sourceDir).path to sha256(it) }
    }

    fun isExcluded(
        relativePath: String,
        excludePaths: Set<String>,
    ): Boolean =
        excludePaths.any { excluded ->
            relativePath == excluded || relativePath.startsWith("$excluded/")
        }

    private fun sha256(file: File): String =
        java.security.MessageDigest
            .getInstance("SHA-256")
            .digest(file.readText().toByteArray())
            .joinToString("") { "%02x".format(it) }
}
