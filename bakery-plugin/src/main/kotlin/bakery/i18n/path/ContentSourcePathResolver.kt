package bakery.i18n.path

import java.io.File

/**
 * Resolves the `contentI18nSource` path of the `migrateContentI18n` task.
 *
 * Three rules, in priority order:
 * 1. Absolute path → used as-is.
 * 2. Relative path already prefixed with the content root segment(s)
 *    (`bake.srcPath`) → resolved against the project dir, the prefix is NOT
 *    re-applied (no double `content/content/...`).
 * 3. Any other relative path → resolved against the content root
 *    (`projectDir/srcPath`). This is the backward-compatible behaviour.
 *
 * The prefix match is segment-aware: `srcPath=jb` never matches the path
 * `jbake/...`.
 */
internal object ContentSourcePathResolver {
    fun resolve(
        projectDir: File,
        srcPath: String,
        sourceDir: String,
    ): File {
        val candidate = File(sourceDir)
        if (candidate.isAbsolute) return candidate
        return if (alreadyPrefixed(srcPath, sourceDir)) {
            projectDir.resolve(sourceDir)
        } else {
            projectDir.resolve(srcPath).resolve(sourceDir)
        }
    }

    private fun sourceSegments(path: String): List<String> =
        path.split("/").filter { it.isNotBlank() }

    private fun alreadyPrefixed(srcPath: String, sourceDir: String): Boolean {
        val expected = sourceSegments(srcPath)
        if (expected.isEmpty()) return false
        val actual = sourceSegments(sourceDir)
        return actual.size >= expected.size && actual.take(expected.size) == expected
    }
}