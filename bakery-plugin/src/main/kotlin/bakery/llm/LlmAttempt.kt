package bakery.llm

/**
 * XF-FP2-3 — one endpoint attempt is a value (success or typed failure), never a
 * mutable accumulator.
 *
 * Internal on purpose: the public surface stays [LlmService]; Arrow types never
 * leak into a public signature.
 */
internal sealed interface LlmAttempt {
    data class Success(
        val text: String,
    ) : LlmAttempt

    data class Failure(
        val endpoint: String,
        val cause: String,
    ) : LlmAttempt
}

/**
 * Every pool endpoint failed: [failures] keeps one value per attempted endpoint,
 * [attempts] is the number of attempts actually made.
 */
internal data class LlmExhausted(
    val failures: List<LlmAttempt.Failure>,
    val attempts: Int,
)
