package bakery

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf

class DomainResolverTest {
    @Nested
    inner class Structure {
        @Test
        fun `holds name resolver and fallback`() {
            val dr = DomainResolver("firebase", { "resolved" }, "fallback")
            assertEquals("firebase", dr.name)
            assertEquals("fallback", dr.fallback)
        }

        @Test
        fun `resolver is a zero-arg lambda returning T`() {
            val dr = DomainResolver("theme", { ThemeConfig(mode = "dark") }, ThemeConfig())
            val result = dr.resolver()
            assertEquals(ThemeConfig(mode = "dark"), result)
        }
    }

    @Nested
    inner class Resolve {
        @Test
        fun `returns resolved value when resolver succeeds`() {
            val expected = FirebaseProjectInfo(projectId = "p1", apiKey = "k1")
            val dr = DomainResolver("firebase", { expected }, FirebaseProjectInfo())

            val outcome = dr.resolveOutcome()

            assertEquals(expected, outcome.value)
            assertNull(outcome.error, "No error expected on success")
        }

        @Test
        fun `returns fallback when resolver throws`() {
            val fallback = ThemeConfig()
            val dr = DomainResolver("theme", { throw RuntimeException("boom") }, fallback)

            val outcome = dr.resolveOutcome()

            assertEquals(fallback, outcome.value)
        }

        @Test
        fun `accumulates DomainFailure error when resolver throws`() {
            val dr = DomainResolver("analytics", { throw RuntimeException("parse fail") }, AnalyticsConfig())

            val outcome = dr.resolveOutcome()

            val error = assertInstanceOf<ConfigResolutionError.DomainFailure>(outcome.error)
            assertEquals("analytics", error.domain)
            assertEquals("parse fail", error.message)
        }

        @Test
        fun `error message falls back to Unknown error when exception has no message`() {
            val dr = DomainResolver("language", { throw RuntimeException() }, "fr")

            val outcome = dr.resolveOutcome()

            val error = assertInstanceOf<ConfigResolutionError.DomainFailure>(outcome.error)
            assertEquals("Unknown error", error.message)
        }

        @Test
        fun `captures exception as cause when it is an Exception subtype`() {
            val cause = RuntimeException("boom")
            val dr = DomainResolver("theme", { throw cause }, ThemeConfig())

            val outcome = dr.resolveOutcome()

            val error = assertInstanceOf<ConfigResolutionError.DomainFailure>(outcome.error)
            assertSame(cause, error.cause)
        }

        @Test
        fun `cause is null when thrown throwable is not an Exception subtype`() {
            val dr = DomainResolver("lang", { throw Throwable("not exception") }, "fr")

            val outcome = dr.resolveOutcome()

            val error = assertInstanceOf<ConfigResolutionError.DomainFailure>(outcome.error)
            assertNull(error.cause)
        }

        @Test
        fun `does not accumulate error on success`() {
            val dr = DomainResolver("comments", { CommentsConfig(enabled = true) }, CommentsConfig())

            val outcome = dr.resolveOutcome()

            assertNull(outcome.error)
        }

        @Test
        fun `multiple resolvers report errors independently`() {
            val dr1 = DomainResolver("firebase", { throw RuntimeException("e1") }, FirebaseProjectInfo())
            val dr2 = DomainResolver("theme", { throw RuntimeException("e2") }, ThemeConfig())

            val first = assertInstanceOf<ConfigResolutionError.DomainFailure>(dr1.resolveOutcome().error)
            val second = assertInstanceOf<ConfigResolutionError.DomainFailure>(dr2.resolveOutcome().error)

            assertEquals("firebase", first.domain)
            assertEquals("theme", second.domain)
        }
    }
}
