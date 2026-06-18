package contracts.i18n

data class I18nConfig(
    val activeLanguage: String = "fr",
    val supportedLanguages: List<String> = listOf("fr"),
    val fallbackLanguage: String = "en"
) {
    fun validate(): I18nValidationResult {
        if (activeLanguage !in supportedLanguages) {
            return I18nValidationResult.Invalid(
                "Active language '$activeLanguage' is not in supported languages: $supportedLanguages"
            )
        }
        if (fallbackLanguage !in supportedLanguages) {
            return I18nValidationResult.Invalid(
                "Fallback language '$fallbackLanguage' is not in supported languages: $supportedLanguages"
            )
        }
        return I18nValidationResult.Valid
    }
}

sealed class I18nValidationResult {
    data object Valid : I18nValidationResult()
    data class Invalid(val message: String) : I18nValidationResult()
}
