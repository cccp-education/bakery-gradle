package contracts.pipeline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReleaseNotesConfigTest {

    @Test
    fun `should create config with defaults`() {
        val config = ReleaseNotesConfig()

        assertNull(config.fromTag)
        assertEquals("HEAD", config.toTag)
        assertEquals(7, config.categories.size)
        assertEquals("feat", config.categories.keys.first())
        assertEquals("Nouveautés", config.categories["feat"])
        assertEquals("fix", config.categories.keys.elementAt(1))
        assertEquals("Corrections", config.categories["fix"])
        assertEquals("build/release-notes", config.outputDir)
        assertNull(config.version)
        assertEquals("asciidoc", config.rendererType)
        assertTrue(config.includeDownloads)
    }

    @Test
    fun `should support custom categories`() {
        val customCategories = mapOf(
            "feat" to "Features",
            "fix" to "Bug Fixes"
        )
        val config = ReleaseNotesConfig(categories = customCategories)

        assertEquals(2, config.categories.size)
        assertEquals("Features", config.categories["feat"])
        assertEquals("Bug Fixes", config.categories["fix"])
    }

    @Test
    fun `should support all fields custom`() {
        val config = ReleaseNotesConfig(
            fromTag = "v1.0.0",
            toTag = "v1.1.0",
            categories = mapOf("feat" to "New"),
            outputDir = "output/release",
            version = "1.1.0",
            rendererType = "markdown",
            includeDownloads = false
        )

        assertEquals("v1.0.0", config.fromTag)
        assertEquals("v1.1.0", config.toTag)
        assertEquals(1, config.categories.size)
        assertEquals("New", config.categories["feat"])
        assertEquals("output/release", config.outputDir)
        assertEquals("1.1.0", config.version)
        assertEquals("markdown", config.rendererType)
        assertEquals(false, config.includeDownloads)
    }

    @Test
    fun `copy should preserve default categories`() {
        val original = ReleaseNotesConfig(
            version = "2.0.0",
            rendererType = "json"
        )

        val updated = original.copy(fromTag = "v1.0.0")

        assertEquals("v1.0.0", updated.fromTag)
        assertEquals("2.0.0", updated.version)
        assertEquals("json", updated.rendererType)
        assertEquals(7, updated.categories.size)
    }
}
