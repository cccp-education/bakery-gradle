package bakery.langswitch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LangSwitchPathTest {
    private fun resolve(
        currentPageUri: String,
        currentLang: String,
        targetLang: String,
        defaultLang: String = "fr",
    ) = LangSwitchPath.resolveSamePage(currentPageUri, currentLang, targetLang, defaultLang)

    @Test
    fun `self link on default root stays on the same page`() {
        assertEquals("index.html", resolve("index.html", "fr", "fr"))
    }

    @Test
    fun `default root to non-default root goes down into the language dir`() {
        assertEquals("en/index.html", resolve("index.html", "fr", "en"))
    }

    @Test
    fun `non-default root back to default root goes up`() {
        assertEquals("../index.html", resolve("en/index.html", "en", "fr"))
    }

    @Test
    fun `nested default page to non-default keeps the page`() {
        assertEquals("../en/blog/foo.html", resolve("blog/foo.html", "fr", "en"))
    }

    @Test
    fun `nested non-default page back to default keeps the page`() {
        assertEquals("../../blog/foo.html", resolve("en/blog/foo.html", "en", "fr"))
    }

    @Test
    fun `nested non-default page to another language keeps the page`() {
        assertEquals("../../ar/blog/foo.html", resolve("en/blog/foo.html", "en", "ar"))
    }

    @Test
    fun `deeply nested page to default keeps the whole path`() {
        assertEquals("../../../a/b/c.html", resolve("en/a/b/c.html", "en", "fr"))
    }

    @Test
    fun `self link on non-default nested page points to itself`() {
        assertEquals("foo.html", resolve("en/blog/foo.html", "en", "en"))
    }

    @Test
    fun `self link on default nested page points to itself`() {
        assertEquals("foo.html", resolve("blog/foo.html", "fr", "fr"))
    }

    @Test
    fun `non-default root to another non-default language`() {
        assertEquals("../ar/index.html", resolve("en/index.html", "en", "ar"))
    }

    @Test
    fun `missing target page degrades to the language index`() {
        assertEquals(
            "../en/index.html",
            LangSwitchPath.resolveSamePage("blog/foo.html", "fr", "en", "fr") { false },
        )
    }

    @Test
    fun `existing target page is kept`() {
        assertEquals(
            "../en/blog/foo.html",
            LangSwitchPath.resolveSamePage("blog/foo.html", "fr", "en", "fr") { it == "en/blog/foo.html" },
        )
    }

    @Test
    fun `missing nested target degrades preserving the current depth`() {
        assertEquals(
            "../../ar/index.html",
            LangSwitchPath.resolveSamePage("en/blog/foo.html", "en", "ar", "fr") { false },
        )
    }

    @Test
    fun `leading slash is normalised`() {
        assertEquals("../en/blog/foo.html", resolve("/blog/foo.html", "fr", "en"))
    }

    @Test
    fun `blank current page uri throws`() {
        assertThrows<IllegalArgumentException> { resolve("  ", "fr", "en") }
    }

    @Test
    fun `blank current language throws`() {
        assertThrows<IllegalArgumentException> { resolve("index.html", "", "en") }
    }

    @Test
    fun `blank target language throws`() {
        assertThrows<IllegalArgumentException> { resolve("index.html", "fr", "") }
    }

    @Test
    fun `blank default language throws`() {
        assertThrows<IllegalArgumentException> { resolve("index.html", "fr", "en", "") }
    }
}
