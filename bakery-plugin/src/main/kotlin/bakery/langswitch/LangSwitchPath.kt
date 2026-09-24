package bakery.langswitch

/**
 * BKY-LANG-NAV-1 — the single pure rule of same-page language switching.
 *
 * Given the page the visitor currently stands on, this resolves the relative
 * URL to the *translation of that same page* in the target language. It is the
 * one source of truth shared by both hosts (the Thymeleaf renderer at build
 * time and the JS module at runtime) — the anti split-brain rule of D3.
 *
 * Site tree model:
 *   - the default language lives at the site root (`blog/foo.html`);
 *   - every other language lives under its own directory (`en/blog/foo.html`).
 *
 * The returned value is a relative URL expressed from the current page's
 * directory, so it works unchanged inside a rendered document. Pure domain:
 * no I/O, no Gradle, no template engine.
 */
object LangSwitchPath {
    /**
     * The *symbolic* sibling of [resolveSamePage] — the Thymeleaf host of the
     * same rule (BKY-LANG-NAV-2, D4).
     *
     * `injectLangSwitch` injects a fragment into `menu.thyme`, a template shared
     * by every page of a language: no single "current page" exists at injection
     * time. The link must therefore be a Thymeleaf expression evaluated *per
     * page* at bake time, in terms of `${content.rootpath}` (ascends to the
     * language tree root) and `${content.uri}` (the page path within that tree).
     *
     * The output equals [resolveSamePage] once the engine substitutes a page —
     * the anti split-brain proof is `LangSwitchPathThymeleafHrefTest`.
     */
    fun thymeleafHref(
        currentLang: String,
        targetLang: String,
        defaultLang: String,
    ): String {
        require(currentLang.isNotBlank()) { "currentLang must not be blank" }
        require(targetLang.isNotBlank()) { "targetLang must not be blank" }
        require(defaultLang.isNotBlank()) { "defaultLang must not be blank" }

        if (targetLang == currentLang) {
            return "\${content.uri.substring(content.uri.lastIndexOf('/') + 1)}"
        }

        if (currentLang == defaultLang) {
            val target = if (targetLang == defaultLang) "" else "$targetLang/"
            return "\${content.rootpath + '$target' + content.uri}"
        }

        val target = if (targetLang == defaultLang) "" else "$targetLang/"
        return "|../\${content.rootpath}${target}\${content.uri}|"
    }

    /**
     * BKY-LANG-NAV-4 — the *dynamic* (`th:each`) projection of the same rule.
     *
     * The ex-nihilo model menus (`site-basic/templates/menu.thyme`,
     * `site/templates/menu.thyme`) build the selector with a **single `th:each`
     * over `${supportedLanguages}`**, shared by every page of every language
     * tree. There is no per-link injection like [thymeleafHref] (NAV-2): one
     * expression is evaluated once per iteration, so the target language must be
     * read from the loop variable (`lang.code`) and the current language from
     * `config.site_language` at bake time.
     *
     * The evaluation equals [resolveSamePage] for every shared vector once the
     * engine substitutes a page (per-page branch on `lang.code` /
     * `config.site_language`) — the anti split-brain proof is
     * `LangSwitchPathEachHrefTest`, replaying the same fixture as the Kotlin pure
     * rule (NAV-1) and the JS host (NAV-3).
     */
    fun thymeleafEachHref(defaultLang: String): String {
        require(defaultLang.isNotBlank()) { "defaultLang must not be blank" }

        val self = "content.uri.substring(content.uri.lastIndexOf('/') + 1)"
        val downToNonDefault = "content.rootpath + lang.code + '/' + content.uri"
        val upToDefault = "'../' + content.rootpath + content.uri"
        val upToNonDefault = "'../' + content.rootpath + lang.code + '/' + content.uri"

        return "\${lang.code == config.site_language ? $self : " +
            "(config.site_language == '$defaultLang' ? $downToNonDefault : " +
            "(lang.code == '$defaultLang' ? $upToDefault : $upToNonDefault))}"
    }

    fun resolveSamePage(
        currentPageUri: String,
        currentLang: String,
        targetLang: String,
        defaultLang: String,
        pageExists: (String) -> Boolean = { true },
    ): String {
        require(currentPageUri.isNotBlank()) { "currentPageUri must not be blank" }
        require(currentLang.isNotBlank()) { "currentLang must not be blank" }
        require(targetLang.isNotBlank()) { "targetLang must not be blank" }
        require(defaultLang.isNotBlank()) { "defaultLang must not be blank" }

        val uri = normalise(currentPageUri)

        if (targetLang == currentLang) {
            return uri.substringAfterLast('/')
        }

        val pageWithinLang = stripLanguagePrefix(uri, currentLang, defaultLang)
        val targetAbsolute = absolutePath(pageWithinLang, targetLang, defaultLang, pageExists)
        val currentDir = uri.substringBeforeLast('/', missingDelimiterValue = "")
        return relativise(fromDir = currentDir, toPath = targetAbsolute)
    }

    private fun normalise(uri: String): String = uri.trim().trimStart('/')

    private fun stripLanguagePrefix(
        uri: String,
        currentLang: String,
        defaultLang: String,
    ): String {
        if (currentLang == defaultLang) return uri
        val prefix = "$currentLang/"
        return if (uri.startsWith(prefix)) uri.removePrefix(prefix) else uri
    }

    private fun absolutePath(
        pageWithinLang: String,
        targetLang: String,
        defaultLang: String,
        pageExists: (String) -> Boolean,
    ): String {
        val candidate = if (targetLang == defaultLang) pageWithinLang else "$targetLang/$pageWithinLang"
        if (pageExists(candidate)) return candidate
        return if (targetLang == defaultLang) "index.html" else "$targetLang/index.html"
    }

    private fun relativise(
        fromDir: String,
        toPath: String,
    ): String {
        val dirSegments = fromDir.split('/').filter { it.isNotEmpty() }
        val targetSegments = toPath.split('/').filter { it.isNotEmpty() }

        var common = 0
        while (common < dirSegments.size &&
            common < targetSegments.size &&
            dirSegments[common] == targetSegments[common]
        ) {
            common++
        }

        val ups = "../".repeat(dirSegments.size - common)
        val downs = targetSegments.drop(common).joinToString("/")
        return ups + downs
    }
}
