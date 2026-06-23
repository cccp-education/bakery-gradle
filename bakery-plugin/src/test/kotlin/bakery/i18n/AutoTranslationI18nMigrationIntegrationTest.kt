package bakery.i18n

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * BKY-I18N-MIG US-4 — Tests d'intégration du mode auto-LLM.
 *
 * Valide que [I18nMigrationService] remplit `messages_en.properties` avec
 * les traductions retournées par un [TranslationService] injecté, sans
 * jamais appeler de vrai LLM.
 */
class AutoTranslationI18nMigrationIntegrationTest {

    @Test
    fun `auto-translation fills english messages from fake translator`(@TempDir tempDir: File) {
        val siteDir = tempDir.resolve("site")
        siteDir.mkdirs()
        val templatesDir = siteDir.resolve("templates")
        templatesDir.mkdirs()
        templatesDir.resolve("header.thyme").writeText("<p>Bienvenue</p>")
        templatesDir.resolve("footer.thyme").writeText("<p>Contactez-nous</p>")
        siteDir.resolve("jbake.properties").writeText("site.host=http://example.com\n")

        val fakeTranslator = FakeTranslationService()
        val service = I18nMigrationService(fakeTranslator)

        val result = service.migrate(
            siteDir = siteDir,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = false
        )

        assertTrue(result.keysExtracted > 0)
        assertEquals(2, result.filesGenerated)

        val enFile = templatesDir.resolve("messages_en.properties")
        val props = Properties()
        enFile.inputStream().use { props.load(it) }
        assertTrue(props.getProperty("header.1").startsWith("[en] "))
        assertTrue(props.getProperty("footer.1").startsWith("[en] "))

        val frFile = templatesDir.resolve("messages_fr.properties")
        val frProps = Properties()
        frFile.inputStream().use { frProps.load(it) }
        assertEquals("Bienvenue", frProps.getProperty("header.1"))
        assertEquals("Contactez-nous", frProps.getProperty("footer.1"))

        assertEquals(result.keysExtracted, fakeTranslator.requestsReceived.size)
    }

    @Test
    fun `auto-translation reuses existing translations and only translates missing keys`(@TempDir tempDir: File) {
        val siteDir = tempDir.resolve("site")
        siteDir.mkdirs()
        val templatesDir = siteDir.resolve("templates")
        templatesDir.mkdirs()
        templatesDir.resolve("header.thyme").writeText("<p>Bienvenue</p>")
        templatesDir.resolve("footer.thyme").writeText("<p>Contactez-nous</p>")
        siteDir.resolve("jbake.properties").writeText("site.host=http://example.com\n")

        val enFile = templatesDir.resolve("messages_en.properties")
        val existingProps = Properties()
        existingProps.setProperty("header.1", "Welcome (manual)")
        enFile.outputStream().use { existingProps.store(it, null) }

        val fakeTranslator = FakeTranslationService()
        val service = I18nMigrationService(fakeTranslator)

        service.migrate(
            siteDir = siteDir,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = false
        )

        val props = Properties()
        enFile.inputStream().use { props.load(it) }
        assertEquals("Welcome (manual)", props.getProperty("header.1"),
            "La traduction existante de header.1 doit être conservée")
        assertTrue(props.getProperty("footer.1").startsWith("[en] "),
            "La clé non-traduite footer.1 doit être traduite par le LLM")
        assertEquals(1, fakeTranslator.requestsReceived.size,
            "Le LLM ne doit être appelé que pour les clés manquantes")
    }

    @Test
    fun `auto-translation falls back to french when translator fails`(@TempDir tempDir: File) {
        val siteDir = tempDir.resolve("site")
        siteDir.mkdirs()
        val templatesDir = siteDir.resolve("templates")
        templatesDir.mkdirs()
        templatesDir.resolve("header.thyme").writeText("<p>Bienvenue</p>")
        siteDir.resolve("jbake.properties").writeText("site.host=http://example.com\n")

        val fakeTranslator = FakeTranslationService()
        fakeTranslator.enqueueResult(TranslationResult.Failure("LLM unavailable"))
        val service = I18nMigrationService(fakeTranslator)

        service.migrate(
            siteDir = siteDir,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = false
        )

        val enFile = templatesDir.resolve("messages_en.properties")
        val props = Properties()
        enFile.inputStream().use { props.load(it) }
        assertEquals("Bienvenue", props.getProperty("header.1"))
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
