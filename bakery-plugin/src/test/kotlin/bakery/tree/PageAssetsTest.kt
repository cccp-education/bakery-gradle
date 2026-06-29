package bakery.tree

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PageAssetsTest {

    @Test
    fun `merge with null returns this`() {
        val assets = PageAssets(css = listOf(AssetRef(path = "a.css")))
        val merged = assets.merge(null)
        assertEquals(assets, merged)
    }

    @Test
    fun `merge with empty keeps existing`() {
        val assets = PageAssets(css = listOf(AssetRef(path = "a.css")))
        val merged = assets.merge(PageAssets())
        assertEquals(assets, merged)
    }

    @Test
    fun `merge combines css from child and js from parent`() {
        val parent = PageAssets(js = listOf(AssetRef(path = "parent.js")))
        val child = PageAssets(css = listOf(AssetRef(path = "child.css")))
        val merged = child.merge(parent)
        assertEquals(listOf(AssetRef(path = "child.css")), merged.css)
        assertEquals(listOf(AssetRef(path = "parent.js")), merged.js)
    }

    @Test
    fun `merge child css overrides parent css`() {
        val parent = PageAssets(css = listOf(AssetRef(path = "parent.css")))
        val child = PageAssets(css = listOf(AssetRef(path = "child.css")))
        val merged = child.merge(parent)
        assertEquals(listOf(AssetRef(path = "child.css")), merged.css)
    }

    @Test
    fun `merge child js overrides parent js`() {
        val parent = PageAssets(js = listOf(AssetRef(path = "parent.js")))
        val child = PageAssets(js = listOf(AssetRef(path = "child.js")))
        val merged = child.merge(parent)
        assertEquals(listOf(AssetRef(path = "child.js")), merged.js)
    }

    @Test
    fun `empty page assets has null css and js`() {
        val assets = PageAssets()
        assertNull(assets.css)
        assertNull(assets.js)
    }

    @Test
    fun `page assets with css only`() {
        val css = listOf(AssetRef(path = "style.css"), AssetRef(path = "print.css"))
        val assets = PageAssets(css = css)
        assertEquals(2, assets.css?.size)
        assertEquals("style.css", assets.css!![0].path)
        assertEquals("print.css", assets.css!![1].path)
        assertNull(assets.js)
    }

    @Test
    fun `page assets with js only`() {
        val js = listOf(AssetRef(path = "app.js", defer = true))
        val assets = PageAssets(js = js)
        assertEquals(1, assets.js?.size)
        assertEquals("app.js", assets.js!![0].path)
        assertEquals(true, assets.js!![0].defer)
        assertNull(assets.css)
    }

    @Test
    fun `page assets with both css and js`() {
        val css = listOf(AssetRef(path = "theme.css"))
        val js = listOf(AssetRef(path = "analytics.js", async = true))
        val assets = PageAssets(css = css, js = js)
        assertEquals(1, assets.css?.size)
        assertEquals(1, assets.js?.size)
        assertEquals(true, assets.js!![0].async)
    }

    @Test
    fun `structural equality holds for same fields`() {
        val css = listOf(AssetRef(path = "a.css"))
        val a = PageAssets(css = css)
        val b = PageAssets(css = css)
        assertEquals(a, b)
    }

    @Test
    fun `copy produces new instance with overridden field`() {
        val original = PageAssets(css = listOf(AssetRef(path = "a.css")))
        val copy = original.copy(css = emptyList())
        assertEquals(0, copy.css?.size)
        assertEquals(1, original.css?.size)
    }

    @Test
    fun `page assets with null lists has isEmpty semantics`() {
        val empty = PageAssets()
        val withEmpty = PageAssets(css = emptyList(), js = emptyList())
        assertNull(empty.css)
        assertNull(empty.js)
        assertTrue(withEmpty.css?.isEmpty() ?: false)
        assertTrue(withEmpty.js?.isEmpty() ?: false)
    }
}
