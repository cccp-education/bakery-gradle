package bakery.tree

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AssetRefTest {

    @Test
    fun `asset ref with path only`() {
        val asset = AssetRef(path = "styles.css")
        assertEquals("styles.css", asset.path)
        assertNull(asset.integrity)
        assertNull(asset.async)
        assertNull(asset.defer)
    }

    @Test
    fun `asset ref with all fields`() {
        val asset = AssetRef(
            path = "app.js",
            integrity = "sha256-abc123",
            async = true,
            defer = true
        )
        assertEquals("app.js", asset.path)
        assertEquals("sha256-abc123", asset.integrity)
        assertEquals(true, asset.async)
        assertEquals(true, asset.defer)
    }

    @Test
    fun `asset ref with integrity only`() {
        val asset = AssetRef(path = "vendor.js", integrity = "sha384-def456")
        assertEquals("sha384-def456", asset.integrity)
        assertNull(asset.async)
        assertNull(asset.defer)
    }

    @Test
    fun `structural equality holds for same fields`() {
        val a = AssetRef(path = "style.css", integrity = "sha256-x", async = true)
        val b = AssetRef(path = "style.css", integrity = "sha256-x", async = true)
        assertEquals(a, b)
    }

    @Test
    fun `copy produces new instance with overridden field`() {
        val original = AssetRef(path = "a.js", async = true)
        val copy = original.copy(async = false)
        assertEquals("a.js", copy.path)
        assertEquals(false, copy.async)
    }

    @Test
    fun `css asset with integrity only has null async and defer`() {
        val asset = AssetRef(path = "print.css", integrity = "sha256-xxx")
        assertNull(asset.async)
        assertNull(asset.defer)
    }
}
