package bakery.langswitch

class LangSwitchThymeleafRenderer(
    private val languageLabels: Map<String, String>
) {
    fun render(links: List<LangSwitchUrl>): String {
        require(links.isNotEmpty()) { "links must not be empty" }
        val items = links.joinToString("\n") { link ->
            val activeClass = if (link.isSelfLink()) " active" else ""
            val href = link.resolve()
            val label = languageLabels[link.targetLanguage] ?: link.targetLanguage
            """            <li><a class="dropdown-item lang-option$activeClass" th:href="'$href'" data-lang="${link.targetLanguage}">$label</a></li>"""
        }
        return """            <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="langDropdown">
$items
            </ul>"""
    }
}