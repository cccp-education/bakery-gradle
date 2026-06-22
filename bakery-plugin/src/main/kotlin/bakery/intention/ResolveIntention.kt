package bakery.intention

import arrow.core.Either
import arrow.core.left
import arrow.core.right

sealed class ResolveIntentionError {
    abstract val cliFlag: String
    abstract val dslPath: String

    abstract fun toException(): IllegalArgumentException

    data class MissingRequiredField(
        override val cliFlag: String,
        override val dslPath: String,
    ) : ResolveIntentionError() {
        override fun toException(): IllegalArgumentException = IllegalArgumentException(
            "Aucune valeur spécifiée. Utilisez $cliFlag=\"...\" ou configurez $dslPath",
        )
    }
}

object ResolveIntention {

    fun fromCli(cli: String?, dsl: String?, default: String): String =
        cli.firstNotBlank() ?: dsl.firstNotBlank() ?: default

    fun fromCliNullable(cli: String?, dsl: String?): String? =
        cli.firstNotBlank() ?: dsl.firstNotBlank()

    fun fromCliRequired(
        cli: String?,
        dsl: String?,
        error: ResolveIntentionError,
    ): Either<ResolveIntentionError, String> {
        val resolved = cli.firstNotBlank() ?: dsl.firstNotBlank()
        return resolved?.right() ?: error.left()
    }

    fun fromCliList(cli: String?, dsl: List<String>?, default: List<String>): List<String> {
        val parsed = cli?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
        return parsed ?: dsl ?: default
    }

    fun fromCliBoolean(cli: String?, dsl: Boolean?, default: Boolean): Boolean =
        cli?.takeIf { it.isNotBlank() }?.toBooleanStrictOrNull() ?: dsl ?: default

    private fun String?.firstNotBlank(): String? =
        this?.takeIf { it.isNotBlank() }
}