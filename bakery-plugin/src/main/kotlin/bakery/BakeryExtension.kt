package bakery

import bakery.llm.IaConfig
import bakery.article.ArticleIntentionDsl
import bakery.lens.AugmentedContextDsl
import bakery.scaffold.ScaffoldIntentionDsl
import bakery.theme.ThemeIntentionDsl
import bakery.a11y.AccessibilityDsl
import bakery.i18n.ContentMigrationIntentionDsl
import bakery.i18n.I18nMigrationIntentionDsl
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
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
 *     siteType = "blog"        // blog | basic
 *     ia {
 *         baseUrl = "http://localhost:11464"
 *         modelName = "gpt-oss:120b-cloud"
 *     }
 * }
 * ```
 */
open class BakeryExtension @Inject constructor(objects: ObjectFactory) {
    val configPath: Property<String> = objects.property(String::class.java)
    val sitesBaseDir: Property<String> = objects.property(String::class.java)
    val siteName: Property<String> = objects.property(String::class.java)

    /**
     * Type de site JBake à scaffolding.
     * - `"blog"` (défaut) : site/blog classique avec articles, tags, archives
     * - `"basic"` : site minimal (index, about, contact)
     */
    val siteType: Property<String> = objects.property(String::class.java)

    /** Configuration du service IA (LangChain4j + Ollama) — BKY-IA-0 */
    val ia: IaConfig = IaConfig()

    /** Configuration Google Forms embed — BKY-JB-3 */
    val googleForms: GoogleFormsDsl = GoogleFormsDsl()

    /** Configuration Firebase Auth + Comments — BKY-JB-4 */
    val firebaseAuth: FirebaseAuthDsl = FirebaseAuthDsl()

    /** Configuration Comments (Firestore) — BKY-JB-4 */
    val commentsConfig: CommentsDsl = CommentsDsl()

    /** Configuration Analytics (Plausible/Matomo) — BKY-JB-5 */
    val analytics: AnalyticsDsl = AnalyticsDsl()

    /** Configuration Newsletter (mail footer) — BKY-JB-5 */
    val newsletter: NewsletterDsl = NewsletterDsl()

    /** Configuration Theme (CSS variables, dark/light, logo) — BKY-JB-6 */
    val theme: ThemeDsl = ThemeDsl()

    /** Configuration Layout (FULL_WIDTH, SIDEBAR_LEFT, SIDEBAR_RIGHT, CENTERED) — BKY-JB-7 */
    val layout: LayoutDsl = LayoutDsl()

    /** Configuration Article Intention (topic, ton, audience, keywords, lang) — BKY-JB-8 */
    val articleIntention: ArticleIntentionDsl = ArticleIntentionDsl()

    /** Configuration Augmented Context (Pattern LENTILLE) — BKY-LENS */
    val augmentedContext: AugmentedContextDsl = AugmentedContextDsl()

    /** Configuration Scaffold Intention (description, siteType, lang, projectName) — BKY-IA-1 */
    val scaffoldIntention: ScaffoldIntentionDsl = ScaffoldIntentionDsl()

    /** Configuration Theme Intention (description, variante, surcharges) — BKY-IA-2 */
    val themeIntention: ThemeIntentionDsl = ThemeIntentionDsl()

    /** Configuration I18n Migration Intention (siteDir, languages, defaultLanguage, dryRun) — BKY-I18N-MIG */
    val i18nMigration: I18nMigrationIntentionDsl = I18nMigrationIntentionDsl()

    /** Configuration Content I18n Migration Intention (sourceDir, outputDir, targetLanguages, sourceLanguage, dryRun) — BKY-I18N-REAL */
    val contentI18nMigration: ContentMigrationIntentionDsl = ContentMigrationIntentionDsl()

    /** Configuration Accessibilité (auditDir, reportPath, conformanceLevel) — BKY-A11Y-1 */
    val a11y: AccessibilityDsl = AccessibilityDsl(objects)

    /** Langue active du site (code ISO 639-1, ex: "fr", "en", "ar") — BKY-I18N */
    val language: Property<String> = objects.property(String::class.java)

    /** Langues supportées (liste de codes ISO 639-1) — BKY-I18N */
    val supportedLanguages: ListProperty<String> = objects.listProperty(String::class.java)

    /** DSL : bakery { googleForms { ... } } */
    fun googleForms(action: Action<GoogleFormsDsl>) {
        action.execute(googleForms)
    }

    /** DSL : bakery { firebaseAuth { ... } } */
    fun firebaseAuth(action: Action<FirebaseAuthDsl>) {
        action.execute(firebaseAuth)
    }

    /** DSL : bakery { commentsConfig { ... } } */
    fun commentsConfig(action: Action<CommentsDsl>) {
        action.execute(commentsConfig)
    }

    /** DSL : bakery { analytics { ... } } */
    fun analytics(action: Action<AnalyticsDsl>) {
        action.execute(analytics)
    }

    /** DSL : bakery { newsletter { ... } } */
    fun newsletter(action: Action<NewsletterDsl>) {
        action.execute(newsletter)
    }

    /** DSL : bakery { theme { ... } } */
    fun theme(action: Action<ThemeDsl>) {
        action.execute(theme)
    }

    /** DSL : bakery { layout { ... } } */
    fun layout(action: Action<LayoutDsl>) {
        action.execute(layout)
    }

    /** DSL : bakery { articleIntention { ... } } */
    fun articleIntention(action: Action<ArticleIntentionDsl>) {
        action.execute(articleIntention)
    }

    /** DSL : bakery { ia { ... } } */
    fun ia(action: Action<IaConfig>) {
        action.execute(ia)
    }

    /** DSL : bakery { augmentedContext { ... } } */
    fun augmentedContext(action: Action<AugmentedContextDsl>) {
        action.execute(augmentedContext)
    }

    /** DSL : bakery { scaffoldIntention { ... } } — BKY-IA-1 */
    fun scaffoldIntention(action: Action<ScaffoldIntentionDsl>) {
        action.execute(scaffoldIntention)
    }

    /** DSL : bakery { themeIntention { ... } } — BKY-IA-2 */
    fun themeIntention(action: Action<ThemeIntentionDsl>) {
        action.execute(themeIntention)
    }

    /** DSL : bakery { i18nMigration { ... } } — BKY-I18N-MIG */
    fun i18nMigration(action: Action<I18nMigrationIntentionDsl>) {
        action.execute(i18nMigration)
    }

    /** DSL : bakery { contentI18nMigration { ... } } — BKY-I18N-REAL */
    fun contentI18nMigration(action: Action<ContentMigrationIntentionDsl>) {
        action.execute(contentI18nMigration)
    }

    /** DSL : bakery { a11y { ... } } — BKY-A11Y-1 */
    fun a11y(action: Action<AccessibilityDsl>) {
        action.execute(a11y)
    }
}
