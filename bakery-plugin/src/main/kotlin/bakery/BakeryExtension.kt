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
 *     siteType = "blog"        // blog | basic
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

    /** DSL : bakery { ia { ... } } */
    fun ia(action: Action<IaConfig>) {
        action.execute(ia)
    }
}
