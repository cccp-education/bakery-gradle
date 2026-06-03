package bakery.theme

/**
 * DSL bridge pour [ThemeIntention] — passage Gradle → domaine.
 *
 * Pattern identique a [bakery.scaffold.ScaffoldIntentionDsl] :
 * - Open class avec var mutables pour le DSL Gradle
 * - Methode [toIntention()] pour convertir en modele domaine valide
 * - Validateurs dans le modele, pas dans la DSL
 *
 * BKY-IA-2 — Theme IA parametrique.
 */
open class ThemeIntentionDsl {
    var description: String = ""
    var variant: String = "minimal"
    var primaryColor: String? = null
    var secondaryColor: String? = null
    var accentColor: String? = null
    var backgroundColor: String? = null
    var textColor: String? = null
    var fontFamily: String? = null
    var headingFont: String? = null
    var logoUrl: String? = null
    var faviconUrl: String? = null

    /**
     * Convertit la DSL en modele domaine valide.
     *
     * @throws IllegalArgumentException si la description est vide
     */
    fun toIntention(): ThemeIntention = ThemeIntention(
        description = description,
        variant = ThemeVariant.fromStringOrDefault(variant),
        overrides = ThemeOverrides(
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            accentColor = accentColor,
            backgroundColor = backgroundColor,
            textColor = textColor,
            fontFamily = fontFamily,
            headingFont = headingFont,
            logoUrl = logoUrl,
            faviconUrl = faviconUrl
        )
    )
}