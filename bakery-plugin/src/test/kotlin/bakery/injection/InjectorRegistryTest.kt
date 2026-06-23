package bakery.injection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class InjectorRegistryTest {

    @Nested
    inner class Structure {

        @Test
        fun `exposes exactly 9 domains`() {
            assertEquals(9, InjectorRegistry.all.size)
        }

        @Test
        fun `exposes all expected domain keys`() {
            assertEquals(
                listOf("analytics", "comments", "firebase", "firebaseAuth", "googleForms", "language", "layout", "newsletter", "theme"),
                InjectorRegistry.all.keys.toList().sorted()
            )
        }

        @Test
        fun `firebase is GateOnTriggerField`() {
            assertTrue(InjectorRegistry.all["firebase"] is InjectorSpec.GateOnTriggerField)
        }

        @Test
        fun `googleForms is GateOnTriggerField`() {
            assertTrue(InjectorRegistry.all["googleForms"] is InjectorSpec.GateOnTriggerField)
        }

        @Test
        fun `firebaseAuth is GateOnTriggerField`() {
            assertTrue(InjectorRegistry.all["firebaseAuth"] is InjectorSpec.GateOnTriggerField)
        }

        @Test
        fun `analytics is GateOnTriggerField`() {
            assertTrue(InjectorRegistry.all["analytics"] is InjectorSpec.GateOnTriggerField)
        }

        @Test
        fun `comments is GateOnBoolean`() {
            assertTrue(InjectorRegistry.all["comments"] is InjectorSpec.GateOnBoolean)
        }

        @Test
        fun `newsletter is GateOnBoolean`() {
            assertTrue(InjectorRegistry.all["newsletter"] is InjectorSpec.GateOnBoolean)
        }

        @Test
        fun `theme is Always`() {
            assertTrue(InjectorRegistry.all["theme"] is InjectorSpec.Always)
        }

        @Test
        fun `layout is Always`() {
            assertTrue(InjectorRegistry.all["layout"] is InjectorSpec.Always)
        }

        @Test
        fun `language is Always`() {
            assertTrue(InjectorRegistry.all["language"] is InjectorSpec.Always)
        }
    }

    @Nested
    inner class GateOnTriggerFieldBehaviour {

        private val spec = InjectorRegistry.all["firebase"]!! as InjectorSpec.GateOnTriggerField

        @Test
        fun `injects properties when trigger non-blank`() {
            val resolver = { k: String, d: String -> mapOf("firebaseApiKey" to "k1", "firebaseProjectId" to "p1")[k] ?: d }
            val lines = mutableListOf("firebaseApiKey=", "firebaseProjectId=")

            spec.inject(lines, resolver)

            assertEquals("k1", lines.find { it.startsWith("firebaseApiKey=") }?.substringAfter("="))
            assertEquals("p1", lines.find { it.startsWith("firebaseProjectId=") }?.substringAfter("="))
        }

        @Test
        fun `injects nothing when trigger blank`() {
            val resolver = { _: String, d: String -> d }
            val lines = mutableListOf("firebaseApiKey=", "firebaseProjectId=")

            spec.inject(lines, resolver)

            assertEquals("", lines.find { it.startsWith("firebaseApiKey=") }?.substringAfter("="))
        }
    }

    @Nested
    inner class GateOnBooleanBehaviour {

        private val spec = InjectorRegistry.all["comments"]!! as InjectorSpec.GateOnBoolean

        @Test
        fun `injects when enabled is true`() {
            val resolver = { k: String, d: String ->
                mapOf("commentsEnabled" to "true", "commentsCollection" to "c1")[k] ?: d
            }
            val lines = mutableListOf("commentsEnabled=", "commentsCollection=")

            spec.inject(lines, resolver)

            assertEquals("true", lines.find { it.startsWith("commentsEnabled=") }?.substringAfter("="))
            assertEquals("c1", lines.find { it.startsWith("commentsCollection=") }?.substringAfter("="))
        }

        @Test
        fun `injects when collection differs from default`() {
            val resolver = { k: String, d: String ->
                mapOf("commentsEnabled" to "false", "commentsCollection" to "custom")[k] ?: d
            }
            val lines = mutableListOf("commentsEnabled=", "commentsCollection=")

            spec.inject(lines, resolver)

            assertEquals("false", lines.find { it.startsWith("commentsEnabled=") }?.substringAfter("="))
            assertEquals("custom", lines.find { it.startsWith("commentsCollection=") }?.substringAfter("="))
        }

        @Test
        fun `injects nothing when enabled false and collection is default`() {
            val resolver = { _: String, d: String -> d }
            val lines = mutableListOf("commentsEnabled=", "commentsCollection=")

            spec.inject(lines, resolver)

            assertEquals("", lines.find { it.startsWith("commentsEnabled=") }?.substringAfter("="))
        }
    }

    @Nested
    inner class AlwaysBehaviour {

        private val spec = InjectorRegistry.all["layout"]!! as InjectorSpec.Always

        @Test
        fun `always injects with default when resolver empty`() {
            val resolver = { _: String, d: String -> d }
            val lines = mutableListOf("layoutType=")

            spec.inject(lines, resolver)

            assertEquals("FULL_WIDTH", lines.find { it.startsWith("layoutType=") }?.substringAfter("="))
        }
    }

    @Nested
    inner class LanguageWriteKey {

        private val spec = InjectorRegistry.all["language"]!! as InjectorSpec.Always

        @Test
        fun `reads language key but writes site dot language`() {
            val resolver = { k: String, d: String -> if (k == "language") "ar" else d }
            val lines = mutableListOf("site.language=")

            spec.inject(lines, resolver)

            assertEquals("ar", lines.find { it.startsWith("site.language=") }?.substringAfter("="))
        }
    }
}