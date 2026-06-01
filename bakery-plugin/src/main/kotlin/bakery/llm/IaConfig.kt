package bakery.llm

import java.time.Duration

/**
 * Configuration du service IA pour bakery.
 *
 * Point d'entrée DSL via [bakery.BakeryExtension.ia].
 *
 * Mode simple (single instance) :
 * ```
 * bakery { ia { baseUrl = "http://localhost:11464"; modelName = "deepseek-v4-pro" } }
 * ```
 *
 * Mode pool (multi-instances Docker, ports 11437-11465, modèle gpt-oss:120b-cloud) :
 * ```
 * bakery { ia { poolPorts = "11437,11438,11439"; poolModel = "gpt-oss:120b-cloud" } }
 * ```
 */
open class IaConfig(
    var baseUrl: String = "http://localhost:11464",
    var modelName: String = "deepseek-v4-pro",
    var timeout: Duration = Duration.ofSeconds(120),
    var enabled: Boolean = false,
    /** Ports du pool Docker (ex: "11437,11438,11439"). Active le mode pool si non-null. */
    var poolPorts: String? = null,
    /** Modèle à utiliser pour chaque instance du pool. Obligatoire si poolPorts est défini. */
    var poolModel: String? = null
)
