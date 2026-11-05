package bakery.toolchain

/**
 * BKY-PLT-3 — the anti-drift guard of the bake serializer toolchain.
 *
 * Root cause (BKY-PLANTUML-D1, GBL-007): `document-plugin` dragged
 * `org.w3c:epubcheck`, which dragged `net.sf.saxon:Saxon-HE:11.4`. PlantUML
 * serialises its SVG through the Saxon on the bake classpath; the 11.x branch
 * emits `xmlns=''` on `<title>`/`<defs>`/`<g>` and the browser paints a white
 * box. `9.9.1-7` is the correct branch. `BKY-PLT-1` excludes `epubcheck` at the
 * dependency source (bakery never runs EPUB validation); this guard prevents the
 * 11.x branch from returning unnoticed through a transitive dependency.
 *
 * The resolved coordinates are injected by Gradle (patron
 * `PublishedCatalogVersion` / graphify D5-RACE): the test never resolves a
 * configuration itself. Pure decision logic, no I/O, no Gradle.
 */
object SaxonToolchainGuard {
    const val SAXON_GROUP = "net.sf.saxon"
    const val SAXON_MODULE = "Saxon-HE"
    const val FORBIDDEN_MAJOR = 11

    data class Artifact(
        val group: String,
        val module: String,
        val version: String,
    ) {
        override fun toString(): String = "$group:$module:$version"
    }

    /** Parses one `group:module:version` coordinate per non-blank line. */
    fun parse(encoded: String): List<Artifact> =
        encoded
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split(':')
                if (parts.size != 3) null else Artifact(parts[0], parts[1], parts[2])
            }.toList()

    /**
     * Reads a Gradle-injected coordinate list. A missing property is an explicit
     * error (broken wiring), never a silent green; an empty list is valid (no
     * Saxon on the classpath at all).
     */
    fun resolvedArtifacts(property: String): List<Artifact> {
        val encoded =
            System.getProperty(property)
                ?: error("system property '$property' is missing — the Gradle wiring is broken")
        return parse(encoded)
    }

    fun isForbiddenSaxon(artifact: Artifact): Boolean =
        artifact.group == SAXON_GROUP &&
            artifact.module == SAXON_MODULE &&
            majorOf(artifact.version) == FORBIDDEN_MAJOR

    fun violations(artifacts: List<Artifact>): List<Artifact> = artifacts.filter(::isForbiddenSaxon)

    internal fun majorOf(version: String): Int? = version.trim().takeWhile { it.isDigit() }.toIntOrNull()
}
