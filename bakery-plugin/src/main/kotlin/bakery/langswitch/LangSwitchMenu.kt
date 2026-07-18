package bakery.langswitch

class LangSwitchMenu(
    val supportedLanguages: List<String>,
    val defaultLanguage: String,
    val currentLanguage: String,
    val currentPath: String
) {
    init {
        require(supportedLanguages.isNotEmpty()) { "supportedLanguages must not be empty" }
        require(defaultLanguage in supportedLanguages) {
            "defaultLanguage '$defaultLanguage' is not in supportedLanguages: $supportedLanguages"
        }
        require(currentLanguage in supportedLanguages) {
            "currentLanguage '$currentLanguage' is not in supportedLanguages: $supportedLanguages"
        }
    }

    fun generateLinks(): List<LangSwitchUrl> =
        supportedLanguages.map { lang ->
            LangSwitchUrl(
                targetLanguage = lang,
                currentLanguage = currentLanguage,
                currentPath = currentPath,
                defaultLanguage = defaultLanguage
            )
        }
}