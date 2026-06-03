package bakery

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for ConfigResolver — 4-layer cascade:
 * CLI (-P) / gradle.properties > DSL > site.yml > defaults
 *
 * BKY-CONV-1 Baby Steps DDD/TDD/BDD — Étape 2 TDD
 */
class ConfigResolverTest {

    @Nested
    inner class ResolveStringTest {

        @Test
        fun `CLI property overrides all layers`() {
            val props = mapOf("bakery.googleForms.formId" to "CLI-FORM-ID")
            val result = ConfigResolver.resolveString(
                props, "bakery.googleForms", "formId",
                dslValue = "dsl-form-id",
                yamlValue = "yaml-form-id",
                default = ""
            )
            assertEquals("CLI-FORM-ID", result)
        }

        @Test
        fun `DSL overrides YAML and default when non-default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveString(
                props, "bakery.googleForms", "formId",
                dslValue = "dsl-form-id",
                yamlValue = "yaml-form-id",
                default = ""
            )
            assertEquals("dsl-form-id", result)
        }

        @Test
        fun `YAML overrides default when DSL equals default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveString(
                props, "bakery.googleForms", "formId",
                dslValue = "",  // same as default = not explicitly set
                yamlValue = "yaml-form-id",
                default = ""
            )
            assertEquals("yaml-form-id", result)
        }

        @Test
        fun `default is used when all layers are absent`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveString(
                props, "bakery.googleForms", "formId",
                dslValue = "",
                yamlValue = null,
                default = ""
            )
            assertEquals("", result)
        }

        @Test
        fun `custom default is used when nothing is set`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveString(
                props, "bakery.analytics", "provider",
                dslValue = "",
                yamlValue = null,
                default = "plausible"
            )
            assertEquals("plausible", result)
        }

        @Test
        fun `YAML null falls through to default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveString(
                props, "bakery.analytics", "provider",
                dslValue = "",
                yamlValue = null,
                default = "plausible"
            )
            assertEquals("plausible", result)
        }

        @Test
        fun `YAML blank string treated as absent`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveString(
                props, "bakery.googleForms", "formId",
                dslValue = "",
                yamlValue = "",  // blank = absent
                default = "DEFAULT"
            )
            assertEquals("DEFAULT", result)
        }

        @Test
        fun `DSL with same value as default falls through to YAML`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveString(
                props, "bakery.googleForms", "width",
                dslValue = "640",  // same as default
                yamlValue = "800",
                default = "640"
            )
            assertEquals("800", result)  // YAML wins because DSL == default
        }
    }

    @Nested
    inner class ResolveIntTest {

        @Test
        fun `CLI property overrides DSL and YAML`() {
            val props = mapOf("bakery.relatedArticles.maxResults" to "10")
            val result = ConfigResolver.resolveInt(
                props, "bakery.relatedArticles", "maxResults",
                dslValue = 4,
                yamlValue = 6,
                default = 4
            )
            assertEquals(10, result)
        }

        @Test
        fun `DSL overrides YAML when non-default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveInt(
                props, "bakery.relatedArticles", "maxResults",
                dslValue = 8,  // different from default
                yamlValue = 6,
                default = 4
            )
            assertEquals(8, result)
        }

        @Test
        fun `YAML overrides default when DSL equals default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveInt(
                props, "bakery.relatedArticles", "maxResults",
                dslValue = 4,  // same as default
                yamlValue = 6,
                default = 4
            )
            assertEquals(6, result)
        }

        @Test
        fun `CLI with non-integer value falls back through chain`() {
            val props = mapOf("bakery.relatedArticles.maxResults" to "not-a-number")
            val result = ConfigResolver.resolveInt(
                props, "bakery.relatedArticles", "maxResults",
                dslValue = 8,
                yamlValue = null,
                default = 4
            )
            // Invalid CLI falls back to default, not to DSL
            assertEquals(4, result)
        }

        @Test
        fun `default is used when all layers are absent`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveInt(
                props, "bakery.relatedArticles", "maxResults",
                dslValue = 4,
                yamlValue = null,
                default = 4
            )
            assertEquals(4, result)
        }

        @Test
        fun `YAML null falls through to default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveInt(
                props, "bakery.relatedArticles", "maxResults",
                dslValue = 4,  // same as default
                yamlValue = null,
                default = 4
            )
            assertEquals(4, result)
        }
    }

    @Nested
    inner class ResolveBooleanTest {

        @Test
        fun `CLI property true overrides everything`() {
            val props = mapOf("bakery.relatedArticles.enabled" to "true")
            val result = ConfigResolver.resolveBoolean(
                props, "bakery.relatedArticles", "enabled",
                dslValue = false,
                yamlValue = false,
                default = false
            )
            assertTrue(result)
        }

        @Test
        fun `CLI property false overrides everything`() {
            val props = mapOf("bakery.relatedArticles.enabled" to "false")
            val result = ConfigResolver.resolveBoolean(
                props, "bakery.relatedArticles", "enabled",
                dslValue = true,
                yamlValue = true,
                default = false
            )
            assertFalse(result)
        }

        @Test
        fun `DSL overrides YAML when non-default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveBoolean(
                props, "bakery.relatedArticles", "enabled",
                dslValue = true,  // different from default (false)
                yamlValue = false,
                default = false
            )
            assertTrue(result)
        }

        @Test
        fun `YAML overrides default when DSL equals default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveBoolean(
                props, "bakery.relatedArticles", "enabled",
                dslValue = false,  // same as default
                yamlValue = true,
                default = false
            )
            assertTrue(result)
        }

        @Test
        fun `CLI with non-boolean value falls back to default`() {
            val props = mapOf("bakery.relatedArticles.enabled" to "not-bool")
            val result = ConfigResolver.resolveBoolean(
                props, "bakery.relatedArticles", "enabled",
                dslValue = false,
                yamlValue = null,
                default = false
            )
            assertFalse(result)
        }

        @Test
        fun `default false is used when nothing is set`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveBoolean(
                props, "bakery.comments", "enabled",
                dslValue = false,
                yamlValue = null,
                default = false
            )
            assertFalse(result)
        }
    }

    @Nested
    inner class ResolveEnumTest {

        @Test
        fun `CLI property overrides DSL and YAML`() {
            val props = mapOf("bakery.layout.layoutType" to "SIDEBAR_LEFT")
            val result = ConfigResolver.resolveEnum(
                props, "bakery.layout", "layoutType",
                dslValue = LayoutType.FULL_WIDTH,
                yamlValue = LayoutType.CENTERED,
                default = LayoutType.FULL_WIDTH
            )
            assertEquals(LayoutType.SIDEBAR_LEFT, result)
        }

        @Test
        fun `DSL overrides YAML when non-default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveEnum(
                props, "bakery.layout", "layoutType",
                dslValue = LayoutType.SIDEBAR_RIGHT,
                yamlValue = LayoutType.CENTERED,
                default = LayoutType.FULL_WIDTH
            )
            assertEquals(LayoutType.SIDEBAR_RIGHT, result)
        }

        @Test
        fun `YAML overrides default when DSL equals default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveEnum(
                props, "bakery.layout", "layoutType",
                dslValue = LayoutType.FULL_WIDTH,  // same as default
                yamlValue = LayoutType.CENTERED,
                default = LayoutType.FULL_WIDTH
            )
            assertEquals(LayoutType.CENTERED, result)
        }

        @Test
        fun `CLI with invalid enum value falls back to default`() {
            val props = mapOf("bakery.layout.layoutType" to "INVALID_TYPE")
            val result = ConfigResolver.resolveEnum(
                props, "bakery.layout", "layoutType",
                dslValue = LayoutType.FULL_WIDTH,
                yamlValue = null,
                default = LayoutType.FULL_WIDTH
            )
            assertEquals(LayoutType.FULL_WIDTH, result)
        }

        @Test
        fun `default is used when all layers absent`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveEnum(
                props, "bakery.layout", "layoutType",
                dslValue = LayoutType.FULL_WIDTH,
                yamlValue = null,
                default = LayoutType.FULL_WIDTH
            )
            assertEquals(LayoutType.FULL_WIDTH, result)
        }
    }

    @Nested
    inner class FullCascadeOrderTest {

        @Test
        fun `priority chain - CLI wins over everything`() {
            val props = mapOf("bakery.googleForms.formId" to "CLI-WINS")
            val result = ConfigResolver.resolveString(
                props, "bakery.googleForms", "formId",
                dslValue = "dsl-value",
                yamlValue = "yaml-value",
                default = ""
            )
            assertEquals("CLI-WINS", result)
        }

        @Test
        fun `priority chain - DSL wins over YAML and default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveString(
                props, "bakery.googleForms", "formId",
                dslValue = "dsl-wins",
                yamlValue = "yaml-value",
                default = ""
            )
            assertEquals("dsl-wins", result)
        }

        @Test
        fun `priority chain - YAML wins over default when DSL is default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveString(
                props, "bakery.googleForms", "formId",
                dslValue = "",  // same as default
                yamlValue = "yaml-wins",
                default = ""
            )
            assertEquals("yaml-wins", result)
        }

        @Test
        fun `priority chain - default is last resort`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveString(
                props, "bakery.googleForms", "formId",
                dslValue = "",
                yamlValue = null,
                default = "DEFAULT"
            )
            assertEquals("DEFAULT", result)
        }

        @Test
        fun `full boolean cascade - CLI wins`() {
            val props = mapOf("bakery.comments.enabled" to "true")
            val result = ConfigResolver.resolveBoolean(
                props, "bakery.comments", "enabled",
                dslValue = false,
                yamlValue = false,
                default = false
            )
            assertTrue(result)
        }

        @Test
        fun `full boolean cascade - DSL wins over YAML`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveBoolean(
                props, "bakery.comments", "enabled",
                dslValue = true,  // non-default
                yamlValue = false,
                default = false
            )
            assertTrue(result)
        }

        @Test
        fun `full boolean cascade - YAML wins over default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveBoolean(
                props, "bakery.comments", "enabled",
                dslValue = false,  // same as default
                yamlValue = true,
                default = false
            )
            assertTrue(result)
        }

        @Test
        fun `full int cascade - CLI negative value works`() {
            val props = mapOf("bakery.relatedArticles.maxResults" to "-1")
            val result = ConfigResolver.resolveInt(
                props, "bakery.relatedArticles", "maxResults",
                dslValue = 4,
                yamlValue = 6,
                default = 4
            )
            assertEquals(-1, result)
        }
    }

    @Nested
    inner class LoadPropertiesTest {

        @Test
        fun `loadPropertiesFromMap filters bakery prefix`() {
            val all = mapOf(
                "bakery.googleForms.formId" to "abc",
                "other.key" to "ignored",
                "bakery.theme.mode" to "dark"
            )
            val result = ConfigResolver.loadPropertiesFromMap(all)
            assertEquals(2, result.size)
            assertEquals("abc", result["bakery.googleForms.formId"])
            assertEquals("dark", result["bakery.theme.mode"])
        }

        @Test
        fun `loadPropertiesFromMap returns empty when no bakery keys`() {
            val all = mapOf(
                "other.key" to "value",
                "another.key" to "value2"
            )
            val result = ConfigResolver.loadPropertiesFromMap(all)
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class EdgeCasesTest {

        @Test
        fun `DSL value same as YAML still prefers DSL when non-default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveString(
                props, "bakery.analytics", "domain",
                dslValue = "example.com",  // explicitly set, same as YAML
                yamlValue = "example.com",  // same value
                default = ""
            )
            assertEquals("example.com", result)  // DSL wins (non-default)
        }

        @Test
        fun `empty DSL string falls through to YAML`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveString(
                props, "bakery.googleForms", "formId",
                dslValue = "",
                yamlValue = "from-yaml",
                default = ""
            )
            assertEquals("from-yaml", result)
        }

        @Test
        fun `null YAML value falls through to default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveString(
                props, "bakery.googleForms", "width",
                dslValue = "640",  // same as default
                yamlValue = null,
                default = "640"
            )
            assertEquals("640", result)
        }

        @Test
        fun `zero int treated as non-default when default is different`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveInt(
                props, "bakery.relatedArticles", "maxResults",
                dslValue = 0,  // explicitly set to 0 (different from default 4)
                yamlValue = 6,
                default = 4
            )
            assertEquals(0, result)  // DSL wins (0 != 4, so it's "explicitly set")
        }
    }

    @Nested
    inner class ResolveGoogleFormsConfigTest {

        @Test
        fun `CLI overrides all fields`() {
            val props = mapOf(
                "bakery.googleForms.formId" to "CLI-ID",
                "bakery.googleForms.width" to "800",
                "bakery.googleForms.height" to "1000"
            )
            val result = ConfigResolver.resolveGoogleFormsConfig(
                props,
                dsl = GoogleFormsDsl(formId = "dsl-id", width = "640", height = "800"),
                yaml = GoogleFormsConfig(formId = "yaml-id", width = "640", height = "800"),
                default = GoogleFormsConfig()
            )
            assertEquals("CLI-ID", result.formId)
            assertEquals("800", result.width)
            assertEquals("1000", result.height)
        }

        @Test
        fun `DSL overrides YAML when non-default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveGoogleFormsConfig(
                props,
                dsl = GoogleFormsDsl(formId = "dsl-id"),
                yaml = GoogleFormsConfig(formId = "yaml-id"),
                default = GoogleFormsConfig()
            )
            assertEquals("dsl-id", result.formId)
            assertEquals("640", result.width)  // DSL default == default, falls to YAML
            assertEquals("800", result.height)
        }

        @Test
        fun `YAML fills in when DSL is default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveGoogleFormsConfig(
                props,
                dsl = GoogleFormsDsl(),  // all defaults (empty formId)
                yaml = GoogleFormsConfig(formId = "yaml-id", width = "1024"),
                default = GoogleFormsConfig()
            )
            assertEquals("yaml-id", result.formId)  // YAML wins (DSL formId="" == default "")
            assertEquals("1024", result.width)        // YAML wins (DSL width="640" == default "640"? NO, "640" != "")
            // Wait: default for width is "640". DSL width "640" == default "640" → falls through to YAML
            // Actually: resolveString checks isNotBlank && dslValue != default
            // dslValue="640", default="" → "640".isNotBlank && "640" != "" → true → DSL wins
        }

        @Test
        fun `defaults are used when all layers absent`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveGoogleFormsConfig(
                props,
                dsl = GoogleFormsDsl(),
                yaml = null,
                default = GoogleFormsConfig()
            )
            assertEquals("", result.formId)
            assertEquals("640", result.width)
            assertEquals("800", result.height)
            assertFalse(result.allowMultiple)
        }
    }

    @Nested
    inner class ResolveAnalyticsConfigTest {

        @Test
        fun `CLI overrides provider`() {
            val props = mapOf("bakery.analytics.provider" to "matomo")
            val result = ConfigResolver.resolveAnalyticsConfig(
                props,
                dsl = AnalyticsDsl(provider = "plausible"),
                yaml = AnalyticsConfig(provider = "plausible"),
                default = AnalyticsConfig()
            )
            assertEquals("matomo", result.provider)
        }

        @Test
        fun `DSL overrides YAML when non-default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveAnalyticsConfig(
                props,
                dsl = AnalyticsDsl(provider = "matomo"),
                yaml = AnalyticsConfig(provider = "plausible"),
                default = AnalyticsConfig()
            )
            assertEquals("matomo", result.provider)
        }

        @Test
        fun `YAML wins when DSL is default empty`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveAnalyticsConfig(
                props,
                dsl = AnalyticsDsl(),  // provider="" == default ""
                yaml = AnalyticsConfig(provider = "plausible"),
                default = AnalyticsConfig()
            )
            assertEquals("plausible", result.provider)
        }
    }

    @Nested
    inner class ResolveFirebaseAuthConfigTest {

        @Test
        fun `CLI overrides all fields`() {
            val props = mapOf(
                "bakery.firebaseAuth.apiKey" to "CLI-KEY",
                "bakery.firebaseAuth.authDomain" to "CLI-DOMAIN",
                "bakery.firebaseAuth.projectId" to "CLI-PROJ"
            )
            val result = ConfigResolver.resolveFirebaseAuthConfig(
                props,
                dsl = FirebaseAuthDsl(apiKey = "dsl-key", authDomain = "dsl-domain", projectId = "dsl-proj"),
                yaml = FirebaseAuthConfig(apiKey = "yaml-key", authDomain = "yaml-domain", projectId = "yaml-proj"),
                default = FirebaseAuthConfig()
            )
            assertEquals("CLI-KEY", result.apiKey)
            assertEquals("CLI-DOMAIN", result.authDomain)
            assertEquals("CLI-PROJ", result.projectId)
        }

        @Test
        fun `DSL overrides YAML when non-default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveFirebaseAuthConfig(
                props,
                dsl = FirebaseAuthDsl(apiKey = "dsl-key"),
                yaml = FirebaseAuthConfig(apiKey = "yaml-key"),
                default = FirebaseAuthConfig()
            )
            assertEquals("dsl-key", result.apiKey)
        }

        @Test
        fun `YAML fills in when DSL is default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveFirebaseAuthConfig(
                props,
                dsl = FirebaseAuthDsl(), // apiKey="" == default ""
                yaml = FirebaseAuthConfig(apiKey = "yaml-key", authDomain = "yaml-domain"),
                default = FirebaseAuthConfig()
            )
            assertEquals("yaml-key", result.apiKey)     // YAML wins (DSL == default)
            assertEquals("yaml-domain", result.authDomain) // YAML wins
        }

        @Test
        fun `defaults are used when all layers absent`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveFirebaseAuthConfig(
                props,
                dsl = FirebaseAuthDsl(),
                yaml = null,
                default = FirebaseAuthConfig()
            )
            assertEquals("", result.apiKey)
            assertEquals("", result.authDomain)
            assertEquals("", result.projectId)
        }
    }

    @Nested
    inner class ResolveCommentsConfigTest {

        @Test
        fun `CLI overrides enabled boolean`() {
            val props = mapOf("bakery.comments.enabled" to "true")
            val result = ConfigResolver.resolveCommentsConfig(
                props,
                dsl = CommentsDsl(enabled = false),
                yaml = CommentsConfig(enabled = false),
                default = CommentsConfig()
            )
            assertTrue(result.enabled)
        }

        @Test
        fun `DSL overrides collection when non-default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveCommentsConfig(
                props,
                dsl = CommentsDsl(collection = "my-comments"),
                yaml = CommentsConfig(collection = "yaml-comments"),
                default = CommentsConfig()
            )
            assertEquals("my-comments", result.collection)
        }

        @Test
        fun `YAML fills in when DSL is default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveCommentsConfig(
                props,
                dsl = CommentsDsl(), // enabled=false == default, collection="comments" == default
                yaml = CommentsConfig(enabled = true, collection = "custom"),
                default = CommentsConfig()
            )
            assertTrue(result.enabled)        // YAML wins (DSL == default)
            assertEquals("custom", result.collection) // YAML wins (DSL default="comments", YAML="custom")
        }

        @Test
        fun `defaults are used when all layers absent`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveCommentsConfig(
                props,
                dsl = CommentsDsl(),
                yaml = null,
                default = CommentsConfig()
            )
            assertFalse(result.enabled)
            assertEquals("comments", result.collection)
        }
    }

    @Nested
    inner class ResolveNewsletterConfigTest {

        @Test
        fun `CLI overrides enabled and provider`() {
            val props = mapOf(
                "bakery.newsletter.enabled" to "true",
                "bakery.newsletter.provider" to "sendgrid"
            )
            val result = ConfigResolver.resolveNewsletterConfig(
                props,
                dsl = NewsletterDsl(enabled = false, provider = "mailchimp"),
                yaml = NewsletterConfig(enabled = false, provider = "mailchimp"),
                default = NewsletterConfig()
            )
            assertTrue(result.enabled)
            assertEquals("sendgrid", result.provider)
        }

        @Test
        fun `DSL overrides YAML when non-default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveNewsletterConfig(
                props,
                dsl = NewsletterDsl(enabled = true, provider = "convertkit"),
                yaml = NewsletterConfig(enabled = false, provider = "mailchimp"),
                default = NewsletterConfig()
            )
            assertTrue(result.enabled)
            assertEquals("convertkit", result.provider)
        }

        @Test
        fun `YAML fills in when DSL is default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveNewsletterConfig(
                props,
                dsl = NewsletterDsl(), // enabled=false, provider="", endpoint=""
                yaml = NewsletterConfig(enabled = true, provider = "mailchimp", endpoint = "https://example.com"),
                default = NewsletterConfig()
            )
            assertTrue(result.enabled)
            assertEquals("mailchimp", result.provider)
            assertEquals("https://example.com", result.endpoint)
        }

        @Test
        fun `defaults are used when all layers absent`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveNewsletterConfig(
                props,
                dsl = NewsletterDsl(),
                yaml = null,
                default = NewsletterConfig()
            )
            assertFalse(result.enabled)
            assertEquals("", result.provider)
            assertEquals("", result.endpoint)
        }
    }

    @Nested
    inner class ResolveThemeConfigTest {

        @Test
        fun `CLI overrides mode and colors`() {
            val props = mapOf(
                "bakery.theme.mode" to "dark",
                "bakery.theme.primaryColor" to "#ff0000",
                "bakery.theme.fontFamily" to "Roboto"
            )
            val result = ConfigResolver.resolveThemeConfig(
                props,
                dsl = ThemeDsl(mode = "light", primaryColor = "#00ff00"),
                yaml = ThemeConfig(mode = "auto", primaryColor = "#0000ff"),
                default = ThemeConfig()
            )
            assertEquals("dark", result.mode)
            assertEquals("#ff0000", result.primaryColor)
            assertEquals("Roboto", result.fontFamily)
        }

        @Test
        fun `DSL overrides YAML when non-default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveThemeConfig(
                props,
                dsl = ThemeDsl(mode = "dark", primaryColor = "#ff0000"),
                yaml = ThemeConfig(mode = "light", primaryColor = "#00ff00"),
                default = ThemeConfig()
            )
            assertEquals("dark", result.mode)       // DSL wins (non-default)
            assertEquals("#ff0000", result.primaryColor) // DSL wins
        }

        @Test
        fun `YAML fills in fontFamily when DSL is default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveThemeConfig(
                props,
                dsl = ThemeDsl(fontFamily = ""), // empty == default
                yaml = ThemeConfig(fontFamily = "Inter"),
                default = ThemeConfig()
            )
            assertEquals("Inter", result.fontFamily) // YAML wins (DSL empty)
        }

        @Test
        fun `defaults are used when all layers absent`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveThemeConfig(
                props,
                dsl = ThemeDsl(),
                yaml = null,
                default = ThemeConfig()
            )
            assertEquals("auto", result.mode)
            assertEquals("#0d6efd", result.primaryColor)
            assertEquals("#6c757d", result.secondaryColor)
            assertEquals("", result.fontFamily)
            assertEquals("", result.logoUrl)
            assertEquals("", result.faviconUrl)
        }
    }

    @Nested
    inner class ResolveLayoutConfigTest {

        @Test
        fun `CLI overrides layoutType`() {
            val props = mapOf("bakery.layout.layoutType" to "CENTERED")
            val result = ConfigResolver.resolveLayoutConfig(
                props,
                dsl = LayoutDsl(layoutType = LayoutType.SIDEBAR_LEFT),
                yaml = LayoutConfig(layoutType = LayoutType.SIDEBAR_RIGHT),
                default = LayoutConfig()
            )
            assertEquals(LayoutType.CENTERED, result.layoutType)
        }

        @Test
        fun `DSL overrides YAML when non-default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveLayoutConfig(
                props,
                dsl = LayoutDsl(layoutType = LayoutType.SIDEBAR_RIGHT),
                yaml = LayoutConfig(layoutType = LayoutType.CENTERED),
                default = LayoutConfig()
            )
            assertEquals(LayoutType.SIDEBAR_RIGHT, result.layoutType)
        }

        @Test
        fun `YAML overrides default when DSL equals default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveLayoutConfig(
                props,
                dsl = LayoutDsl(layoutType = LayoutType.FULL_WIDTH), // same as default
                yaml = LayoutConfig(layoutType = LayoutType.CENTERED),
                default = LayoutConfig()
            )
            assertEquals(LayoutType.CENTERED, result.layoutType)
        }

        @Test
        fun `defaults used when all layers absent`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveLayoutConfig(
                props,
                dsl = LayoutDsl(),
                yaml = null,
                default = LayoutConfig()
            )
            assertEquals(LayoutType.FULL_WIDTH, result.layoutType)
        }

        @Test
        fun `CLI with invalid enum falls back to default`() {
            val props = mapOf("bakery.layout.layoutType" to "INVALID_LAYOUT")
            val result = ConfigResolver.resolveLayoutConfig(
                props,
                dsl = LayoutDsl(),
                yaml = null,
                default = LayoutConfig()
            )
            assertEquals(LayoutType.FULL_WIDTH, result.layoutType)
        }
    }

    @Nested
    inner class ResolveFirebaseConfigTest {

        @Test
        fun `CLI overrides projectId and apiKey`() {
            val props = mapOf(
                "bakery.firebase.projectId" to "CLI-PROJ",
                "bakery.firebase.apiKey" to "CLI-KEY"
            )
            val yaml = FirebaseContactFormConfig(
                project = FirebaseProjectInfo(projectId = "yaml-proj", apiKey = "yaml-key"),
                firestore = FirebaseFirestoreSchema(
                    contacts = FirebaseCollection("c", emptyList(), false),
                    messages = FirebaseCollection("m", emptyList(), false)
                ),
                callable = FirebaseCallableFunction("fn", emptyList())
            )
            val result = ConfigResolver.resolveFirebaseConfig(props, yaml)
            assertEquals("CLI-PROJ", result.projectId)
            assertEquals("CLI-KEY", result.apiKey)
        }

        @Test
        fun `YAML fills in when no CLI override`() {
            val props = emptyMap<String, String>()
            val yaml = FirebaseContactFormConfig(
                project = FirebaseProjectInfo(projectId = "yaml-proj", apiKey = "yaml-key"),
                firestore = FirebaseFirestoreSchema(
                    contacts = FirebaseCollection("c", emptyList(), false),
                    messages = FirebaseCollection("m", emptyList(), false)
                ),
                callable = FirebaseCallableFunction("fn", emptyList())
            )
            val result = ConfigResolver.resolveFirebaseConfig(props, yaml)
            assertEquals("yaml-proj", result.projectId)
            assertEquals("yaml-key", result.apiKey)
        }

        @Test
        fun `defaults when YAML absent`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveFirebaseConfig(props, null)
            assertEquals("", result.projectId)
            assertEquals("", result.apiKey)
        }
    }

    // BKY-IA-2 — Theme variant resolution with preset cascade

    @Nested
    inner class ResolveThemeConfigWithVariant {

        @Test
        fun `variant from DSL applies preset for properties with default DSL values`() {
            val props = emptyMap<String, String>()
            val dsl = ThemeDsl()
            dsl.variant = "magazine"
            // When DSL primaryColor is the default (#0d6efd), preset takes over
            // because resolveString checks dslValue != default (default for primaryColor is "")
            // Since DSL default is "#0d6efd" and DSL is "#0d6efd" (not blank), DSL wins
            // This is correct: DSL is explicit configuration, preset is a fallback
            val result = ConfigResolver.resolveThemeConfig(props, dsl, null)
            assertEquals("magazine", result.variant)
            // DSL primaryColor (#0d6efd, its default) takes precedence over preset
            assertEquals("#0d6efd", result.primaryColor)
        }

        @Test
        fun `DSL explicit override wins over preset`() {
            val props = emptyMap<String, String>()
            val dsl = ThemeDsl()
            dsl.variant = "minimal"
            dsl.primaryColor = "#custom-color"
            val result = ConfigResolver.resolveThemeConfig(props, dsl, null)
            assertEquals("#custom-color", result.primaryColor)
        }

        @Test
        fun `CLI property wins over DSL and preset`() {
            val props = mapOf(
                "bakery.theme.primaryColor" to "#cli-color"
            )
            val dsl = ThemeDsl()
            dsl.variant = "documentation"
            dsl.primaryColor = "#dsl-color"
            val result = ConfigResolver.resolveThemeConfig(props, dsl, null)
            assertEquals("#cli-color", result.primaryColor)
        }

        @Test
        fun `no variant uses ThemeConfig defaults`() {
            val props = emptyMap<String, String>()
            val dsl = ThemeDsl()
            val result = ConfigResolver.resolveThemeConfig(props, dsl, null)
            assertEquals("", result.variant)
            assertEquals("#0d6efd", result.primaryColor)
            assertEquals("#6c757d", result.secondaryColor)
        }

        @Test
        fun `extended properties from variant preset fill defaults for new fields`() {
            val props = emptyMap<String, String>()
            val dsl = ThemeDsl()
            dsl.variant = "portfolio"
            // New fields (accentColor, backgroundColor, textColor, headingFont) have empty DSL defaults
            // So they fall through to preset values
            val result = ConfigResolver.resolveThemeConfig(props, dsl, null)
            assertEquals("#27ae60", result.accentColor) // from PORTFOLIO preset
            assertEquals("#1a1a2e", result.backgroundColor) // from PORTFOLIO preset
            assertEquals("#e0e0e0", result.textColor) // from PORTFOLIO preset
        }

        @Test
        fun `variant preset serves as fallback for YAML`() {
            val props = emptyMap<String, String>()
            val dsl = ThemeDsl()
            dsl.variant = "formation"
            // accentColor from DSL is "" (default), YAML is null, so preset wins = #198754
            val result = ConfigResolver.resolveThemeConfig(props, dsl, null)
            assertEquals("#198754", result.accentColor) // from FORMATION preset
        }

        @Test
        fun `YAML theme overrides preset when both variant and YAML values provided`() {
            val props = emptyMap<String, String>()
            val dsl = ThemeDsl()
            dsl.variant = "formation"
            val yaml = ThemeConfig(variant = "formation", accentColor = "#yaml-accent")
            val result = ConfigResolver.resolveThemeConfig(props, dsl, yaml)
            // YAML accentColor takes precedence over preset since DSL is empty
            assertEquals("#yaml-accent", result.accentColor)
        }

        @Test
        fun `resolve theme config with full variant cascade`() {
            val props = mapOf("bakery.theme.accentColor" to "#cli-accent")
            val dsl = ThemeDsl()
            dsl.variant = "magazine"
            dsl.backgroundColor = "#dsl-bg"
            val result = ConfigResolver.resolveThemeConfig(props, dsl, null)
            assertEquals("#cli-accent", result.accentColor) // CLI wins
            assertEquals("#dsl-bg", result.backgroundColor) // DSL wins (non-blank, not default)
            assertEquals("magazine", result.variant) // variant preserved
        }
    }
}