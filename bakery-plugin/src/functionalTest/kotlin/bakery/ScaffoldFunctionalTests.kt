@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner.create
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

class ScaffoldFunctionalTests {

    @Nested
    @DisplayName("generateSite avec sites.base.dir + site.name")
    inner class HappyPathTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should scaffold site into office-sites-mycompany when both properties are defined`() {
            createMinimalBakeryProject(projectDir, sitesBaseDir = "office/sites", siteName = "my-company")

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val siteDir = projectDir.resolve("office/sites/my-company")
            assertThat(siteDir).exists().isDirectory
            assertThat(siteDir.resolve("site.yml")).exists().isFile
            assertThat(siteDir.resolve("site/jbake.properties")).exists().isFile
            assertThat(siteDir.resolve("maquette/index.html")).exists().isFile
            assertThat(siteDir.resolve(".gitignore")).exists().isFile
            assertThat(siteDir.resolve(".gitattributes")).exists().isFile
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }
    }

    @Nested
    @DisplayName("generateSite failures")
    inner class ErrorHandlingTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should fail when sitesBaseDir is defined but siteName is missing`() {
            createMinimalBakeryProject(projectDir, sitesBaseDir = "office/sites", siteName = null)

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite", "--stacktrace")
                .buildAndFail()

            assertThat(result.output).contains("siteName must be defined")
        }

        @Test
        fun `should fail when site directory already exists`() {
            createMinimalBakeryProject(projectDir, sitesBaseDir = "office/sites", siteName = "existing-site")
            projectDir.resolve("office/sites/existing-site").mkdirs()

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite", "--stacktrace")
                .buildAndFail()

            assertThat(result.output).contains("already exists")
        }
    }

    @Nested
    @DisplayName("generateSite with siteName only")
    inner class SiteNameOnlyTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should scaffold site into projectDir-siteName when only siteName is defined`() {
            createMinimalBakeryProject(projectDir, sitesBaseDir = null, siteName = "mysite")

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val siteDir = projectDir.resolve("mysite")
            assertThat(siteDir).exists().isDirectory
            assertThat(siteDir.resolve("site.yml")).exists().isFile
            assertThat(siteDir.resolve("site/jbake.properties")).exists().isFile
            assertThat(siteDir.resolve("maquette/index.html")).exists().isFile
        }
    }

    @Nested
    @DisplayName("generateSite backward compat (ni sites.base.dir ni site.name)")
    inner class BackwardCompatTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should scaffold into projectDir root when no scaffolding properties are defined`() {
            createMinimalBakeryProject(projectDir, sitesBaseDir = null, siteName = null)

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            assertThat(projectDir.resolve("site.yml")).exists().isFile
            assertThat(projectDir.resolve("site/jbake.properties")).exists().isFile
            assertThat(projectDir.resolve("maquette/index.html")).exists().isFile
            assertThat(projectDir.resolve(".gitignore")).exists().isFile
            assertThat(projectDir.resolve(".gitattributes")).exists().isFile
        }
    }

    @Nested
    @DisplayName("generateSite avec siteType = basic")
    inner class BasicSiteTypeTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should scaffold basic site with minimal structure`() {
            createMinimalBakeryProject(
                projectDir,
                sitesBaseDir = null,
                siteName = "mon-site",
                siteType = "basic"
            )

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val siteDir = projectDir.resolve("mon-site")
            assertThat(siteDir).exists().isDirectory
            // Basic-specific resource path: site-basic
            assertThat(siteDir.resolve("site-basic/jbake.properties")).exists().isFile
            assertThat(siteDir.resolve("site-basic/templates/index.thyme")).exists().isFile
            assertThat(siteDir.resolve("site-basic/templates/page.thyme")).exists().isFile
            assertThat(siteDir.resolve("site-basic/content/index.adoc")).exists().isFile
            assertThat(siteDir.resolve("site-basic/content/about.adoc")).exists().isFile
            assertThat(siteDir.resolve("site-basic/content/contact.adoc")).exists().isFile
            assertThat(siteDir.resolve("maquette/index.html")).exists().isFile
            assertThat(siteDir.resolve("site.yml")).exists().isFile
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }

        @Test
        fun `should scaffold basic site with minimal jbake config`() {
            createMinimalBakeryProject(
                projectDir,
                sitesBaseDir = null,
                siteName = "minimal",
                siteType = "basic"
            )

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val siteDir = projectDir.resolve("minimal")
            val jbakeProps = siteDir.resolve("site-basic/jbake.properties")
            assertThat(jbakeProps).exists().isFile
            val props = jbakeProps.readText(UTF_8)
            assertThat(props).contains("template.index.file=index.thyme")
            assertThat(props).contains("render.feed=false")
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }
    }

    @Nested
    @DisplayName("generateSite avec siteType explicite = blog (backward compat)")
    inner class BlogSiteTypeTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should scaffold blog site with explicit siteType blog`() {
            createMinimalBakeryProject(
                projectDir,
                sitesBaseDir = null,
                siteName = "mon-blog",
                siteType = "blog"
            )

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val siteDir = projectDir.resolve("mon-blog")
            assertThat(siteDir).exists().isDirectory
            // Blog-specific resource path: site (backward compat)
            assertThat(siteDir.resolve("site/jbake.properties")).exists().isFile
            assertThat(siteDir.resolve("site/templates/blog.thyme")).exists().isFile
            assertThat(siteDir.resolve("site/templates/post.thyme")).exists().isFile
            assertThat(siteDir.resolve("site/content/blog.adoc")).exists().isFile
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }
    }

    @Nested
    @DisplayName("generateSite blog includes Reusable Thymeleaf components (BKY-JB-2)")
    inner class ThymeleafComponentsTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should scaffold blog site with breadcrumb component`() {
            createMinimalBakeryProject(projectDir, sitesBaseDir = null, siteName = "compo-test", siteType = "blog")

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val templatesDir = projectDir.resolve("compo-test/site/templates")
            assertThat(templatesDir.resolve("breadcrumb.thyme")).exists().isFile
            assertThat(templatesDir.resolve("breadcrumb.thyme").readText(UTF_8))
                .contains("breadcrumb")
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }

        @Test
        fun `should scaffold blog site with toc-sidebar component`() {
            createMinimalBakeryProject(projectDir, sitesBaseDir = null, siteName = "compo-test2", siteType = "blog")

            create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val templatesDir = projectDir.resolve("compo-test2/site/templates")
            assertThat(templatesDir.resolve("toc-sidebar.thyme")).exists().isFile
            assertThat(templatesDir.resolve("toc-sidebar.thyme").readText(UTF_8))
                .contains("toc")
        }

        @Test
        fun `should scaffold blog site with progress-bar component`() {
            createMinimalBakeryProject(projectDir, sitesBaseDir = null, siteName = "compo-test3", siteType = "blog")

            create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val templatesDir = projectDir.resolve("compo-test3/site/templates")
            assertThat(templatesDir.resolve("progress-bar.thyme")).exists().isFile
            assertThat(templatesDir.resolve("progress-bar.thyme").readText(UTF_8))
                .contains("progress")
        }

        @Test
        fun `should scaffold blog site with pdf-viewer component`() {
            createMinimalBakeryProject(projectDir, sitesBaseDir = null, siteName = "compo-test4", siteType = "blog")

            create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val templatesDir = projectDir.resolve("compo-test4/site/templates")
            assertThat(templatesDir.resolve("pdf-viewer.thyme")).exists().isFile
            assertThat(templatesDir.resolve("pdf-viewer.thyme").readText(UTF_8))
                .contains("pdf")
        }
    }

    @Nested
    @DisplayName("generateSite with Google Forms embed (BKY-JB-3)")
    inner class GoogleFormsEmbedTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should scaffold blog site with google-forms template deployed`() {
            createMinimalBakeryProject(
                projectDir,
                sitesBaseDir = null,
                siteName = "gforms-test",
                siteType = "blog"
            )

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val templatesDir = projectDir.resolve("gforms-test/site/templates")
            assertThat(templatesDir.resolve("google-forms.thyme")).exists().isFile
            assertThat(templatesDir.resolve("google-forms.thyme").readText(UTF_8))
                .contains("google-forms-container")
                .contains("iframe")
                .contains("googleFormsFormId")
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }

        @Test
        fun `should scaffold site with google-forms template but no injection when site yml has no googleForms`() {
            createMinimalBakeryProject(
                projectDir,
                sitesBaseDir = null,
                siteName = "no-gforms",
                siteType = "blog"
            )

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val siteDir = projectDir.resolve("no-gforms")
            val templatesDir = siteDir.resolve("site/templates")
            assertThat(templatesDir.resolve("google-forms.thyme")).exists().isFile

            val jbakeProps = siteDir.resolve("site/jbake.properties")
            assertThat(jbakeProps).exists().isFile
            assertThat(jbakeProps.readText(UTF_8))
                .doesNotContain("googleForms")

            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }
    }

    @Nested
    @DisplayName("generateSite with Firebase Auth + Comments (BKY-JB-4)")
    inner class FirebaseAuthCommentsTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should scaffold blog site with auth-header and comments templates deployed`() {
            createMinimalBakeryProject(
                projectDir,
                sitesBaseDir = null,
                siteName = "auth-test",
                siteType = "blog"
            )

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val templatesDir = projectDir.resolve("auth-test/site/templates")
            assertThat(templatesDir.resolve("auth-header.thyme")).exists().isFile
            assertThat(templatesDir.resolve("auth-header.thyme").readText(UTF_8))
                .contains("auth-header")
                .contains("firebaseAuthApiKey")
                .contains("th:if")
            assertThat(templatesDir.resolve("comments.thyme")).exists().isFile
            assertThat(templatesDir.resolve("comments.thyme").readText(UTF_8))
                .contains("comments-section")
                .contains("commentsEnabled")
                .contains("th:if")
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }

        @Test
        fun `should scaffold site with auth and comments templates but no injection when site yml has no firebaseAuth`() {
            createMinimalBakeryProject(
                projectDir,
                sitesBaseDir = null,
                siteName = "no-auth",
                siteType = "blog"
            )

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val siteDir = projectDir.resolve("no-auth")
            val templatesDir = siteDir.resolve("site/templates")
            assertThat(templatesDir.resolve("auth-header.thyme")).exists().isFile
            assertThat(templatesDir.resolve("comments.thyme")).exists().isFile

            val jbakeProps = siteDir.resolve("site/jbake.properties")
            assertThat(jbakeProps).exists().isFile
            assertThat(jbakeProps.readText(UTF_8))
                .doesNotContain("firebaseAuth")
                .doesNotContain("commentsEnabled")

            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }
    }

    @Nested
    @DisplayName("generateSite with Analytics + Newsletter (BKY-JB-5)")
    inner class AnalyticsNewsletterTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should scaffold blog site with analytics-script and newsletter-form templates deployed`() {
            createMinimalBakeryProject(
                projectDir,
                sitesBaseDir = null,
                siteName = "analytics-test",
                siteType = "blog"
            )

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val templatesDir = projectDir.resolve("analytics-test/site/templates")
            assertThat(templatesDir.resolve("analytics-script.thyme")).exists().isFile
            assertThat(templatesDir.resolve("analytics-script.thyme").readText(UTF_8))
                .contains("analytics-script")
                .contains("analyticsProvider")
                .contains("th:if")
            assertThat(templatesDir.resolve("newsletter-form.thyme")).exists().isFile
            assertThat(templatesDir.resolve("newsletter-form.thyme").readText(UTF_8))
                .contains("newsletter-section")
                .contains("newsletterEnabled")
                .contains("th:if")
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }

        @Test
        fun `should scaffold site with analytics and newsletter templates but no injection when site yml has no analytics`() {
            createMinimalBakeryProject(
                projectDir,
                sitesBaseDir = null,
                siteName = "no-analytics",
                siteType = "blog"
            )

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val siteDir = projectDir.resolve("no-analytics")
            val templatesDir = siteDir.resolve("site/templates")
            assertThat(templatesDir.resolve("analytics-script.thyme")).exists().isFile
            assertThat(templatesDir.resolve("newsletter-form.thyme")).exists().isFile

            val jbakeProps = siteDir.resolve("site/jbake.properties")
            assertThat(jbakeProps).exists().isFile
            assertThat(jbakeProps.readText(UTF_8))
                .doesNotContain("analyticsProvider")
                .doesNotContain("newsletterEnabled")

            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }
    }

    @Nested
    @DisplayName("generateSite with Theme system (BKY-JB-6)")
    inner class ThemeSystemTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should scaffold blog site with theme-script template deployed`() {
            createMinimalBakeryProject(
                projectDir,
                sitesBaseDir = null,
                siteName = "theme-test",
                siteType = "blog"
            )

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val templatesDir = projectDir.resolve("theme-test/site/templates")
            assertThat(templatesDir.resolve("theme-script.thyme")).exists().isFile
            assertThat(templatesDir.resolve("theme-script.thyme").readText(UTF_8))
                .contains("theme-script")
                .contains("themePrimaryColor")
                .contains("th:if")
                .contains("--bakery-primary")
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }

        @Test
        fun `should scaffold site with theme template but no injection when site yml has no theme`() {
            createMinimalBakeryProject(
                projectDir,
                sitesBaseDir = null,
                siteName = "no-theme",
                siteType = "blog"
            )

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val siteDir = projectDir.resolve("no-theme")
            val templatesDir = siteDir.resolve("site/templates")
            assertThat(templatesDir.resolve("theme-script.thyme")).exists().isFile

            val jbakeProps = siteDir.resolve("site/jbake.properties")
            assertThat(jbakeProps).exists().isFile
            assertThat(jbakeProps.readText(UTF_8))
                .doesNotContain("themeMode")
                .doesNotContain("themePrimaryColor")

            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }
    }

    @Nested
    @DisplayName("generateSite with Layout system (BKY-JB-7)")
    inner class LayoutTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should scaffold blog site with all layout templates deployed`() {
            createMinimalBakeryProject(
                projectDir,
                sitesBaseDir = null,
                siteName = "layout-test",
                siteType = "blog"
            )

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val templatesDir = projectDir.resolve("layout-test/site/templates")
            assertThat(templatesDir.resolve("layout-full-width.thyme")).exists().isFile
            assertThat(templatesDir.resolve("layout-full-width.thyme").readText(UTF_8))
                .contains("layout-full-width")
                .contains("FULL_WIDTH")
                .contains("th:if")
            assertThat(templatesDir.resolve("layout-sidebar-left.thyme")).exists().isFile
            assertThat(templatesDir.resolve("layout-sidebar-right.thyme")).exists().isFile
            assertThat(templatesDir.resolve("layout-centered.thyme")).exists().isFile
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }

        @Test
        fun `should scaffold site with default layoutType FULL_WIDTH in jbake properties`() {
            createMinimalBakeryProject(
                projectDir,
                sitesBaseDir = null,
                siteName = "layout-default",
                siteType = "blog"
            )

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val siteDir = projectDir.resolve("layout-default")
            val jbakeProps = siteDir.resolve("site/jbake.properties")
            assertThat(jbakeProps).exists().isFile
            assertThat(jbakeProps.readText(UTF_8))
                .contains("layoutType=FULL_WIDTH")

            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }
    }

    @Nested
    @DisplayName("generateSite with Related Articles KG (BKY-BKG)")
    inner class RelatedArticlesTest {

        @TempDir
        lateinit var projectDir: File

        @Test
        fun `should scaffold blog site with related-articles template deployed`() {
            createMinimalBakeryProject(
                projectDir,
                sitesBaseDir = null,
                siteName = "related-articles-test",
                siteType = "blog"
            )

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val templatesDir = projectDir.resolve("related-articles-test/site/templates")
            assertThat(templatesDir.resolve("related-articles.thyme")).exists().isFile
            assertThat(templatesDir.resolve("related-articles.thyme").readText(UTF_8))
                .contains("related-articles")
                .contains("relatedArticlesEnabled")
                .contains("th:if")
            assertThat(templatesDir.resolve("post.thyme").readText(UTF_8))
                .contains("related-articles.thyme::related-articles")
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }

        @Test
        fun `should scaffold site with no relatedArticles injection when site yml has no relatedArticles`() {
            createMinimalBakeryProject(
                projectDir,
                sitesBaseDir = null,
                siteName = "no-related-articles",
                siteType = "blog"
            )

            val result = create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateSite")
                .build()

            val siteDir = projectDir.resolve("no-related-articles")
            val jbakeProps = siteDir.resolve("site/jbake.properties")
            assertThat(jbakeProps).exists().isFile
            assertThat(jbakeProps.readText(UTF_8))
                .doesNotContain("relatedArticlesEnabled")
                .doesNotContain("relatedArticlesMaxResults")
                .doesNotContain("relatedArticlesHeading")

            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }
    }

    companion object {
        private fun createMinimalBakeryProject(
            projectDir: File,
            sitesBaseDir: String?,
            siteName: String?,
            siteType: String? = null
        ) {
            val sbDsl = StringBuilder()
            if (sitesBaseDir != null) {
                sbDsl.append("    sitesBaseDir = file(\"$sitesBaseDir\").absolutePath\n")
            }
            if (siteName != null) {
                sbDsl.append("    siteName = \"$siteName\"\n")
            }
            if (siteType != null) {
                sbDsl.append("    siteType = \"$siteType\"\n")
            }

            projectDir.resolve("settings.gradle.kts").writeText("""
                pluginManagement.repositories.gradlePluginPortal()
                rootProject.name = "scaffold-test"
            """.trimIndent(), UTF_8)

            projectDir.resolve("build.gradle.kts").writeText("""
                plugins { id("education.cccp.bakery") }
                bakery {
                    configPath = file("site.yml").absolutePath
            $sbDsl    }
            """.trimIndent() + "\n", UTF_8)
        }
    }
}
