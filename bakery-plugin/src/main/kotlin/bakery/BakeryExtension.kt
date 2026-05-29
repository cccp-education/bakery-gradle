package bakery

import bakery.llm.IaConfig
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * DSL extension for bakery configuration.
 *
 * Usage:
 * ```
 * bakery {
 *     configPath = "site.yml"
 *     sitesBaseDir = "..."
 *     siteName = "..."
 *     ia {
 *         baseUrl = "http://localhost:11434"
 *         modelName = "deepseek-v4-pro"
 *     }
 * }
 * ```
 */
open class BakeryExtension @Inject constructor(objects: ObjectFactory) {
    val configPath: Property<String> = objects.property(String::class.java)
    val sitesBaseDir: Property<String> = objects.property(String::class.java)
    val siteName: Property<String> = objects.property(String::class.java)

    /** Configuration du service IA (LangChain4j + Ollama) — BKY-IA-0 */
    val ia: IaConfig = IaConfig()

    /** DSL : bakery { ia { ... } } */
    fun ia(action: Action<IaConfig>) {
        action.execute(ia)
    }
}
