package bakery.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DivergentArticlePlannerTest {
    @TempDir
    lateinit var testDir: File

    private fun write(
        root: File,
        relative: String,
        content: String,
    ): File =
        root.resolve(relative).also {
            it.parentFile.mkdirs()
            it.writeText(content)
        }

    @Test
    fun `aligned variants are not divergent`() {
        val sourceDir = testDir.resolve("src")
        val variantDir = testDir.resolve("en")
        write(sourceDir, "intro.adoc", "= Intro\n\nBonjour.\n")
        write(variantDir, "intro.adoc", "= Intro\n\nHello.\n")

        val plan =
            DivergentArticlePlanner.divergent(
                sourceDir = sourceDir,
                variantDir = variantDir,
                existingTargetFiles = setOf("intro.adoc"),
            )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun `divergent variants are listed sorted`() {
        val sourceDir = testDir.resolve("src")
        val variantDir = testDir.resolve("en")
        write(sourceDir, "b.adoc", "= B\n\nFirst.\n\nSecond.\n")
        write(variantDir, "b.adoc", "= B\n\nFirst.\n")
        write(sourceDir, "a.adoc", "= A\n\nFirst.\n\nSecond.\n")
        write(variantDir, "a.adoc", "= A\n")

        val plan =
            DivergentArticlePlanner.divergent(
                sourceDir = sourceDir,
                variantDir = variantDir,
                existingTargetFiles = setOf("b.adoc", "a.adoc"),
            )

        assertEquals(listOf("a.adoc", "b.adoc"), plan)
    }

    @Test
    fun `excluded subtrees are never planned`() {
        val sourceDir = testDir.resolve("src")
        val variantDir = testDir.resolve("en")
        write(sourceDir, "draft/post.adoc", "= Draft\n\nFirst.\n\nSecond.\n")
        write(variantDir, "draft/post.adoc", "= Draft\n")

        val plan =
            DivergentArticlePlanner.divergent(
                sourceDir = sourceDir,
                variantDir = variantDir,
                existingTargetFiles = setOf("draft/post.adoc"),
                excludePaths = setOf("draft"),
            )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun `a missing variant is not divergent`() {
        val sourceDir = testDir.resolve("src")
        val variantDir = testDir.resolve("en")
        write(sourceDir, "intro.adoc", "= Intro\n\nBonjour.\n")
        variantDir.mkdirs()

        val plan =
            DivergentArticlePlanner.divergent(
                sourceDir = sourceDir,
                variantDir = variantDir,
                existingTargetFiles = setOf("intro.adoc"),
            )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun `reset stores deletes only the divergent block checksums`() {
        val blockStores = testDir.resolve("en/.bakery-block-checksums")
        val divergentStore = write(blockStores, "intro.adoc.checksums", "0=hash:TRANSLATED")
        val keptStore = write(blockStores, "other.adoc.checksums", "0=hash:TRANSLATED")

        val reset =
            DivergentArticlePlanner.resetStores(
                blockStoreDir = blockStores,
                relativePaths = listOf("intro.adoc", "missing.adoc"),
            )

        assertEquals(listOf("intro.adoc"), reset)
        assertTrue(!divergentStore.exists())
        assertTrue(keptStore.exists())
    }
}
