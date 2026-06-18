package contracts.i18n

object LanguageCatalog {

    val ALL: List<SupportedLanguage> = listOf(
        SupportedLanguage(code = "en", name = "English", nativeName = "English", localeTag = "en-US"),
        SupportedLanguage(code = "zh", name = "Chinese Mandarin", nativeName = "中文", localeTag = "zh-CN"),
        SupportedLanguage(code = "hi", name = "Hindi", nativeName = "हिन्दी", localeTag = "hi-IN"),
        SupportedLanguage(code = "es", name = "Spanish", nativeName = "Español", localeTag = "es-ES"),
        SupportedLanguage(code = "fr", name = "French", nativeName = "Français", localeTag = "fr-FR"),
        SupportedLanguage(code = "ar", name = "Arabic Standard", nativeName = "العربية", rtl = true, localeTag = "ar-SA"),
        SupportedLanguage(code = "bn", name = "Bengali", nativeName = "বাংলা", localeTag = "bn-BD"),
        SupportedLanguage(code = "pt", name = "Portuguese", nativeName = "Português", localeTag = "pt-BR"),
        SupportedLanguage(code = "ru", name = "Russian", nativeName = "Русский", localeTag = "ru-RU"),
        SupportedLanguage(code = "ur", name = "Urdu", nativeName = "اردو", rtl = true, localeTag = "ur-PK")
    )

    fun findByCode(code: String): SupportedLanguage? = ALL.find { it.code == code }

    fun supportedCodes(): Set<String> = ALL.map { it.code }.toSet()
}
