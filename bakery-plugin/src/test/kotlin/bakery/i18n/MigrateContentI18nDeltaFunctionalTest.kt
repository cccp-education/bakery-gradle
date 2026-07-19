package bakery.i18n

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MigrateContentI18nDeltaFunctionalTest {

    @TempDir
    lateinit var testDir: File

    private fun createAdocSource(dir: File, vararg files: Pair<String, String>) {
        for ((name, content) in files) {
            dir.resolve(name).also {
                it.parentFile.mkdirs()
                it.writeText(content)
            }
        }
    }

    private fun setupTask(
        projectName: String,
        sourceDir: File,
        outputBase: File,
        targetLangs: String = "en",
        dryRun: String = "false"
    ): MigrateContentI18nTask {
        val project = ProjectBuilder.builder()
            .withProjectDir(testDir)
            .withName(projectName)
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("migrateContentI18n", MigrateContentI18nTask::class.java).get()
        task.contentI18nSource.set(sourceDir.absolutePath)
        task.contentI18nOutput.set(outputBase.absolutePath)
        task.contentI18nTargetLangs.set(targetLangs)
        task.contentI18nDryRun.set(dryRun)
        return task
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FreshMigration {

        @Test
        fun `fresh migration translates all articles`() {
            val sourceDir = testDir.resolve("src/content")
            createAdocSource(
                sourceDir,
                "intro.adoc" to "= Intro\n\nBonjour le monde.",
                "blog/post-1.adoc" to "= Post 1\n\nContenu du post."
            )
            val outputBase = testDir.resolve("build/i18n")

            val task = setupTask("test-fresh", sourceDir, outputBase)
            task.translationService = FakeTranslationService(" [EN]")

            task.executeContentMigration()

            val enDir = outputBase.resolve("en")
            assertTrue(enDir.resolve("intro.adoc").readText().contains("[EN]"))
            assertTrue(enDir.resolve("blog/post-1.adoc").readText().contains("[EN]"))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DeltaMigration {

        @Test
        fun `second run with unchanged source translates nothing`() {
            val sourceDir = testDir.resolve("src/content")
            createAdocSource(
                sourceDir,
                "intro.adoc" to "= Intro\n\nBonjour le monde.",
                "blog/post-1.adoc" to "= Post 1\n\nContenu du post."
            )
            val outputBase = testDir.resolve("build/i18n")

            val task1 = setupTask("test-delta-1", sourceDir, outputBase)
            task1.translationService = FakeTranslationService(" [EN]")
            task1.executeContentMigration()

            val enDir = outputBase.resolve("en")
            val introAfterFirst = enDir.resolve("intro.adoc").readText()
            val postAfterFirst = enDir.resolve("blog/post-1.adoc").readText()
            assertTrue(introAfterFirst.contains("[EN]"))
            assertTrue(postAfterFirst.contains("[EN]"))

            val task2 = setupTask("test-delta-2", sourceDir, outputBase)
            task2.translationService = FakeTranslationService(" [EN]")
            task2.executeContentMigration()

            val introAfterSecond = enDir.resolve("intro.adoc").readText()
            val postAfterSecond = enDir.resolve("blog/post-1.adoc").readText()
            assertEquals(introAfterFirst, introAfterSecond)
            assertEquals(postAfterFirst, postAfterSecond)
        }

        @Test
        fun `second run with one modified article translates only that one`() {
            val sourceDir = testDir.resolve("src/content")
            createAdocSource(
                sourceDir,
                "intro.adoc" to "= Intro\n\nBonjour le monde.",
                "blog/post-1.adoc" to "= Post 1\n\nContenu du post."
            )
            val outputBase = testDir.resolve("build/i18n")

            val task1 = setupTask("test-delta-mod-1", sourceDir, outputBase)
            task1.translationService = FakeTranslationService(" [EN]")
            task1.executeContentMigration()

            val enDir = outputBase.resolve("en")
            val introAfterFirst = enDir.resolve("intro.adoc").readText()

            sourceDir.resolve("blog/post-1.adoc").writeText("= Post 1\n\nContenu modifié du post.")

            val task2 = setupTask("test-delta-mod-2", sourceDir, outputBase)
            task2.translationService = FakeTranslationService(" [EN]")
            task2.executeContentMigration()

            val introAfterSecond = enDir.resolve("intro.adoc").readText()
            val postAfterSecond = enDir.resolve("blog/post-1.adoc").readText()
            assertEquals(introAfterFirst, introAfterSecond)
            assertTrue(postAfterSecond.contains("[EN]"))
            assertTrue(postAfterSecond.contains("modifié"))
        }

        @Test
        fun `second run with new article translates only the new one`() {
            val sourceDir = testDir.resolve("src/content")
            createAdocSource(
                sourceDir,
                "intro.adoc" to "= Intro\n\nBonjour le monde."
            )
            val outputBase = testDir.resolve("build/i18n")

            val task1 = setupTask("test-delta-new-1", sourceDir, outputBase)
            task1.translationService = FakeTranslationService(" [EN]")
            task1.executeContentMigration()

            val enDir = outputBase.resolve("en")
            val introAfterFirst = enDir.resolve("intro.adoc").readText()

            sourceDir.resolve("blog/post-2.adoc").also {
                it.parentFile.mkdirs()
                it.writeText("= Post 2\n\nNouvel article.")
            }

            val task2 = setupTask("test-delta-new-2", sourceDir, outputBase)
            task2.translationService = FakeTranslationService(" [EN]")
            task2.executeContentMigration()

            val introAfterSecond = enDir.resolve("intro.adoc").readText()
            val postAfterSecond = enDir.resolve("blog/post-2.adoc").readText()
            assertEquals(introAfterFirst, introAfterSecond)
            assertTrue(postAfterSecond.contains("[EN]"))
        }
    }
}
