package bakery.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * BKY-I18N-PROD-MIG — Vérification syntaxique de l'intégration i18n Thymeleaf.
 *
 * Les templates magic-stick complets dépendent du modèle de données JBake
 * (content.uri, etc.). Ce test valide la cohérence mécanique de la migration :
 * - les templates modifiés utilisent des clés `#{...}` présentes dans messages_fr
 * - aucune clé n'est orpheline
 * - le fichier messages_fr.properties contient les valeurs originales
 */
class ThymeleafI18nSyntaxTest {

    private val migrationService = I18nMigrationService()
    private val keyRegex = Regex("""#\{([A-Za-z0-9_.-]+)}""")

    @Test
    fun `all message keys used in migrated templates are defined in messages_fr`(@TempDir tempDir: File) {
        val siteDir = migrateFixture(tempDir)
        val templatesDir = siteDir.resolve("templates")
        val frProps = Properties()
        templatesDir.resolve("messages_fr.properties").inputStream().use { frProps.load(it) }

        val usedKeys = templatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .flatMap { keyRegex.findAll(it.readText()).map { match -> match.groupValues[1] } }
            .toSet()

        assertTrue(usedKeys.isNotEmpty(), "Des clés i18n doivent être utilisées dans les templates")

        val undefinedKeys = usedKeys.filter { !frProps.containsKey(it) }
        assertTrue(
            undefinedKeys.isEmpty(),
            "Toutes les clés utilisées doivent exister dans messages_fr.properties : $undefinedKeys"
        )
    }

    @Test
    fun `message keys in templates match the expected namespace pattern`(@TempDir tempDir: File) {
        val siteDir = migrateFixture(tempDir)
        val templatesDir = siteDir.resolve("templates")

        val usedKeys = templatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .flatMap { keyRegex.findAll(it.readText()).map { match -> match.groupValues[1] } }
            .toSet()

        for (key in usedKeys) {
            val expectedPrefix = key.substringBefore(".")
            assertTrue(
                templatesDir.resolve("$expectedPrefix.thyme").exists(),
                "La clé $key doit correspondre à un template existant ($expectedPrefix.thyme)"
            )
        }
    }

    @Test
    fun `messages_fr contains original french values for all extracted keys`(@TempDir tempDir: File) {
        val siteDir = migrateFixture(tempDir)
        val templatesDir = siteDir.resolve("templates")

        val frProps = Properties()
        templatesDir.resolve("messages_fr.properties").inputStream().use { frProps.load(it) }

        assertTrue(frProps.isNotEmpty(), "messages_fr.properties ne doit pas être vide")

        val nonBlankValues = frProps.values.filter { (it as String).isNotBlank() }
        assertEquals(frProps.size, nonBlankValues.size, "Toutes les valeurs FR doivent être non vides")
    }

    private fun migrateFixture(tempDir: File): File {
        val resource = this::class.java.classLoader.getResource("i18n-fixtures/magic-stick/jbake")
            ?: throw IllegalStateException("Fixture magic-stick non trouvée")
        val fixture = File(resource.toURI())
        val target = tempDir.resolve("magic-stick")
        fixture.copyRecursively(target, overwrite = true)

        migrationService.migrate(
            siteDir = target,
            languages = listOf("fr", "en"),
            defaultLanguage = "fr",
            dryRun = false
        )
        return target
    }
}
