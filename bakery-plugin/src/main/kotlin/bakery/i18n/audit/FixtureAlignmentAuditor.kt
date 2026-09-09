package bakery.i18n.audit

import java.io.File

/**
 * Service métier pur (DDD) : compare une fixture de test i18n avec le site réel correspondant.
 *
 * Objectif : détecter les dérives entre les fixtures versionnées (src/test/resources/i18n-fixtures)
 * et les templates JBake réels (office/sites/<site>/jbake/templates).
 *
 * Métriques produites :
 * - templates manquants dans la fixture (présents sur le site réel, absents de la fixture)
 * - templates en trop dans la fixture (absents du site réel, présents dans la fixture)
 * - templates communs avec contenu différent
 */
class FixtureAlignmentAuditor {
    data class AlignmentReport(
        val fixtureDir: File,
        val realSiteDir: File,
        val missingInFixture: List<String>,
        val extraInFixture: List<String>,
        val mismatchedContent: List<TemplateMismatch>,
    ) {
        val isAligned: Boolean
            get() = missingInFixture.isEmpty() && extraInFixture.isEmpty() && mismatchedContent.isEmpty()
    }

    data class TemplateMismatch(
        val templateName: String,
        val fixtureSha256: String,
        val realSiteSha256: String,
    )

    fun audit(
        fixtureTemplatesDir: File,
        realSiteTemplatesDir: File,
    ): AlignmentReport {
        require(fixtureTemplatesDir.isDirectory) { "fixtureTemplatesDir must be a directory: $fixtureTemplatesDir" }
        require(realSiteTemplatesDir.isDirectory) { "realSiteTemplatesDir must be a directory: $realSiteTemplatesDir" }

        val fixtureTemplates = listTemplateFiles(fixtureTemplatesDir)
        val realSiteTemplates = listTemplateFiles(realSiteTemplatesDir)

        val fixtureRelativeNames = fixtureTemplates.map { it.toRelativeString(fixtureTemplatesDir) }.toSortedSet()
        val realSiteRelativeNames = realSiteTemplates.map { it.toRelativeString(realSiteTemplatesDir) }.toSortedSet()

        val missingInFixture = (realSiteRelativeNames - fixtureRelativeNames).toList()
        val extraInFixture = (fixtureRelativeNames - realSiteRelativeNames).toList()

        val commonNames = fixtureRelativeNames.intersect(realSiteRelativeNames)
        val mismatchedContent =
            commonNames.mapNotNull { relativeName ->
                val fixtureFile = fixtureTemplatesDir.resolve(relativeName)
                val realSiteFile = realSiteTemplatesDir.resolve(relativeName)
                val fixtureHash = sha256(fixtureFile.readText().normalizeEol().neutralizeSecrets())
                val realSiteHash = sha256(realSiteFile.readText().normalizeEol().neutralizeSecrets())
                if (fixtureHash != realSiteHash) {
                    TemplateMismatch(relativeName, fixtureHash, realSiteHash)
                } else {
                    null
                }
            }

        return AlignmentReport(
            fixtureDir = fixtureTemplatesDir,
            realSiteDir = realSiteTemplatesDir,
            missingInFixture = missingInFixture,
            extraInFixture = extraInFixture,
            mismatchedContent = mismatchedContent,
        )
    }

    private fun listTemplateFiles(dir: File): List<File> =
        dir
            .walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .sortedBy { it.name }
            .toList()

    private fun String.normalizeEol(): String = this.replace("\r\n", "\n")

    /**
     * Replaces injected secrets (Firebase API keys, app IDs) with canonical
     * placeholders so that a fixture carrying placeholders aligns with the
     * real site carrying production values. Only the placeholder divergence
     * is ignored — any other divergence is still reported.
     */
    private fun String.neutralizeSecrets(): String =
        this
            .replace(Regex("apiKey:\\s*\"[^\"]+\""), "apiKey: \"$SECRET_PLACEHOLDER\"")
            .replace(Regex("appId:\\s*\"[^\"]+\""), "appId: \"$SECRET_PLACEHOLDER\"")

    private companion object {
        const val SECRET_PLACEHOLDER = "REMPLACER_PAR_VOTRE_PLACEHOLDER"
    }

    private fun sha256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
