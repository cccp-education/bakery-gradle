package bakery.i18n.js

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-6.
 *
 * Pure structural reader of the structured catalogue (`TALARIA.I18N.CATALOG`).
 *
 * The flat [I18nJsDictionary] exposes no key of the nested catalogue. This object
 * flattens one language's block into an ordered list of [Literal]s, each with a
 * stable dot/bracket [Literal.path] (`fpa.title`, `fpa.objectives[0]`,
 * `fpa.modules[0].desc`). The path is the unit of work of the delta (Ink Economy
 * Law): only literals the target language does not already own are sent to the
 * model.
 *
 * The byte positions of each literal's quoted content are carried by the
 * internal [LocatedLiteral] so the writer can splice translated values in place
 * without re-parsing. No I/O, no Gradle, no LLM.
 */
object I18nCatalogDocument {
    /** One quoted literal of a language: its [path] within the entry and its decoded [value]. */
    data class Literal(
        val path: String,
        val value: String,
    )

    /** A [literal] plus the byte range of its quoted content in the source. */
    internal data class LocatedLiteral(
        val literal: Literal,
        val contentStart: Int,
        val contentEnd: Int,
    )

    /** Ordered literals of [language], or empty when the block/source is absent. */
    fun literals(
        source: String,
        language: String,
    ): List<Literal> = locatedLiterals(source, language).map { it.literal }

    /**
     * Reference literals [language] does not already own, in reference document
     * order. A language without a block misses every reference literal; the
     * reference language misses nothing (Ink Economy Law).
     */
    fun missingLiterals(
        source: String,
        referenceLanguage: String,
        language: String,
    ): List<Literal> {
        if (language == referenceLanguage) return emptyList()
        val reference = literals(source, referenceLanguage)
        val owned = literals(source, language).map { it.path }.toSet()
        return reference.filterNot { it.path in owned }
    }

    internal fun locatedLiterals(
        source: String,
        language: String,
    ): List<LocatedLiteral> {
        val object_ = languageObject(source, language) ?: return emptyList()
        return locatedLiteralsIn(object_, "")
    }

    /** Ordered literals of the object opening at [brace] in [source]. */
    internal fun locatedLiteralsInBlock(
        source: String,
        brace: Int,
    ): List<LocatedLiteral> {
        val object_ = I18nJsObjectParser.parseObjectAt(source, brace) ?: return emptyList()
        return locatedLiteralsIn(object_, "")
    }

    /** Structural object of [language], or null when the block/source is absent. */
    internal fun languageObject(
        source: String,
        language: String,
    ): I18nJsObject? {
        val brace = I18nCatalog.languageBrace(source, language) ?: return null
        return I18nJsObjectParser.parseObjectAt(source, brace)
    }

    /** Object entry [key] of [parent], or null. */
    internal fun objectOf(
        parent: I18nJsObject,
        key: String,
    ): I18nJsObject? = parent.entries.firstOrNull { it.key == key }?.value as? I18nJsObject

    /** Every quoted literal of [node], prefixed by [prefix] (`""` for a language entry). */
    internal fun locatedLiteralsIn(
        node: I18nJsNode,
        prefix: String,
    ): List<LocatedLiteral> {
        val located = mutableListOf<LocatedLiteral>()
        flattenNode(node, prefix, located)
        return located
    }

    private fun flattenNode(
        node: I18nJsNode,
        path: String,
        located: MutableList<LocatedLiteral>,
    ) {
        when (node) {
            is I18nJsStringNode ->
                located +=
                    LocatedLiteral(
                        Literal(path, node.value),
                        node.contentStart,
                        node.contentEnd,
                    )
            is I18nJsObject ->
                node.entries.forEach { entry ->
                    val child = if (path.isEmpty()) entry.key else "$path.${entry.key}"
                    flattenNode(entry.value, child, located)
                }
            is I18nJsArray ->
                node.items.forEachIndexed { index, item ->
                    flattenNode(item, "$path[$index]", located)
                }
        }
    }
}
