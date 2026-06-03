package bakery

/**
 * DSL configuration for Theme system — BKY-JB-6 + BKY-IA-2.
 *
 * Supporte 2 modes de configuration :
 * 1. **Manuel** : couleurs/polices explicites (mode existant JB-6)
 * 2. **Catalogue** : variante de preset + surcharges optionnelles (mode IA-2)
 *
 * Le mode catalogue est prioritaire : si `variant` est defini,
 * le preset correspondant est charge, puis surcharge par les proprietes explicites.
 *
 * Usage (mode catalogue) :
 * ```
 * bakery {
 *     theme {
 *         variant = "magazine"
 *         primaryColor = "#c0392b"  // surcharge le preset magazine
 *     }
 * }
 * ```
 *
 * Usage (mode manuel, existant) :
 * ```
 * bakery {
 *     theme {
 *         mode = "auto"
 *         primaryColor = "#0d6efd"
 *         fontFamily = "Inter"
 *     }
 * }
 * ```
 */
open class ThemeDsl(
    var mode: String = "auto",
    var primaryColor: String = "#0d6efd",
    var secondaryColor: String = "#6c757d",
    var fontFamily: String = "",
    var logoUrl: String = "",
    var faviconUrl: String = "",
    /** Variante de catalogue pour le theme IA — BKY-IA-2 */
    var variant: String = "",
    /** Couleur d'accent pour le theme — BKY-IA-2 */
    var accentColor: String = "",
    /** Couleur de fond pour le theme — BKY-IA-2 */
    var backgroundColor: String = "",
    /** Couleur de texte pour le theme — BKY-IA-2 */
    var textColor: String = "",
    /** Police des titres pour le theme — BKY-IA-2 */
    var headingFont: String = "",
)