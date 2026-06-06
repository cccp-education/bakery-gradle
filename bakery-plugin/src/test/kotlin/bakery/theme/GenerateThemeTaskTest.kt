package bakery.theme

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GenerateThemeTaskTest {

    @TempDir
    lateinit var tempDir: File

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TaskRegistration {

        @Test
        fun `task is registered with correct group and description`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-theme")
                .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()

            assertNotNull(task)
            assertEquals("generate", task.group)
            assertTrue(task.description!!.contains("thème"))
        }

        @Test
        fun `task initializes all properties with empty defaults`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-theme-defaults")
                .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()

            assertEquals("", task.themeDescription.get())
            assertEquals("", task.themeVariant.get())
            assertEquals("", task.themePrimaryColor.get())
            assertEquals("", task.themeSecondaryColor.get())
            assertEquals("", task.themeAccentColor.get())
            assertEquals("", task.themeBackgroundColor.get())
            assertEquals("", task.themeTextColor.get())
            assertEquals("", task.themeFontFamily.get())
            assertEquals("", task.themeHeadingFont.get())
            assertEquals(null, task.dslIntention)
            assertEquals(null, task.targetDir)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResolveIntention {

        @Test
        fun `uses CLI description when set`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-cli-desc")
                .build()

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()
            task.themeDescription.set("Blog tech moderne CLI")
            task.themeVariant.set("magazine")

            val intention = task.resolveIntention()

            assertEquals("Blog tech moderne CLI", intention.description)
            assertEquals(ThemeVariant.MAGAZINE, intention.variant)
        }

        @Test
        fun `falls back to DSL description when CLI is blank`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-dsl-desc")
                .build()

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()
            task.themeVariant.set("documentation")
            task.dslIntention = ThemeIntention(
                description = "Site de doc DSL",
                variant = ThemeVariant.DOCUMENTATION
            )

            val intention = task.resolveIntention()

            assertEquals("Site de doc DSL", intention.description)
            assertEquals(ThemeVariant.DOCUMENTATION, intention.variant)
        }

        @Test
        fun `uses default description when both CLI and DSL absent`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-default-desc")
                .build()

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()

            val intention = task.resolveIntention()

            assertEquals("Thème généré par catalogue", intention.description)
            assertEquals(ThemeVariant.MINIMAL, intention.variant)
        }

        @Test
        fun `CLI description wins over DSL when both set`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-cli-wins-desc")
                .build()

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()
            task.themeDescription.set("CLI desc prioritaire")
            task.themeVariant.set("portfolio")
            task.dslIntention = ThemeIntention(
                description = "DSL desc ignoree",
                variant = ThemeVariant.MAGAZINE
            )

            val intention = task.resolveIntention()

            assertEquals("CLI desc prioritaire", intention.description)
            assertEquals(ThemeVariant.PORTFOLIO, intention.variant)
        }

        @Test
        fun `CLI variant wins over DSL variant`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-cli-wins-variant")
                .build()

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()
            task.themeVariant.set("formation")
            task.dslIntention = ThemeIntention(
                description = "DSL desc",
                variant = ThemeVariant.MINIMAL
            )

            val intention = task.resolveIntention()

            assertEquals(ThemeVariant.FORMATION, intention.variant)
        }

        @Test
        fun `uses DSL variant when CLI is blank`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-dsl-variant")
                .build()

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()
            task.dslIntention = ThemeIntention(
                description = "Portfolio DSL",
                variant = ThemeVariant.PORTFOLIO
            )

            val intention = task.resolveIntention()

            assertEquals(ThemeVariant.PORTFOLIO, intention.variant)
        }

        @Test
        fun `defaults to MINIMAL for unknown variant string`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-unknown-variant")
                .build()

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()
            task.themeVariant.set("nonexistent")

            val intention = task.resolveIntention()

            assertEquals(ThemeVariant.MINIMAL, intention.variant)
        }

        @Test
        fun `CLI overrides win over DSL overrides`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-cli-overrides")
                .build()

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()
            task.themePrimaryColor.set("#ff0000")
            task.themeAccentColor.set("#00ff00")
            task.dslIntention = ThemeIntention(
                description = "DSL desc",
                variant = ThemeVariant.MINIMAL,
                overrides = ThemeOverrides(primaryColor = "#0000ff", accentColor = "#000000")
            )

            val intention = task.resolveIntention()

            assertEquals("#ff0000", intention.overrides.primaryColor)
            assertEquals("#00ff00", intention.overrides.accentColor)
        }

        @Test
        fun `DSL overrides used when CLI override blanks`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-dsl-overrides")
                .build()

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()
            task.dslIntention = ThemeIntention(
                description = "DSL desc",
                variant = ThemeVariant.MINIMAL,
                overrides = ThemeOverrides(fontFamily = "Fira Sans", headingFont = "Montserrat")
            )

            val intention = task.resolveIntention()

            assertEquals("Fira Sans", intention.overrides.fontFamily)
            assertEquals("Montserrat", intention.overrides.headingFont)
        }

        @Test
        fun `null DSL overrides stay null when CLI blank`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-null-overrides")
                .build()

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()
            task.dslIntention = ThemeIntention(
                description = "Simple",
                variant = ThemeVariant.MINIMAL,
                overrides = ThemeOverrides()
            )

            val intention = task.resolveIntention()

            assertEquals(null, intention.overrides.primaryColor)
            assertEquals(null, intention.overrides.fontFamily)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ExecuteGenerate {

        @TempDir
        lateinit var testDir: File

        private var propsCounter = 0

        private fun setupSiteDir(): File {
            propsCounter++
            val siteDir = testDir.resolve("site/myblog$propsCounter/site")
            siteDir.mkdirs()
            val props = siteDir.resolve("jbake.properties")
            props.writeText(
                """
                site.host=http://localhost:8820
                template.jbake.extension=.thyme
                themeVariant=minimal
                themePrimaryColor=#2c3e50
            """.trimIndent()
            )
            return testDir.resolve("site/myblog$propsCounter")
        }

        private fun parseProperties(file: File): Map<String, String> =
            file.readLines()
                .filter { it.contains("=") }
                .associate {
                    val (k, v) = it.split("=", limit = 2)
                    k to v
                }

        @Test
        fun `throws when targetDir is null`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-no-target")
                .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()

            assertThrows<IllegalStateException> {
                task.executeGenerate()
            }
        }

        @Test
        fun `writes theme properties to existing jbake properties file`() {
            val contentRoot = setupSiteDir()
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-execute-write")
                .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()
            task.targetDir = contentRoot
            task.themeVariant.set("magazine")
            task.themeDescription.set("Magazine theme via CLI")

            task.executeGenerate()

            val propsFile = contentRoot.resolve("site/jbake.properties")
            assertTrue(propsFile.exists(), "jbake.properties should exist")

            val props = parseProperties(propsFile)

            assertEquals("MAGAZINE", props["themeVariant"])
            assertEquals("#e74c3c", props["themePrimaryColor"])
            assertEquals("#ecf0f1", props["themeSecondaryColor"])
            assertEquals("#c0392b", props["themeAccentColor"])
            assertEquals("#ffffff", props["themeBackgroundColor"])
            assertEquals("#2c3e50", props["themeTextColor"])
            assertEquals("Georgia, serif", props["themeFontFamily"])
            assertEquals("Playfair Display, serif", props["themeHeadingFont"])
            assertEquals("auto", props["themeMode"])
            assertEquals("", props["themeLogoUrl"])
            assertEquals("", props["themeFaviconUrl"])
        }

        @Test
        fun `creates new theme properties when key does not exist`() {
            val siteDir = testDir.resolve("site/freshblog/site")
            siteDir.mkdirs()
            val propsFile = siteDir.resolve("jbake.properties")
            propsFile.writeText("site.host=http://localhost:8820\n")

            val contentRoot = testDir.resolve("site/freshblog")
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-create-new")
                .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()
            task.targetDir = contentRoot
            task.themeVariant.set("formation")

            task.executeGenerate()

            val props = parseProperties(propsFile)
            assertEquals("FORMATION", props["themeVariant"])
            assertEquals("auto", props["themeMode"])
        }

        @Test
        fun `logs warning when jbake properties file is missing`() {
            val contentRoot = testDir.resolve("site/noprops")
            contentRoot.mkdirs()

            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-missing-props")
                .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()
            task.targetDir = contentRoot
            task.themeVariant.set("minimal")

            task.executeGenerate()
        }

        @Test
        fun `overrides preset colors via CLI properties`() {
            val siteDir = testDir.resolve("site/customblog/site")
            siteDir.mkdirs()
            siteDir.resolve("jbake.properties").writeText(
                "site.host=http://localhost:8820\n"
            )

            val contentRoot = testDir.resolve("site/customblog")
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-override-colors")
                .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()
            task.targetDir = contentRoot
            task.themeVariant.set("minimal")
            task.themePrimaryColor.set("#custom123")
            task.themeAccentColor.set("#accent456")
            task.themeFontFamily.set("Custom Font")
            task.themeHeadingFont.set("Custom Heading")

            task.executeGenerate()

            val props = parseProperties(siteDir.resolve("jbake.properties"))

            assertEquals("#custom123", props["themePrimaryColor"])
            assertEquals("#accent456", props["themeAccentColor"])
            assertEquals("Custom Font", props["themeFontFamily"])
            assertEquals("Custom Heading", props["themeHeadingFont"])
        }

        @Test
        fun `uses MINIMAL variant as default fallback for empty variant`() {
            val siteDir = testDir.resolve("site/emptyvariant/site")
            siteDir.mkdirs()
            siteDir.resolve("jbake.properties").writeText(
                "site.host=http://localhost:8820\n"
            )

            val contentRoot = testDir.resolve("site/emptyvariant")
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-empty-variant")
                .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()
            task.targetDir = contentRoot

            task.executeGenerate()

            val props = parseProperties(siteDir.resolve("jbake.properties"))
            assertEquals("MINIMAL", props["themeVariant"])
            assertEquals("#2c3e50", props["themePrimaryColor"])
        }

        @Test
        fun `uses DSL intention when no CLI properties set`() {
            val siteDir = testDir.resolve("site/dslblog/site")
            siteDir.mkdirs()
            siteDir.resolve("jbake.properties").writeText(
                "site.host=http://localhost:8820\n"
            )

            val contentRoot = testDir.resolve("site/dslblog")
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-dsl-execute")
                .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("generateTheme", GenerateThemeTask::class.java).get()
            task.targetDir = contentRoot
            task.dslIntention = ThemeIntention(
                description = "Portfolio DSL",
                variant = ThemeVariant.PORTFOLIO,
                overrides = ThemeOverrides(primaryColor = "#dslcolor")
            )

            task.executeGenerate()

            val props = parseProperties(siteDir.resolve("jbake.properties"))
            assertEquals("PORTFOLIO", props["themeVariant"])
            assertEquals("#dslcolor", props["themePrimaryColor"])
            assertEquals("#1a1a2e", props["themeBackgroundColor"])
        }
    }
}
