package bakery

import org.junit.jupiter.api.Assertions.*
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
            val errors = mutableListOf<ConfigResolutionError>()
            val expected = FirebaseProjectInfo(projectId = "p1", apiKey = "k1")
            val dr = DomainResolver("firebase", { expected }, FirebaseProjectInfo())

            val result = dr.resolve(errors)

            assertEquals(expected, result)
            assertTrue(errors.isEmpty(), "No error expected on success")
        }

        @Test
        fun `returns fallback when resolver throws`() {
            val errors = mutableListOf<ConfigResolutionError>()
            val fallback = ThemeConfig()
            val dr = DomainResolver("theme", { throw RuntimeException("boom") }, fallback)

            val result = dr.resolve(errors)

            assertEquals(fallback, result)
        }

        @Test
        fun `accumulates DomainFailure error when resolver throws`() {
            val errors = mutableListOf<ConfigResolutionError>()
            val dr = DomainResolver("analytics", { throw RuntimeException("parse fail") }, AnalyticsConfig())

            dr.resolve(errors)

            assertEquals(1, errors.size)
            val error = assertInstanceOf<ConfigResolutionError.DomainFailure>(errors[0])
            assertEquals("analytics", error.domain)
            assertEquals("parse fail", error.message)
        }

        @Test
        fun `error message falls back to Unknown error when exception has no message`() {
            val errors = mutableListOf<ConfigResolutionError>()
            val dr = DomainResolver("language", { throw RuntimeException() }, "fr")

            dr.resolve(errors)

            val error = errors[0] as ConfigResolutionError.DomainFailure
            assertEquals("Unknown error", error.message)
        }

        @Test
        fun `captures exception as cause when it is an Exception subtype`() {
            val errors = mutableListOf<ConfigResolutionError>()
            val cause = RuntimeException("boom")
            val dr = DomainResolver("theme", { throw cause }, ThemeConfig())

            dr.resolve(errors)

            val error = errors[0] as ConfigResolutionError.DomainFailure
            assertSame(cause, error.cause)
        }

        @Test
        fun `cause is null when thrown throwable is not an Exception subtype`() {
            val errors = mutableListOf<ConfigResolutionError>()
            val dr = DomainResolver("lang", { throw Throwable("not exception") }, "fr")

            dr.resolve(errors)

            val error = errors[0] as ConfigResolutionError.DomainFailure
            assertNull(error.cause)
        }

        @Test
        fun `does not accumulate error on success`() {
            val errors = mutableListOf<ConfigResolutionError>()
            val dr = DomainResolver("comments", { CommentsConfig(enabled = true) }, CommentsConfig())

            dr.resolve(errors)

            assertTrue(errors.isEmpty())
        }

        @Test
        fun `multiple resolvers accumulate errors independently`() {
            val errors = mutableListOf<ConfigResolutionError>()
            val dr1 = DomainResolver("firebase", { throw RuntimeException("e1") }, FirebaseProjectInfo())
            val dr2 = DomainResolver("theme", { throw RuntimeException("e2") }, ThemeConfig())

            dr1.resolve(errors)
            dr2.resolve(errors)

            assertEquals(2, errors.size)
            assertEquals("firebase", (errors[0] as ConfigResolutionError.DomainFailure).domain)
            assertEquals("theme", (errors[1] as ConfigResolutionError.DomainFailure).domain)
        }
    }
}