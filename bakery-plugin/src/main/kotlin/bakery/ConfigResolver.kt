package bakery

import bakery.theme.ThemeCatalog
import bakery.theme.ThemeVariant
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

    private fun <T> resolve(
        props: Map<String, String>,
        prefix: String,
        key: String,
        dslValue: T,
        yamlValue: T?,
        default: T,
        parseCli: (String) -> T?,
        isExplicitlySet: (T, T) -> Boolean,
        isPresent: (T?) -> Boolean
    ): T {
        val cliValue = props["${prefix}.${key}"]
        if (cliValue != null) {
            return parseCli(cliValue) ?: default
        }
        if (isExplicitlySet(dslValue, default)) {
            return dslValue
        }
        if (isPresent(yamlValue)) {
            return yamlValue!!
        }
        return default
    }

    /**
     * Resolves a String property through the 4-layer cascade.
     */
    fun resolveString(
        props: Map<String, String>,
        prefix: String,
        key: String,
        dslValue: String,
        yamlValue: String?,
        default: String = ""
    ): String = resolve(
        props, prefix, key, dslValue, yamlValue, default,
        parseCli = { it },
        isExplicitlySet = { dsl, def -> dsl.isNotBlank() && dsl != def },
        isPresent = { it != null && it.isNotBlank() }
    )

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
    ): Int = resolve(
        props, prefix, key, dslValue, yamlValue, default,
        parseCli = { it.toIntOrNull() },
        isExplicitlySet = { dsl, def -> dsl != def },
        isPresent = { it != null }
    )

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
    ): Boolean = resolve(
        props, prefix, key, dslValue, yamlValue, default,
        parseCli = { it.toBooleanStrictOrNull() },
        isExplicitlySet = { dsl, def -> dsl != def },
        isPresent = { it != null }
    )

    private class Scope(
        val props: Map<String, String>,
        val prefix: String
    ) {
        fun string(key: String, dsl: String, yaml: String?, default: String = "") =
            resolveString(props, prefix, key, dsl, yaml, default)
        fun int(key: String, dsl: Int, yaml: Int?, default: Int) =
            resolveInt(props, prefix, key, dsl, yaml, default)
        fun boolean(key: String, dsl: Boolean, yaml: Boolean?, default: Boolean) =
            resolveBoolean(props, prefix, key, dsl, yaml, default)
        inline fun <reified E : Enum<E>> enum(key: String, dsl: E, yaml: E?, default: E) =
            resolveEnum(props, prefix, key, dsl, yaml, default)
    }

    private inline fun <T> resolveConfig(
        props: Map<String, String>,
        prefix: String,
        block: Scope.() -> T
    ): T = Scope(props, prefix).block()

    /**
     * Resolves a GoogleFormsConfig through the 4-layer cascade.
     * Each field follows CLI > gradle.properties > DSL > YAML > defaults.
     */
    fun resolveGoogleFormsConfig(
        props: Map<String, String>,
        dsl: GoogleFormsDsl,
        yaml: GoogleFormsConfig?,
        default: GoogleFormsConfig = GoogleFormsConfig()
    ): GoogleFormsConfig = resolveConfig(props, "bakery.googleForms") {
        GoogleFormsConfig(
            formId = string("formId", dsl.formId, yaml?.formId, default.formId),
            width = string("width", dsl.width, yaml?.width, default.width),
            height = string("height", dsl.height, yaml?.height, default.height),
            allowMultiple = boolean("allowMultiple", dsl.allowMultiple, yaml?.allowMultiple, default.allowMultiple),
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
    ): AnalyticsConfig = resolveConfig(props, "bakery.analytics") {
        AnalyticsConfig(
            provider = string("provider", dsl.provider, yaml?.provider, default.provider),
            domain = string("domain", dsl.domain, yaml?.domain, default.domain),
            scriptSrc = string("scriptSrc", dsl.scriptSrc, yaml?.scriptSrc, default.scriptSrc),
        )
    }

    /**
     * Resolves a FirebaseAuthConfig through the 4-layer cascade.
     */
    fun resolveFirebaseAuthConfig(
        props: Map<String, String>,
        dsl: FirebaseAuthDsl,
        yaml: FirebaseAuthConfig?,
        default: FirebaseAuthConfig = FirebaseAuthConfig()
    ): FirebaseAuthConfig = resolveConfig(props, "bakery.firebaseAuth") {
        FirebaseAuthConfig(
            apiKey = string("apiKey", dsl.apiKey, yaml?.apiKey, default.apiKey),
            authDomain = string("authDomain", dsl.authDomain, yaml?.authDomain, default.authDomain),
            projectId = string("projectId", dsl.projectId, yaml?.projectId, default.projectId),
        )
    }

    /**
     * Resolves a CommentsConfig through the 4-layer cascade.
     */
    fun resolveCommentsConfig(
        props: Map<String, String>,
        dsl: CommentsDsl,
        yaml: CommentsConfig?,
        default: CommentsConfig = CommentsConfig()
    ): CommentsConfig = resolveConfig(props, "bakery.comments") {
        CommentsConfig(
            enabled = boolean("enabled", dsl.enabled, yaml?.enabled, default.enabled),
            collection = string("collection", dsl.collection, yaml?.collection, default.collection),
        )
    }

    /**
     * Resolves a NewsletterConfig through the 4-layer cascade.
     */
    fun resolveNewsletterConfig(
        props: Map<String, String>,
        dsl: NewsletterDsl,
        yaml: NewsletterConfig?,
        default: NewsletterConfig = NewsletterConfig()
    ): NewsletterConfig = resolveConfig(props, "bakery.newsletter") {
        NewsletterConfig(
            enabled = boolean("enabled", dsl.enabled, yaml?.enabled, default.enabled),
            provider = string("provider", dsl.provider, yaml?.provider, default.provider),
            endpoint = string("endpoint", dsl.endpoint, yaml?.endpoint, default.endpoint),
        )
    }

    /**
     * Resolves a ThemeConfig through the 4-layer cascade.
     * BKY-IA-2: supports variant selection — when variant is set, preset values
     * serve as YAML-layer defaults before individual overrides are applied.
     */
    fun resolveThemeConfig(
        props: Map<String, String>,
        dsl: ThemeDsl,
        yaml: ThemeConfig?,
        default: ThemeConfig = ThemeConfig()
    ): ThemeConfig {
        val prefix = "bakery.theme"
        // BKY-IA-2: if variant is set, resolve it to a ThemePreset and use as YAML-layer defaults
        val variantName = resolveString(props, prefix, "variant", dsl.variant, yaml?.variant, default.variant)
        val presetVariant = ThemeVariant.fromStringOrDefault(variantName.ifBlank { null })
        val preset = if (variantName.isNotBlank()) ThemeCatalog.presetFor(presetVariant) else null

        return ThemeConfig(
            mode = resolveString(props, prefix, "mode", dsl.mode, yaml?.mode, default.mode),
            primaryColor = resolveString(props, prefix, "primaryColor", dsl.primaryColor, yaml?.primaryColor, preset?.primaryColor ?: default.primaryColor),
            secondaryColor = resolveString(props, prefix, "secondaryColor", dsl.secondaryColor, yaml?.secondaryColor, preset?.secondaryColor ?: default.secondaryColor),
            fontFamily = resolveString(props, prefix, "fontFamily", dsl.fontFamily, yaml?.fontFamily, preset?.fontFamily ?: default.fontFamily),
            logoUrl = resolveString(props, prefix, "logoUrl", dsl.logoUrl, yaml?.logoUrl, preset?.logoUrl ?: default.logoUrl),
            faviconUrl = resolveString(props, prefix, "faviconUrl", dsl.faviconUrl, yaml?.faviconUrl, preset?.faviconUrl ?: default.faviconUrl),
            variant = variantName,
            accentColor = resolveString(props, prefix, "accentColor", dsl.accentColor, yaml?.accentColor, preset?.accentColor ?: default.accentColor),
            backgroundColor = resolveString(props, prefix, "backgroundColor", dsl.backgroundColor, yaml?.backgroundColor, preset?.backgroundColor ?: default.backgroundColor),
            textColor = resolveString(props, prefix, "textColor", dsl.textColor, yaml?.textColor, preset?.textColor ?: default.textColor),
            headingFont = resolveString(props, prefix, "headingFont", dsl.headingFont, yaml?.headingFont, preset?.headingFont ?: default.headingFont),
        )
    }

    /**
     * Resolves a LayoutConfig through the 4-layer cascade.
     */
    fun resolveLayoutConfig(
        props: Map<String, String>,
        dsl: LayoutDsl,
        yaml: LayoutConfig?,
        default: LayoutConfig = LayoutConfig()
    ): LayoutConfig = resolveConfig(props, "bakery.layout") {
        LayoutConfig(
            layoutType = enum("layoutType", dsl.layoutType, yaml?.layoutType, default.layoutType),
        )
    }

    /**
     * Resolves Firebase contact form config (apiKey, projectId) through the 4-layer cascade.
     * Note: Firebase contact form has no DSL class (uses nested config in site.yml only).
     */
    fun resolveFirebaseConfig(
        props: Map<String, String>,
        yaml: FirebaseContactFormConfig?,
        default: FirebaseProjectInfo = FirebaseProjectInfo(projectId = "", apiKey = "")
    ): FirebaseProjectInfo {
        val prefix = "bakery.firebase"
        val yamlProject = yaml?.project ?: default
        return FirebaseProjectInfo(
            projectId = resolveString(props, prefix, "projectId", default.projectId, yamlProject.projectId, default.projectId),
            apiKey = resolveString(props, prefix, "apiKey", default.apiKey, yamlProject.apiKey, default.apiKey),
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

    fun resolveAll(
        props: Map<String, String>,
        extension: BakeryExtension,
        site: SiteConfiguration
    ): Pair<ResolvedConfigs, List<ConfigResolutionError>> {
        val errors = mutableListOf<ConfigResolutionError>()

        val resolvedFirebase = try {
            resolveFirebaseConfig(props, site.firebase)
        } catch (e: Exception) {
            errors.add(ConfigResolutionError.DomainFailure("firebase", e.message ?: "Unknown error", e))
            FirebaseProjectInfo(projectId = "", apiKey = "")
        }

        val resolvedGoogleForms = try {
            resolveGoogleFormsConfig(props, extension.googleForms, site.googleForms)
        } catch (e: Exception) {
            errors.add(ConfigResolutionError.DomainFailure("googleForms", e.message ?: "Unknown error", e))
            GoogleFormsConfig()
        }

        val resolvedFirebaseAuth = try {
            resolveFirebaseAuthConfig(props, extension.firebaseAuth, site.firebaseAuth)
        } catch (e: Exception) {
            errors.add(ConfigResolutionError.DomainFailure("firebaseAuth", e.message ?: "Unknown error", e))
            FirebaseAuthConfig()
        }

        val resolvedComments = try {
            resolveCommentsConfig(props, extension.commentsConfig, site.comments)
        } catch (e: Exception) {
            errors.add(ConfigResolutionError.DomainFailure("comments", e.message ?: "Unknown error", e))
            CommentsConfig()
        }

        val resolvedAnalytics = try {
            resolveAnalyticsConfig(props, extension.analytics, site.analytics)
        } catch (e: Exception) {
            errors.add(ConfigResolutionError.DomainFailure("analytics", e.message ?: "Unknown error", e))
            AnalyticsConfig()
        }

        val resolvedNewsletter = try {
            resolveNewsletterConfig(props, extension.newsletter, site.newsletter)
        } catch (e: Exception) {
            errors.add(ConfigResolutionError.DomainFailure("newsletter", e.message ?: "Unknown error", e))
            NewsletterConfig()
        }

        val resolvedTheme = try {
            resolveThemeConfig(props, extension.theme, site.theme)
        } catch (e: Exception) {
            errors.add(ConfigResolutionError.DomainFailure("theme", e.message ?: "Unknown error", e))
            ThemeConfig()
        }

        val resolvedLayout = try {
            resolveLayoutConfig(props, extension.layout, site.layout)
        } catch (e: Exception) {
            errors.add(ConfigResolutionError.DomainFailure("layout", e.message ?: "Unknown error", e))
            LayoutConfig()
        }

        return ResolvedConfigs(
            firebase = resolvedFirebase,
            googleForms = resolvedGoogleForms,
            firebaseAuth = resolvedFirebaseAuth,
            comments = resolvedComments,
            analytics = resolvedAnalytics,
            newsletter = resolvedNewsletter,
            theme = resolvedTheme,
            layout = resolvedLayout
        ) to errors.toList()
    }
}