package bakery.theme

import bakery.injection.updateProperty
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Tâche Gradle de génération de thème assistée par catalogue.
 *
 * Pipeline :
 * ```
 * generateTheme → (résolution variante + surcharges) → jbake.properties mis à jour
 * ```
 *
 * Usage CLI (variante seule) :
 * ```
 * ./gradlew generateTheme -PthemeVariant=magazine
 * ```
 *
 * Usage CLI (variante + surcharges) :
 * ```
 * ./gradlew generateTheme -PthemeVariant=formation -PthemeAccentColor=#FF5733
 * ```
 *
 * Usage DSL :
 * ```
 * bakery {
 *     themeIntention {
 *         description = "Blog tech moderne avec couleurs froides"
 *         variant = "magazine"
 *         primaryColor = "#c0392b"
 *     }
 * }
 * ```
 *
 * Résolution cascade : CLI > gradle.properties > DSL > YAML > preset (variante) > defaults.
 */
@DisableCachingByDefault(because = "Résolution de thème — résultat déterministe mais dépendant de la config")
abstract class GenerateThemeTask : DefaultTask() {

    /**
     * Répertoire cible du site JBake.
     * Résolu depuis `bake.srcPath` du site config.
     */
    @get:Internal
    var targetDir: File? = null

    /**
     * Description du thème en langage naturel (obligatoire pour IA, optionnel pour catalogue).
     * CLI : `-PthemeDescription="Blog tech moderne"`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "themeDescription", description = "Description du thème à générer")
    abstract val themeDescription: Property<String>

    /**
     * Variante de catalogue : minimal, magazine, documentation, portfolio, formation.
     * CLI : `-PthemeVariant=magazine`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "themeVariant", description = "Variante de catalogue (minimal/magazine/documentation/portfolio/formation)")
    abstract val themeVariant: Property<String>

    /**
     * Couleur primaire de surcharge.
     * CLI : `-PthemePrimaryColor=#c0392b`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "themePrimaryColor", description = "Couleur primaire (hex)")
    abstract val themePrimaryColor: Property<String>

    /**
     * Couleur secondaire de surcharge.
     * CLI : `-PthemeSecondaryColor=#6c757d`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "themeSecondaryColor", description = "Couleur secondaire (hex)")
    abstract val themeSecondaryColor: Property<String>

    /**
     * Couleur d'accent de surcharge.
     * CLI : `-PthemeAccentColor=#FF5733`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "themeAccentColor", description = "Couleur d'accent (hex)")
    abstract val themeAccentColor: Property<String>

    /**
     * Couleur de fond de surcharge.
     * CLI : `-PthemeBackgroundColor=#FFFFFF`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "themeBackgroundColor", description = "Couleur de fond (hex)")
    abstract val themeBackgroundColor: Property<String>

    /**
     * Couleur de texte de surcharge.
     * CLI : `-PthemeTextColor=#333333`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "themeTextColor", description = "Couleur de texte (hex)")
    abstract val themeTextColor: Property<String>

    /**
     * Police de caractères de surcharge.
     * CLI : `-PthemeFontFamily=Inter`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "themeFontFamily", description = "Police de caractères")
    abstract val themeFontFamily: Property<String>

    /**
     * Police des titres de surcharge.
     * CLI : `-PthemeHeadingFont=Merriweather`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "themeHeadingFont", description = "Police des titres")
    abstract val themeHeadingFont: Property<String>

    /**
     * Intention DSL injectée depuis [bakery.BakeryExtension.themeIntention].
     * Priorité : CLI > DSL > defaults.
     */
    @get:Internal
    var dslIntention: ThemeIntention? = null

    init {
        group = "generate"
        description = "Génère un thème à partir du catalogue — résolution variante + surcharges"
        themeDescription.convention("")
        themeVariant.convention("")
        themePrimaryColor.convention("")
        themeSecondaryColor.convention("")
        themeAccentColor.convention("")
        themeBackgroundColor.convention("")
        themeTextColor.convention("")
        themeFontFamily.convention("")
        themeHeadingFont.convention("")
    }

    @TaskAction
    fun executeGenerate() {
        val resolvedIntention = resolveIntention()

        logger.lifecycle("[generateTheme] Thème : variante={}, description={}",
            resolvedIntention.variant, resolvedIntention.description)

        // Résoudre le preset du catalogue
        val preset = ThemeCatalog.resolve(resolvedIntention.variant, resolvedIntention.overrides)

        logger.lifecycle("[generateTheme] Preset résolu : primaryColor={}, accentColor={}, backgroundColor={}",
            preset.primaryColor, preset.accentColor, preset.backgroundColor)

        // Écrire les propriétés de thème dans jbake.properties
        val dir = targetDir
            ?: throw IllegalStateException(
                "Aucun targetDir configuré. Le site doit être initialisé avec generateSite d'abord."
            )

        val jbakeProps = dir.resolve("site/jbake.properties")
        if (jbakeProps.exists()) {
            val lines = jbakeProps.readText(Charsets.UTF_8).lines().toMutableList()
            updateProperty(lines, "themeVariant", resolvedIntention.variant.name)
            updateProperty(lines, "themePrimaryColor", preset.primaryColor)
            updateProperty(lines, "themeSecondaryColor", preset.secondaryColor)
            updateProperty(lines, "themeAccentColor", preset.accentColor)
            updateProperty(lines, "themeBackgroundColor", preset.backgroundColor)
            updateProperty(lines, "themeTextColor", preset.textColor)
            updateProperty(lines, "themeFontFamily", preset.fontFamily)
            updateProperty(lines, "themeHeadingFont", preset.headingFont)
            updateProperty(lines, "themeMode", "auto")
            updateProperty(lines, "themeLogoUrl", preset.logoUrl)
            updateProperty(lines, "themeFaviconUrl", preset.faviconUrl)
            jbakeProps.writeText(lines.joinToString("\n"), Charsets.UTF_8)
            logger.lifecycle("[generateTheme] Thème injecté dans jbake.properties")
        } else {
            logger.warn("[generateTheme] jbake.properties non trouvé à ${jbakeProps.absolutePath}")
        }
    }

    /**
     * Résout la [ThemeIntention] finale en fusionnant CLI > DSL > defaults.
     *
     * Priorité de résolution :
     * 1. CLI : `-PthemeVariant`, `-PthemeAccentColor`, etc.
     * 2. DSL : `bakery { themeIntention { ... } }`
     * 3. Defaults : variante MINIMAL, couleurs du preset
     */
    internal fun resolveIntention(): ThemeIntention {
        val resolvedDescription = themeDescription.orNull?.takeIf { it.isNotBlank() }
            ?: dslIntention?.description
            ?: "Thème généré par catalogue"

        val resolvedVariant = themeVariant.orNull?.takeIf { it.isNotBlank() }
            ?: dslIntention?.variant?.name?.lowercase()
            ?: "minimal"

        val resolvedOverrides = ThemeOverrides(
            primaryColor = themePrimaryColor.orNull?.takeIf { it.isNotBlank() }
                ?: dslIntention?.overrides?.primaryColor,
            secondaryColor = themeSecondaryColor.orNull?.takeIf { it.isNotBlank() }
                ?: dslIntention?.overrides?.secondaryColor,
            accentColor = themeAccentColor.orNull?.takeIf { it.isNotBlank() }
                ?: dslIntention?.overrides?.accentColor,
            backgroundColor = themeBackgroundColor.orNull?.takeIf { it.isNotBlank() }
                ?: dslIntention?.overrides?.backgroundColor,
            textColor = themeTextColor.orNull?.takeIf { it.isNotBlank() }
                ?: dslIntention?.overrides?.textColor,
            fontFamily = themeFontFamily.orNull?.takeIf { it.isNotBlank() }
                ?: dslIntention?.overrides?.fontFamily,
            headingFont = themeHeadingFont.orNull?.takeIf { it.isNotBlank() }
                ?: dslIntention?.overrides?.headingFont,
        )

        return ThemeIntention(
            description = resolvedDescription,
            variant = ThemeVariant.fromStringOrDefault(resolvedVariant),
            overrides = resolvedOverrides
        )
    }
}