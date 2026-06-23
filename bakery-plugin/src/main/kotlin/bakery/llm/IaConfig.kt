package bakery.llm

import java.time.Duration

/**
 * Configuration du service IA pour bakery.
 *
 * Point d'entrée DSL via [bakery.BakeryExtension.ia].
 *
 * ```
 * bakery { ia { baseUrl = "http://localhost:11464"; modelName = "gpt-oss:120b-cloud" } }
 * ```
 */
open class IaConfig(
    var baseUrl: String = "http://localhost:11464",
    var modelName: String = "gpt-oss:120b-cloud",
    var timeout: Duration = Duration.ofSeconds(120),
    var enabled: Boolean = false
)
