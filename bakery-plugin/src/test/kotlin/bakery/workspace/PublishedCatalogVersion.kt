package bakery.workspace

/**
 * BKY-CI-ISOLATION (US-2c) — resolves a version of the *published* workspace
 * catalog, injected by Gradle (`ws.versions.*` → `systemProperty`).
 *
 * The publication hygiene guard must never read a neighbour repository's working
 * tree (`../workspace-bom/…`): that is racy between sessions and absent from an
 * isolated checkout — the same failure mode graphify-gradle's D5-RACE EPIC fixed
 * (S-029). A missing injected property is an **explicit error**, never a silent
 * green.
 */
object PublishedCatalogVersion {
    fun require(
        property: String,
        lookup: (String) -> String? = System::getProperty,
    ): String =
        lookup(property)?.takeIf { it.isNotBlank() }
            ?: error(
                "propriété '$property' absente — la version du catalog publié doit être injectée " +
                    "par Gradle (systemProperty dans build.gradle.kts), jamais lue d'un dépôt voisin",
            )
}
