package bakery.i18n.snapshot

import java.io.File
import java.io.IOException
import java.util.Properties

/**
 * BKY-I18N-REAL-2 — Loads a [PostMigrationSnapshot] from a
 * `post-migration/` directory.
 *
 * I/O is isolated in this loader. The domain ([PostMigrationSnapshot])
 * stays pure.
 *
 * Expected layout:
 * ```
 * post-migration/
 *   templates/
 *     menu.thyme
 *     footer.thyme
 *     TODO/about.thyme
 *     messages_fr.properties
 *     messages_en.properties
 * ```
 *
 * Templates are indexed by relative path (`TODO/about.thyme`).
 * Non-`.thyme` files are ignored.
 */
class PostMigrationSnapshotLoader {

    fun load(postMigrationDir: File): PostMigrationSnapshot {
        if (!postMigrationDir.exists()) {
            throw IOException("Post-migration directory not found: ${postMigrationDir.absolutePath}")
        }

        val templatesDir = postMigrationDir.resolve("templates")
        val templatesMigrated = loadTemplates(templatesDir)
        val messagesFr = loadProperties(templatesDir.resolve("messages_fr.properties"))
        val messagesEn = loadProperties(templatesDir.resolve("messages_en.properties"))

        return PostMigrationSnapshot(
            templatesMigrated = templatesMigrated,
            messagesFr = messagesFr,
            messagesEn = messagesEn
        )
    }

    private fun loadTemplates(templatesDir: File): Map<String, String> {
        if (!templatesDir.exists()) return emptyMap()
        return templatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .associate { file ->
                val relativePath = file.relativeTo(templatesDir).path
                relativePath to file.readText()
            }
    }

    private fun loadProperties(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        val props = Properties()
        file.inputStream().use { props.load(it) }
        return props.map { it.key.toString() to it.value.toString() }.toMap()
    }
}