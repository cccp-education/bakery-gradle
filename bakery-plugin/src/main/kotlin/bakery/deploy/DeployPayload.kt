package bakery.deploy

import java.io.File
import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8

/**
 * One file of a deployment payload, identified by its content.
 *
 * EPIC BKY-NETLIFY (D5) — the payload is the *same* for every target: only the
 * exclusion list differs. Hashing the content (not the mtime) makes the digest
 * stable across a re-bake that produces identical bytes (Ink Economy).
 */
data class PayloadFile(
    val relativePath: String,
    val size: Long,
    val sha256: String,
)

/**
 * Canonical, target-neutral description of what a deployment ships.
 *
 * The baked tree is the source of truth (`build/<bake.destDirPath>`), which
 * already carries the `{lang}/` variants and the `pagefind/` index. Targets
 * never re-bake their own tree; they only filter what they must not ship.
 */
object DeployPayload {
    /** Git metadata is never part of a site payload. */
    val BASE_EXCLUDES: List<String> = listOf(".git/**")

    /**
     * Netlify owns the domain binding: `CNAME` is a GitHub Pages artifact and
     * must not travel. Editor backups are noise.
     */
    val NETLIFY_EXCLUDES: List<String> = listOf("CNAME", "**/*.bak")

    /**
     * Lists the payload files under [root], sorted by relative path.
     *
     * @throws IllegalArgumentException when [root] is not a directory.
     */
    fun collect(
        root: File,
        excludes: List<String> = BASE_EXCLUDES,
    ): List<PayloadFile> {
        require(root.isDirectory) { "Payload root is not a directory: ${root.absolutePath}" }
        val rules = excludes.map(::globToRegex)
        return root
            .walkTopDown()
            .filter { it.isFile }
            .map { it to it.relativeTo(root).invariantSeparatorsPath }
            .filterNot { (_, path) -> rules.any { it.matches(path) } }
            .sortedBy { it.second }
            .map { (file, path) -> PayloadFile(path, file.length(), sha256(file)) }
            .toList()
    }

    /**
     * SHA-256 over the sorted `(relativePath, contentHash)` pairs.
     *
     * The order of the input list is irrelevant; a single byte change (or a
     * renamed/added/removed file) changes the digest. Two different trees that
     * would ship the same bytes produce the same digest — that is the point.
     */
    fun digest(files: List<PayloadFile>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        files.sortedBy { it.relativePath }.forEach { file ->
            digest.update(file.relativePath.toByteArray(UTF_8))
            digest.update(0)
            digest.update(file.sha256.toByteArray(UTF_8))
            digest.update(0)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Total payload size in bytes. */
    fun size(files: List<PayloadFile>): Long = files.sumOf { it.size }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Minimal glob support for payload exclusions:
     * two stars (any depth), one star (within one segment), question mark
     * (one character). Two stars followed by a slash also match zero
     * directories, so the "any `.bak` at any depth" pattern catches a
     * root-level file too.
     */
    internal fun globToRegex(glob: String): Regex {
        val out = StringBuilder("^")
        var index = 0
        while (index < glob.length) {
            val char = glob[index]
            when {
                glob.startsWith("**/", index) -> {
                    out.append("(?:.*/)?")
                    index += 3
                }

                glob.startsWith("**", index) -> {
                    out.append(".*")
                    index += 2
                }

                char == '*' -> {
                    out.append("[^/]*")
                    index++
                }

                char == '?' -> {
                    out.append("[^/]")
                    index++
                }

                else -> {
                    out.append(Regex.escape(char.toString()))
                    index++
                }
            }
        }
        out.append("$")
        return Regex(out.toString())
    }
}
