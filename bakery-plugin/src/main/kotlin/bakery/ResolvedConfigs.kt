package bakery

data class ResolvedConfigs(
    val firebase: FirebaseProjectInfo,
    val googleForms: GoogleFormsConfig,
    val firebaseAuth: FirebaseAuthConfig,
    val comments: CommentsConfig,
    val analytics: AnalyticsConfig,
    val newsletter: NewsletterConfig,
    val theme: ThemeConfig,
    val layout: LayoutConfig,
) {
    fun toResolver(): (String, String) -> String = { key, defaultValue ->
        when (key) {
            "firebaseApiKey" -> firebase.apiKey
            "firebaseProjectId" -> firebase.projectId
            "googleFormsFormId" -> googleForms.formId
            "googleFormsWidth" -> googleForms.width
            "googleFormsHeight" -> googleForms.height
            "googleFormsAllowMultiple" -> googleForms.allowMultiple.toString()
            "firebaseAuthApiKey" -> firebaseAuth.apiKey
            "firebaseAuthDomain" -> firebaseAuth.authDomain
            "firebaseAuthProjectId" -> firebaseAuth.projectId
            "commentsEnabled" -> comments.enabled.toString()
            "commentsCollection" -> comments.collection
            "analyticsProvider" -> analytics.provider
            "analyticsDomain" -> analytics.domain
            "analyticsScriptSrc" -> analytics.scriptSrc
            "newsletterEnabled" -> newsletter.enabled.toString()
            "newsletterProvider" -> newsletter.provider
            "newsletterEndpoint" -> newsletter.endpoint
            "themeMode" -> theme.mode
            "themePrimaryColor" -> theme.primaryColor
            "themeSecondaryColor" -> theme.secondaryColor
            "themeFontFamily" -> theme.fontFamily
            "themeLogoUrl" -> theme.logoUrl
            "themeFaviconUrl" -> theme.faviconUrl
            "themeVariant" -> theme.variant
            "themeAccentColor" -> theme.accentColor
            "themeBackgroundColor" -> theme.backgroundColor
            "themeTextColor" -> theme.textColor
            "themeHeadingFont" -> theme.headingFont
            "layoutType" -> layout.layoutType.name
            else -> defaultValue
        }
    }
}

sealed class ConfigResolutionError {
    abstract val domain: String
    abstract val message: String

    data class MissingRequiredField(
        override val domain: String,
        val field: String,
        override val message: String
    ) : ConfigResolutionError()

    data class InvalidValue(
        override val domain: String,
        val field: String,
        val value: String,
        override val message: String
    ) : ConfigResolutionError()

    data class DomainFailure(
        override val domain: String,
        override val message: String,
        val cause: Exception? = null
    ) : ConfigResolutionError()
}