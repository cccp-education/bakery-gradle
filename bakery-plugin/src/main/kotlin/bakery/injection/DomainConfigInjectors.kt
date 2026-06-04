package bakery.injection

val configInjectors: Map<String, ConfigInjector> = mapOf(
    "firebase" to FirebaseInjector,
    "googleForms" to GoogleFormsInjector,
    "firebaseAuth" to FirebaseAuthInjector,
    "comments" to CommentsInjector,
    "analytics" to AnalyticsInjector,
    "newsletter" to NewsletterInjector,
    "theme" to ThemeInjector,
    "layout" to LayoutInjector
)

object FirebaseInjector : ConfigInjector {
    override fun inject(lines: MutableList<String>, resolver: (String, String) -> String) {
        val apiKey = resolver("firebaseApiKey", "")
        if (apiKey.isNotBlank()) {
            updateProperty(lines, "firebaseApiKey", apiKey)
            updateProperty(lines, "firebaseProjectId", resolver("firebaseProjectId", ""))
        }
    }
}

object GoogleFormsInjector : ConfigInjector {
    override fun inject(lines: MutableList<String>, resolver: (String, String) -> String) {
        val formId = resolver("googleFormsFormId", "")
        if (formId.isNotBlank()) {
            updateProperty(lines, "googleFormsFormId", formId)
            updateProperty(lines, "googleFormsWidth", resolver("googleFormsWidth", "640"))
            updateProperty(lines, "googleFormsHeight", resolver("googleFormsHeight", "800"))
        }
    }
}

object FirebaseAuthInjector : ConfigInjector {
    override fun inject(lines: MutableList<String>, resolver: (String, String) -> String) {
        val apiKey = resolver("firebaseAuthApiKey", "")
        if (apiKey.isNotBlank()) {
            updateProperty(lines, "firebaseAuthApiKey", apiKey)
            updateProperty(lines, "firebaseAuthDomain", resolver("firebaseAuthDomain", ""))
            updateProperty(lines, "firebaseAuthProjectId", resolver("firebaseAuthProjectId", ""))
        }
    }
}

object CommentsInjector : ConfigInjector {
    override fun inject(lines: MutableList<String>, resolver: (String, String) -> String) {
        val enabled = resolver("commentsEnabled", "false")
        if (enabled != "false" || resolver("commentsCollection", "comments") != "comments") {
            updateProperty(lines, "commentsEnabled", enabled)
            updateProperty(lines, "commentsCollection", resolver("commentsCollection", "comments"))
        }
    }
}

object AnalyticsInjector : ConfigInjector {
    override fun inject(lines: MutableList<String>, resolver: (String, String) -> String) {
        val provider = resolver("analyticsProvider", "")
        if (provider.isNotBlank()) {
            updateProperty(lines, "analyticsProvider", provider)
            updateProperty(lines, "analyticsDomain", resolver("analyticsDomain", ""))
            updateProperty(lines, "analyticsScriptSrc", resolver("analyticsScriptSrc", ""))
        }
    }
}

object NewsletterInjector : ConfigInjector {
    override fun inject(lines: MutableList<String>, resolver: (String, String) -> String) {
        val enabled = resolver("newsletterEnabled", "false")
        if (enabled != "false" || resolver("newsletterProvider", "").isNotBlank()) {
            updateProperty(lines, "newsletterEnabled", enabled)
            updateProperty(lines, "newsletterProvider", resolver("newsletterProvider", ""))
            updateProperty(lines, "newsletterEndpoint", resolver("newsletterEndpoint", ""))
        }
    }
}

object ThemeInjector : ConfigInjector {
    override fun inject(lines: MutableList<String>, resolver: (String, String) -> String) {
        updateProperty(lines, "themeMode", resolver("themeMode", "auto"))
        updateProperty(lines, "themePrimaryColor", resolver("themePrimaryColor", "#0d6efd"))
        updateProperty(lines, "themeSecondaryColor", resolver("themeSecondaryColor", "#6c757d"))
        updateProperty(lines, "themeFontFamily", resolver("themeFontFamily", ""))
        updateProperty(lines, "themeLogoUrl", resolver("themeLogoUrl", ""))
        updateProperty(lines, "themeFaviconUrl", resolver("themeFaviconUrl", ""))
        updateProperty(lines, "themeVariant", resolver("themeVariant", ""))
        updateProperty(lines, "themeAccentColor", resolver("themeAccentColor", ""))
        updateProperty(lines, "themeBackgroundColor", resolver("themeBackgroundColor", ""))
        updateProperty(lines, "themeTextColor", resolver("themeTextColor", ""))
        updateProperty(lines, "themeHeadingFont", resolver("themeHeadingFont", ""))
    }
}

object LayoutInjector : ConfigInjector {
    override fun inject(lines: MutableList<String>, resolver: (String, String) -> String) {
        updateProperty(lines, "layoutType", resolver("layoutType", "FULL_WIDTH"))
    }
}
