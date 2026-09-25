package bakery.i18n

/**
 * BKY-LANG-NAV-8 — strips the generated language-switcher content of a reference
 * template before it is keyed for frozen-bundle materialisation.
 *
 * The `lang-switcher-container` div is written **in place** by
 * `injectLangSwitch`; its inner markup lists every language in its own name. It
 * is generated, not author prose, and `injectLangSwitch` re-injects it after
 * every materialisation. If the keyer sees those generated labels they become
 * new `menu.N` keys absent from the frozen bundle → all keys unresolved, the
 * task writes **nothing**, and the materialised variant goes stale (Ink Economy
 * Law broken — constat dogfooding S-238).
 *
 * The strip keeps the empty container so `injectLangSwitch` can refill it, and
 * is a strict no-op on a template without the container or with an already
 * empty one (idempotent).
 *
 * Domain-pure: string in, string out. No I/O, no LLM, no Gradle.
 */
object LangSwitcherContainerMask {
    private const val MARKER = "lang-switcher-container"

    /** Matches the container div and any nested divs up to the matching close. */
    private val CONTAINER_PATTERN =
        Regex(
            """(<div[^>]*lang-switcher-container[^>]*>)([\s\S]*?)(</div>)""",
            RegexOption.MULTILINE,
        )

    fun strip(template: String): String {
        if (!template.contains(MARKER)) return template
        return CONTAINER_PATTERN.replace(template) { match ->
            val openingTag = match.groupValues[1]
            val closingTag = match.groupValues[3]
            // A container with a non-blank, non-whitespace body is generated
            // content: keep the tags, drop the body (inject re-fills it). An
            // already empty container is left byte-identical (idempotent).
            val body = match.groupValues[2]
            if (body.isBlank()) match.value else "$openingTag$closingTag"
        }
    }
}
