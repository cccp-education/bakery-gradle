package contracts.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.Locale

class MessageBundleTest {

    private class MockMessageBundle(
        private val messages: Map<String, String>,
        private val bundleLocale: Locale = Locale.FRENCH
    ) : MessageBundle {
        override fun get(key: String, locale: Locale): String? = messages[key]
        override fun keys(): Set<String> = messages.keys
        override fun locale(): Locale = bundleLocale
    }

    @Test
    fun `should get message by key`() {
        val bundle = MockMessageBundle(mapOf("menu.home" to "Accueil", "menu.blog" to "Blog"))
        assertEquals("Accueil", bundle.get("menu.home", Locale.FRENCH))
        assertEquals("Blog", bundle.get("menu.blog", Locale.FRENCH))
    }

    @Test
    fun `should return null for missing key`() {
        val bundle = MockMessageBundle(mapOf("menu.home" to "Accueil"))
        assertNull(bundle.get("menu.contact", Locale.FRENCH))
    }

    @Test
    fun `should return all keys`() {
        val bundle = MockMessageBundle(mapOf("a" to "1", "b" to "2", "c" to "3"))
        assertEquals(setOf("a", "b", "c"), bundle.keys())
    }

    @Test
    fun `should return locale`() {
        val bundle = MockMessageBundle(mapOf("key" to "value"), Locale.ENGLISH)
        assertEquals(Locale.ENGLISH, bundle.locale())
    }
}
