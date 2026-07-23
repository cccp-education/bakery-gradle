package bakery

import bakery.tree.SiteNodeDto
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import contracts.i18n.OllamaConfig
import contracts.i18n.OllamaDeviceKey

data class GitPushConfiguration(
    val from: String = "",
    val to: String = "",
    val repo: RepositoryConfiguration = RepositoryConfiguration(),
    val branch: String = "",
    val message: String = "",
)

data class RepositoryConfiguration(
    val name: String = "",
    val repository: String = "",
    val credentials: RepositoryCredentials = RepositoryCredentials(),
) {
    companion object {
        const val ORIGIN = "origin"
        const val CNAME = "CNAME"
        const val REMOTE = "remote"
    }
}

data class RepositoryCredentials(val username: String = "", val password: String = "") {
    override fun toString(): String =
        "RepositoryCredentials(username='${maskSecret(SecretField.Token(username))}', password='${maskSecret(SecretField.Password(password))}')"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class SiteConfiguration(
    val bake: BakeConfiguration = BakeConfiguration(),
    val pushPage: GitPushConfiguration = GitPushConfiguration(),
    val pushMaquette: GitPushConfiguration = GitPushConfiguration(),
    val pushProfile: GitPushConfiguration? = null,
    val profileFiles: List<String> = emptyList(),
    val pushSource: GitPushConfiguration? = null,
    val pushTemplate: GitPushConfiguration? = null,
    val firebase: FirebaseContactFormConfig? = null,
    val googleForms: GoogleFormsConfig? = null,
    val firebaseAuth: FirebaseAuthConfig? = null,
    val comments: CommentsConfig? = null,
    val analytics: AnalyticsConfig? = null,
    val newsletter: NewsletterConfig? = null,
    val theme: ThemeConfig? = null,
    val layout: LayoutConfig? = null,
    val language: String = "fr",
    val supportedLanguages: List<String> = listOf("fr"),
    val tree: SiteNodeDto? = null,
    val ollama: OllamaConfig? = null,
)

data class BakeConfiguration(
    val srcPath: String = "",
    val destDirPath: String = "",
    val cname: String = "",
)

data class FirebaseContactFormConfig(
    val project: FirebaseProjectInfo = FirebaseProjectInfo(),
    val firestore: FirebaseFirestoreSchema = FirebaseFirestoreSchema(),
    val callable: FirebaseCallableFunction = FirebaseCallableFunction()
)

data class FirebaseProjectInfo(
    val projectId: String = "",
    val apiKey: String = ""
)

data class FirebaseFirestoreSchema(
    val contacts: FirebaseCollection = FirebaseCollection(),
    val messages: FirebaseCollection = FirebaseCollection()
)

data class FirebaseCollection(
    val name: String = "",
    val fields: List<FirebaseField> = emptyList(),
    val rulesEnabled: Boolean = false
)

data class FirebaseField(
    val name: String = "",
    val type: String = ""
)

data class FirebaseCallableFunction(
    val name: String = "",
    val params: List<FirebaseCallableParam> = emptyList()
)

data class FirebaseCallableParam(
    val name: String = "",
    val type: String = ""
)

data class GoogleFormsConfig(
    val formId: String = "",
    val width: String = "640",
    val height: String = "800",
    val allowMultiple: Boolean = false,
)

data class FirebaseAuthConfig(
    val apiKey: String = "",
    val authDomain: String = "",
    val projectId: String = "",
)

data class CommentsConfig(
    val enabled: Boolean = false,
    val collection: String = "comments",
)

data class AnalyticsConfig(
    val provider: String = "",
    val domain: String = "",
    val scriptSrc: String = "",
)

data class NewsletterConfig(
    val enabled: Boolean = false,
    val provider: String = "",
    val endpoint: String = "",
)

data class ThemeConfig(
    val mode: String = "auto",
    val primaryColor: String = "#0d6efd",
    val secondaryColor: String = "#6c757d",
    val fontFamily: String = "",
    val logoUrl: String = "",
    val faviconUrl: String = "",
    /** Variante de catalogue — BKY-IA-2 */
    val variant: String = "",
    /** Couleur d'accent — BKY-IA-2 */
    val accentColor: String = "",
    /** Couleur de fond — BKY-IA-2 */
    val backgroundColor: String = "",
    /** Couleur de texte — BKY-IA-2 */
    val textColor: String = "",
    /** Police des titres — BKY-IA-2 */
    val headingFont: String = "",
)

data class LayoutConfig(
    val layoutType: LayoutType = LayoutType.FULL_WIDTH,
)

