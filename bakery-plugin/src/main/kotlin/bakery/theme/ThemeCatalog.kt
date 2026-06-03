package bakery.theme

/**
 * Catalogue de themes predefinis — mappe chaque [ThemeVariant] a un [ThemePreset].
 *
 * Le catalogue fournit les presets par defaut pour les 5 variantes supportees.
 * L'utilisateur selectionne une variante (via DSL, YAML ou LLM), puis le catalogue
 * fournit le preset de base. Les variables peuvent etre surchargees individuellement.
 *
 * Usage (DSL) :
 * ```
 * bakery {
 *     theme {
 *         variant = "magazine"
 *         primaryColor = "#c0392b"  // surcharge le preset
 *     }
 * }
 * ```
 *
 * Usage (LLM) :
 * ```
 * bakery {
 *     themeIntention {
 *         description = "Site de blog tech sur Kotlin"
 *         variant = "documentation"
 *     }
 * }
 * ```
 *
 * BKY-IA-2 — Theme IA parametrique.
 */
object ThemeCatalog {

    private val presets: Map<ThemeVariant, ThemePreset> = mapOf(
        ThemeVariant.MINIMAL to ThemePreset(
            variant = ThemeVariant.MINIMAL,
            primaryColor = "#2c3e50",
            secondaryColor = "#95a5a6",
            accentColor = "#34495e",
            backgroundColor = "#ffffff",
            textColor = "#333333",
            fontFamily = "system-ui, sans-serif",
            headingFont = "system-ui, sans-serif"
        ),
        ThemeVariant.MAGAZINE to ThemePreset(
            variant = ThemeVariant.MAGAZINE,
            primaryColor = "#e74c3c",
            secondaryColor = "#ecf0f1",
            accentColor = "#c0392b",
            backgroundColor = "#ffffff",
            textColor = "#2c3e50",
            fontFamily = "Georgia, serif",
            headingFont = "Playfair Display, serif"
        ),
        ThemeVariant.DOCUMENTATION to ThemePreset(
            variant = ThemeVariant.DOCUMENTATION,
            primaryColor = "#2980b9",
            secondaryColor = "#ecf0f1",
            accentColor = "#8e44ad",
            backgroundColor = "#fafafa",
            textColor = "#333333",
            fontFamily = "'Roboto Mono', monospace",
            headingFont = "'Inter', sans-serif"
        ),
        ThemeVariant.PORTFOLIO to ThemePreset(
            variant = ThemeVariant.PORTFOLIO,
            primaryColor = "#2ecc71",
            secondaryColor = "#f5f5f5",
            accentColor = "#27ae60",
            backgroundColor = "#1a1a2e",
            textColor = "#e0e0e0",
            fontFamily = "'Fira Sans', sans-serif",
            headingFont = "'Montserrat', sans-serif"
        ),
        ThemeVariant.FORMATION to ThemePreset(
            variant = ThemeVariant.FORMATION,
            primaryColor = "#0d6efd",
            secondaryColor = "#f8f9fa",
            accentColor = "#198754",
            backgroundColor = "#ffffff",
            textColor = "#212529",
            fontFamily = "'Inter', sans-serif",
            headingFont = "'Inter', sans-serif"
        )
    )

    /**
     * Retourne le preset pour une variante donnee.
     *
     * @throws IllegalArgumentException si la variante n'est pas dans le catalogue
     */
    fun presetFor(variant: ThemeVariant): ThemePreset =
        presets[variant]
            ?: throw IllegalArgumentException("No preset found for variant: $variant")

    /**
     * Retourne toutes les variantes disponibles dans le catalogue.
     */
    fun availableVariants(): List<ThemeVariant> = presets.keys.toList()

    /**
     * Resout le theme final en fusionnant le preset de la variante avec les surcharges utilisateur.
     *
     * Ordre de resolution (du plus prioritaire au moins) :
     * 1. Surcharge utilisateur (DSL ou YAML)
     * 2. Preset catalogue
     * 3. Valeurs par defaut du ThemeConfig (deja dans ThemeConfig)
     */
    fun resolve(
        variant: ThemeVariant,
        overrides: ThemeOverrides = ThemeOverrides()
    ): ThemePreset {
        val base = presetFor(variant)
        return base.copy(
            primaryColor = overrides.primaryColor ?: base.primaryColor,
            secondaryColor = overrides.secondaryColor ?: base.secondaryColor,
            accentColor = overrides.accentColor ?: base.accentColor,
            backgroundColor = overrides.backgroundColor ?: base.backgroundColor,
            textColor = overrides.textColor ?: base.textColor,
            fontFamily = overrides.fontFamily ?: base.fontFamily,
            headingFont = overrides.headingFont ?: base.headingFont,
            logoUrl = overrides.logoUrl ?: base.logoUrl,
            faviconUrl = overrides.faviconUrl ?: base.faviconUrl
        )
    }
}

/**
 * Surcharges optionnelles du theme — les variables que l'utilisateur peut surcharger.
 *
 * Les valeurs `null` signifient "utiliser la valeur du preset".
 * Pattern identique a [bakery.ConfigResolver] : chaque champ est nullable,
 * la resolution se fait dans l'ordre surcharge > preset > defaut.
 */
data class ThemeOverrides(
    val primaryColor: String? = null,
    val secondaryColor: String? = null,
    val accentColor: String? = null,
    val backgroundColor: String? = null,
    val textColor: String? = null,
    val fontFamily: String? = null,
    val headingFont: String? = null,
    val logoUrl: String? = null,
    val faviconUrl: String? = null
)