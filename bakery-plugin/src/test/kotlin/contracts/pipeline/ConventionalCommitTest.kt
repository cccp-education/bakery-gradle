package contracts.pipeline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConventionalCommitTest {

    @Test
    fun `should create commit with all fields`() {
        val commit = ConventionalCommit(
            type = "feat",
            scope = "api",
            message = "add login endpoint",
            hash = "abc123def456",
            date = "2026-06-14T10:30:00Z"
        )

        assertEquals("feat", commit.type)
        assertEquals("api", commit.scope)
        assertEquals("add login endpoint", commit.message)
        assertEquals("abc123def456", commit.hash)
        assertEquals("2026-06-14T10:30:00Z", commit.date)
    }

    @Test
    fun `should support null scope`() {
        val commit = ConventionalCommit(
            type = "fix",
            scope = null,
            message = "fix null pointer",
            hash = "def789",
            date = "2026-06-14T11:00:00Z"
        )

        assertNull(commit.scope)
        assertEquals("fix", commit.type)
    }

    @Test
    fun `should support all conventional commit types`() {
        val types = listOf("feat", "fix", "chore", "perf", "refactor", "docs", "test", "ci", "build", "style")
        for (type in types) {
            val commit = ConventionalCommit(
                type = type,
                scope = null,
                message = "test message",
                hash = "abc",
                date = "2026-01-01T00:00:00Z"
            )
            assertEquals(type, commit.type)
        }
    }

    @Test
    fun `should have correct toString representation`() {
        val commit = ConventionalCommit(
            type = "feat",
            scope = "core",
            message = "implement feature X",
            hash = "abc123",
            date = "2026-06-14"
        )

        val str = commit.toString()
        assertEquals(
            "ConventionalCommit(type=feat, scope=core, message=implement feature X, hash=abc123, date=2026-06-14)",
            str
        )
    }

    @Test
    fun `should support copy operator`() {
        val original = ConventionalCommit(
            type = "feat",
            scope = "old",
            message = "old message",
            hash = "oldhash",
            date = "2026-01-01"
        )
        val updated = original.copy(
            scope = "new",
            message = "new message"
        )

        assertEquals("feat", updated.type)
        assertEquals("new", updated.scope)
        assertEquals("new message", updated.message)
        assertEquals("oldhash", updated.hash)
        assertEquals("2026-01-01", updated.date)
    }
}
