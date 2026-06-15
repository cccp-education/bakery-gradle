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
    val language: String = "fr",
    val supportedLanguages: List<String> = listOf("fr"),
) {
    private val propertyMap: Map<String, String> by lazy {
        mapOf(
            "firebaseApiKey" to firebase.apiKey,
            "firebaseProjectId" to firebase.projectId,
            "googleFormsFormId" to googleForms.formId,
            "googleFormsWidth" to googleForms.width,
            "googleFormsHeight" to googleForms.height,
            "googleFormsAllowMultiple" to googleForms.allowMultiple.toString(),
            "firebaseAuthApiKey" to firebaseAuth.apiKey,
            "firebaseAuthDomain" to firebaseAuth.authDomain,
            "firebaseAuthProjectId" to firebaseAuth.projectId,
            "commentsEnabled" to comments.enabled.toString(),
            "commentsCollection" to comments.collection,
            "analyticsProvider" to analytics.provider,
            "analyticsDomain" to analytics.domain,
            "analyticsScriptSrc" to analytics.scriptSrc,
            "newsletterEnabled" to newsletter.enabled.toString(),
            "newsletterProvider" to newsletter.provider,
            "newsletterEndpoint" to newsletter.endpoint,
            "themeMode" to theme.mode,
            "themePrimaryColor" to theme.primaryColor,
            "themeSecondaryColor" to theme.secondaryColor,
            "themeFontFamily" to theme.fontFamily,
            "themeLogoUrl" to theme.logoUrl,
            "themeFaviconUrl" to theme.faviconUrl,
            "themeVariant" to theme.variant,
            "themeAccentColor" to theme.accentColor,
            "themeBackgroundColor" to theme.backgroundColor,
            "themeTextColor" to theme.textColor,
            "themeHeadingFont" to theme.headingFont,
            "layoutType" to layout.layoutType.name,
            "language" to language,
            "supportedLanguages" to supportedLanguages.joinToString(",")
        )
    }

    fun toResolver(): (String, String) -> String = { key, defaultValue ->
        propertyMap[key] ?: defaultValue
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