package bakery.langswitch

/**
 * BKY-LANG-NAV-3 — the client-host parity guard (decision D6).
 *
 * The same-page switcher has two hosts (D3): the Thymeleaf renderer resolved at
 * bake time (NAV-2) and the browser module `lang-switch.js` resolved at runtime
 * (NAV-3). The hybrid's structural failure mode is a split-brain between the
 * two copies of the JS module: `maquette/js/` is the development source and the
 * publication tree is byte-identical (decision S-039, pattern
 * [bakery.i18n.js.I18nClientPropagation]).
 *
 * Presence alone is a false green — a module that keeps `setLang` but drops the
 * shared resolver does not preserve the page. This object therefore checks three
 * things on the *same* byte content:
 *
 *   1. the module exists in both trees;
 *   2. the two copies are byte-identical (no drift);
 *   3. the contract surface is exposed: the pure resolver `resolveLangPath`
 *      (replaying the shared vectors) **and** the DOM adapter `attachLangSwitch`.
 *
 * Domain-pure: strings in, violations out. No I/O, no file system, no Gradle.
 */
object LangSwitchClientParity {
    /** File name of the client-side switching module, shared by both hosts. */
    const val MODULE_NAME = "lang-switch.js"

    /** The pure resolver entry point, sibling of [LangSwitchPath.resolveSamePage]. */
    const val RESOLVER_FUNCTION = "resolveLangPath"

    /** The DOM adapter entry point (reads `data-lang`, rewrites the link, navigates). */
    const val ADAPTER_FUNCTION = "attachLangSwitch"

    const val MISSING_MODULE = "missing-module"
    const val DRIFTED_MODULE = "drifted-module"
    const val MISSING_RESOLVER = "missing-resolver"
    const val MISSING_ADAPTER = "missing-adapter"

    data class Violation(
        val kind: String,
        val detail: String,
    )

    data class Report(
        val violations: List<Violation>,
    ) {
        val isSuccess: Boolean get() = violations.isEmpty()
    }

    fun verify(
        maquetteModule: String?,
        publicationModule: String?,
    ): Report {
        val violations = mutableListOf<Violation>()

        if (maquetteModule == null) {
            violations += Violation(MISSING_MODULE, "maquette/js/$MODULE_NAME")
        }
        if (publicationModule == null) {
            violations += Violation(MISSING_MODULE, "assets/js/$MODULE_NAME")
        }

        val reference = maquetteModule ?: publicationModule
        if (reference == null) return Report(violations)

        if (maquetteModule != null && publicationModule != null && maquetteModule != publicationModule) {
            violations += Violation(DRIFTED_MODULE, MODULE_NAME)
        }

        if (!exposes(reference, RESOLVER_FUNCTION)) {
            violations += Violation(MISSING_RESOLVER, RESOLVER_FUNCTION)
        }
        if (!exposes(reference, ADAPTER_FUNCTION)) {
            violations += Violation(MISSING_ADAPTER, ADAPTER_FUNCTION)
        }

        return Report(violations)
    }

    /**
     * A function is considered exposed when it is declared or exported. The
     * module is plain browser JS (UMD-style, no build step), so both `function
     * name(` and `name:`/`name =` export forms are accepted.
     */
    private fun exposes(
        module: String,
        function: String,
    ): Boolean = Regex("""\b${Regex.escape(function)}\s*[:(=]""").containsMatchIn(module)
}
