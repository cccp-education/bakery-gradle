package bakery.i18n

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
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

    private fun createAdocSource(
        dir: File,
        vararg files: Pair<String, String>,
    ) {
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
        dryRun: String = "false",
    ): MigrateContentI18nTask {
        val project =
            ProjectBuilder
                .builder()
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
                "blog/post-1.adoc" to "= Post 1\n\nContenu du post.",
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
                "blog/post-1.adoc" to "= Post 1\n\nContenu du post.",
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
                "blog/post-1.adoc" to "= Post 1\n\nContenu du post.",
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
                "intro.adoc" to "= Intro\n\nBonjour le monde.",
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

    private class FakeTranslationService(
        private val suffix: String,
    ) : TranslationService {
        override fun translate(request: TranslationRequest): TranslationResult {
            val sourceText = request.sourceText
            if (sourceText.isBlank()) return TranslationResult.Success(sourceText)
            return TranslationResult.Success("$sourceText$suffix")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BlockDeltaMigration {
        @Test
        fun `debug direct DocumentTranslator in bakery classpath`() {
            val fake = FakeTranslationService(" [EN]")
            val translator = document.translation.DocumentTranslator(fake)
            val result = translator.translate("""= Intro

== Heading One

First paragraph.

== Heading Two

Second paragraph.
""", "fr", "en")
            assertTrue(result.contains("Heading One [EN]"), "Heading One should be translated")
            assertTrue(result.contains("Second paragraph. [EN]"), "Second paragraph should be translated")
        }

        @Test
        fun `debug direct ContentTranslationService in bakery`() {
            val fake = FakeTranslationService(" [EN]")
            val service = document.translation.ContentTranslationService(fake)
            val src = testDir.resolve("src.adoc")
            val tgt = testDir.resolve("tgt.adoc")

            src.writeText("""= Intro

== Heading One

First paragraph.

== Heading Two

Second paragraph.
""")
            val first = service.translateSingleFileWithBlockDelta(src, tgt, emptyMap(), "fr", "en")

            src.writeText("""= Intro

== Heading One

First paragraph modified.

== Heading Two

Second paragraph.
""")
            service.translateSingleFileWithBlockDelta(src, tgt, first, "fr", "en")

            val content = tgt.readText()
            assertTrue(content.contains("First paragraph modified. [EN]"))
            assertTrue(content.contains("Second paragraph. [EN]"))
        }

        @Test
        fun `debug with plantuml adapter and parsed blocks`() {
            val fake = FakeTranslationService(" [EN]")
            val plantUmlAdapter = document.translation.plantuml.PlantUmlTranslationAdapter(fake)
            val service = document.translation.ContentTranslationService(fake, plantUmlAdapter = plantUmlAdapter)
            val src = testDir.resolve("src.adoc")
            val tgt = testDir.resolve("tgt.adoc")

            src.writeText("""= Intro

== Heading One

First paragraph.

== Heading Two

Second paragraph.
""")
            val first = service.translateSingleFileWithBlockDelta(src, tgt, emptyMap(), "fr", "en")

            src.writeText("""= Intro

== Heading One

First paragraph modified.

== Heading Two

Second paragraph.
""")
            service.translateSingleFileWithBlockDelta(src, tgt, first, "fr", "en")
            assertTrue(tgt.readText().contains("Second paragraph. [EN]"))
        }

        @Test
        fun `debug exact task flow with checksum file roundtrip`() {
            val fake = FakeTranslationService(" [EN]")
            val sourceDir = testDir.resolve("src/content")
            createAdocSource(sourceDir, "intro.adoc" to """= Intro

== Heading One

First paragraph.

== Heading Two

Second paragraph.
""")
            val outputBase = testDir.resolve("build/i18n")
            val langDir = outputBase.resolve("en")
            val relPath = "intro.adoc"
            val sourceFile = sourceDir.resolve(relPath)
            val targetFile = langDir.resolve(relPath)
            targetFile.parentFile.mkdirs()

            val plantUmlAdapter = document.translation.plantuml.PlantUmlTranslationAdapter(fake)
            val contentService = document.translation.ContentTranslationService(fake, plantUmlAdapter = plantUmlAdapter)

            val first = contentService.translateSingleFileWithBlockDelta(sourceFile, targetFile, emptyMap(), "fr", "en")

            val checksumsFile = langDir.resolve(".bakery-block-checksums").resolve("$relPath.checksums")
            checksumsFile.parentFile.mkdirs()
            checksumsFile.writeText(first.entries.joinToString("\n") { "${it.key}=${it.value.serialize()}" })

            sourceDir.resolve("intro.adoc").writeText("""= Intro

== Heading One

First paragraph modified.

== Heading Two

Second paragraph.
""")

            val loaded = checksumsFile.readLines()
                .filter { it.contains("=") }
                .associate { line ->
                    val (idx, raw) = line.split("=", limit = 2)
                    idx to document.translation.delta.BlockChecksumEntry.parse(raw)
                }

            contentService.translateSingleFileWithBlockDelta(sourceFile, targetFile, loaded, "fr", "en")

            assertTrue(targetFile.readText().contains("Second paragraph. [EN]"))
        }

        @Test
        fun `fresh block-delta migration translates all blocks`() {
            val sourceDir = testDir.resolve("src/content")
            createAdocSource(
                sourceDir,
                "intro.adoc" to """= Intro

== Heading One

First paragraph.

== Heading Two

Second paragraph.
""",
            )
            val outputBase = testDir.resolve("build/i18n")

            val task = setupTask("test-block-fresh", sourceDir, outputBase)
            task.translationService = FakeTranslationService(" [EN]")
            task.executeContentMigration()

            val enDir = outputBase.resolve("en")
            val content = enDir.resolve("intro.adoc").readText()
            assertTrue(content.contains("[EN]"))
            val blockChecksumsFile = enDir.resolve(".bakery-block-checksums/intro.adoc.checksums")
            assertTrue(blockChecksumsFile.exists(), "block checksums file should exist after fresh run")
            assertTrue(blockChecksumsFile.readText().isNotBlank())
        }

        @Test
        fun `second run with one modified block preserves other blocks`() {
            val sourceDir = testDir.resolve("src/content")
            val outputBase = testDir.resolve("build/i18n")
            outputBase.deleteRecursively()
            createAdocSource(
                sourceDir,
                "intro.adoc" to """= Intro

== Heading One

First paragraph.

== Heading Two

Second paragraph.
""",
            )
            val task1 = setupTask("test-block-partial-1", sourceDir, outputBase)
            task1.translationService = FakeTranslationService(" [EN]")
            task1.executeContentMigration()

            val enDir = outputBase.resolve("en")
            val afterFirst = enDir.resolve("intro.adoc").readText()
            assertTrue(afterFirst.contains("First paragraph. [EN]"))
            assertTrue(afterFirst.contains("Second paragraph. [EN]"))

            sourceDir.resolve("intro.adoc").writeText("""= Intro

== Heading One

First paragraph modified.

== Heading Two

Second paragraph.
""")

            val task2 = setupTask("test-block-partial-2", sourceDir, outputBase)
            task2.translationService = FakeTranslationService(" [EN]")
            task2.executeContentMigration()

            val afterSecond = enDir.resolve("intro.adoc").readText()
            assertTrue(afterSecond.contains("First paragraph modified. [EN]"))
            assertTrue(afterSecond.contains("Second paragraph. [EN]"))
        }

        @Test
        fun `idempotence block-delta re-run with unchanged source preserves all blocks`() {
            val sourceDir = testDir.resolve("src/content")
            createAdocSource(
                sourceDir,
                "intro.adoc" to """= Intro

== Heading One

First paragraph.

== Heading Two

Second paragraph.
""",
            )
            val outputBase = testDir.resolve("build/i18n")

            val task1 = setupTask("test-block-idem-1", sourceDir, outputBase)
            task1.translationService = FakeTranslationService(" [EN]")
            task1.executeContentMigration()

            val enDir = outputBase.resolve("en")
            val afterFirst = enDir.resolve("intro.adoc").readText()

            val task2 = setupTask("test-block-idem-2", sourceDir, outputBase)
            task2.translationService = FakeTranslationService(" [EN]")
            task2.executeContentMigration()

            val afterSecond = enDir.resolve("intro.adoc").readText()
            assertEquals(afterFirst, afterSecond)
        }
    }
}
