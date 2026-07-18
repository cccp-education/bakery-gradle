package bakery.i18n.plantuml

/**
 * EPIC BKY-I18N-DEPLOY — US DEPLOY-1.
 *
 * Stratégie de traduction d'un bloc `[plantuml]` extrait d'un article AsciiDoc.
 * Inspiré de HTOWN EPIC PLT-BOUNDARY (S163-166) — TranslationStrategy.TRANSLATE/BORROW/PRESERVE
 * adapté au contexte bakery.
 *
 * 3 variantes :
 * - [TranslateLabels] : le bloc contient des labels traduisibles (acteurs, classes, usecases)
 *   → traduire les labels via TranslationService.
 * - [PreserveTechnical] : le bloc est purement technique (syntaxe PlantUML sans labels humains)
 *   → retourner le bloc inchangé.
 * - [BorrowVocabulary] : le bloc contient du vocabulaire métier REAC/AFNOR/DC/TS
 *   → traduire + préserver le vocabulaire métier dans une registry.
 */
sealed interface PlantUmlStrategy {
    data object TranslateLabels : PlantUmlStrategy
    data object PreserveTechnical : PlantUmlStrategy
    data object BorrowVocabulary : PlantUmlStrategy
}

/**
 * Représente un bloc `[plantuml]` extrait d'un article AsciiDoc.
 *
 * Domaine pur — pas de logique I/O, pas d'appel LLM. La méthode [labels] extrait
 * les labels traduisibles (entre guillemets), en skippant les identifiants
 * techniques (noms de classes Java qualifiés, packages).
 */
data class PlantUmlBlock(
    val raw: String
) {
    private val labelRegex = Regex("\"([^\"]+)\"")

    private val technicalIdentifierRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*\\.[a-zA-Z0-9_.]+$")

    private val borrowedVocabulary = setOf("REAC", "AFNOR", "DC", "TS", "RNCP", "CP", "ECF")

    fun labels(): List<String> {
        val matches = labelRegex.findAll(raw).map { it.groupValues[1] }.toList()
        return matches.filter { label ->
            !technicalIdentifierRegex.matches(label) &&
                label.any { it.isLetter() }
        }
    }

    internal fun hasBorrowedVocabulary(): Boolean {
        return borrowedVocabulary.any { vocab -> raw.contains("\"$vocab\"") }
    }

    internal fun hasTranslatableLabels(): Boolean = labels().isNotEmpty()
}

/**
 * Classifie un bloc `[plantuml]` en stratégie de traduction.
 *
 * Domaine pur — pas d'I/O, pas d'appel LLM. L'ordre de priorité est :
 * 1. BorrowVocabulary si le bloc contient du vocabulaire métier (REAC, AFNOR, DC, TS, RNCP, CP, ECF)
 * 2. TranslateLabels si le bloc contient au moins un label traduisible
 * 3. PreserveTechnical sinon (bloc purement technique)
 */
class PlantUmlClassifier {

    fun classify(block: PlantUmlBlock): PlantUmlStrategy {
        return when {
            block.hasBorrowedVocabulary() -> PlantUmlStrategy.BorrowVocabulary
            block.hasTranslatableLabels() -> PlantUmlStrategy.TranslateLabels
            else -> PlantUmlStrategy.PreserveTechnical
        }
    }
}