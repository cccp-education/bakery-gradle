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
 */
open class IaConfig(
    var baseUrl: String = "http://localhost:11464",
    var modelName: String = "gpt-oss:120b-cloud",
    var timeout: Duration = Duration.ofSeconds(120),
    var enabled: Boolean = false,
    var portRange: IntRange? = null
)
