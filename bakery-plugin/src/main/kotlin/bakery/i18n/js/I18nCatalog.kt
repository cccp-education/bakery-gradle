package bakery.i18n.js

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-5.
 *
 * One target language's coverage gap in the structured catalogue
 * (`TALARIA.I18N.CATALOG`). [missingFormations] maps a language to the reference
 * formations it owns no block for; [missingFields] nests language → formation →
 * fields the formation owns no entry for.
 */
data class I18nCatalogPlan(
    val languages: List<String>,
    val missingLanguages: List<String>,
    val missingFormations: Map<String, List<String>>,
    val missingFields: Map<String, Map<String, List<String>>>,
) {
    val missingLanguageCount: Int get() = missingLanguages.size

    val missingFormationCount: Int get() = missingFormations.values.sumOf { it.size }

    val missingFieldCount: Int get() = missingFields.values.sumOf { formations -> formations.values.sumOf { it.size } }

    val hasGap: Boolean
        get() = missingLanguages.isNotEmpty() || missingFormations.isNotEmpty() || missingFields.isNotEmpty()

    companion object {
        val EMPTY = I18nCatalogPlan(emptyList(), emptyList(), emptyMap(), emptyMap())
    }
}

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-5.
 *
 * Pure coverage planner for the structured catalogue dictionary
 * (`maquette/js/i18n-content.js`, `TALARIA.I18N.CATALOG`). Its values are nested
 * — languages → formations (`fpa`/`cda`) → scalar fields and lists — so the flat
 * [I18nJsDictionary] parser sees the language blocks but none of the fields.
 *
 * Contract (cadrage talaria S-054): a missing language block is a missing
 * language; inside an existing block every formation and field of the reference
 * language must be present. Coverage only — the catalogue translation itself is
 * a separate concern. No I/O, no Gradle, no LLM.
 */
object I18nCatalog {
    private val MARKER = Regex("TALARIA\\s*\\.\\s*I18N\\s*\\.\\s*CATALOG\\s*=\\s*\\{")

    /**
     * Extracts the `code: { ... }` blocks at [indent] spaces, in document order,
     * pairing each name with its balanced body. The scanner is string-aware, so
     * braces inside a quoted value never truncate a block.
     */
    private fun blocks(
        source: String,
        indent: Int,
    ): List<Pair<String, String>> {
        val regex = Regex("^\\s{$indent}([a-zA-Z][a-zA-Z0-9_]*):\\s*\\{", RegexOption.MULTILINE)
        return regex.findAll(source).mapNotNull { match ->
            I18nJsBlockScanner.bodyFrom(source, match.range.first)?.let { match.groupValues[1] to it }
        }.toList()
    }

    /** The body of the top-level `TALARIA.I18N.CATALOG = { ... }` object, or null. */
    private fun catalogueBody(source: String): String? {
        val marker = MARKER.find(source) ?: return null
        return I18nJsBlockScanner.bodyFrom(source, marker.range.first)
    }

    /** Language codes owned by the catalogue, in document order. */
    fun languagesOf(source: String): List<String> {
        val body = catalogueBody(source) ?: return emptyList()
        return blocks(body, LANGUAGE_INDENT).map { it.first }
    }

    /** Formation names owned by [language], in document order. */
    fun formationsOf(
        source: String,
        language: String,
    ): List<String> {
        val languageBody = languageBody(source, language) ?: return emptyList()
        return blocks(languageBody, FORMATION_INDENT).map { it.first }
    }

    /** Top-level field keys of a formation (scalars and lists), in document order. */
    fun fieldsOf(
        source: String,
        language: String,
        formation: String,
    ): List<String> {
        val languageBody = languageBody(source, language) ?: return emptyList()
        val formationBody = blocks(languageBody, FORMATION_INDENT).firstOrNull { it.first == formation }?.second
            ?: return emptyList()
        return Regex("^\\s{$FIELD_INDENT}([a-zA-Z][a-zA-Z0-9_]*):", RegexOption.MULTILINE)
            .findAll(formationBody)
            .map { it.groupValues[1] }
            .toList()
    }

    /**
     * Builds the catalogue coverage plan for [targetLanguages]. A target with no
     * block is reported in [I18nCatalogPlan.missingLanguages]; a target whose
     * block exists is reported in [I18nCatalogPlan.languages] and, unless it is
     * the reference, its formation/field gaps are computed. When the reference
     * owns no catalogue block the plan is [I18nCatalogPlan.EMPTY] — a flat
     * dictionary has no catalogue to reconcile.
     */
    fun plan(
        source: String,
        referenceLanguage: String,
        targetLanguages: List<String>,
    ): I18nCatalogPlan {
        val ownedLanguages = languagesOf(source)
        if (referenceLanguage !in ownedLanguages) return I18nCatalogPlan.EMPTY

        val referenceFormations = formationsOf(source, referenceLanguage)
        val languages = mutableListOf<String>()
        val missingLanguages = mutableListOf<String>()
        val missingFormations = linkedMapOf<String, List<String>>()
        val missingFields = linkedMapOf<String, Map<String, List<String>>>()

        for (target in targetLanguages) {
            if (target !in ownedLanguages) {
                missingLanguages += target
                continue
            }
            languages += target
            if (target == referenceLanguage) continue

            val targetFormations = formationsOf(source, target).toSet()
            val absentFormations = referenceFormations.filterNot { it in targetFormations }
            if (absentFormations.isNotEmpty()) missingFormations[target] = absentFormations

            val fieldGaps = linkedMapOf<String, List<String>>()
            for (formation in referenceFormations.filter { it in targetFormations }) {
                val targetFields = fieldsOf(source, target, formation).toSet()
                val absentFields = fieldsOf(source, referenceLanguage, formation).filterNot { it in targetFields }
                if (absentFields.isNotEmpty()) fieldGaps[formation] = absentFields
            }
            if (fieldGaps.isNotEmpty()) missingFields[target] = fieldGaps
        }

        return I18nCatalogPlan(languages, missingLanguages, missingFormations, missingFields)
    }

    private fun languageBody(
        source: String,
        language: String,
    ): String? {
        val body = catalogueBody(source) ?: return null
        return blocks(body, LANGUAGE_INDENT).firstOrNull { it.first == language }?.second
    }

    private const val LANGUAGE_INDENT = 4
    private const val FORMATION_INDENT = 6
    private const val FIELD_INDENT = 8
}
