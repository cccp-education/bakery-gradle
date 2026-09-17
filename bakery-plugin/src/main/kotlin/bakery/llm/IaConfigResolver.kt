package bakery.llm

import contracts.i18n.OllamaConfig
import java.time.Duration

/**
 * T-I18N-BAKERY US-2 — resolves the effective LLM pool configuration of a
 * consumer run.
 *
 * The consumer site declares its Ollama pool in `site.yml` (`ollama:` section,
 * N0 contract [OllamaConfig]): model, port range and timeout — the rotating
 * endpoints live on `localhost`, no credential is needed by the caller (the
 * CI writes the Device Keys to `~/.ollama/id_ed25519`). The shared runner owns
 * the build, so the caller cannot mutate the `bakery { ia { … } }` DSL; this
 * resolver bridges the YAML section to an effective [IaConfig].
 *
 * Precedence: an **explicitly enabled** DSL config is never overridden (the
 * caller knows best); otherwise a present `ollama:` section activates the
 * rotating pool; an absent section leaves the DSL untouched (backward compat).
 *
 * Pure domain: no I/O, no Gradle, no LLM.
 */
object IaConfigResolver {
    /**
     * Returns the effective config, or [dsl] unchanged when neither the DSL is
     * enabled nor a pool section is declared.
     */
    fun resolve(
        dsl: IaConfig,
        ollama: OllamaConfig?,
    ): IaConfig {
        if (dsl.enabled) return dsl
        if (ollama == null) return dsl

        return IaConfig(
            baseUrl = ollama.baseUrls().first(),
            modelName = ollama.model,
            timeout = Duration.ofSeconds(ollama.timeoutSeconds),
            enabled = true,
            portRange = ollama.portRange,
        )
    }
}
