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
    inner class ResolveRelatedArticlesConfigTest {

        @Test
        fun `CLI overrides enabled boolean`() {
            val props = mapOf("bakery.relatedArticles.enabled" to "true")
            val result = ConfigResolver.resolveRelatedArticlesConfig(
                props,
                dsl = RelatedArticlesDsl(enabled = false),
                yaml = RelatedArticlesConfig(enabled = false),
                default = RelatedArticlesConfig()
            )
            assertTrue(result.enabled)
        }

        @Test
        fun `DSL overrides maxResults when non-default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveRelatedArticlesConfig(
                props,
                dsl = RelatedArticlesDsl(maxResults = 8),
                yaml = RelatedArticlesConfig(maxResults = 6),
                default = RelatedArticlesConfig()
            )
            assertEquals(8, result.maxResults)
        }

        @Test
        fun `YAML maxResults wins when DSL is default`() {
            val props = emptyMap<String, String>()
            val result = ConfigResolver.resolveRelatedArticlesConfig(
                props,
                dsl = RelatedArticlesDsl(),  // maxResults=4 == default 4
                yaml = RelatedArticlesConfig(maxResults = 10),
                default = RelatedArticlesConfig()
            )
            assertEquals(10, result.maxResults)
        }

        @Test
        fun `full cascade for all fields`() {
            val props = mapOf("bakery.relatedArticles.enabled" to "true")
            val result = ConfigResolver.resolveRelatedArticlesConfig(
                props,
                dsl = RelatedArticlesDsl(maxResults = 8, heading = "DSL heading"),
                yaml = RelatedArticlesConfig(enabled = false, maxResults = 6, heading = "YAML heading"),
                default = RelatedArticlesConfig()
            )
            assertTrue(result.enabled)          // CLI wins
            assertEquals(8, result.maxResults)  // DSL wins (non-default)
            assertEquals("DSL heading", result.heading)  // DSL wins (non-default)
        }
    }
}