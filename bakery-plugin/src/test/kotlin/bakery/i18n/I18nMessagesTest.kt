package bakery.i18n

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Properties

class I18nMessagesTest {

    private val languages = listOf("fr", "en", "zh", "hi", "es", "ar", "bn", "pt", "ru", "ur")
    private val templateDirs = listOf("site/templates", "site-basic/templates")

    private fun loadProperties(dir: String, lang: String): Properties {
        val path = "$dir/messages_$lang.properties"
        val stream = javaClass.classLoader.getResourceAsStream(path)
            ?: throw AssertionError("File not found: $path")
        return Properties().apply { load(stream) }
    }

    @Nested
    inner class FileExistenceTest {

        @Test
        fun `all 20 message files exist`() {
            for (dir in templateDirs) {
                for (lang in languages) {
                    val path = "$dir/messages_$lang.properties"
                    val stream = javaClass.classLoader.getResourceAsStream(path)
                    assertNotNull(stream, "Missing: $path")
                    stream.close()
                }
            }
        }
    }

    @Nested
    inner class KeyConsistencyTest {

        @Test
        fun `all languages have same keys as fr reference in site templates`() {
            val ref = loadProperties("site/templates", "fr")
            val refKeys = ref.stringPropertyNames()

            for (lang in languages.filter { it != "fr" }) {
                val props = loadProperties("site/templates", lang)
                val keys = props.stringPropertyNames()

                val missingInLang = refKeys.filter { it !in keys }
                val extraInLang = keys.filter { it !in refKeys }

                assertTrue(missingInLang.isEmpty(),
                    "[$lang] Missing keys: $missingInLang")
                assertTrue(extraInLang.isEmpty(),
                    "[$lang] Extra keys: $extraInLang")
            }
        }

        @Test
        fun `all languages have same keys as fr reference in site-basic templates`() {
            val ref = loadProperties("site-basic/templates", "fr")
            val refKeys = ref.stringPropertyNames()

            for (lang in languages.filter { it != "fr" }) {
                val props = loadProperties("site-basic/templates", lang)
                val keys = props.stringPropertyNames()

                val missingInLang = refKeys.filter { it !in keys }
                val extraInLang = keys.filter { it !in refKeys }

                assertTrue(missingInLang.isEmpty(),
                    "[$lang] Missing keys: $missingInLang")
                assertTrue(extraInLang.isEmpty(),
                    "[$lang] Extra keys: $extraInLang")
            }
        }
    }

    @Nested
    inner class NonEmptyValuesTest {

        @Test
        fun `all values are non-blank in site templates`() {
            for (lang in languages) {
                val props = loadProperties("site/templates", lang)
                for (key in props.stringPropertyNames()) {
                    val value = props.getProperty(key)
                    assertTrue(value.isNotBlank(),
                        "[$lang] Key '$key' has blank value")
                }
            }
        }

        @Test
        fun `all values are non-blank in site-basic templates`() {
            for (lang in languages) {
                val props = loadProperties("site-basic/templates", lang)
                for (key in props.stringPropertyNames()) {
                    val value = props.getProperty(key)
                    assertTrue(value.isNotBlank(),
                        "[$lang] Key '$key' has blank value")
                }
            }
        }
    }

    @Nested
    inner class KeyCountTest {

        @Test
        fun `site templates have expected key count`() {
            val ref = loadProperties("site/templates", "fr")
            val count = ref.stringPropertyNames().size
            assertTrue(count >= 60, "Expected >= 60 keys, got $count")
        }

        @Test
        fun `site-basic templates have expected key count`() {
            val ref = loadProperties("site-basic/templates", "fr")
            val count = ref.stringPropertyNames().size
            assertTrue(count >= 8, "Expected >= 8 keys, got $count")
        }
    }
}
