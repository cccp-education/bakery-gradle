package bakery.i18n.snapshot

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * BKY-I18N-REAL-1 — TDD red for [PostMigrationSnapshot].
 *
 * Pure domain, testable without Gradle, no I/O. Represents a site state
 * after i18n migration: migrated templates + messages_fr + messages_en.
 *
 * Methodology: baby step DDD. Test fails until the class exists.
 * Each assertion maps to a business criterion.
 */
class PostMigrationSnapshotTest {

    @Test
    fun `constructs snapshot with migrated templates and message bundles`() {
        val snapshot = PostMigrationSnapshot(
            templatesMigrated = mapOf("menu.thyme" to "<span th:text=\"#{menu.1}\">Accueil</span>"),
            messagesFr = mapOf("menu.1" to "Accueil"),
            messagesEn = mapOf("menu.1" to "Home")
        )

        assertEquals(1, snapshot.templatesMigrated.size)
        assertEquals(1, snapshot.messagesFr.size)
        assertEquals(1, snapshot.messagesEn.size)
    }

    @Test
    fun `hasAllKeysTranslated returns true when every FR key has a non-blank EN value`() {
        val snapshot = PostMigrationSnapshot(
            templatesMigrated = emptyMap(),
            messagesFr = mapOf("menu.1" to "Accueil", "footer.1" to "Documentation"),
            messagesEn = mapOf("menu.1" to "Home", "footer.1" to "Documentation")
        )

        assertTrue(snapshot.hasAllKeysTranslated(), "Every FR key must have a non-blank EN translation")
    }

    @Test
    fun `hasAllKeysTranslated returns false when an EN value is blank`() {
        val snapshot = PostMigrationSnapshot(
            templatesMigrated = emptyMap(),
            messagesFr = mapOf("menu.1" to "Accueil", "footer.1" to "Documentation"),
            messagesEn = mapOf("menu.1" to "Home", "footer.1" to "")
        )

        assertFalse(snapshot.hasAllKeysTranslated(), "A blank EN translation must fail")
    }

    @Test
    fun `hasAllKeysTranslated returns false when an FR key is missing in EN`() {
        val snapshot = PostMigrationSnapshot(
            templatesMigrated = emptyMap(),
            messagesFr = mapOf("menu.1" to "Accueil", "footer.1" to "Documentation"),
            messagesEn = mapOf("menu.1" to "Home")
        )

        assertFalse(snapshot.hasAllKeysTranslated(), "A FR key missing from EN must fail")
    }

    @Test
    fun `templateKeys extracts i18n keys from migrated templates`() {
        val snapshot = PostMigrationSnapshot(
            templatesMigrated = mapOf(
                "menu.thyme" to "<span th:text=\"#{menu.1}\">Accueil</span>\n<a th:text=\"#{menu.2}\">Blog</a>",
                "footer.thyme" to "<p th:text=\"#{footer.1}\">Documentation</p>"
            ),
            messagesFr = emptyMap(),
            messagesEn = emptyMap()
        )

        val keys = snapshot.templateKeys()
        assertTrue(keys.contains("menu.1"), "menu.1 must be extracted")
        assertTrue(keys.contains("menu.2"), "menu.2 must be extracted")
        assertTrue(keys.contains("footer.1"), "footer.1 must be extracted")
        assertEquals(3, keys.size, "3 unique keys expected")
    }

    @Test
    fun `templateKeys returns empty when no th text attributes present`() {
        val snapshot = PostMigrationSnapshot(
            templatesMigrated = mapOf("menu.thyme" to "<span>Magic Stick</span>"),
            messagesFr = emptyMap(),
            messagesEn = emptyMap()
        )

        assertTrue(snapshot.templateKeys().isEmpty(), "No keys expected without th:text")
    }

    @Test
    fun `templateKeys handles th placeholder and th attr keys`() {
        val snapshot = PostMigrationSnapshot(
            templatesMigrated = mapOf(
                "search.thyme" to "<input th:placeholder=\"#{search.1}\" placeholder=\"Keywords...\"/>",
                "nav.thyme" to "<button th:attr=\"aria-label=#{nav.1}\" aria-label=\"Open menu\">Open</button>"
            ),
            messagesFr = emptyMap(),
            messagesEn = emptyMap()
        )

        val keys = snapshot.templateKeys()
        assertTrue(keys.contains("search.1"), "th:placeholder key must be extracted")
        assertTrue(keys.contains("nav.1"), "th:attr aria-label key must be extracted")
    }

    @Test
    fun `isComplete returns true when templates migrated and all keys translated`() {
        val snapshot = PostMigrationSnapshot(
            templatesMigrated = mapOf("menu.thyme" to "<span th:text=\"#{menu.1}\">Accueil</span>"),
            messagesFr = mapOf("menu.1" to "Accueil"),
            messagesEn = mapOf("menu.1" to "Home")
        )

        assertTrue(snapshot.isComplete(), "Snapshot with templates + full FR/EN messages must be complete")
    }

    @Test
    fun `isComplete returns false when no templates migrated`() {
        val snapshot = PostMigrationSnapshot(
            templatesMigrated = emptyMap(),
            messagesFr = mapOf("menu.1" to "Accueil"),
            messagesEn = mapOf("menu.1" to "Home")
        )

        assertFalse(snapshot.isComplete(), "Without migrated templates the snapshot is incomplete")
    }

    @Test
    fun `isComplete returns false when messages_fr is empty`() {
        val snapshot = PostMigrationSnapshot(
            templatesMigrated = mapOf("menu.thyme" to "<span th:text=\"#{menu.1}\">Accueil</span>"),
            messagesFr = emptyMap(),
            messagesEn = emptyMap()
        )

        assertFalse(snapshot.isComplete(), "Without messages_fr the snapshot is incomplete")
    }

    @Test
    fun `isComplete returns false when keys not fully translated`() {
        val snapshot = PostMigrationSnapshot(
            templatesMigrated = mapOf("menu.thyme" to "<span th:text=\"#{menu.1}\">Accueil</span>"),
            messagesFr = mapOf("menu.1" to "Accueil"),
            messagesEn = mapOf("menu.1" to "")
        )

        assertFalse(snapshot.isComplete(), "A blank EN translation makes the snapshot incomplete")
    }

    @Test
    fun `two snapshots with same content are equal`() {
        val snapshot1 = PostMigrationSnapshot(
            templatesMigrated = mapOf("menu.thyme" to "<span th:text=\"#{menu.1}\">Accueil</span>"),
            messagesFr = mapOf("menu.1" to "Accueil"),
            messagesEn = mapOf("menu.1" to "Home")
        )
        val snapshot2 = PostMigrationSnapshot(
            templatesMigrated = mapOf("menu.thyme" to "<span th:text=\"#{menu.1}\">Accueil</span>"),
            messagesFr = mapOf("menu.1" to "Accueil"),
            messagesEn = mapOf("menu.1" to "Home")
        )

        assertEquals(snapshot1, snapshot2, "Identical snapshots must be equal")
        assertEquals(snapshot1.hashCode(), snapshot2.hashCode(), "Hash codes must match")
    }

    @Test
    fun `snapshot is immutable via data class copy`() {
        val snapshot = PostMigrationSnapshot(
            templatesMigrated = mapOf("menu.thyme" to "<span th:text=\"#{menu.1}\">Accueil</span>"),
            messagesFr = mapOf("menu.1" to "Accueil"),
            messagesEn = emptyMap()
        )
        val completed = snapshot.copy(messagesEn = mapOf("menu.1" to "Home"))

        assertFalse(snapshot.isComplete(), "Original stays incomplete (immutability)")
        assertTrue(completed.isComplete(), "Modified copy is complete")
    }
}