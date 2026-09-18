package bakery.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

/**
 * CHE-I18N-UNIFY — walking a source tree must not descend into an excluded
 * subtree. `FileTreeWalk` (Kotlin `walkTopDown`) only reports the excluded
 * directory: its descendants are still visited, so a task that writes its output
 * *inside* the source tree (`jbake/i18n/{lang}` inside `jbake/`) would copy the
 * whole `i18n` tree back into every variant — the S-045 7.3 GB recursion.
 */
class SourceTreeWalkTest {

    @TempDir
    lateinit var root: File

    @Test
    fun `an excluded subtree is never descended`(@TempDir tempDir: File) {
        val source = root.resolve("src")
        source.resolve("content/blog").mkdirs()
        source.resolve("content/blog/a.adoc").writeText("= A")
        source.resolve("i18n/ar/content").mkdirs()
        source.resolve("i18n/ar/content/x.adoc").writeText("= X")
        source.resolve("i18n/ar/content/deep").mkdirs()
        source.resolve("i18n/ar/content/deep/y.adoc").writeText("= Y")

        val visited =
            SourceTree
                .walk(source, setOf("i18n", "assets"))
                .map { it.relativeTo(source).path }
                .toList()

        assertEquals(listOf("content", "content/blog", "content/blog/a.adoc"), visited.sorted())
        assert(visited.none { it.startsWith("i18n") }) { "The i18n subtree must not be visited: $visited" }
    }

    @Test
    fun `an excluded file path is skipped`() {
        val source = root.resolve("src")
        source.resolve("content").mkdirs()
        source.resolve("content/keep.adoc").writeText("= Keep")
        source.resolve("content/drop.adoc").writeText("= Drop")

        val visited =
            SourceTree
                .walk(source, setOf("content/drop.adoc"))
                .map { it.relativeTo(source).path }
                .toList()

        assert(visited.contains("content/keep.adoc"))
        assert(!visited.contains("content/drop.adoc"))
    }

    @Test
    fun `a nested exclusion prunes only its subtree`() {
        val source = root.resolve("src")
        source.resolve("content/blog").mkdirs()
        source.resolve("content/blog/ok.adoc").writeText("= Ok")
        source.resolve("content/draft").mkdirs()
        source.resolve("content/draft/no.adoc").writeText("= No")

        val visited =
            SourceTree
                .walk(source, setOf("content/draft"))
                .map { it.relativeTo(source).path }
                .toList()

        assert(visited.contains("content/blog/ok.adoc"))
        assert(visited.none { it.startsWith("content/draft") }) { "draft must be pruned: $visited" }
    }

    @Test
    fun `no exclusion visits the whole tree`() {
        val source = root.resolve("src")
        source.resolve("a/b/c").mkdirs()
        source.resolve("a/b/c/deep.txt").writeText("x")

        val visited = SourceTree.walk(source, emptySet()).map { it.relativeTo(source).path }.toList()

        assert(visited.contains("a/b/c/deep.txt"))
    }
}
