package bakery.site

import bakery.AnalyticsConfig
import bakery.lens.AugmentedContextDsl
import bakery.CommentsConfig
import bakery.FirebaseAuthConfig
import bakery.FirebaseProjectInfo
import bakery.GoogleFormsConfig
import bakery.LayoutConfig
import bakery.LayoutType
import bakery.NewsletterConfig
import bakery.ResolvedConfigs
import bakery.SiteConfiguration
import bakery.SiteType
import bakery.ThemeConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files

class GenerateSiteServiceTest {

    @TempDir
    lateinit var tempDir: File

    @Nested
    inner class ResourcePathForTypeTest {

        @Test
        fun `BLOG returns site directory`() {
            assertEquals("site", GenerateSiteService.resourcePathForType(SiteType.BLOG))
        }

        @Test
        fun `BASIC returns site-basic directory`() {
            assertEquals("site-basic", GenerateSiteService.resourcePathForType(SiteType.BASIC))
        }
    }

    @Nested
    inner class DefaultSiteDescriptionTest {

        @Test
        fun `BLOG returns descriptive text`() {
            val desc = GenerateSiteService.defaultSiteDescription(SiteType.BLOG)
            assertTrue(desc.contains("Blog"))
            assertTrue(desc.contains("JBake"))
        }

        @Test
        fun `BASIC returns minimal description`() {
            val desc = GenerateSiteService.defaultSiteDescription(SiteType.BASIC)
            assertTrue(desc.contains("Site"))
            assertTrue(desc.contains("minimal"))
        }
    }

    @Nested
    inner class CreateAndConfigureSiteYmlTest {

        @Test
        fun `creates siteYml with BLOG type configuration`() {
            val siteYmlFile = tempDir.resolve("site.yml")
            assertFalse(siteYmlFile.exists())

            GenerateSiteService.createAndConfigureSiteYml(siteYmlFile, SiteType.BLOG)

            assertTrue(siteYmlFile.exists())
            val content = siteYmlFile.readText()
            assertTrue(content.contains("site"), "BLOG type should use 'site' as resource path")
            assertTrue(content.contains("bake"), "Should have bake section")
        }

        @Test
        fun `creates siteYml with BASIC type configuration`() {
            val siteYmlFile = tempDir.resolve("site.yml")
            assertFalse(siteYmlFile.exists())

            GenerateSiteService.createAndConfigureSiteYml(siteYmlFile, SiteType.BASIC)

            assertTrue(siteYmlFile.exists())
            val content = siteYmlFile.readText()
            assertTrue(content.contains("site-basic"), "BASIC type should use 'site-basic' as resource path")
        }
    }

    @Nested
    inner class SetupGitIgnoreTest {

        @Test
        fun `creates gitignore when it does not exist`() {
            val gitignore = tempDir.resolve(".gitignore")
            assertFalse(gitignore.exists())

            val updated = GenerateSiteService.setupGitIgnore(tempDir)

            assertTrue(updated)
            assertTrue(gitignore.exists())
            val content = gitignore.readText()
            assertTrue(content.contains("site.yml"), "Should contain site.yml pattern")
            assertTrue(content.contains(".gradle"))
        }

        @Test
        fun `appends siteYml when gitignore exists without it`() {
            val gitignore = tempDir.resolve(".gitignore")
            gitignore.writeText("build\n")

            val updated = GenerateSiteService.setupGitIgnore(tempDir)

            assertTrue(updated)
            val content = gitignore.readText()
            assertTrue(content.contains("site.yml"), "Should append site.yml")
        }

        @Test
        fun `returns false when gitignore already contains siteYml`() {
            val gitignore = tempDir.resolve(".gitignore")
            gitignore.writeText("build\nsite.yml\n")

            val updated = GenerateSiteService.setupGitIgnore(tempDir)

            assertFalse(updated)
        }
    }

    @Nested
    inner class SetupGitAttributesTest {

        @Test
        fun `creates gitattributes when it does not exist`() {
            val gitattributes = tempDir.resolve(".gitattributes")
            assertFalse(gitattributes.exists())

            val updated = GenerateSiteService.setupGitAttributes(tempDir)

            assertTrue(updated)
            assertTrue(gitattributes.exists())
            val content = gitattributes.readText()
            assertTrue(content.isNotBlank())
        }

        @Test
        fun `returns false when gitattributes already exists`() {
            val gitattributes = tempDir.resolve(".gitattributes")
            gitattributes.writeText("existing content")

            val updated = GenerateSiteService.setupGitAttributes(tempDir)

            assertFalse(updated)
            assertEquals("existing content", gitattributes.readText())
        }
    }

    @Nested
    inner class InjectConfigIntoJbakePropertiesTest {

        @Test
        fun `returns false when jbake properties does not exist`() {
            val targetDir = tempDir.resolve("nonexistent")
            targetDir.mkdirs()
            val site = SiteConfiguration()

            val result = GenerateSiteService.injectConfigIntoJbakeProperties(
                targetDir, site, createDefaultResolvedConfigs()
            )

            assertFalse(result)
        }

        @Test
        fun `injects basic config into existing jbake properties`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("blog.version=1.0\n")

            val site = SiteConfiguration(bake = bakery.BakeConfiguration("site", "build"))
            val result = GenerateSiteService.injectConfigIntoJbakeProperties(
                tempDir, site, createDefaultResolvedConfigs()
            )

            assertTrue(result)
            val content = jbakeProps.readText()
            assertTrue(content.contains("blog.version=1.0"), "Should preserve existing properties")
            assertTrue(content.contains("themeMode="), "Should inject theme config (always injected)")
            assertTrue(content.contains("layoutType="), "Should inject layout config (always injected)")
        }

        @Test
        fun `injects augmented context when enabled`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("blog.version=1.0\n")

            val site = SiteConfiguration(bake = bakery.BakeConfiguration("site", "build"))
            val augmentedDsl = AugmentedContextDsl()
            augmentedDsl.enabled = true
            augmentedDsl.budget.maxArticlesPerPage = 6
            augmentedDsl.budget.minSimilarity = 0.8

            val result = GenerateSiteService.injectConfigIntoJbakeProperties(
                tempDir, site, createDefaultResolvedConfigs(), augmentedDsl
            )

            assertTrue(result)
            val content = jbakeProps.readText()
            assertTrue(content.contains("augmentedContextEnabled=true"))
            assertTrue(content.contains("lensBudgetMaxArticlesPerPage=6"))
            assertTrue(content.contains("lensBudgetMinSimilarity=0.8"))
        }

        @Test
        fun `does not inject lens when augmented context disabled`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("blog.version=1.0\n")

            val site = SiteConfiguration(bake = bakery.BakeConfiguration("site", "build"))
            val augmentedDsl = AugmentedContextDsl()
            augmentedDsl.enabled = false

            val result = GenerateSiteService.injectConfigIntoJbakeProperties(
                tempDir, site, createDefaultResolvedConfigs(), augmentedDsl
            )

            assertTrue(result)
            val content = jbakeProps.readText()
            assertFalse(content.contains("augmentedContextEnabled="))
        }

        @Test
        fun `injects site dot language from resolved configs`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("blog.version=1.0\n")

            val site = SiteConfiguration(bake = bakery.BakeConfiguration("site", "build"))
            val configs = ResolvedConfigs(
                firebase = FirebaseProjectInfo(projectId = "", apiKey = ""),
                googleForms = GoogleFormsConfig(),
                firebaseAuth = FirebaseAuthConfig(),
                comments = CommentsConfig(),
                analytics = AnalyticsConfig(),
                newsletter = NewsletterConfig(),
                theme = ThemeConfig(),
                layout = LayoutConfig(),
                language = "en"
            )

            val result = GenerateSiteService.injectConfigIntoJbakeProperties(
                tempDir, site, configs
            )

            assertTrue(result)
            val content = jbakeProps.readText()
            assertTrue(content.contains("site.language=en"))
        }

        @Test
        fun `injects site dot language with default fr`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("blog.version=1.0\n")

            val site = SiteConfiguration(bake = bakery.BakeConfiguration("site", "build"))
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

            val result = GenerateSiteService.injectConfigIntoJbakeProperties(
                tempDir, site, configs
            )

            assertTrue(result)
            val content = jbakeProps.readText()
            assertTrue(content.contains("site.language=fr"))
        }

        @Test
        fun `updates existing site dot language property`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("blog.version=1.0\nsite.language=fr\n")

            val site = SiteConfiguration(bake = bakery.BakeConfiguration("site", "build"))
            val configs = ResolvedConfigs(
                firebase = FirebaseProjectInfo(projectId = "", apiKey = ""),
                googleForms = GoogleFormsConfig(),
                firebaseAuth = FirebaseAuthConfig(),
                comments = CommentsConfig(),
                analytics = AnalyticsConfig(),
                newsletter = NewsletterConfig(),
                theme = ThemeConfig(),
                layout = LayoutConfig(),
                language = "ar"
            )

            val result = GenerateSiteService.injectConfigIntoJbakeProperties(
                tempDir, site, configs
            )

            assertTrue(result)
            val content = jbakeProps.readText()
            assertTrue(content.contains("site.language=ar"))
            assertFalse(content.contains("site.language=fr"))
        }
    }

    private fun createDefaultResolvedConfigs() = ResolvedConfigs(
        firebase = FirebaseProjectInfo(projectId = "test-project", apiKey = "test-key"),
        googleForms = GoogleFormsConfig(),
        firebaseAuth = FirebaseAuthConfig(),
        comments = CommentsConfig(),
        analytics = AnalyticsConfig(),
        newsletter = NewsletterConfig(),
        theme = ThemeConfig(),
        layout = LayoutConfig()
    )
}