package bakery.llm

import java.time.Duration

/**
 * Configuration du service IA pour bakery.
 *
 * Point d'entrée DSL via [bakery.BakeryExtension.ia].
 *
 * Single endpoint:
 * ```
 * bakery { ia { baseUrl = "http://localhost:11464"; modelName = "gpt-oss:120b-cloud" } }
 * ```
 *
 * Port range rotation (via api-key-pool round-robin):
 * ```
 * bakery { ia { portRange = 11437..11465; modelName = "gemma4:31b-cloud" } }
 * ```
 *
 * Dedicated diagram model (branche B, `[plantuml]` leaf — prose and diagrams
 * routed to two models, same endpoint):
 * ```
 * bakery { ia { modelName = "nemotron-3-super:cloud"; plantumlModelName = "deepseek-v4-flash:cloud" } }
 * ```
 */
open class IaConfig(
    var baseUrl: String = "http://localhost:11464",
    var modelName: String = "gpt-oss:120b-cloud",
    var timeout: Duration = Duration.ofSeconds(120),
    var enabled: Boolean = false,
    var portRange: IntRange? = null,
    /**
     * CHE-DIAGRAM D2 / GBL-010 branche B — dedicated model for the PlantUML
     * `[plantuml]` leaf translation (the diagram's *labels*), distinct from the
     * prose [modelName]. `null` (default) means "same model as the content" —
     * strict backward compatibility. When set, `migrateContentI18n` injects a
     * second [contracts.i18n.TranslationService] into the plantuml port so the
     * prose and the diagrams can be routed to different models (e.g. prose
     * `nemotron-3-super:cloud`, diagrams `deepseek-v4-flash:cloud`).
     */
    var plantumlModelName: String? = null,
)
