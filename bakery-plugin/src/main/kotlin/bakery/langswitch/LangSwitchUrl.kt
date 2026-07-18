package bakery.langswitch

data class LangSwitchUrl(
    val targetLanguage: String,
    val currentLanguage: String,
    val currentPath: String,
    val defaultLanguage: String
) {
    init {
        require(targetLanguage.isNotBlank()) { "targetLanguage must not be blank" }
        require(currentLanguage.isNotBlank()) { "currentLanguage must not be blank" }
        require(defaultLanguage.isNotBlank()) { "defaultLanguage must not be blank" }
    }

    fun isSelfLink(): Boolean = targetLanguage == currentLanguage

    fun rootpath(): String {
        if (currentPath.isBlank()) return ""
        val segments = currentPath.trim('/').split('/').filter { it.isNotEmpty() }
        return if (segments.isEmpty()) "" else "../".repeat(segments.size)
    }

    fun resolve(): String {
        if (isSelfLink()) return "index.html"
        val root = rootpath()
        return if (targetLanguage == defaultLanguage) {
            "${root}index.html"
        } else {
            "${root}${targetLanguage}/index.html"
        }
    }
}