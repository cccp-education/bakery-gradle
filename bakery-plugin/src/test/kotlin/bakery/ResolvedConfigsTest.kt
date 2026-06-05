package bakery

import bakery.injection.configInjectors
import bakery.injection.updateProperty
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ResolvedConfigsTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().withProjectDir(tempDir).build()
    }

    private fun createExtension(): BakeryExtension =
        project.extensions.create("bakery_test", BakeryExtension::class.java) as BakeryExtension

    private fun createSite(): SiteConfiguration = SiteConfiguration()

    @Nested
    inner class ResolveAllConfigsTest {

        @Test
        fun `resolves all 8 configs with defaults when nothing is set`() {
            val props = emptyMap<String, String>()
            val extension = createExtension()
            val site = createSite()

            val (configs, errors) = ConfigResolver.resolveAll(props, extension, site)

            assertAll(
                { assertEquals("", configs.firebase.apiKey) },
                { assertEquals("", configs.firebase.projectId) },
                { assertEquals("", configs.googleForms.formId) },
                { assertEquals("", configs.firebaseAuth.apiKey) },
                { assertEquals(false, configs.comments.enabled) },
                { assertEquals("", configs.analytics.provider) },
                { assertEquals(false, configs.newsletter.enabled) },
                { assertEquals("auto", configs.theme.mode) },
                { assertEquals(LayoutType.FULL_WIDTH, configs.layout.layoutType) },
                { assertTrue(errors.isEmpty(), "No errors expected with defaults") }
            )
        }

        @Test
        fun `resolves all 8 configs with CLI properties overriding everything`() {
            val props = mapOf(
                "bakery.firebase.apiKey" to "cli-api-key",
                "bakery.firebase.projectId" to "cli-project-id",
                "bakery.googleForms.formId" to "cli-form-id",
                "bakery.firebaseAuth.apiKey" to "cli-auth-key",
                "bakery.firebaseAuth.authDomain" to "cli-auth-domain",
                "bakery.firebaseAuth.projectId" to "cli-auth-project",
                "bakery.analytics.provider" to "cli-matomo",
                "bakery.analytics.domain" to "cli-domain.com",
                "bakery.newsletter.enabled" to "true",
                "bakery.theme.mode" to "dark",
                "bakery.layout.layoutType" to "SIDEBAR_LEFT"
            )
            val extension = createExtension()
            val site = createSite()

            val (configs, errors) = ConfigResolver.resolveAll(props, extension, site)

            assertAll(
                { assertEquals("cli-api-key", configs.firebase.apiKey) },
                { assertEquals("cli-project-id", configs.firebase.projectId) },
                { assertEquals("cli-form-id", configs.googleForms.formId) },
                { assertEquals("cli-auth-key", configs.firebaseAuth.apiKey) },
                { assertEquals("cli-auth-domain", configs.firebaseAuth.authDomain) },
                { assertEquals("cli-matomo", configs.analytics.provider) },
                { assertTrue(configs.newsletter.enabled) },
                { assertEquals("dark", configs.theme.mode) },
                { assertEquals(LayoutType.SIDEBAR_LEFT, configs.layout.layoutType) },
                { assertTrue(errors.isEmpty(), "No errors expected with CLI override") }
            )
        }

        @Test
        fun `resolves all 8 configs with YAML values when DSL equals defaults`() {
            val props = emptyMap<String, String>()
            val extension = createExtension()
            val site = SiteConfiguration(
                firebase = FirebaseContactFormConfig(
                    project = FirebaseProjectInfo(projectId = "yaml-project", apiKey = "yaml-key"),
                    firestore = FirebaseFirestoreSchema(
                        contacts = FirebaseCollection("contacts", emptyList(), false),
                        messages = FirebaseCollection("messages", emptyList(), false)
                    ),
                    callable = FirebaseCallableFunction("sendMail", emptyList())
                ),
                googleForms = GoogleFormsConfig(formId = "yaml-form", width = "800", height = "600"),
                firebaseAuth = FirebaseAuthConfig(apiKey = "yaml-auth-key", authDomain = "yaml-domain", projectId = "yaml-proj"),
                comments = CommentsConfig(enabled = true, collection = "yaml-comments"),
                analytics = AnalyticsConfig(provider = "yaml-plausible", domain = "yaml-domain.com"),
                newsletter = NewsletterConfig(enabled = true, provider = "yaml-mailchimp", endpoint = "https://yaml.api"),
                theme = ThemeConfig(mode = "light", primaryColor = "#yaml-color"),
                layout = LayoutConfig(layoutType = LayoutType.CENTERED)
            )

            val (configs, errors) = ConfigResolver.resolveAll(props, extension, site)

            assertAll(
                { assertEquals("yaml-key", configs.firebase.apiKey) },
                { assertEquals("yaml-project", configs.firebase.projectId) },
                { assertEquals("yaml-form", configs.googleForms.formId) },
                { assertEquals("yaml-auth-key", configs.firebaseAuth.apiKey) },
                { assertTrue(configs.comments.enabled) },
                { assertEquals("yaml-plausible", configs.analytics.provider) },
                { assertTrue(configs.newsletter.enabled) },
                { assertEquals("light", configs.theme.mode) },
                { assertEquals(LayoutType.CENTERED, configs.layout.layoutType) },
                { assertTrue(errors.isEmpty()) }
            )
        }

        @Test
        fun `DSL values override YAML when explicitly set`() {
            val props = emptyMap<String, String>()
            val extension = createExtension()
            extension.googleForms.formId = "dsl-form"
            extension.analytics.provider = "dsl-matomo"
            extension.newsletter.enabled = true
            extension.theme.mode = "dark"
            extension.layout.layoutType = LayoutType.SIDEBAR_RIGHT

            val site = SiteConfiguration(
                googleForms = GoogleFormsConfig(formId = "yaml-form"),
                analytics = AnalyticsConfig(provider = "yaml-plausible"),
                theme = ThemeConfig(mode = "light")
            )

            val (configs, _) = ConfigResolver.resolveAll(props, extension, site)

            assertAll(
                { assertEquals("dsl-form", configs.googleForms.formId) },
                { assertEquals("dsl-matomo", configs.analytics.provider) },
                { assertTrue(configs.newsletter.enabled) },
                { assertEquals("dark", configs.theme.mode) },
                { assertEquals(LayoutType.SIDEBAR_RIGHT, configs.layout.layoutType) }
            )
        }

        @Test
        fun `CLI overrides DSL which overrides YAML`() {
            val props = mapOf(
                "bakery.googleForms.formId" to "cli-form",
                "bakery.analytics.provider" to "cli-analytics"
            )
            val extension = createExtension()
            extension.googleForms.formId = "dsl-form"
            extension.googleForms.width = "dsl-width"
            extension.analytics.provider = "dsl-matomo"

            val site = SiteConfiguration(
                googleForms = GoogleFormsConfig(formId = "yaml-form", width = "yaml-width"),
                analytics = AnalyticsConfig(provider = "yaml-plausible", domain = "yaml-domain")
            )

            val (configs, _) = ConfigResolver.resolveAll(props, extension, site)

            assertAll(
                { assertEquals("cli-form", configs.googleForms.formId) },
                { assertEquals("dsl-width", configs.googleForms.width) },
                { assertEquals("cli-analytics", configs.analytics.provider) },
                { assertEquals("yaml-domain", configs.analytics.domain) }
            )
        }
    }

    @Nested
    inner class ResolveAllAccumulatesErrorsTest {

        @Test
        fun `returns empty errors when all configs resolve successfully`() {
            val props = emptyMap<String, String>()
            val extension = createExtension()
            val site = createSite()

            val (_, errors) = ConfigResolver.resolveAll(props, extension, site)

            assertTrue(errors.isEmpty(), "No errors expected for default configs")
        }
    }

    @Nested
    inner class ResolvedConfigsDataClassTest {

        @Test
        fun `data class equality works`() {
            val configs1 = ResolvedConfigs(
                firebase = FirebaseProjectInfo(projectId = "p1", apiKey = "k1"),
                googleForms = GoogleFormsConfig(formId = "f1"),
                firebaseAuth = FirebaseAuthConfig(apiKey = "a1", authDomain = "d1", projectId = "p1"),
                comments = CommentsConfig(enabled = true, collection = "c1"),
                analytics = AnalyticsConfig(provider = "matomo", domain = "d1"),
                newsletter = NewsletterConfig(enabled = true, provider = "mc", endpoint = "e1"),
                theme = ThemeConfig(mode = "dark"),
                layout = LayoutConfig(layoutType = LayoutType.CENTERED)
            )
            val configs2 = configs1.copy()

            assertEquals(configs1, configs2)
            assertEquals(configs1.hashCode(), configs2.hashCode())
        }

        @Test
        fun `data class copy allows targeted modification`() {
            val original = ResolvedConfigs(
                firebase = FirebaseProjectInfo(projectId = "", apiKey = ""),
                googleForms = GoogleFormsConfig(),
                firebaseAuth = FirebaseAuthConfig(),
                comments = CommentsConfig(),
                analytics = AnalyticsConfig(),
                newsletter = NewsletterConfig(),
                theme = ThemeConfig(),
                layout = LayoutConfig()
            )

            val modified = original.copy(
                googleForms = original.googleForms.copy(formId = "new-form-id"),
                theme = original.theme.copy(mode = "dark")
            )

            assertAll(
                { assertEquals("new-form-id", modified.googleForms.formId) },
                { assertEquals("dark", modified.theme.mode) },
                { assertEquals("", original.googleForms.formId) },
                { assertEquals("auto", original.theme.mode) }
            )
        }
    }

    @Nested
    inner class ConfigResolutionErrorTest {

        @Test
        fun `MissingRequiredField holds domain and field`() {
            val error = ConfigResolutionError.MissingRequiredField(
                domain = "firebase",
                field = "apiKey",
                message = "apiKey is required"
            )
            assertEquals("firebase", error.domain)
            assertEquals("apiKey", error.field)
            assertEquals("apiKey is required", error.message)
        }

        @Test
        fun `InvalidValue holds domain field and value`() {
            val error = ConfigResolutionError.InvalidValue(
                domain = "layout",
                field = "layoutType",
                value = "INVALID",
                message = "Unknown layout type: INVALID"
            )
            assertEquals("layout", error.domain)
            assertEquals("INVALID", error.value)
        }

        @Test
        fun `DomainFailure holds domain and optional cause`() {
            val cause = RuntimeException("parse error")
            val error = ConfigResolutionError.DomainFailure(
                domain = "theme",
                message = "Failed to resolve theme",
                cause = cause
            )
            assertEquals("theme", error.domain)
            assertEquals(cause, error.cause)
        }

        @Test
        fun `sealed class allows exhaustive when`() {
            val errors = listOf<ConfigResolutionError>(
                ConfigResolutionError.MissingRequiredField("a", "b", "c"),
                ConfigResolutionError.InvalidValue("d", "e", "f", "g"),
                ConfigResolutionError.DomainFailure("h", "i")
            )
            val domains = errors.map { error ->
                when (error) {
                    is ConfigResolutionError.MissingRequiredField -> error.domain
                    is ConfigResolutionError.InvalidValue -> error.domain
                    is ConfigResolutionError.DomainFailure -> error.domain
                }
            }
            assertEquals(listOf("a", "d", "h"), domains)
        }
    }

    @Nested
    inner class ResolvedConfigsToResolverIntegrationTest {

        @Test
        fun `toResolver feeds all 8 configInjectors without missing keys`() {
            val configs = ResolvedConfigs(
                firebase = FirebaseProjectInfo(projectId = "proj-1", apiKey = "key-1"),
                googleForms = GoogleFormsConfig(formId = "form-1", width = "800", height = "600"),
                firebaseAuth = FirebaseAuthConfig(apiKey = "auth-key", authDomain = "domain", projectId = "proj-1"),
                comments = CommentsConfig(enabled = true, collection = "my-comments"),
                analytics = AnalyticsConfig(provider = "matomo", domain = "stats.example.com", scriptSrc = "https://cdn.matomo"),
                newsletter = NewsletterConfig(enabled = true, provider = "mailchimp", endpoint = "https://api.mc"),
                theme = ThemeConfig(mode = "dark", primaryColor = "#333", variant = "magazine"),
                layout = LayoutConfig(layoutType = LayoutType.SIDEBAR_LEFT)
            )

            val resolver = configs.toResolver()
            val lines = mutableListOf(
                "firebaseApiKey=",
                "firebaseProjectId=",
                "googleFormsFormId=",
                "googleFormsWidth=",
                "googleFormsHeight=",
                "firebaseAuthApiKey=",
                "firebaseAuthDomain=",
                "firebaseAuthProjectId=",
                "commentsEnabled=",
                "commentsCollection=",
                "analyticsProvider=",
                "analyticsDomain=",
                "analyticsScriptSrc=",
                "newsletterEnabled=",
                "newsletterProvider=",
                "newsletterEndpoint=",
                "themeMode=",
                "themePrimaryColor=",
                "themeSecondaryColor=",
                "themeFontFamily=",
                "themeLogoUrl=",
                "themeFaviconUrl=",
                "themeVariant=",
                "themeAccentColor=",
                "themeBackgroundColor=",
                "themeTextColor=",
                "themeHeadingFont=",
                "layoutType="
            )

            configInjectors.values.forEach { it.inject(lines, resolver) }

            assertAll(
                { assertEquals("key-1", lines.find { it.startsWith("firebaseApiKey=") }?.substringAfter("=")) },
                { assertEquals("proj-1", lines.find { it.startsWith("firebaseProjectId=") }?.substringAfter("=")) },
                { assertEquals("form-1", lines.find { it.startsWith("googleFormsFormId=") }?.substringAfter("=")) },
                { assertEquals("auth-key", lines.find { it.startsWith("firebaseAuthApiKey=") }?.substringAfter("=")) },
                { assertEquals("true", lines.find { it.startsWith("commentsEnabled=") }?.substringAfter("=")) },
                { assertEquals("matomo", lines.find { it.startsWith("analyticsProvider=") }?.substringAfter("=")) },
                { assertEquals("true", lines.find { it.startsWith("newsletterEnabled=") }?.substringAfter("=")) },
                { assertEquals("dark", lines.find { it.startsWith("themeMode=") }?.substringAfter("=")) },
                { assertEquals("SIDEBAR_LEFT", lines.find { it.startsWith("layoutType=") }?.substringAfter("=")) }
            )
        }

        @Test
        fun `toResolver returns defaultValue for unknown keys`() {
            val configs = ResolvedConfigs(
                firebase = FirebaseProjectInfo(projectId = "", apiKey = ""),
                googleForms = GoogleFormsConfig(),
                firebaseAuth = FirebaseAuthConfig(),
                comments = CommentsConfig(),
                analytics = AnalyticsConfig(),
                newsletter = NewsletterConfig(),
                theme = ThemeConfig(),
                layout = LayoutConfig()
            )

            val resolver = configs.toResolver()

            assertEquals("fallback", resolver("unknownKey", "fallback"))
            assertEquals("", resolver("unknownKey", ""))
        }

        @Test
        fun `toResolver maps all property keys from SiteTaskRegistrar when block`() {
            val configs = ResolvedConfigs(
                firebase = FirebaseProjectInfo(projectId = "p", apiKey = "k"),
                googleForms = GoogleFormsConfig(formId = "f", width = "w", height = "h"),
                firebaseAuth = FirebaseAuthConfig(apiKey = "ak", authDomain = "ad", projectId = "ap"),
                comments = CommentsConfig(enabled = true, collection = "col"),
                analytics = AnalyticsConfig(provider = "prov", domain = "dom", scriptSrc = "src"),
                newsletter = NewsletterConfig(enabled = false, provider = "mc", endpoint = "ep"),
                theme = ThemeConfig(
                    mode = "dark", primaryColor = "#1", secondaryColor = "#2",
                    fontFamily = "ff", logoUrl = "lu", faviconUrl = "fu",
                    variant = "var", accentColor = "#3", backgroundColor = "#4",
                    textColor = "#5", headingFont = "hf"
                ),
                layout = LayoutConfig(layoutType = LayoutType.CENTERED)
            )

            val resolver = configs.toResolver()

            assertAll(
                { assertEquals("k", resolver("firebaseApiKey", "")) },
                { assertEquals("p", resolver("firebaseProjectId", "")) },
                { assertEquals("f", resolver("googleFormsFormId", "")) },
                { assertEquals("w", resolver("googleFormsWidth", "")) },
                { assertEquals("h", resolver("googleFormsHeight", "")) },
                { assertEquals("ak", resolver("firebaseAuthApiKey", "")) },
                { assertEquals("ad", resolver("firebaseAuthDomain", "")) },
                { assertEquals("ap", resolver("firebaseAuthProjectId", "")) },
                { assertEquals("true", resolver("commentsEnabled", "")) },
                { assertEquals("col", resolver("commentsCollection", "")) },
                { assertEquals("prov", resolver("analyticsProvider", "")) },
                { assertEquals("dom", resolver("analyticsDomain", "")) },
                { assertEquals("src", resolver("analyticsScriptSrc", "")) },
                { assertEquals("false", resolver("newsletterEnabled", "")) },
                { assertEquals("mc", resolver("newsletterProvider", "")) },
                { assertEquals("ep", resolver("newsletterEndpoint", "")) },
                { assertEquals("dark", resolver("themeMode", "")) },
                { assertEquals("#1", resolver("themePrimaryColor", "")) },
                { assertEquals("#2", resolver("themeSecondaryColor", "")) },
                { assertEquals("ff", resolver("themeFontFamily", "")) },
                { assertEquals("lu", resolver("themeLogoUrl", "")) },
                { assertEquals("fu", resolver("themeFaviconUrl", "")) },
                { assertEquals("var", resolver("themeVariant", "")) },
                { assertEquals("#3", resolver("themeAccentColor", "")) },
                { assertEquals("#4", resolver("themeBackgroundColor", "")) },
                { assertEquals("#5", resolver("themeTextColor", "")) },
                { assertEquals("hf", resolver("themeHeadingFont", "")) },
                { assertEquals("CENTERED", resolver("layoutType", "")) }
            )
        }
    }
}