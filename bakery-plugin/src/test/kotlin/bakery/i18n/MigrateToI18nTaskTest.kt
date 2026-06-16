package bakery.i18n

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

class MigrateToI18nTaskTest {

    @TempDir
    lateinit var tempDir: File

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TaskRegistration {

        @Test
        fun `task is registered with correct group and description`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-migrate-i18n")
                .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()

            assertNotNull(task)
            assertEquals("transform", task.group)
            assertTrue(task.description!!.contains("Migre un site bakery"))
        }

        @Test
        fun `task initializes all CLI properties with empty defaults`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-migrate-defaults")
                .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()

            assertEquals("", task.i18nSite.get())
            assertEquals("", task.i18nLangs.get())
            assertEquals("", task.i18nDefaultLang.get())
            assertEquals("", task.i18nDryRun.get())
            assertEquals(null, task.dslIntention)
            assertEquals(null, task.llmService)
            assertEquals(null, task.contentRootDir)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResolveIntention {

        @Test
        fun `uses CLI siteDir when set`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-cli-site")
                .build()

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.i18nSite.set("/home/user/mon-site")

            val intention = task.resolveIntention()

            assertEquals("/home/user/mon-site", intention.siteDir)
            assertEquals(listOf("en"), intention.languages)
            assertEquals("fr", intention.defaultLanguage)
            assertEquals(true, intention.dryRun)
        }

        @Test
        fun `uses CLI langs when set`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-cli-langs")
                .build()

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.i18nSite.set("/home/user/mon-site")
            task.i18nLangs.set("en,ar,zh")

            val intention = task.resolveIntention()

            assertEquals(listOf("en", "ar", "zh"), intention.languages)
        }

        @Test
        fun `uses CLI defaultLang when set`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-cli-default-lang")
                .build()

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.i18nSite.set("/home/user/mon-site")
            task.i18nDefaultLang.set("en")

            val intention = task.resolveIntention()

            assertEquals("en", intention.defaultLanguage)
        }

        @Test
        fun `uses CLI dryRun false when set`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-cli-dryrun")
                .build()

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.i18nSite.set("/home/user/mon-site")
            task.i18nDryRun.set("false")

            val intention = task.resolveIntention()

            assertEquals(false, intention.dryRun)
        }

        @Test
        fun `falls back to DSL when CLI is blank`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-dsl-fallback")
                .build()

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.dslIntention = I18nMigrationIntention(
                siteDir = "/home/user/dsl-site",
                languages = listOf("en", "es"),
                defaultLanguage = "en",
                dryRun = false
            )

            val intention = task.resolveIntention()

            assertEquals("/home/user/dsl-site", intention.siteDir)
            assertEquals(listOf("en", "es"), intention.languages)
            assertEquals("en", intention.defaultLanguage)
            assertEquals(false, intention.dryRun)
        }

        @Test
        fun `CLI siteDir wins over DSL`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-cli-wins-site")
                .build()

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.i18nSite.set("/home/user/cli-site")
            task.dslIntention = I18nMigrationIntention(
                siteDir = "/home/user/dsl-site",
                languages = listOf("en")
            )

            val intention = task.resolveIntention()

            assertEquals("/home/user/cli-site", intention.siteDir)
        }

        @Test
        fun `CLI langs win over DSL`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-cli-wins-langs")
                .build()

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.i18nSite.set("/home/user/mon-site")
            task.i18nLangs.set("en,ar")
            task.dslIntention = I18nMigrationIntention(
                siteDir = "/home/user/mon-site",
                languages = listOf("en", "es", "zh")
            )

            val intention = task.resolveIntention()

            assertEquals(listOf("en", "ar"), intention.languages)
        }

        @Test
        fun `CLI defaultLang wins over DSL`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-cli-wins-default-lang")
                .build()

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.i18nSite.set("/home/user/mon-site")
            task.i18nDefaultLang.set("ar")
            task.dslIntention = I18nMigrationIntention(
                siteDir = "/home/user/mon-site",
                defaultLanguage = "en"
            )

            val intention = task.resolveIntention()

            assertEquals("ar", intention.defaultLanguage)
        }

        @Test
        fun `CLI dryRun wins over DSL`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-cli-wins-dryrun")
                .build()

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.i18nSite.set("/home/user/mon-site")
            task.i18nDryRun.set("false")
            task.dslIntention = I18nMigrationIntention(
                siteDir = "/home/user/mon-site",
                dryRun = true
            )

            val intention = task.resolveIntention()

            assertEquals(false, intention.dryRun)
        }

        @Test
        fun `throws when no siteDir in CLI or DSL`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-no-site")
                .build()

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()

            assertThrows<IllegalArgumentException> {
                task.resolveIntention()
            }
        }

        @Test
        fun `uses defaults for langs and dryRun when only CLI siteDir set`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-defaults")
                .build()

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.i18nSite.set("/home/user/mon-site")

            val intention = task.resolveIntention()

            assertEquals(listOf("en"), intention.languages)
            assertEquals("fr", intention.defaultLanguage)
            assertEquals(true, intention.dryRun)
        }

        @Test
        fun `parses comma-separated langs with whitespace`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-langs-whitespace")
                .build()

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.i18nSite.set("/home/user/mon-site")
            task.i18nLangs.set(" en , ar , zh ")

            val intention = task.resolveIntention()

            assertEquals(listOf("en", "ar", "zh"), intention.languages)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ExecuteMigration {

        @TempDir
        lateinit var testDir: File

        @Test
        fun `warns when site directory does not exist`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-no-site-dir")
                .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.i18nSite.set("/nonexistent/path")

            task.executeMigration()
        }

        @Test
        fun `warns when templates directory does not exist`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-no-templates")
                .build()
            project.pluginManager.apply("java-base")

            val siteDir = testDir.resolve("site-no-tpl")
            siteDir.mkdirs()

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.i18nSite.set(siteDir.absolutePath)

            task.executeMigration()
        }

        @Test
        fun `executes migration with FakeLlmService and logs result`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-execute-migration")
                .build()
            project.pluginManager.apply("java-base")

            val siteDir = testDir.resolve("my-site")
            siteDir.mkdirs()
            val templatesDir = siteDir.resolve("templates")
            templatesDir.mkdirs()
            templatesDir.resolve("header.thyme").writeText("<html lang=\"fr\">")
            templatesDir.resolve("footer.thyme").writeText("<footer>")

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.i18nSite.set(siteDir.absolutePath)
            task.i18nLangs.set("en,ar")
            task.i18nDefaultLang.set("fr")
            task.i18nDryRun.set("true")

            task.executeMigration()
        }

        @Test
        fun `dry-run true does not write files`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-dryrun-true")
                .build()
            project.pluginManager.apply("java-base")

            val siteDir = testDir.resolve("dryrun-site")
            siteDir.mkdirs()
            val templatesDir = siteDir.resolve("templates")
            templatesDir.mkdirs()
            templatesDir.resolve("header.thyme").writeText("<html>")

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.i18nSite.set(siteDir.absolutePath)
            task.i18nDryRun.set("true")

            task.executeMigration()

            val messagesEn = templatesDir.resolve("messages_en.properties")
            assertTrue(!messagesEn.exists(), "messages_en.properties should NOT exist in dry-run mode")
        }

        @Test
        fun `resolves siteDir relative to contentRootDir`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-relative-site")
                .build()
            project.pluginManager.apply("java-base")

            val contentRoot = testDir.resolve("content-root")
            contentRoot.mkdirs()
            val siteDir = contentRoot.resolve("my-site")
            siteDir.mkdirs()
            siteDir.resolve("templates").mkdirs()
            siteDir.resolve("templates/header.thyme").writeText("<html>")

            val task = project.tasks.register("migrateToI18n", MigrateToI18nTask::class.java).get()
            task.contentRootDir = contentRoot
            task.i18nSite.set("my-site")

            task.executeMigration()
        }
    }
}
