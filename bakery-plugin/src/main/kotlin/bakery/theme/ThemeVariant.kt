package bakery.theme

/**
 * Variantes de theme predefinies dans le catalogue Bakery.
 *
 * Chaque variante fournit un preset de variables CSS (couleurs, polices, espacements)
 * adapte a un type de site specifique. L'utilisateur peut surcharger toute variable
 * via le DSL `bakery { theme { ... } }` ou via `site.yml`.
 *
 * BKY-IA-2 — Theme IA parametrique.
 */
enum class ThemeVariant(val label: String, val description: String) {
    MINIMAL("minimal", "Theme epuré, monochrome, pour Portfolio personnel ou CV"),
    MAGAZINE("magazine", "Theme editorial riche, typographie serif, pour blog magazine"),
    DOCUMENTATION("documentation", "Theme technique, navigation laterale, pour documentation"),
    PORTFOLIO("portfolio", "Theme visuel, grands espaces, pour portfolio creatif"),
    FORMATION("formation", "Theme pedagogique, structure claire, pour site de formation");

    companion object {
        /**
         * Resout une variante depuis une chaine, avec fallback sur BLOG (MINIMAL).
         * Insensible a la casse.
         */
        fun fromStringOrDefault(value: String?): ThemeVariant =
            if (value.isNullOrBlank()) MINIMAL
            else entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: MINIMAL
    }
}