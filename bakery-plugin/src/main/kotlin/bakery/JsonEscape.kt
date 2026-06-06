package bakery

/**
 * CS-FIN-2 (CS-13) — Échappe un string JSON pour qu'il soit stockable
 * comme valeur dans un fichier Java `.properties` (UTF-8).
 *
 * Le format `.properties` interprète littéralement :
 * - `\` comme caractère d'échappement
 * - les sauts de ligne (`\n`, 0x0A) comme séparateurs de paires clé=valeur
 *
 * Un JSON sérialisé par Jackson peut contenir ces caractères (par
 * exemple dans le contenu d'une string JSON : `"ligne1\nligne2"` où
 * `\n` est la séquence d'échappement JSON représentant un saut de
 * ligne). Quand on injecte ce JSON comme valeur `.properties`, on
 * DOIT doubler les backslashes pour préserver le sens JSON après
 * parsing par `Properties.load()`.
 *
 * Caractères gérés :
 * - `\` → `\\` (backslash littéral)
 * - `\n` (0x0A) → `\n` (séquence properties, ne PAS confondre avec
 *   séparateur de lignes)
 * - `\r` (0x0D) → `\r` (séquence properties)
 * - `\t` (0x09) → `\t` (séquence properties)
 *
 * **ATTENTION : cette fonction n'est PAS idempotente.**
 * Un double appel corrompt le JSON (\\ → \\\\, puis \\\\\\\\). Appeler
 * UNE seule fois par valeur JSON. Si vous devez propager une valeur
 * déjà échappée, la repasser dans la fonction d'origine (Jackson)
 * avant de la ré-échapper.
 *
 * @param rawJson string JSON produit par Jackson (séquences d'échappement
 *                 JSON déjà en place : `\n`, `\"`, `\\`, etc.)
 * @return string prêt à être écrit comme valeur dans un fichier
 *         `.properties` (key=value)
 *
 * @see bakery.SiteTaskRegistrar.injectLensBudgetIntoJbakeProperties
 *      pour l'usage en contexte (LENS pipeline → jbake.properties)
 */
internal fun escapeJsonForJavaProperties(rawJson: String): String =
    rawJson
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
