package bakery.i18n.variant

/**
 * BKY-LANG-NAV-8 — the variants a full rollout must bake.
 *
 * The reference language is baked at the root by the normal `bake` path; only
 * non-default, **deployable** variants are scheduled. An undeployable variant is
 * skipped, never baked (S-049) — the partial corpus stays deployable without
 * the defective language.
 *
 * Pure domain: no I/O, no Gradle, no JBake.
 */
object VariantBakePlan {
    fun languagesToBake(
        layout: VariantLayout,
        supportedLanguages: List<String>,
        referenceLanguage: String,
    ): List<String> {
        val candidates = resolveCandidates(layout, supportedLanguages, referenceLanguage)
        val undeployable =
            VariantDeployability
                .undeployableVariants(layout, candidates, referenceLanguage)
                .keys

        return candidates
            .filter { it != referenceLanguage }
            .filter { layout.variant(it).exists() }
            .filterNot { it in undeployable }
    }

    /**
     * The languages to consider. When `site.yml` declares only the reference,
     * the i18n tree itself is the source of truth ([VariantLayout.discoverLanguages]):
     * a site that ships `jbake/i18n/{lang}/` wants those languages baked, even
     * without an i18n section in its (often secret) config.
     */
    private fun resolveCandidates(
        layout: VariantLayout,
        supportedLanguages: List<String>,
        referenceLanguage: String,
    ): List<String> {
        val configured = supportedLanguages.filter { it != referenceLanguage }
        return if (configured.isEmpty()) {
            layout.discoverLanguages(referenceLanguage)
        } else {
            supportedLanguages
        }
    }
}
