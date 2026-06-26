package bakery.i18n.snapshot

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * BKY-I18N-REAL-2 — TDD red for [PostMigrationSnapshotLoader].
 *
 * I/O is isolated in this loader. The domain ([PostMigrationSnapshot])
 * stays pure. Loads a snapshot from a `post-migration/` directory
 * containing migrated `templates/` + `messages_fr.properties` +
 * `messages_en.properties`.
 *
 * Methodology: baby step DDD -> TDD. Test fails until the loader exists.
 */
class PostMigrationSnapshotLoaderTest {

    private val loader = PostMigrationSnapshotLoader()

    @Test
    fun `loads complete snapshot from post-migration directory`(@TempDir tempDir: File) {
        val postMigrationDir = tempDir.resolve("post-migration")
        val templatesDir = postMigrationDir.resolve("templates")
        templatesDir.mkdirs()

        templatesDir.resolve("menu.thyme").writeText("<span th:text=\"#{menu.1}\">Accueil</span>")
        templatesDir.resolve("footer.thyme").writeText("<p th:text=\"#{footer.1}\">Documentation</p>")

        writeProperties(templatesDir.resolve("messages_fr.properties"), mapOf("menu.1" to "Accueil", "footer.1" to "Documentation"))
        writeProperties(templatesDir.resolve("messages_en.properties"), mapOf("menu.1" to "Home", "footer.1" to "Documentation"))

        val snapshot = loader.load(postMigrationDir)

        assertTrue(snapshot.isComplete(), "Loaded snapshot must be complete")
        assertEquals(2, snapshot.templatesMigrated.size)
        assertEquals(2, snapshot.messagesFr.size)
        assertEquals(2, snapshot.messagesEn.size)
        assertEquals("Home", snapshot.messagesEn["menu.1"])
    }

    @Test
    fun `loads snapshot with nested templates subdirectory`(@TempDir tempDir: File) {
        val postMigrationDir = tempDir.resolve("post-migration")
        val templatesDir = postMigrationDir.resolve("templates")
        val todoDir = templatesDir.resolve("TODO")
        todoDir.mkdirs()

        templatesDir.resolve("index.thyme").writeText("<h1 th:text=\"#{index.1}\">Accueil</h1>")
        todoDir.resolve("about.thyme").writeText("<p th:text=\"#{about.1}\">A propos</p>")

        writeProperties(templatesDir.resolve("messages_fr.properties"), mapOf("index.1" to "Accueil", "about.1" to "A propos"))
        writeProperties(templatesDir.resolve("messages_en.properties"), mapOf("index.1" to "Home", "about.1" to "About"))

        val snapshot = loader.load(postMigrationDir)

        assertTrue(snapshot.isComplete())
        assertTrue(snapshot.templatesMigrated.keys.any { it.contains("about.thyme") }, "Nested template must be loaded")
    }

    @Test
    fun `loads snapshot returns incomplete when messages_en missing`(@TempDir tempDir: File) {
        val postMigrationDir = tempDir.resolve("post-migration")
        val templatesDir = postMigrationDir.resolve("templates")
        templatesDir.mkdirs()

        templatesDir.resolve("menu.thyme").writeText("<span th:text=\"#{menu.1}\">Accueil</span>")
        writeProperties(templatesDir.resolve("messages_fr.properties"), mapOf("menu.1" to "Accueil"))

        val snapshot = loader.load(postMigrationDir)

        assertFalse(snapshot.isComplete(), "Without messages_en the snapshot is incomplete")
        assertTrue(snapshot.messagesEn.isEmpty(), "messagesEn must be empty")
    }

    @Test
    fun `loads snapshot returns empty templates when no thyme files`(@TempDir tempDir: File) {
        val postMigrationDir = tempDir.resolve("post-migration")
        val templatesDir = postMigrationDir.resolve("templates")
        templatesDir.mkdirs()

        writeProperties(templatesDir.resolve("messages_fr.properties"), mapOf("menu.1" to "Accueil"))
        writeProperties(templatesDir.resolve("messages_en.properties"), mapOf("menu.1" to "Home"))

        val snapshot = loader.load(postMigrationDir)

        assertFalse(snapshot.isComplete(), "Without templates the snapshot is incomplete")
        assertTrue(snapshot.templatesMigrated.isEmpty())
    }

    @Test
    fun `loads snapshot uses relative path as template key`(@TempDir tempDir: File) {
        val postMigrationDir = tempDir.resolve("post-migration")
        val templatesDir = postMigrationDir.resolve("templates")
        val todoDir = templatesDir.resolve("TODO")
        todoDir.mkdirs()

        todoDir.resolve("about.thyme").writeText("<p th:text=\"#{about.1}\">A propos</p>")
        writeProperties(templatesDir.resolve("messages_fr.properties"), mapOf("about.1" to "A propos"))
        writeProperties(templatesDir.resolve("messages_en.properties"), mapOf("about.1" to "About"))

        val snapshot = loader.load(postMigrationDir)

        assertTrue(snapshot.templatesMigrated.containsKey("TODO/about.thyme"), "Key must be the relative path")
    }

    @Test
    fun `throws when post-migration directory does not exist`(@TempDir tempDir: File) {
        val missingDir = tempDir.resolve("does-not-exist")

        assertThrows<IOException> {
            loader.load(missingDir)
        }
    }

    @Test
    fun `loads snapshot with only non-thyme files in templates returns empty templates`(@TempDir tempDir: File) {
        val postMigrationDir = tempDir.resolve("post-migration")
        val templatesDir = postMigrationDir.resolve("templates")
        templatesDir.mkdirs()

        templatesDir.resolve("styles.css").writeText("body { color: red; }")
        templatesDir.resolve("script.js").writeText("console.log('hello');")
        writeProperties(templatesDir.resolve("messages_fr.properties"), mapOf("menu.1" to "Accueil"))
        writeProperties(templatesDir.resolve("messages_en.properties"), mapOf("menu.1" to "Home"))

        val snapshot = loader.load(postMigrationDir)

        assertTrue(snapshot.templatesMigrated.isEmpty(), "Non-thyme files must not be loaded as templates")
    }

    @Test
    fun `round trip load preserves template content byte-identical`(@TempDir tempDir: File) {
        val postMigrationDir = tempDir.resolve("post-migration")
        val templatesDir = postMigrationDir.resolve("templates")
        templatesDir.mkdirs()

        val originalContent = "<!DOCTYPE html>\n<html>\n<body>\n<span th:text=\"#{menu.1}\">Accueil</span>\n</body>\n</html>"
        templatesDir.resolve("menu.thyme").writeText(originalContent)
        writeProperties(templatesDir.resolve("messages_fr.properties"), mapOf("menu.1" to "Accueil"))
        writeProperties(templatesDir.resolve("messages_en.properties"), mapOf("menu.1" to "Home"))

        val snapshot = loader.load(postMigrationDir)

        assertEquals(originalContent, snapshot.templatesMigrated["menu.thyme"], "Byte-identical content expected")
    }

    private fun writeProperties(file: File, entries: Map<String, String>) {
        val props = Properties()
        entries.forEach { (k, v) -> props.setProperty(k, v) }
        file.outputStream().use { props.store(it, null) }
    }
}