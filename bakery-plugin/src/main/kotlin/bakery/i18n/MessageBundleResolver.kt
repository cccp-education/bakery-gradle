package bakery.i18n

/**
 * BKY-LANG-NAV-8 — resolves a Thymeleaf template written with message keys
 * (`#{key}`) into a *full-swap* literal template, from a frozen bundle.
 *
 * `jbake-core:2.7.0` installs **no MessageResolver**: the `#{key}` bundles
 * `migrateToI18n` produces are never resolved at bake time — which is why
 * `translateTemplates` uses a full-template swap. A site that already owns a
 * frozen `messages_{lang}.properties` (the i18n golden masters) can therefore
 * materialise its translated templates **without any LLM call**: the values were
 * computed once and are reused (Ink Economy Law).
 *
 * The resolution is a literal substitution of `#{key}` by its bundle value,
 * leaving every tag, attribute and Thymeleaf expression intact. An unknown key
 * is preserved verbatim and reported — never a corrupted template.
 *
 * Domain-pure: strings in, strings out. No I/O, no Gradle, no LLM.
 */
object MessageBundleResolver {
    data class Result(
        val content: String,
        val unresolvedKeys: List<String>,
    )

    private val KEY_PATTERN = Regex("""#\{([^}]+)}""")

    fun resolve(
        template: String,
        bundle: Map<String, String>,
    ): Result {
        val unresolved = linkedSetOf<String>()
        val resolved =
            KEY_PATTERN.replace(template) { match ->
                val key = match.groupValues[1]
                val value = bundle[key]
                if (value == null) {
                    unresolved += key
                    match.value
                } else {
                    value
                }
            }
        return Result(resolved, unresolved.toList())
    }
}
