package bakery.deploy

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * EPIC BKY-NETLIFY (US-1, D5) — the payload is target-neutral and hashed by
 * content. Every target consumes this exact listing.
 */
class DeployPayloadTest {
    @TempDir
    lateinit var root: File

    private fun file(
        path: String,
        content: String = "x",
    ): File =
        root
            .resolve(path)
            .apply {
                parentFile.mkdirs()
                writeText(content)
            }

    @Test
    @DisplayName("lists files with sorted relative paths and content digests")
    fun listsFilesDeterministically() {
        file("b.html", "b")
        file("a/index.html", "a")
        file("assets/style.css", "body{}")

        val payload = DeployPayload.collect(root)

        assertThat(payload.map { it.relativePath })
            .containsExactly("a/index.html", "assets/style.css", "b.html")
        assertThat(payload.map { it.size }).containsExactly(1L, 6L, 1L)
        assertThat(payload.map { it.sha256 }).allMatch { it.matches(Regex("[0-9a-f]{64}")) }
    }

    @Test
    @DisplayName("CNAME and backups are excluded from a Netlify payload")
    fun excludesNetlifyArtifacts() {
        file("index.html")
        file("CNAME", "talaria.school")
        file("old.bak")
        file("nested/deep.bak")
        file(".git/config", "[core]")

        val payload = DeployPayload.collect(root, DeployPayload.BASE_EXCLUDES + DeployPayload.NETLIFY_EXCLUDES)

        assertThat(payload.map { it.relativePath })
            .containsExactly("index.html")
    }

    @Test
    @DisplayName("the Git base exclusion drops the .git directory at any depth")
    fun excludesGitMetadata() {
        file(".git/config")
        file("sub/.git/HEAD")
        file("index.html")

        val payload = DeployPayload.collect(root)

        assertThat(payload.map { it.relativePath }).containsExactly("index.html")
    }

    @Test
    @DisplayName("digest is independent of the input order")
    fun digestIsOrderInsensitive() {
        file("a.txt", "a")
        file("b.txt", "b")
        val payload = DeployPayload.collect(root)

        assertThat(DeployPayload.digest(payload.reversed()))
            .isEqualTo(DeployPayload.digest(payload))
    }

    @Test
    @DisplayName("digest changes when one byte changes")
    fun digestIsContentSensitive() {
        file("index.html", "hello")
        val before = DeployPayload.digest(DeployPayload.collect(root))

        file("index.html", "hellO")
        val after = DeployPayload.digest(DeployPayload.collect(root))

        assertThat(after).isNotEqualTo(before)
    }

    @Test
    @DisplayName("digest changes when a file is renamed without content change")
    fun digestIsPathSensitive() {
        file("a.txt", "same")
        val before = DeployPayload.digest(DeployPayload.collect(root))

        root.resolve("a.txt").renameTo(root.resolve("b.txt"))
        val after = DeployPayload.digest(DeployPayload.collect(root))

        assertThat(after).isNotEqualTo(before)
    }

    @Test
    @DisplayName("size sums the payload bytes")
    fun sizeSumsBytes() {
        file("a.txt", "123")
        file("b.txt", "45")

        assertThat(DeployPayload.size(DeployPayload.collect(root))).isEqualTo(5)
    }

    @Test
    @DisplayName("a non-directory root is rejected")
    fun rejectsNonDirectoryRoot() {
        val notADir = file("file.txt")

        assertThatThrownBy { DeployPayload.collect(notADir) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("not a directory")
    }

    @Test
    @DisplayName("glob matches a single segment only with one star")
    fun singleStarStaysInOneSegment() {
        assertThat(DeployPayload.globToRegex("*.bak").matches("a.bak")).isTrue
        assertThat(DeployPayload.globToRegex("*.bak").matches("deep/a.bak")).isFalse
    }
}
