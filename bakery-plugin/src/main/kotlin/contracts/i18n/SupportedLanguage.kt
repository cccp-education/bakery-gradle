package contracts.i18n

data class SupportedLanguage(
    val code: String,
    val name: String,
    val nativeName: String,
    val rtl: Boolean = false,
    val localeTag: String = code
)
