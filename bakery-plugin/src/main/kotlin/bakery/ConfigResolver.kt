package bakery

import org.gradle.api.Project

/**
 * Resolves configuration from multiple sources with priority:
 *
 *   CLI (-P params) > gradle.properties > DSL (BakeryExtension) > site.yml (YAML) > hardcoded defaults
 *
 * Each property follows the same cascade:
 * 1. **CLI / gradle.properties**: `-Pbakery.googleForms.formId=...` or `bakery.googleForms.formId=...` in gradle.properties
 * 2. **DSL**: `bakery { googleForms { formId = "..." } }` via the extension
 * 3. **YAML**: `site.yml` parsed by Jackson into `SiteConfiguration`
 * 4. **Defaults**: hardcoded values in data classes
 *
 * Usage:
 * ```kotlin
 * val props = ConfigResolver.loadProperties(project)
 * val formId = ConfigResolver.resolveString(props, "bakery.googleForms", "formId",
 *     dslValue = ext.googleForms.formId,
 *     yamlValue = site.googleForms?.formId,
 *     default = "")
 * ```
 *
 * Inspired by plantuml-gradle ConfigMerger, simplified with Map-based approach
 * for testability and idiomatie Kotlin.
 */
object ConfigResolver {

    /**
     * Loads all bakery-related properties from the Gradle project.
     * Merges CLI -P params and gradle.properties into a single map.
     * Project.properties includes both sources.
     */
    fun loadProperties(project: Project): Map<String, String> {
        val result = mutableMapOf<String, String>()
        project.properties.forEach { (key, value) ->
            if (key.startsWith("bakery.")) {
                result[key] = value.toString()
            }
        }
        return result
    }

    /**
     * Loads properties from a map (for testing or direct usage).
     */
    fun loadPropertiesFromMap(props: Map<String, String>): Map<String, String> {
        return props.filterKeys { it.startsWith("bakery.") }
    }

    /**
     * Resolves a String property through the 4-layer cascade.
     *
     * @param props     Properties map (CLI + gradle.properties merged)
     * @param prefix    The property prefix (e.g. "bakery.googleForms")
     * @param key       The property key (e.g. "formId")
     * @param dslValue  The value from the DSL extension
     * @param yamlValue The value from site.yml (may be null if absent)
     * @param default   The hardcoded default value
     * @return The resolved value following the priority chain
     */
    fun resolveString(
        props: Map<String, String>,
        prefix: String,
        key: String,
        dslValue: String,
        yamlValue: String?,
        default: String = ""
    ): String {
        // Layer 1+2: CLI / gradle.properties
        val cliValue = props["${prefix}.${key}"]
        if (cliValue != null) {
            return cliValue
        }

        // Layer 3: DSL (non-blank and different from default means explicitly set)
        if (dslValue.isNotBlank() && dslValue != default) {
            return dslValue
        }

        // Layer 4: YAML
        if (!yamlValue.isNullOrBlank()) {
            return yamlValue
        }

        // Layer 5: Default
        return default
    }

    /**
     * Resolves an Int property through the 4-layer cascade.
     */
    fun resolveInt(
        props: Map<String, String>,
        prefix: String,
        key: String,
        dslValue: Int,
        yamlValue: Int?,
        default: Int
    ): Int {
        // Layer 1+2: CLI / gradle.properties
        val cliValue = props["${prefix}.${key}"]
        if (cliValue != null) {
            return cliValue.toIntOrNull() ?: default
        }

        // Layer 3: DSL (different from default means explicitly set)
        if (dslValue != default) {
            return dslValue
        }

        // Layer 4: YAML
        if (yamlValue != null) {
            return yamlValue
        }

        // Layer 5: Default
        return default
    }

    /**
     * Resolves a Boolean property through the 4-layer cascade.
     */
    fun resolveBoolean(
        props: Map<String, String>,
        prefix: String,
        key: String,
        dslValue: Boolean,
        yamlValue: Boolean?,
        default: Boolean
    ): Boolean {
        // Layer 1+2: CLI / gradle.properties
        val cliValue = props["${prefix}.${key}"]
        if (cliValue != null) {
            return cliValue.toBooleanStrictOrNull() ?: default
        }

        // Layer 3: DSL (different from default means explicitly set)
        if (dslValue != default) {
            return dslValue
        }

        // Layer 4: YAML
        if (yamlValue != null) {
            return yamlValue
        }

        // Layer 5: Default
        return default
    }

    /**
     * Resolves a GoogleFormsConfig through the 4-layer cascade.
     * Each field follows CLI > gradle.properties > DSL > YAML > defaults.
     */
    fun resolveGoogleFormsConfig(
        props: Map<String, String>,
        dsl: GoogleFormsDsl,
        yaml: GoogleFormsConfig?,
        default: GoogleFormsConfig = GoogleFormsConfig()
    ): GoogleFormsConfig {
        val prefix = "bakery.googleForms"
        return GoogleFormsConfig(
            formId = resolveString(props, prefix, "formId", dsl.formId, yaml?.formId, default.formId),
            width = resolveString(props, prefix, "width", dsl.width, yaml?.width, default.width),
            height = resolveString(props, prefix, "height", dsl.height, yaml?.height, default.height),
            allowMultiple = resolveBoolean(props, prefix, "allowMultiple", dsl.allowMultiple, yaml?.allowMultiple, default.allowMultiple),
        )
    }

    /**
     * Resolves an AnalyticsConfig through the 4-layer cascade.
     */
    fun resolveAnalyticsConfig(
        props: Map<String, String>,
        dsl: AnalyticsDsl,
        yaml: AnalyticsConfig?,
        default: AnalyticsConfig = AnalyticsConfig()
    ): AnalyticsConfig {
        val prefix = "bakery.analytics"
        return AnalyticsConfig(
            provider = resolveString(props, prefix, "provider", dsl.provider, yaml?.provider, default.provider),
            domain = resolveString(props, prefix, "domain", dsl.domain, yaml?.domain, default.domain),
            scriptSrc = resolveString(props, prefix, "scriptSrc", dsl.scriptSrc, yaml?.scriptSrc, default.scriptSrc),
        )
    }

    /**
     * Resolves a RelatedArticlesConfig through the 4-layer cascade.
     */
    fun resolveRelatedArticlesConfig(
        props: Map<String, String>,
        dsl: RelatedArticlesDsl,
        yaml: RelatedArticlesConfig?,
        default: RelatedArticlesConfig = RelatedArticlesConfig()
    ): RelatedArticlesConfig {
        val prefix = "bakery.relatedArticles"
        return RelatedArticlesConfig(
            enabled = resolveBoolean(props, prefix, "enabled", dsl.enabled, yaml?.enabled, default.enabled),
            maxResults = resolveInt(props, prefix, "maxResults", dsl.maxResults, yaml?.maxResults, default.maxResults),
            heading = resolveString(props, prefix, "heading", dsl.heading, yaml?.heading, default.heading),
            graphFilePath = resolveString(props, prefix, "graphFilePath", dsl.graphFilePath, yaml?.graphFilePath, default.graphFilePath),
        )
    }

    /**
     * Resolves an Enum property through the 4-layer cascade.
     */
    inline fun <reified E : Enum<E>> resolveEnum(
        props: Map<String, String>,
        prefix: String,
        key: String,
        dslValue: E,
        yamlValue: E?,
        default: E
    ): E {
        // Layer 1+2: CLI / gradle.properties
        val cliValue = props["${prefix}.${key}"]
        if (cliValue != null) {
            return try {
                java.lang.Enum.valueOf(E::class.java, cliValue)
            } catch (_: IllegalArgumentException) {
                default
            }
        }

        // Layer 3: DSL
        if (dslValue != default) {
            return dslValue
        }

        // Layer 4: YAML
        if (yamlValue != null) {
            return yamlValue
        }

        // Layer 5: Default
        return default
    }
}