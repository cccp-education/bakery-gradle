package bakery.langswitch

class LangSwitchInjector {

    fun inject(menuThyme: String, renderedFragment: String): String {
        val containerRegex = Regex(
            "(<div[^>]*lang-switcher-container[^>]*>)(\\s*\\n?)([\\s\\S]*?)(</div>)",
            RegexOption.MULTILINE
        )
        val match = containerRegex.find(menuThyme) ?: return menuThyme
        val openingTag = match.groupValues[1]
        val closingTag = match.groupValues[4]
        val replacement = "$openingTag\n$renderedFragment\n                $closingTag"
        return menuThyme.replace(match.value, replacement)
    }
}