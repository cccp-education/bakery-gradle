package bakery.i18n.js

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-6.
 *
 * Pure structural guard for the nested catalogue (`TALARIA.I18N.CATALOG`). It is
 * the nested counterpart of [I18nClientFormatGuard], which only knows the flat
 * dictionaries and explicitly excludes the catalogue.
 *
 * Three invariants are enforced:
 *
 * . **Balanced body** — the `TALARIA.I18N.CATALOG` body is balanced. An
 *    unbalanced catalogue would be silently skipped by the reader.
 * . **Structural completeness** — after the writer runs, the target language
 *    owns every reference literal path (cadrage talaria S-054: a language block
 *    must cover the reference formations and fields).
 * . **Round-trip** — re-running the writer on its own result must be a strict
 *    no-op (writer idempotence, Ink Economy Law).
 *
 * Domain-pure: no I/O, no Gradle, no LLM.
 */
object I18nCatalogFormatGuard {
    const val UNBALANCED_CATALOGUE_BLOCK = "unbalanced-catalogue-block"
    const val LANGUAGE_STRUCTURE_DRIFT = "language-structure-drift"
    const val ROUND_TRIP_DRIFT = "round-trip-drift"

    data class Violation(
        val kind: String,
        val detail: String,
    )

    data class Report(
        val violations: List<Violation>,
    ) {
        val isValid: Boolean get() = violations.isEmpty()
    }

    /**
     * Verifies that completing [language] against [referenceSource] produces a
     * structurally complete and idempotent catalogue. [rewriter] defaults to
     * [I18nCatalogWriter.complete] and can be overridden to prove the guard
     * detects a non-preserving writer.
     */
    fun verify(
        source: String,
        referenceSource: String,
        referenceLanguage: String,
        language: String,
        translations: Map<String, String>,
        rewriter: (String, String, String, String, Map<String, String>) -> String =
            I18nCatalogWriter::complete,
    ): Report {
        val violations = mutableListOf<Violation>()

        val isCatalogue = I18nJsFormat.of(source).isCatalogue
        if (isCatalogue && I18nCatalog.catalogueBodyRange(source) == null) {
            violations += Violation(UNBALANCED_CATALOGUE_BLOCK, language)
        }

        if (!isCatalogue || !I18nJsFormat.of(referenceSource).isCatalogue) {
            return Report(violations)
        }

        val completed = rewriter(source, referenceSource, referenceLanguage, language, translations)
        val referencePaths = I18nCatalogDocument.literals(referenceSource, referenceLanguage).map { it.path }.toSet()
        val completedPaths = I18nCatalogDocument.literals(completed, language).map { it.path }.toSet()
        val missing = referencePaths - completedPaths
        if (missing.isNotEmpty()) {
            violations += Violation(LANGUAGE_STRUCTURE_DRIFT, "$language:${missing.joinToString(",")}")
        }

        val twice = rewriter(completed, referenceSource, referenceLanguage, language, translations)
        if (twice != completed) {
            violations += Violation(ROUND_TRIP_DRIFT, language)
        }

        return Report(violations)
    }
}
