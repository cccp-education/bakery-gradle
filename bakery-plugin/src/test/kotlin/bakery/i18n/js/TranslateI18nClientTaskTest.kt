package bakery.i18n.js

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
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

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-3.
 *
 * Unit tests for [TranslateI18nClientTask]: intention resolution
 * (CLI > DSL > defaults), ink-economy delta, dry-run safety and LLM failure
 * degradation (a failed key stays missing, the document is left intact).
 */
class TranslateI18nClientTaskTest {
    @TempDir
    lateinit var tempDir: File

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TaskRegistration {
        @Test
        fun `task is registered with transform group and description`() {
            val task = setupTask("test-i18n-client-registration")

            assertNotNull(task)
            assertEquals("transform", task.group)
            assertTrue(task.description!!.contains("i18n JS client"))
        }

        @Test
        fun `task initializes all CLI properties with empty defaults`() {
            val task = setupTask("test-i18n-client-defaults")

            assertEquals("", task.i18nClientSource.get())
            assertEquals("", task.i18nClientTargetLangs.get())
            assertEquals("", task.i18nClientSourceLang.get())
            assertEquals("", task.i18nClientDryRun.get())
            assertNull(task.dslIntention)
            assertNull(task.translationService)

        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResolveIntention {
        @Test
        fun `uses CLI source langs and reference when set`() {
            val task = setupTask("test-i18n-client-cli")
            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en,de")
            task.i18nClientSourceLang.set("fr")
            task.i18nClientDryRun.set("false")

            val intention = task.resolveIntention()

            assertEquals(listOf("maquette/js"), intention.sourceDirs)
            assertEquals(listOf("en", "de"), intention.targetLanguages)
            assertEquals("fr", intention.referenceLanguage)
            assertEquals(false, intention.dryRun)
        }

        @Test
        fun `falls back to DSL when CLI is blank`() {
            val task = setupTask("test-i18n-client-dsl")
            task.dslIntention =
                I18nClientMigrationIntention(
                    sourceDirs = listOf("maquette/js"),
                    referenceLanguage = "fr",
                    targetLanguages = listOf("it"),
                    dryRun = false,
                )

            val intention = task.resolveIntention()

            assertEquals(listOf("maquette/js"), intention.sourceDirs)
            assertEquals(listOf("it"), intention.targetLanguages)
            assertEquals(false, intention.dryRun)
        }

        @Test
        fun `CLI source wins over DSL`() {
            val task = setupTask("test-i18n-client-cli-wins")
            task.i18nClientSource.set("cli/js")
            task.dslIntention = I18nClientMigrationIntention(sourceDirs = listOf("dsl/js"))

            assertEquals(listOf("cli/js"), task.resolveIntention().sourceDirs)
        }

        @Test
        fun `defaults are dry-run true with en target`() {
            val task = setupTask("test-i18n-client-default-intention")
            task.i18nClientSource.set("maquette/js")

            val intention = task.resolveIntention()

            assertEquals(true, intention.dryRun)
            assertEquals(listOf("en"), intention.targetLanguages)
            assertEquals("fr", intention.referenceLanguage)
        }

        @Test
        fun `throws when no source is provided`() {
            val task = setupTask("test-i18n-client-no-source")

            assertThrows<IllegalArgumentException> { task.resolveIntention() }
        }

        @Test
        fun `propagation defaults to true and honours the CLI override`() {
            val task = setupTask("test-i18n-client-propagate-cli")
            task.i18nClientSource.set("maquette/js")
            assertEquals(true, task.resolveIntention().propagate)

            task.i18nClientPropagate.set("false")
            assertEquals(false, task.resolveIntention().propagate)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Execute {
        @Test
        fun `dry-run reports the delta and writes nothing`() {
            val source = writeDictionary("maquette/js/i18n.js", chromeWithMissingEn())
            val task = setupTask("test-i18n-client-dryrun")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("true")
            task.translationService = RecordingTranslationService()

            task.executeI18nClientTranslation()

            assertEquals(chromeWithMissingEn(), source.readText())
            assertEquals(0, (task.translationService as RecordingTranslationService).requests.size)
        }

        @Test
        fun `translates only the missing keys and inserts them`() {
            val source = writeDictionary("maquette/js/i18n.js", chromeWithMissingEn())
            val task = setupTask("test-i18n-client-translate")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("false")
            val service = RecordingTranslationService()
            task.translationService = service

            task.executeI18nClientTranslation()

            assertEquals(1, service.requests.size)
            assertEquals("Panier", service.requests.single().sourceText)
            val updated = source.readText()
            assertEquals("Cart", I18nJsDictionary.parse(updated)["en"]!!["nav.cart"])
        }

        @Test
        fun `a complete dictionary is a strict no-op without LLM call`() {
            val complete =
                """
                |  var DICT = {
                |    fr: {
                |      "nav.home": "Accueil"
                |    },
                |    en: {
                |      "nav.home": "Home"
                |    }
                |  };
                """.trimMargin()
            val source = writeDictionary("maquette/js/i18n.js", complete)
            val task = setupTask("test-i18n-client-noop")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("false")
            val service = RecordingTranslationService()
            task.translationService = service

            task.executeI18nClientTranslation()

            assertEquals(0, service.requests.size)
            assertEquals(complete, source.readText())
        }

        @Test
        fun `re-running after translation is idempotent`() {
            val source = writeDictionary("maquette/js/i18n.js", chromeWithMissingEn())
            val task = setupTask("test-i18n-client-idempotent")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("false")
            val service = RecordingTranslationService()
            task.translationService = service

            task.executeI18nClientTranslation()
            val afterFirst = source.readText()
            task.executeI18nClientTranslation()

            assertEquals(afterFirst, source.readText())
            assertEquals(1, service.requests.size)
        }

        @Test
        fun `a failed translation keeps the key missing and the document untouched`() {
            val source = writeDictionary("maquette/js/i18n.js", chromeWithMissingEn())
            val task = setupTask("test-i18n-client-failure")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("false")
            task.translationService = FailingTranslationService()

            task.executeI18nClientTranslation()

            assertEquals(chromeWithMissingEn(), source.readText())
        }

        @Test
        fun `html variants are never sent to the model`() {
            val source =
                """
                |  var DICT = {
                |    fr: {
                |      "nav.home": "Accueil",
                |      "hero.html": "<b>Salut</b>"
                |    },
                |    en: {
                |      "nav.home": "Home"
                |    }
                |  };
                """.trimMargin()
            writeDictionary("maquette/js/i18n.js", source)
            val task = setupTask("test-i18n-client-html")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("false")
            val service = RecordingTranslationService()
            task.translationService = service

            task.executeI18nClientTranslation()

            assertEquals(0, service.requests.size)
        }

        @Test
        fun `an absent source directory is warned not crash`() {
            val task = setupTask("test-i18n-client-missing-dir")

            task.i18nClientSource.set("maquette/nonexistent")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("false")
            task.translationService = RecordingTranslationService()

            task.executeI18nClientTranslation()
        }

        @Test
        fun `translation without a service leaves the source untouched`() {
            val source = writeDictionary("maquette/js/i18n.js", chromeWithMissingEn())
            val task = setupTask("test-i18n-client-null-service")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("false")

            task.executeI18nClientTranslation()

            assertEquals(chromeWithMissingEn(), source.readText())
        }
        @Test
        fun `multiple source directories are merged`() {
            writeDictionary("maquette/js/i18n.js", chromeWithMissingEn())
            writeDictionary(
                "maquette/js/i18n-extra-langs.js",
                """
                |(function(){
                |  TALARIA.I18N.extend({
                |    de: {}
                |  });
                |})();
                """.trimMargin(),
            )
            val task = setupTask("test-i18n-client-multi-dir")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("de")
            task.i18nClientDryRun.set("false")
            val service = RecordingTranslationService()
            task.translationService = service

            task.executeI18nClientTranslation()

            val patch = tempDir.resolve("maquette/js/i18n-extra-langs.js").readText()
            assertEquals("Startseite", I18nJsDictionary.parse(patch)["de"]!!["nav.home"])
        }

        @Test
        fun `translating a maquette dictionary propagates the byte-identical copy to jbake`() {
            val source = writeDictionary("maquette/js/i18n.js", chromeWithMissingEn())
            writeDictionary("jbake/assets/js/i18n.js", chromeWithMissingEn())
            val task = setupTask("test-i18n-client-propagate")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("false")
            task.translationService = RecordingTranslationService()

            task.executeI18nClientTranslation()

            val published = tempDir.resolve("jbake/assets/js/i18n.js").readText()
            assertEquals(source.readText(), published)
            assertEquals("Cart", I18nJsDictionary.parse(published)["en"]!!["nav.cart"])
        }

        @Test
        fun `a complete dictionary still propagates a drifted publication copy`() {
            val source = writeDictionary("maquette/js/i18n.js", completeDictionary())
            writeDictionary("jbake/assets/js/i18n.js", "var DICT = { stale };")
            val task = setupTask("test-i18n-client-propagate-noop")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("false")
            task.translationService = RecordingTranslationService()

            task.executeI18nClientTranslation()

            assertEquals(
                source.readText(),
                tempDir.resolve("jbake/assets/js/i18n.js").readText(),
            )
        }

        @Test
        fun `the propagate flag disables the publication copy`() {
            writeDictionary("maquette/js/i18n.js", chromeWithMissingEn())
            val published = writeDictionary("jbake/assets/js/i18n.js", chromeWithMissingEn())
            val task = setupTask("test-i18n-client-no-propagate")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("false")
            task.i18nClientPropagate.set("false")
            task.translationService = RecordingTranslationService()

            task.executeI18nClientTranslation()

            assertEquals(chromeWithMissingEn(), published.readText())
        }

        @Test
        fun `dry-run never writes the publication copy`() {
            writeDictionary("maquette/js/i18n.js", chromeWithMissingEn())
            val published = writeDictionary("jbake/assets/js/i18n.js", "var DICT = { stale };")
            val task = setupTask("test-i18n-client-propagate-dryrun")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("true")
            task.translationService = RecordingTranslationService()

            task.executeI18nClientTranslation()

            assertEquals("var DICT = { stale };", published.readText())
        }

        @Test
        fun `catalogue coverage reports missing language and fields`() {
            writeDictionary("maquette/js/i18n.js", chromeWithMissingEn())
            writeDictionary("maquette/js/i18n-content.js", catalogueWithGapEn())
            val task = setupTask("test-i18n-client-catalogue-coverage")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en,es")
            task.i18nClientDryRun.set("true")

            val coverage = task.catalogueCoverage(task.resolveIntention())

            assertEquals(listOf("es"), coverage.missingLanguages)
            assertEquals(listOf("cda"), coverage.missingFormations["en"])
            assertEquals(listOf("modules"), coverage.missingFields["en"]!!["fpa"])
        }

        @Test
        fun `a complete catalogue has no coverage gap`() {
            writeDictionary("maquette/js/i18n.js", chromeWithMissingEn())
            writeDictionary("maquette/js/i18n-content.js", catalogueComplete())
            val task = setupTask("test-i18n-client-catalogue-complete")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("true")

            val coverage = task.catalogueCoverage(task.resolveIntention())

            assertEquals(false, coverage.hasGap)
        }

        @Test
        fun `a flat-only tree yields an empty catalogue coverage`() {
            writeDictionary("maquette/js/i18n.js", chromeWithMissingEn())
            val task = setupTask("test-i18n-client-no-catalogue")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("true")

            val coverage = task.catalogueCoverage(task.resolveIntention())

            assertEquals(I18nCatalogPlan.EMPTY, coverage)
        }

        @Test
        fun `catalogue is never sent to the flat writer`() {
            val catalogue = catalogueWithGapEn()
            val file = writeDictionary("maquette/js/i18n-content.js", catalogue)
            val task = setupTask("test-i18n-client-catalogue-untouched")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("false")
            val service = RecordingTranslationService()
            task.translationService = service

            task.executeI18nClientTranslation()

            assertEquals(0, service.requests.size)
            assertEquals(catalogue, file.readText())
        }

        @Test
        fun `a translated but now unbalanced dictionary is not written`() {
            val unbalanced =
                """
                |  var DICT = {
                |    fr: {
                |      "nav.home": "Accueil",
                |      "nav.cart": "Panier"
                |    },
                |    en: {
                |      "nav.home": "Home"
                """.trimMargin()
            val source = writeDictionary("maquette/js/i18n.js", unbalanced)
            val task = setupTask("test-i18n-client-guarded-write")

            task.i18nClientSource.set("maquette/js")
            task.i18nClientTargetLangs.set("en")
            task.i18nClientDryRun.set("false")
            task.translationService = RecordingTranslationService()

            task.executeI18nClientTranslation()

            assertEquals(unbalanced, source.readText())
        }
    }

    private fun completeDictionary(): String =
        """
        |  var DICT = {
        |    fr: {
        |      "nav.home": "Accueil"
        |    },
        |    en: {
        |      "nav.home": "Home"
        |    }
        |  };
        """.trimMargin()

    private fun writeDictionary(
        relative: String,
        content: String,
    ): File =
        tempDir.resolve(relative).also {
            it.parentFile.mkdirs()
            it.writeText(content)
        }

    private fun catalogueWithGapEn(): String =
        """
        |TALARIA.I18N.CATALOG = {
        |    fr: {
        |      fpa: {
        |        title: "Formateur",
        |        modules: [
        |          { title: "M1", desc: "D1" }
        |        ]
        |      },
        |      cda: {
        |        title: "Concepteur"
        |      }
        |    },
        |    en: {
        |      fpa: {
        |        title: "Trainer"
        |      }
        |    }
        |  };
        """.trimMargin()

    private fun catalogueComplete(): String =
        """
        |TALARIA.I18N.CATALOG = {
        |    fr: {
        |      fpa: {
        |        title: "Formateur",
        |        modules: [
        |          { title: "M1", desc: "D1" }
        |        ]
        |      }
        |    },
        |    en: {
        |      fpa: {
        |        title: "Trainer",
        |        modules: [
        |          { title: "M1", desc: "D1" }
        |        ]
        |      }
        |    }
        |  };
        """.trimMargin()

    private fun chromeWithMissingEn(): String =
        """
        |  var DICT = {
        |    fr: {
        |      "nav.home": "Accueil",
        |      "nav.cart": "Panier"
        |    },
        |    en: {
        |      "nav.home": "Home"
        |    }
        |  };
        """.trimMargin()

    private fun setupTask(name: String): TranslateI18nClientTask {
        val project =
            ProjectBuilder
                .builder()
                .withProjectDir(tempDir)
                .withName(name)
                .build()
        project.pluginManager.apply("java-base")
        return project.tasks.register("translateI18nClient", TranslateI18nClientTask::class.java).get()
    }

    private inner class RecordingTranslationService : TranslationService {
        val requests = mutableListOf<TranslationRequest>()

        override fun translate(request: TranslationRequest): TranslationResult {
            requests.add(request)
            val translated =
                when (request.targetLanguage) {
                    "en" -> mapOf("Panier" to "Cart", "Accueil" to "Home")[request.sourceText]
                    "de" -> mapOf("Panier" to "Warenkorb", "Accueil" to "Startseite")[request.sourceText]
                    else -> null
                }
            return TranslationResult.Success(translated ?: "[${request.targetLanguage}]${request.sourceText}")
        }
    }

    private inner class FailingTranslationService : TranslationService {
        override fun translate(request: TranslationRequest): TranslationResult =
            TranslationResult.Failure("quota exceeded")
    }
}
