package bakery

import arrow.core.Either

data class DomainResolver<T>(
    val name: String,
    val resolver: () -> T,
    val fallback: T,
) {
    fun resolve(errors: MutableList<ConfigResolutionError>): T =
        Either.catch(resolver).fold(
            ifLeft = { e ->
                errors.add(
                    ConfigResolutionError.DomainFailure(
                        domain = name,
                        message = e.message ?: "Unknown error",
                        cause = e as? Exception,
                    )
                )
                fallback
            },
            ifRight = { it },
        )
}