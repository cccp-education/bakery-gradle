package bakery.i18n

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class I18nMigrationServiceTest {

    private val service = I18nMigrationService()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ScanTemplates {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `returns empty list when templates dir does not exist`() {
            val nonexistent = tempDir.resolve("nonexistent")
            val result = service.scanTemplates(nonexistent)
            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns empty list when templates dir is empty`() {
            val templatesDir = tempDir.resolve("templates")
            templatesDir.mkdirs()
            val result = service.scanTemplates(templatesDir)
            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns only thyme files`() {
            val templatesDir = tempDir.resolve("templates")
            templatesDir.mkdirs()
            templatesDir.resolve("header.thyme").writeText("<html>")
            templatesDir.resolve("footer.thyme").writeText("<footer>")
            templatesDir.resolve("styles.css").writeText("body {}")
            templatesDir.resolve("script.js").writeText("console.log()")

            val result = service.scanTemplates(templatesDir)

            assertEquals(2, result.size)
            assertTrue(result.all { it.extension == "thyme" })
        }

        @Test
        fun `scans subdirectories recursively`() {
            val templatesDir = tempDir.resolve("templates")
            templatesDir.mkdirs()
            val subDir = templatesDir.resolve("partials")
            subDir.mkdirs()
            templatesDir.resolve("header.thyme").writeText("<html>")
            subDir.resolve("breadcrumb.thyme").writeText("<nav>")

            val result = service.scanTemplates(templatesDir)

            assertEquals(2, result.size)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ExtractHardcodedText {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `extracts text content from HTML elements`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("<p>Bonjour le monde</p>\n<span>Texte court</span>")

            val result = service.extractHardcodedText(file)

            assertTrue(result.isNotEmpty())
            assertTrue(result.values.contains("Bonjour le monde"))
            assertTrue(result.values.contains("Texte court"))
        }

        @Test
        fun `extracts placeholder attributes`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("""<input type="text" placeholder="Votre nom" />""")

            val result = service.extractHardcodedText(file)

            assertTrue(result.values.contains("Votre nom"))
        }

        @Test
        fun `extracts aria-label attributes`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("""<button aria-label="Fermer la fenêtre">X</button>""")

            val result = service.extractHardcodedText(file)

            assertTrue(result.values.any { it.contains("Fermer") })
        }

        @Test
        fun `extracts meta content attributes`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("""<meta name="description" content="Description du site" />""")

            val result = service.extractHardcodedText(file)

            assertTrue(result.values.contains("Description du site"))
        }

        @Test
        fun `extracts alt attributes`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("""<img src="logo.png" alt="Logo du site" />""")

            val result = service.extractHardcodedText(file)

            assertTrue(result.values.contains("Logo du site"))
        }

        @Test
        fun `extracts title attributes`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("""<a href="/" title="Retour à l'accueil">Accueil</a>""")

            val result = service.extractHardcodedText(file)

            assertTrue(result.values.contains("Retour à l'accueil"))
        }

        @Test
        fun `skips already i18n th text attributes`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("""<span th:text="#{nav.home}">Accueil</span>""")

            val result = service.extractHardcodedText(file)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `skips already i18n th placeholder attributes`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("""<input th:placeholder="#{contact.name}" placeholder="Nom" />""")

            val result = service.extractHardcodedText(file)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `skips already i18n th attr with message keys`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("""<button th:attr="aria-label=#{a11y.close}" aria-label="Fermer">X</button>""")

            val result = service.extractHardcodedText(file)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns empty map for file with no hardcoded text`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("<div></div>\n<script>/* JS only */</script>")

            val result = service.extractHardcodedText(file)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `generates keys based on filename`() {
            val file = tempDir.resolve("header.thyme")
            file.writeText("<p>Bienvenue sur le site</p>")

            val result = service.extractHardcodedText(file)

            assertTrue(result.keys.all { it.startsWith("header.") })
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GenerateMessageFiles {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `creates one file per language`() {
            val extractions = mapOf(
                "header.thyme" to mapOf("header.1" to "Accueil", "header.2" to "Contact"),
                "footer.thyme" to mapOf("footer.1" to "Pied de page")
            )
            val templatesDir = tempDir.resolve("templates")
            templatesDir.mkdirs()

            val result = service.generateMessageFiles(extractions, listOf("fr", "en", "ar"), templatesDir)

            assertEquals(3, result.size)
            assertTrue(result.containsKey("messages_fr.properties"))
            assertTrue(result.containsKey("messages_en.properties"))
            assertTrue(result.containsKey("messages_ar.properties"))
        }

        @Test
        fun `fr language gets original values`() {
            val extractions = mapOf(
                "header.thyme" to mapOf("header.1" to "Accueil")
            )
            val templatesDir = tempDir.resolve("templates")
            templatesDir.mkdirs()

            val result = service.generateMessageFiles(extractions, listOf("fr"), templatesDir)
            val frFile = result["messages_fr.properties"]!!

            assertNotNull(frFile)
        }

        @Test
        fun `non-fr languages get empty values when no translator provided`() {
            val extractions = mapOf(
                "header.thyme" to mapOf("header.1" to "Accueil")
            )
            val templatesDir = tempDir.resolve("templates")
            templatesDir.mkdirs()

            val result = service.generateMessageFiles(extractions, listOf("en"), templatesDir)
            val enFile = result["messages_en.properties"]!!

            assertNotNull(enFile)
        }

        @Test
        fun `non-fr languages get translated values when translator provided`() {
            val fakeTranslator = FakeTranslationService()
            val serviceWithTranslator = I18nMigrationService(fakeTranslator)
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val templatesDir = siteDir.resolve("templates")
            templatesDir.mkdirs()
            templatesDir.resolve("header.thyme").writeText("<p>Accueil</p>")
            siteDir.resolve("jbake.properties").writeText("site.host=http://example.com\n")

            serviceWithTranslator.migrate(siteDir, listOf("en"), "fr", dryRun = false)

            val enFile = templatesDir.resolve("messages_en.properties")
            val props = Properties()
            enFile.inputStream().use { props.load(it) }
            assertEquals("[en] Accueil", props.getProperty("header.1"))
            assertThat(fakeTranslator.requestsReceived).hasSize(1)
        }

        @Test
        fun `fr language keeps original values even with translator`() {
            val fakeTranslator = FakeTranslationService()
            val serviceWithTranslator = I18nMigrationService(fakeTranslator)
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val templatesDir = siteDir.resolve("templates")
            templatesDir.mkdirs()
            templatesDir.resolve("header.thyme").writeText("<p>Accueil</p>")
            siteDir.resolve("jbake.properties").writeText("site.host=http://example.com\n")

            serviceWithTranslator.migrate(siteDir, listOf("fr"), "fr", dryRun = false)

            val frFile = templatesDir.resolve("messages_fr.properties")
            val props = Properties()
            frFile.inputStream().use { props.load(it) }
            assertEquals("Accueil", props.getProperty("header.1"))
            assertThat(fakeTranslator.requestsReceived).isEmpty()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ReplaceHardcodedWithMessageKeys {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `replaces text content with th text`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("<p>Bonjour le monde</p>")
            val extractions = mapOf("test.1" to "Bonjour le monde")

            val result = service.replaceHardcodedWithMessageKeys(file, extractions)

            val content = result.readText()
            assertTrue(content.contains("""th:text="#{test.1}""""))
        }

        @Test
        fun `replaces placeholder with th placeholder`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("""<input type="text" placeholder="Votre nom" />""")
            val extractions = mapOf("test.1" to "Votre nom")

            val result = service.replaceHardcodedWithMessageKeys(file, extractions)

            val content = result.readText()
            assertTrue(content.contains("""th:placeholder="#{test.1}""""))
        }

        @Test
        fun `replaces aria-label with th attr`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("""<button aria-label="Fermer">X</button>""")
            val extractions = mapOf("test.1" to "Fermer")

            val result = service.replaceHardcodedWithMessageKeys(file, extractions)

            val content = result.readText()
            assertTrue(content.contains("""aria-label=#{test.1}"""))
        }

        @Test
        fun `replaces meta content with th content`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("""<meta name="description" content="Description du site" />""")
            val extractions = mapOf("test.1" to "Description du site")

            val result = service.replaceHardcodedWithMessageKeys(file, extractions)

            val content = result.readText()
            assertTrue(content.contains("""th:content="#{test.1}""""))
        }

        @Test
        fun `replaces alt with th alt`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("""<img src="logo.png" alt="Logo" />""")
            val extractions = mapOf("test.1" to "Logo")

            val result = service.replaceHardcodedWithMessageKeys(file, extractions)

            val content = result.readText()
            assertTrue(content.contains("""th:alt="#{test.1}""""))
        }

        @Test
        fun `replaces title with th title`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("""<a href="/" title="Accueil">Home</a>""")
            val extractions = mapOf("test.1" to "Accueil")

            val result = service.replaceHardcodedWithMessageKeys(file, extractions)

            val content = result.readText()
            assertTrue(content.contains("""th:title="#{test.1}""""))
        }

        @Test
        fun `does not modify already i18n lines`() {
            val file = tempDir.resolve("test.thyme")
            file.writeText("""<span th:text="#{nav.home}">Accueil</span>""")
            val extractions = mapOf("test.1" to "Accueil")

            val result = service.replaceHardcodedWithMessageKeys(file, extractions)

            val content = result.readText()
            assertTrue(content.contains("""th:text="#{nav.home}""""))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TranslateMessageFiles {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `applyTranslations fills empty english values from reference map`() {
            val templatesDir = tempDir.resolve("templates")
            templatesDir.mkdirs()
            val enFile = templatesDir.resolve("messages_en.properties")
            enFile.writeText("header.1=\nfooter.1=\n")

            val translations = mapOf("header.1" to "Home", "footer.1" to "Footer", "unknown.1" to "Unknown")

            val updated = I18nTranslationApplier.applyTranslations(enFile, translations)

            assertTrue(updated)
            val content = enFile.readText()
            assertTrue(content.contains("header.1=Home"))
            assertTrue(content.contains("footer.1=Footer"))
            assertFalse(content.contains("unknown.1"))
        }

        @Test
        fun `applyTranslations returns false when target file does not exist`() {
            val missingFile = tempDir.resolve("missing.properties")

            val updated = I18nTranslationApplier.applyTranslations(missingFile, mapOf("k" to "v"))

            assertFalse(updated)
        }

        @Test
        fun `applyTranslations returns false when no keys match`() {
            val templatesDir = tempDir.resolve("templates")
            templatesDir.mkdirs()
            val enFile = templatesDir.resolve("messages_en.properties")
            enFile.writeText("header.1=\n")

            val updated = I18nTranslationApplier.applyTranslations(enFile, mapOf("other.1" to "Other"))

            assertFalse(updated)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WriteMessageFiles {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `creates parent directories for message files`() {
            val templatesDir = tempDir.resolve("templates")
            val messageFiles = mapOf(
                "messages_fr.properties" to templatesDir.resolve("messages_fr.properties"),
                "messages_en.properties" to templatesDir.resolve("messages_en.properties")
            )
            val extractions = mapOf(
                "header.thyme" to mapOf("header.1" to "Accueil")
            )

            service.writeMessageFiles(messageFiles, extractions)

            assertTrue(templatesDir.exists())
            assertTrue(templatesDir.resolve("messages_fr.properties").exists())
            assertTrue(templatesDir.resolve("messages_en.properties").exists())
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InjectSiteLanguage {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `adds site language to jbake properties`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("site.host=http://example.com\n")

            val result = service.injectSiteLanguage(siteDir, "fr")

            assertTrue(result)
            val content = jbakeProps.readText()
            assertTrue(content.contains("site.language=fr"))
        }

        @Test
        fun `does not duplicate site language if already present`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("site.host=http://example.com\nsite.language=en\n")

            val result = service.injectSiteLanguage(siteDir, "fr")

            assertFalse(result)
            val content = jbakeProps.readText()
            assertEquals(1, content.lines().count { it.startsWith("site.language=") })
        }

        @Test
        fun `returns false when jbake properties does not exist`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()

            val result = service.injectSiteLanguage(siteDir, "fr")

            assertFalse(result)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Migrate {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `dryRun true does not write files or modify templates`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val templatesDir = siteDir.resolve("templates")
            templatesDir.mkdirs()
            val headerFile = templatesDir.resolve("header.thyme")
            val originalContent = "<p>Bienvenue</p>"
            headerFile.writeText(originalContent)
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("site.host=http://example.com\n")

            val result = service.migrate(siteDir, listOf("en", "ar"), "fr", dryRun = true)

            assertTrue(result.dryRun)
            assertTrue(result.keysExtracted > 0)
            assertEquals(0, result.filesGenerated)
            assertEquals(0, result.templatesModified)
            assertEquals(originalContent, headerFile.readText())
            assertFalse(templatesDir.resolve("messages_en.properties").exists())
        }

        @Test
        fun `dryRun false writes message files and modifies templates`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val templatesDir = siteDir.resolve("templates")
            templatesDir.mkdirs()
            val headerFile = templatesDir.resolve("header.thyme")
            headerFile.writeText("<p>Bienvenue sur le site</p>")
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("site.host=http://example.com\n")

            val result = service.migrate(siteDir, listOf("en", "ar"), "fr", dryRun = false)

            assertFalse(result.dryRun)
            assertTrue(result.keysExtracted > 0)
            assertEquals(2, result.filesGenerated)
            assertEquals(1, result.templatesModified)
            assertTrue(templatesDir.resolve("messages_en.properties").exists())
            assertTrue(templatesDir.resolve("messages_ar.properties").exists())
            val modifiedContent = headerFile.readText()
            assertTrue(modifiedContent.contains("th:text="))
        }

        @Test
        fun `returns zero when templates dir is empty`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            siteDir.resolve("templates").mkdirs()

            val result = service.migrate(siteDir, listOf("en"), "fr", dryRun = false)

            assertEquals(0, result.keysExtracted)
            assertEquals(0, result.filesGenerated)
            assertEquals(0, result.templatesModified)
        }

        @Test
        fun `returns zero when no hardcoded text found`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val templatesDir = siteDir.resolve("templates")
            templatesDir.mkdirs()
            templatesDir.resolve("header.thyme").writeText("<div></div>")

            val result = service.migrate(siteDir, listOf("en"), "fr", dryRun = false)

            assertEquals(0, result.keysExtracted)
            assertEquals(0, result.filesGenerated)
            assertEquals(0, result.templatesModified)
        }

        @Test
        fun `injects site language into jbake properties`() {
            val siteDir = tempDir.resolve("site")
            siteDir.mkdirs()
            val templatesDir = siteDir.resolve("templates")
            templatesDir.mkdirs()
            templatesDir.resolve("header.thyme").writeText("<p>Bienvenue</p>")
            val jbakeProps = siteDir.resolve("jbake.properties")
            jbakeProps.writeText("site.host=http://example.com\n")

            service.migrate(siteDir, listOf("en"), "fr", dryRun = false)

            val content = jbakeProps.readText()
            assertTrue(content.contains("site.language=fr"))
        }
    }

    private class FakeTranslationService : TranslationService {

        val requestsReceived = mutableListOf<TranslationRequest>()
        private val resultQueue: MutableList<TranslationResult> = mutableListOf()

        fun enqueueResult(result: TranslationResult) {
            resultQueue.add(result)
        }

        override fun translate(request: TranslationRequest): TranslationResult {
            requestsReceived.add(request)
            return if (resultQueue.isNotEmpty()) {
                resultQueue.removeAt(0)
            } else {
                TranslationResult.Success("[en] ${request.sourceText}")
            }
        }
    }
}
