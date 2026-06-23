package bakery

import bakery.injection.configInjectors
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DomainConfigInjectorsTest {

    private fun resolverFrom(map: Map<String, String>): (String, String) -> String =
        { key, default -> map[key] ?: default }

    private fun linesOf(vararg lines: String) = mutableListOf(*lines)
    private fun linesOf() = mutableListOf<String>()

    private fun valueOf(lines: MutableList<String>, key: String): String? =
        lines.find { it.startsWith("$key=") }?.substringAfter("$key=")

    @Nested
    inner class TriggerFieldGateVariant {

        @Test
        fun `firebase injects apiKey and projectId when apiKey non-blank`() {
            val resolver = resolverFrom(mapOf("firebaseApiKey" to "key-1", "firebaseProjectId" to "proj-1"))
            val lines = linesOf("firebaseApiKey=", "firebaseProjectId=")

            configInjectors["firebase"]!!.inject(lines, resolver)

            assertEquals("key-1", valueOf(lines, "firebaseApiKey"))
            assertEquals("proj-1", valueOf(lines, "firebaseProjectId"))
        }

        @Test
        fun `firebase injects nothing when apiKey blank`() {
            val resolver = resolverFrom(emptyMap())
            val lines = linesOf("firebaseApiKey=", "firebaseProjectId=")

            configInjectors["firebase"]!!.inject(lines, resolver)

            assertEquals("", valueOf(lines, "firebaseApiKey"))
            assertEquals("", valueOf(lines, "firebaseProjectId"))
        }

        @Test
        fun `googleForms injects formId width height when formId non-blank`() {
            val resolver = resolverFrom(mapOf("googleFormsFormId" to "f-1", "googleFormsWidth" to "700", "googleFormsHeight" to "900"))
            val lines = linesOf("googleFormsFormId=", "googleFormsWidth=", "googleFormsHeight=")

            configInjectors["googleForms"]!!.inject(lines, resolver)

            assertEquals("f-1", valueOf(lines, "googleFormsFormId"))
            assertEquals("700", valueOf(lines, "googleFormsWidth"))
            assertEquals("900", valueOf(lines, "googleFormsHeight"))
        }

        @Test
        fun `googleForms injects nothing when formId blank but width present`() {
            val resolver = resolverFrom(mapOf("googleFormsWidth" to "700"))
            val lines = linesOf("googleFormsFormId=", "googleFormsWidth=")

            configInjectors["googleForms"]!!.inject(lines, resolver)

            assertEquals("", valueOf(lines, "googleFormsFormId"))
            assertEquals("", valueOf(lines, "googleFormsWidth"))
        }

        @Test
        fun `googleForms uses width default 640 when resolver absent`() {
            val resolver = resolverFrom(mapOf("googleFormsFormId" to "f-1"))
            val lines = linesOf("googleFormsFormId=", "googleFormsWidth=", "googleFormsHeight=")

            configInjectors["googleForms"]!!.inject(lines, resolver)

            assertEquals("f-1", valueOf(lines, "googleFormsFormId"))
            assertEquals("640", valueOf(lines, "googleFormsWidth"))
            assertEquals("800", valueOf(lines, "googleFormsHeight"))
        }

        @Test
        fun `firebaseAuth injects apiKey authDomain projectId when apiKey non-blank`() {
            val resolver = resolverFrom(mapOf("firebaseAuthApiKey" to "ak", "firebaseAuthDomain" to "dom", "firebaseAuthProjectId" to "pj"))
            val lines = linesOf("firebaseAuthApiKey=", "firebaseAuthDomain=", "firebaseAuthProjectId=")

            configInjectors["firebaseAuth"]!!.inject(lines, resolver)

            assertEquals("ak", valueOf(lines, "firebaseAuthApiKey"))
            assertEquals("dom", valueOf(lines, "firebaseAuthDomain"))
            assertEquals("pj", valueOf(lines, "firebaseAuthProjectId"))
        }

        @Test
        fun `firebaseAuth injects nothing when apiKey blank`() {
            val resolver = resolverFrom(mapOf("firebaseAuthDomain" to "dom"))
            val lines = linesOf("firebaseAuthApiKey=", "firebaseAuthDomain=")

            configInjectors["firebaseAuth"]!!.inject(lines, resolver)

            assertEquals("", valueOf(lines, "firebaseAuthApiKey"))
            assertEquals("", valueOf(lines, "firebaseAuthDomain"))
        }

        @Test
        fun `analytics injects provider domain scriptSrc when provider non-blank`() {
            val resolver = resolverFrom(mapOf("analyticsProvider" to "matomo", "analyticsDomain" to "d.com", "analyticsScriptSrc" to "s.js"))
            val lines = linesOf("analyticsProvider=", "analyticsDomain=", "analyticsScriptSrc=")

            configInjectors["analytics"]!!.inject(lines, resolver)

            assertEquals("matomo", valueOf(lines, "analyticsProvider"))
            assertEquals("d.com", valueOf(lines, "analyticsDomain"))
            assertEquals("s.js", valueOf(lines, "analyticsScriptSrc"))
        }

        @Test
        fun `analytics injects nothing when provider blank`() {
            val resolver = resolverFrom(mapOf("analyticsDomain" to "d.com"))
            val lines = linesOf("analyticsProvider=", "analyticsDomain=")

            configInjectors["analytics"]!!.inject(lines, resolver)

            assertEquals("", valueOf(lines, "analyticsProvider"))
            assertEquals("", valueOf(lines, "analyticsDomain"))
        }
    }

    @Nested
    inner class BooleanGateVariant {

        @Test
        fun `comments injects when enabled is true`() {
            val resolver = resolverFrom(mapOf("commentsEnabled" to "true", "commentsCollection" to "c1"))
            val lines = linesOf("commentsEnabled=", "commentsCollection=")

            configInjectors["comments"]!!.inject(lines, resolver)

            assertEquals("true", valueOf(lines, "commentsEnabled"))
            assertEquals("c1", valueOf(lines, "commentsCollection"))
        }

        @Test
        fun `comments injects when collection differs from default comments even if enabled false`() {
            val resolver = resolverFrom(mapOf("commentsEnabled" to "false", "commentsCollection" to "custom"))
            val lines = linesOf("commentsEnabled=", "commentsCollection=")

            configInjectors["comments"]!!.inject(lines, resolver)

            assertEquals("false", valueOf(lines, "commentsEnabled"))
            assertEquals("custom", valueOf(lines, "commentsCollection"))
        }

        @Test
        fun `comments injects nothing when enabled false and collection is default comments`() {
            val resolver = resolverFrom(emptyMap())
            val lines = linesOf("commentsEnabled=", "commentsCollection=")

            configInjectors["comments"]!!.inject(lines, resolver)

            assertEquals("", valueOf(lines, "commentsEnabled"))
            assertEquals("", valueOf(lines, "commentsCollection"))
        }

        @Test
        fun `comments uses default collection comments when not set but enabled true`() {
            val resolver = resolverFrom(mapOf("commentsEnabled" to "true"))
            val lines = linesOf("commentsEnabled=", "commentsCollection=")

            configInjectors["comments"]!!.inject(lines, resolver)

            assertEquals("true", valueOf(lines, "commentsEnabled"))
            assertEquals("comments", valueOf(lines, "commentsCollection"))
        }

        @Test
        fun `newsletter injects when enabled true`() {
            val resolver = resolverFrom(mapOf("newsletterEnabled" to "true", "newsletterProvider" to "mc", "newsletterEndpoint" to "ep"))
            val lines = linesOf("newsletterEnabled=", "newsletterProvider=", "newsletterEndpoint=")

            configInjectors["newsletter"]!!.inject(lines, resolver)

            assertEquals("true", valueOf(lines, "newsletterEnabled"))
            assertEquals("mc", valueOf(lines, "newsletterProvider"))
            assertEquals("ep", valueOf(lines, "newsletterEndpoint"))
        }

        @Test
        fun `newsletter injects when provider non-blank even if enabled false`() {
            val resolver = resolverFrom(mapOf("newsletterEnabled" to "false", "newsletterProvider" to "mc"))
            val lines = linesOf("newsletterEnabled=", "newsletterProvider=", "newsletterEndpoint=")

            configInjectors["newsletter"]!!.inject(lines, resolver)

            assertEquals("false", valueOf(lines, "newsletterEnabled"))
            assertEquals("mc", valueOf(lines, "newsletterProvider"))
            assertEquals("", valueOf(lines, "newsletterEndpoint"))
        }

        @Test
        fun `newsletter injects nothing when enabled false and provider blank`() {
            val resolver = resolverFrom(emptyMap())
            val lines = linesOf("newsletterEnabled=", "newsletterProvider=")

            configInjectors["newsletter"]!!.inject(lines, resolver)

            assertEquals("", valueOf(lines, "newsletterEnabled"))
            assertEquals("", valueOf(lines, "newsletterProvider"))
        }
    }

    @Nested
    inner class AlwaysVariant {

        @Test
        fun `theme always injects 11 properties with defaults when resolver empty`() {
            val resolver = resolverFrom(emptyMap())
            val lines = linesOf(
                "themeMode=", "themePrimaryColor=", "themeSecondaryColor=", "themeFontFamily=",
                "themeLogoUrl=", "themeFaviconUrl=", "themeVariant=", "themeAccentColor=",
                "themeBackgroundColor=", "themeTextColor=", "themeHeadingFont="
            )

            configInjectors["theme"]!!.inject(lines, resolver)

            assertEquals("auto", valueOf(lines, "themeMode"))
            assertEquals("#0d6efd", valueOf(lines, "themePrimaryColor"))
            assertEquals("#6c757d", valueOf(lines, "themeSecondaryColor"))
            assertEquals("", valueOf(lines, "themeFontFamily"))
            assertEquals("", valueOf(lines, "themeLogoUrl"))
            assertEquals("", valueOf(lines, "themeFaviconUrl"))
            assertEquals("", valueOf(lines, "themeVariant"))
            assertEquals("", valueOf(lines, "themeAccentColor"))
            assertEquals("", valueOf(lines, "themeBackgroundColor"))
            assertEquals("", valueOf(lines, "themeTextColor"))
            assertEquals("", valueOf(lines, "themeHeadingFont"))
        }

        @Test
        fun `theme injects provided values over defaults`() {
            val resolver = resolverFrom(mapOf("themeMode" to "dark", "themePrimaryColor" to "#abc"))
            val lines = linesOf("themeMode=", "themePrimaryColor=")

            configInjectors["theme"]!!.inject(lines, resolver)

            assertEquals("dark", valueOf(lines, "themeMode"))
            assertEquals("#abc", valueOf(lines, "themePrimaryColor"))
        }

        @Test
        fun `theme adds properties when lines empty`() {
            val resolver = resolverFrom(mapOf("themeMode" to "dark"))
            val lines = linesOf()

            configInjectors["theme"]!!.inject(lines, resolver)

            assertTrue(lines.any { it == "themeMode=dark" })
            assertTrue(lines.any { it == "themePrimaryColor=#0d6efd" })
        }

        @Test
        fun `layout always injects layoutType with default FULL_WIDTH`() {
            val resolver = resolverFrom(emptyMap())
            val lines = linesOf("layoutType=")

            configInjectors["layout"]!!.inject(lines, resolver)

            assertEquals("FULL_WIDTH", valueOf(lines, "layoutType"))
        }

        @Test
        fun `layout injects provided value`() {
            val resolver = resolverFrom(mapOf("layoutType" to "CENTERED"))
            val lines = linesOf("layoutType=")

            configInjectors["layout"]!!.inject(lines, resolver)

            assertEquals("CENTERED", valueOf(lines, "layoutType"))
        }

        @Test
        fun `language always injects site dot language with default fr`() {
            val resolver = resolverFrom(emptyMap())
            val lines = linesOf("site.language=")

            configInjectors["language"]!!.inject(lines, resolver)

            assertEquals("fr", valueOf(lines, "site.language"))
        }

        @Test
        fun `language injects provided value`() {
            val resolver = resolverFrom(mapOf("language" to "ar"))
            val lines = linesOf("site.language=")

            configInjectors["language"]!!.inject(lines, resolver)

            assertEquals("ar", valueOf(lines, "site.language"))
        }
    }

    @Nested
    inner class RegistryContract {

        @Test
        fun `configInjectors exposes exactly 9 domains`() {
            assertEquals(9, configInjectors.size)
        }

        @Test
        fun `configInjectors exposes all expected domain keys`() {
            assertEquals(
                listOf("analytics", "comments", "firebase", "firebaseAuth", "googleForms", "language", "layout", "newsletter", "theme"),
                configInjectors.keys.toList().sorted()
            )
        }
    }
}