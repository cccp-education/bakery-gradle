package bakery

import arrow.core.Either

/**
 * One configuration domain resolver: a pure computation, its fallback value and
 * the name used in diagnostics.
 *
 * XB-FP-3 (arrow-kt): [resolveOutcome] returns an immutable [Outcome] — value plus
 * the optional failure — instead of mutating a shared `MutableList`. The legacy
 * [resolve] keeps the historical signature for callers/tests and delegates to it.
 */
data class DomainResolver<T>(
    val name: String,
    val resolver: () -> T,
    val fallback: T,
) {
    /** Immutable resolution outcome: the resolved value and the failure, if any. */
    data class Outcome<T>(
        val value: T,
        val error: ConfigResolutionError? = null,
    )

    /**
     * Resolves the domain without any shared mutable state. A thrown exception is
     * captured as a value (`Either.catch`) and converted into a
     * [ConfigResolutionError.DomainFailure]; the fallback is then used.
     */
    fun resolveOutcome(): Outcome<T> =
        Either.catch(resolver).fold(
            ifLeft = { throwable ->
                Outcome(
                    value = fallback,
                    error =
                        ConfigResolutionError.DomainFailure(
                            domain = name,
                            message = throwable.message ?: "Unknown error",
                            cause = throwable as? Exception,
                        ),
                )
            },
            ifRight = { resolved -> Outcome(resolved) },
        )

    /**
     * Historical signature retained for external callers and existing tests.
     * Kept thin: it adds the outcome error to the caller-provided list.
     */
    @Deprecated(
        "Use resolveOutcome() — the mutable error list is shared-state (XB-FP-3).",
        ReplaceWith("resolveOutcome()"),
    )
    fun resolve(errors: MutableList<ConfigResolutionError>): T {
        val outcome = resolveOutcome()
        outcome.error?.let(errors::add)
        return outcome.value
    }
}
