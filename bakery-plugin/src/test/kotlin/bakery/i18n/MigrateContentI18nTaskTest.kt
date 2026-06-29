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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MigrateContentI18nTaskTest {

    @TempDir
    lateinit var tempDir: File

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TaskRegistration {

        @Test
        fun `task is registered with correct group and description`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-migrate-content-i18n")
                .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("migrateContentI18n", MigrateContentI18nTask::class.java).get()

            assertNotNull(task)
            assertEquals("transform", task.group)
            assertTrue(task.description!!.contains("Migre le contenu AsciiDoc"))
        }

        @Test
        fun `task initializes all CLI properties with empty defaults`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName("test-migrate-content-defaults")
                .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("migrateContentI18n", MigrateContentI18nTask::class.java).get()

            assertEquals("", task.contentI18nSource.get())
            assertEquals("", task.contentI18nOutput.get())
            assertEquals("", task.contentI18nTargetLangs.get())
            assertEquals("", task.contentI18nSourceLang.get())
            assertEquals("", task.contentI18nDryRun.get())
            assertNull(task.dslIntention)
            assertNull(task.translationService)
            assertNull(task.contentRootDir)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResolveIntention {

        private fun setupTask(name: String): MigrateContentI18nTask {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .withName(name)
                .build()
            return project.tasks.register("migrateContentI18n", MigrateContentI18nTask::class.java).get()
        }

        @Test
        fun `uses CLI source and output when set`() {
            val task = setupTask("test-cli-source")
            task.contentI18nSource.set("/home/user/content")
            task.contentI18nOutput.set("/tmp/output")

            val intention = task.resolveIntention()

            assertEquals("/home/user/content", intention.sourceDir)
            assertEquals("/tmp/output", intention.outputDir)
            assertEquals(listOf("en"), intention.targetLanguages)
            assertEquals("fr", intention.sourceLanguage)
            assertEquals(true, intention.dryRun)
        }

        @Test
        fun `uses CLI targetLangs when set`() {
            val task = setupTask("test-cli-langs")
            task.contentI18nSource.set("/home/user/content")
            task.contentI18nOutput.set("/tmp/output")
            task.contentI18nTargetLangs.set("en,ar,zh")

            val intention = task.resolveIntention()

            assertEquals(listOf("en", "ar", "zh"), intention.targetLanguages)
        }

        @Test
        fun `uses CLI sourceLang when set`() {
            val task = setupTask("test-cli-source-lang")
            task.contentI18nSource.set("/home/user/content")
            task.contentI18nOutput.set("/tmp/output")
            task.contentI18nSourceLang.set("en")
            task.contentI18nTargetLangs.set("es,zh")

            val intention = task.resolveIntention()

            assertEquals("en", intention.sourceLanguage)
            assertEquals(listOf("es", "zh"), intention.targetLanguages)
        }

        @Test
        fun `uses CLI dryRun false when set`() {
            val task = setupTask("test-cli-dryrun")
            task.contentI18nSource.set("/home/user/content")
            task.contentI18nOutput.set("/tmp/output")
            task.contentI18nDryRun.set("false")

            val intention = task.resolveIntention()

            assertEquals(false, intention.dryRun)
        }

        @Test
        fun `falls back to DSL when CLI is blank`() {
            val task = setupTask("test-dsl-fallback")
            task.dslIntention = ContentMigrationIntention(
                sourceDir = "/home/user/dsl-content",
                outputDir = "/tmp/dsl-output",
                targetLanguages = listOf("es", "zh"),
                sourceLanguage = "en",
                dryRun = false
            )

            val intention = task.resolveIntention()

            assertEquals("/home/user/dsl-content", intention.sourceDir)
            assertEquals("/tmp/dsl-output", intention.outputDir)
            assertEquals(listOf("es", "zh"), intention.targetLanguages)
            assertEquals("en", intention.sourceLanguage)
            assertEquals(false, intention.dryRun)
        }

        @Test
        fun `CLI source wins over DSL`() {
            val task = setupTask("test-cli-wins-source")
            task.contentI18nSource.set("/home/user/cli-content")
            task.dslIntention = ContentMigrationIntention(
                sourceDir = "/home/user/dsl-content",
                outputDir = "/tmp/output"
            )

            val intention = task.resolveIntention()

            assertEquals("/home/user/cli-content", intention.sourceDir)
        }

        @Test
        fun `CLI output wins over DSL`() {
            val task = setupTask("test-cli-wins-output")
            task.contentI18nSource.set("/home/user/content")
            task.contentI18nOutput.set("/tmp/cli-output")
            task.dslIntention = ContentMigrationIntention(
                sourceDir = "/home/user/content",
                outputDir = "/tmp/dsl-output"
            )

            val intention = task.resolveIntention()

            assertEquals("/tmp/cli-output", intention.outputDir)
        }

        @Test
        fun `CLI targetLangs win over DSL`() {
            val task = setupTask("test-cli-wins-langs")
            task.contentI18nSource.set("/home/user/content")
            task.contentI18nOutput.set("/tmp/output")
            task.contentI18nTargetLangs.set("en,ar")
            task.dslIntention = ContentMigrationIntention(
                sourceDir = "/home/user/content",
                outputDir = "/tmp/output",
                targetLanguages = listOf("en", "es", "zh")
            )

            val intention = task.resolveIntention()

            assertEquals(listOf("en", "ar"), intention.targetLanguages)
        }

        @Test
        fun `CLI sourceLang wins over DSL`() {
            val task = setupTask("test-cli-wins-source-lang")
            task.contentI18nSource.set("/home/user/content")
            task.contentI18nOutput.set("/tmp/output")
            task.contentI18nSourceLang.set("ar")
            task.dslIntention = ContentMigrationIntention(
                sourceDir = "/home/user/content",
                outputDir = "/tmp/output",
                sourceLanguage = "fr"
            )

            val intention = task.resolveIntention()

            assertEquals("ar", intention.sourceLanguage)
        }

        @Test
        fun `CLI dryRun wins over DSL`() {
            val task = setupTask("test-cli-wins-dryrun")
            task.contentI18nSource.set("/home/user/content")
            task.contentI18nOutput.set("/tmp/output")
            task.contentI18nDryRun.set("false")
            task.dslIntention = ContentMigrationIntention(
                sourceDir = "/home/user/content",
                outputDir = "/tmp/output",
                dryRun = true
            )

            val intention = task.resolveIntention()

            assertEquals(false, intention.dryRun)
        }

        @Test
        fun `throws when no sourceDir in CLI or DSL`() {
            val task = setupTask("test-no-source")
            task.contentI18nOutput.set("/tmp/output")

            assertThrows<IllegalArgumentException> {
                task.resolveIntention()
            }
        }

        @Test
        fun `throws when no outputDir in CLI or DSL`() {
            val task = setupTask("test-no-output")
            task.contentI18nSource.set("/home/user/content")

            assertThrows<IllegalArgumentException> {
                task.resolveIntention()
            }
        }

        @Test
        fun `uses defaults for targetLangs sourceLang and dryRun when only required CLI set`() {
            val task = setupTask("test-defaults")
            task.contentI18nSource.set("/home/user/content")
            task.contentI18nOutput.set("/tmp/output")

            val intention = task.resolveIntention()

            assertEquals(listOf("en"), intention.targetLanguages)
            assertEquals("fr", intention.sourceLanguage)
            assertEquals(true, intention.dryRun)
        }

        @Test
        fun `parses comma-separated targetLangs with whitespace`() {
            val task = setupTask("test-langs-whitespace")
            task.contentI18nSource.set("/home/user/content")
            task.contentI18nOutput.set("/tmp/output")
            task.contentI18nTargetLangs.set(" en , ar , zh ")

            val intention = task.resolveIntention()

            assertEquals(listOf("en", "ar", "zh"), intention.targetLanguages)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ExecuteContentMigration {

        @TempDir
        lateinit var testDir: File

        private fun createAdocSource(dir: File, vararg files: String) {
            for (file in files) {
                dir.resolve(file).also {
                    it.parentFile.mkdirs()
                    it.writeText("= ${file.removeSuffix(".adoc")}\n\nContent for $file.")
                }
            }
        }

        @Test
        fun `warns when source directory does not exist`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-no-source-dir")
                .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("migrateContentI18n", MigrateContentI18nTask::class.java).get()
            task.contentI18nSource.set("/nonexistent/path")
            task.contentI18nOutput.set("/tmp/output")

            task.executeContentMigration()
        }

        @Test
        fun `dryRun does not write any output files`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-dryrun")
                .build()
            project.pluginManager.apply("java-base")

            val sourceDir = testDir.resolve("src/content")
            sourceDir.mkdirs()
            createAdocSource(sourceDir, "intro.adoc", "guide.adoc")
            sourceDir.resolve("image.png").writeText("fake-png")

            val outputBase = testDir.resolve("build/i18n")

            val task = project.tasks.register("migrateContentI18n", MigrateContentI18nTask::class.java).get()
            task.contentI18nSource.set(sourceDir.absolutePath)
            task.contentI18nOutput.set(outputBase.absolutePath)
            task.contentI18nTargetLangs.set("en,es")
            task.contentI18nDryRun.set("true")

            task.executeContentMigration()

            assertTrue(!outputBase.exists(), "Dry-run should not create output directory")
        }

        @Test
        fun `copies files without translation when no translationService`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-copy-only")
                .build()
            project.pluginManager.apply("java-base")

            val sourceDir = testDir.resolve("src/content")
            sourceDir.mkdirs()
            createAdocSource(sourceDir, "intro.adoc")
            sourceDir.resolve("image.png").writeText("fake-png")

            val outputBase = testDir.resolve("build/i18n")

            val task = project.tasks.register("migrateContentI18n", MigrateContentI18nTask::class.java).get()
            task.contentI18nSource.set(sourceDir.absolutePath)
            task.contentI18nOutput.set(outputBase.absolutePath)
            task.contentI18nTargetLangs.set("en")
            task.contentI18nDryRun.set("false")

            task.executeContentMigration()

            assertTrue(outputBase.resolve("en/intro.adoc").exists(), "intro.adoc should be copied")
            assertTrue(outputBase.resolve("en/image.png").exists(), "image.png should be copied")
        }

        @Test
        fun `with translationService translates adoc files`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-translate")
                .build()
            project.pluginManager.apply("java-base")

            val sourceDir = testDir.resolve("src/content")
            sourceDir.mkdirs()
            sourceDir.resolve("intro.adoc").writeText("= Intro\n\nBonjour le monde.")

            val outputBase = testDir.resolve("build/i18n")

            val task = project.tasks.register("migrateContentI18n", MigrateContentI18nTask::class.java).get()
            task.contentI18nSource.set(sourceDir.absolutePath)
            task.contentI18nOutput.set(outputBase.absolutePath)
            task.contentI18nTargetLangs.set("en")
            task.contentI18nDryRun.set("false")
            task.translationService = FakeTranslationService(" [EN]")

            task.executeContentMigration()

            val translated = outputBase.resolve("en/intro.adoc").readText()
            assertTrue(translated.contains("[EN]"), "Translation suffix should be present in translated content")
        }

        @Test
        fun `non-adoc files are copied without translation`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-non-adoc-preserved")
                .build()
            project.pluginManager.apply("java-base")

            val sourceDir = testDir.resolve("src/content")
            sourceDir.mkdirs()
            sourceDir.resolve("intro.adoc").writeText("= Intro\n\nTexte.")
            sourceDir.resolve("logo.png").writeText("fake-png")
            sourceDir.resolve("data.json").writeText("{\"key\": \"value\"}")

            val outputBase = testDir.resolve("build/i18n")

            val task = project.tasks.register("migrateContentI18n", MigrateContentI18nTask::class.java).get()
            task.contentI18nSource.set(sourceDir.absolutePath)
            task.contentI18nOutput.set(outputBase.absolutePath)
            task.contentI18nTargetLangs.set("en")
            task.contentI18nDryRun.set("false")
            task.translationService = FakeTranslationService(" [EN]")

            task.executeContentMigration()

            val enDir = outputBase.resolve("en")
            assertTrue(enDir.resolve("logo.png").exists(), "logo.png should be copied")
            assertTrue(enDir.resolve("data.json").exists(), "data.json should be copied")
        }

        @Test
        fun `directory structure is preserved in output`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-dir-structure")
                .build()
            project.pluginManager.apply("java-base")

            val sourceDir = testDir.resolve("src/content")
            createAdocSource(sourceDir, "index.adoc", "blog/post-1.adoc", "blog/post-2.adoc")

            val outputBase = testDir.resolve("build/i18n")

            val task = project.tasks.register("migrateContentI18n", MigrateContentI18nTask::class.java).get()
            task.contentI18nSource.set(sourceDir.absolutePath)
            task.contentI18nOutput.set(outputBase.absolutePath)
            task.contentI18nTargetLangs.set("en")
            task.contentI18nDryRun.set("false")

            task.executeContentMigration()

            val enDir = outputBase.resolve("en")
            assertTrue(enDir.resolve("index.adoc").exists())
            assertTrue(enDir.resolve("blog/post-1.adoc").exists())
            assertTrue(enDir.resolve("blog/post-2.adoc").exists())
        }

        @Test
        fun `relative sourceDir resolved against contentRootDir`() {
            val project = ProjectBuilder.builder()
                .withProjectDir(testDir)
                .withName("test-relative-source")
                .build()
            project.pluginManager.apply("java-base")

            val contentRoot = testDir.resolve("content-root")
            contentRoot.mkdirs()
            val sourceDir = contentRoot.resolve("my-content")
            sourceDir.mkdirs()
            sourceDir.resolve("article.adoc").writeText("= Article\n\nBody.")

            val outputBase = testDir.resolve("build/i18n")

            val task = project.tasks.register("migrateContentI18n", MigrateContentI18nTask::class.java).get()
            task.contentRootDir = contentRoot
            task.contentI18nSource.set("my-content")
            task.contentI18nOutput.set(outputBase.absolutePath)
            task.contentI18nTargetLangs.set("en")
            task.contentI18nDryRun.set("false")

            task.executeContentMigration()

            assertTrue(outputBase.resolve("en/article.adoc").exists())
        }
    }
}
