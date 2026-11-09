package bakery

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes

/**
 * BKY-CI-MIRROR — assembles the CI mirror payload.
 *
 * `office/sites/<site>` is the single source of truth. The GitHub runner repos
 * (e.g. `cheroliv-contact/cheroliv.com`) are derived mirrors: they receive a
 * copy of the site source plus a workflow overlay, then their own Actions run on
 * that copy. The mirror is disposable and rewritten on every push.
 *
 * The overlay (`.ci-mirror/` in the site directory, gitignored/never copied as
 * source) carries the repository-level files: the `.github/workflows/` YAML
 * definitions, the `.github/actions/` composites and `README.adoc`.
 *
 * Pure filesystem domain: no Gradle, no JGit, no network.
 */
object CiMirrorPayload {
    /** Directory names pruned at any depth (governance, secrets, build artifacts). */
    val EXCLUDED_DIR_NAMES: Set<String> =
        setOf(
            ".agents",
            ".git",
            ".gradle",
            ".kotlin",
            ".idea",
            "build",
            ".ci-mirror",
            ".github",
            ".bakery-block-checksums",
        )

    /** File names excluded at any depth (secrets, local state). */
    val EXCLUDED_FILE_NAMES: Set<String> =
        setOf(
            "site.yml",
            "site.yml.bak",
            "local.properties",
            "graph.json",
            ".bakery-checksums.properties",
        )

    const val SITES_DIR = "sites"

    data class Summary(
        val siteName: String,
        val payloadRoot: String,
        val siteFiles: Int,
        val overlayFiles: Int,
    )

    fun isExcluded(relativePath: String): Boolean {
        val segments = relativePath.replace('\\', '/').split('/').filter { it.isNotEmpty() }
        if (segments.isEmpty()) return false
        if (segments.any { it in EXCLUDED_DIR_NAMES }) return true
        val fileName = segments.last()
        if (fileName in EXCLUDED_FILE_NAMES) return true
        if (fileName.startsWith("site.yml.bak")) return true
        return fileName.endsWith(".iml")
    }

    /**
     * Assembles [payloadDir] from [siteDir] (copied under `sites/<siteDir.name>/`)
     * and [overlayDir] (copied at the payload root).
     *
     * @return the copy summary, or a human-readable error.
     */
    fun assemble(
        siteDir: File,
        payloadDir: File,
        overlayDir: File,
    ): Either<String, Summary> {
        if (!siteDir.isDirectory) {
            return "Site directory not found: ${siteDir.absolutePath}".left()
        }
        if (!overlayDir.isDirectory) {
            return "CI mirror overlay not found: ${overlayDir.absolutePath}".left()
        }

        return try {
            resetDirectory(payloadDir)
            val siteTarget = payloadDir.resolve(SITES_DIR).resolve(siteDir.name)
            siteTarget.mkdirs()

            val siteFiles = copyTree(siteDir.toPath(), siteTarget.toPath(), siteDir.toPath(), ::isExcluded)
            val overlayFiles = copyTree(overlayDir.toPath(), payloadDir.toPath(), overlayDir.toPath()) { false }

            Summary(
                siteName = siteDir.name,
                payloadRoot = payloadDir.absolutePath,
                siteFiles = siteFiles,
                overlayFiles = overlayFiles,
            ).right()
        } catch (e: IOException) {
            "Failed to assemble CI mirror payload: ${e.message}".left()
        }
    }

    private fun resetDirectory(dir: File) {
        if (dir.exists() && !dir.deleteRecursively()) {
            throw IOException("Cannot delete ${dir.absolutePath}")
        }
        if (!dir.mkdirs() && !dir.isDirectory) {
            throw IOException("Cannot create ${dir.absolutePath}")
        }
    }

    private fun copyTree(
        sourceRoot: Path,
        targetRoot: Path,
        relativeBase: Path,
        isExcluded: (String) -> Boolean,
    ): Int {
        var copied = 0
        Files.walkFileTree(
            sourceRoot,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(
                    dir: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    if (dir != sourceRoot) {
                        val relative = relativeBase.relativize(dir).invariantSeparatorsPath()
                        if (isExcluded(relative)) return FileVisitResult.SKIP_SUBTREE
                        targetRoot.resolve(relative).toFile().mkdirs()
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    val relative = relativeBase.relativize(file).invariantSeparatorsPath()
                    if (isExcluded(relative)) return FileVisitResult.CONTINUE
                    val target = targetRoot.resolve(relative)
                    target.parent?.toFile()?.mkdirs()
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING)
                    copied++
                    return FileVisitResult.CONTINUE
                }
            },
        )
        return copied
    }

    private fun Path.invariantSeparatorsPath(): String = toString().replace('\\', '/')
}
