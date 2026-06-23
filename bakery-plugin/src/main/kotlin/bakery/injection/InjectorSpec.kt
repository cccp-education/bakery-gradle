package bakery.injection

sealed class InjectorSpec {

    abstract val domain: String

    protected fun MutableList<String>.injectProp(key: String, value: String) {
        updateProperty(this, key, value)
    }

    abstract fun inject(lines: MutableList<String>, resolver: (String, String) -> String)

    data class GateOnTriggerField(
        override val domain: String,
        private val triggerKey: String,
        private val properties: List<PropSpec>
    ) : InjectorSpec() {
        override fun inject(lines: MutableList<String>, resolver: (String, String) -> String) {
            val trigger = resolver(triggerKey, "")
            if (trigger.isBlank()) return
            properties.forEach { spec ->
                lines.injectProp(spec.writeKey, resolver(spec.readKey, spec.default))
            }
        }
    }

    data class GateOnBoolean(
        override val domain: String,
        private val booleanKey: String,
        private val booleanDefault: String,
        private val booleanGate: (resolved: String, resolver: (String, String) -> String) -> Boolean,
        private val properties: List<PropSpec>
    ) : InjectorSpec() {
        override fun inject(lines: MutableList<String>, resolver: (String, String) -> String) {
            val resolved = resolver(booleanKey, booleanDefault)
            if (!booleanGate(resolved, resolver)) return
            properties.forEach { spec ->
                lines.injectProp(spec.writeKey, resolver(spec.readKey, spec.default))
            }
        }
    }

    data class Always(
        override val domain: String,
        private val properties: List<PropSpec>
    ) : InjectorSpec() {
        override fun inject(lines: MutableList<String>, resolver: (String, String) -> String) {
            properties.forEach { spec ->
                lines.injectProp(spec.writeKey, resolver(spec.readKey, spec.default))
            }
        }
    }
}

data class PropSpec(val readKey: String, val default: String, val writeKey: String = readKey)

object InjectorRegistry {

    val all: Map<String, InjectorSpec> = listOf(
        InjectorSpec.GateOnTriggerField(
            domain = "firebase",
            triggerKey = "firebaseApiKey",
            properties = listOf(
                PropSpec("firebaseApiKey", ""),
                PropSpec("firebaseProjectId", "")
            )
        ),
        InjectorSpec.GateOnTriggerField(
            domain = "googleForms",
            triggerKey = "googleFormsFormId",
            properties = listOf(
                PropSpec("googleFormsFormId", ""),
                PropSpec("googleFormsWidth", "640"),
                PropSpec("googleFormsHeight", "800")
            )
        ),
        InjectorSpec.GateOnTriggerField(
            domain = "firebaseAuth",
            triggerKey = "firebaseAuthApiKey",
            properties = listOf(
                PropSpec("firebaseAuthApiKey", ""),
                PropSpec("firebaseAuthDomain", ""),
                PropSpec("firebaseAuthProjectId", "")
            )
        ),
        InjectorSpec.GateOnTriggerField(
            domain = "analytics",
            triggerKey = "analyticsProvider",
            properties = listOf(
                PropSpec("analyticsProvider", ""),
                PropSpec("analyticsDomain", ""),
                PropSpec("analyticsScriptSrc", "")
            )
        ),
        InjectorSpec.GateOnBoolean(
            domain = "comments",
            booleanKey = "commentsEnabled",
            booleanDefault = "false",
            booleanGate = { resolved, resolver ->
                resolved != "false" || resolver("commentsCollection", "comments") != "comments"
            },
            properties = listOf(
                PropSpec("commentsEnabled", "false"),
                PropSpec("commentsCollection", "comments")
            )
        ),
        InjectorSpec.GateOnBoolean(
            domain = "newsletter",
            booleanKey = "newsletterEnabled",
            booleanDefault = "false",
            booleanGate = { resolved, resolver ->
                resolved != "false" || resolver("newsletterProvider", "").isNotBlank()
            },
            properties = listOf(
                PropSpec("newsletterEnabled", "false"),
                PropSpec("newsletterProvider", ""),
                PropSpec("newsletterEndpoint", "")
            )
        ),
        InjectorSpec.Always(
            domain = "theme",
            properties = listOf(
                PropSpec("themeMode", "auto"),
                PropSpec("themePrimaryColor", "#0d6efd"),
                PropSpec("themeSecondaryColor", "#6c757d"),
                PropSpec("themeFontFamily", ""),
                PropSpec("themeLogoUrl", ""),
                PropSpec("themeFaviconUrl", ""),
                PropSpec("themeVariant", ""),
                PropSpec("themeAccentColor", ""),
                PropSpec("themeBackgroundColor", ""),
                PropSpec("themeTextColor", ""),
                PropSpec("themeHeadingFont", "")
            )
        ),
        InjectorSpec.Always(
            domain = "layout",
            properties = listOf(PropSpec("layoutType", "FULL_WIDTH"))
        ),
        InjectorSpec.Always(
            domain = "language",
            properties = listOf(PropSpec("language", "fr", "site.language"))
        )
    ).associateBy { it.domain }
}