package bakery.deploy

/**
 * A deployment destination.
 *
 * EPIC BKY-NETLIFY (D1/D3) — the historical pipeline only knew a forced Git
 * push; making the destination explicit lets a second target consume the same
 * payload without changing the first one.
 */
sealed interface DeployTarget {
    val id: String

    data object GitPages : DeployTarget {
        override val id: String = "git-pages"
    }

    data object Netlify : DeployTarget {
        override val id: String = "netlify"
    }
}

/**
 * Result of a deployment attempt.
 *
 * `Skipped` is a *success*: the payload digest already matched the last
 * successful deployment (Ink Economy, D7). `Failed` carries a message that is
 * safe to log (never a credential).
 */
sealed interface DeployOutcome {
    val target: DeployTarget

    data class Deployed(
        override val target: DeployTarget,
        val url: String,
        val deployId: String? = null,
    ) : DeployOutcome

    data class Skipped(
        override val target: DeployTarget,
        val reason: String,
    ) : DeployOutcome

    data class Failed(
        override val target: DeployTarget,
        val message: String,
    ) : DeployOutcome
}
