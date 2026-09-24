package bakery.langswitch

data class LangSwitchUrl(
    val targetLanguage: String,
    val currentLanguage: String,
    val currentPath: String,
    val defaultLanguage: String,
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
            "${root}$targetLanguage/index.html"
        }
    }

    /**
     * BKY-LANG-NAV-2 — the page-aware Thymeleaf expression for this link.
     *
     * Delegates to the single rule [LangSwitchPath.thymeleafHref] (D3) so the
     * injected `th:href` points at the *translation of the current page*, not at
     * the language root. [resolve] stays the page-blind degradation target.
     */
    fun thymeleafHref(): String = LangSwitchPath.thymeleafHref(currentLanguage, targetLanguage, defaultLanguage)
}
