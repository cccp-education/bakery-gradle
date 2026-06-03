package bakery.theme

/**
 * Preset de theme — les variables CSS pre-configurees pour une [ThemeVariant].
 *
 * Chaque preset definit les couleurs, polices et espacements par defaut
 * de la variante. L'utilisateur peut surcharger toute variable via le DSL.
 *
 * BKY-IA-2 — Theme IA parametrique.
 */
data class ThemePreset(
    val variant: ThemeVariant,
    val primaryColor: String,
    val secondaryColor: String,
    val accentColor: String,
    val backgroundColor: String,
    val textColor: String,
    val fontFamily: String,
    val headingFont: String,
    val logoUrl: String = "",
    val faviconUrl: String = ""
)