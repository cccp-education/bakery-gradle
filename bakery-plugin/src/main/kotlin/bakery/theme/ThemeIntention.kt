package bakery.theme

/**
 * Intention de theme — modele domaine DDD pour BKY-IA-2.
 *
 * Encapsule la description en langage naturel d'un theme a generer,
 * guidee par le LLM pour selectionner une variante du catalogue et
 * surcharger les variables CSS.
 *
 * Pattern identique a [bakery.scaffold.ScaffoldIntention] :
 * - Modele immuable, valide a la construction
 * - Enum typee pour les choix contraints ([ThemeVariant])
 * - Methode [toPromptContext] pour guider le LLM
 *
 * Usage (DSL) :
 * ```
 * bakery {
 *     themeIntention {
 *         description = "Blog tech moderne avec couleurs froides"
 *         variant = "magazine"
 *     }
 * }
 * ```
 *
 * Usage (LLM) :
 * Le LLM recoit [toPromptContext] et retourne une ThemePreset
 * avec des surcharges personnalisees.
 */
data class ThemeIntention(
    val description: String,
    val variant: ThemeVariant = ThemeVariant.MINIMAL,
    val overrides: ThemeOverrides = ThemeOverrides()
) {
    init {
        require(description.isNotBlank()) { "La description est obligatoire pour le theme assiste par IA." }
    }

    /**
     * Genere un contexte lisible par le LLM a partir de l'intention.
     */
    fun toPromptContext(): String = buildString {
        appendLine("Description du theme : $description")
        appendLine("Variante de base : ${variant.label}")
        appendLine("Variantes disponibles : ${ThemeVariant.entries.joinToString(", ") { "${it.name} (${it.description})" }}")
        if (overrides.primaryColor != null) appendLine("Couleur primaire souhaitee : ${overrides.primaryColor}")
        if (overrides.secondaryColor != null) appendLine("Couleur secondaire souhaitee : ${overrides.secondaryColor}")
        if (overrides.fontFamily != null) appendLine("Police souhaitee : ${overrides.fontFamily}")
    }
}