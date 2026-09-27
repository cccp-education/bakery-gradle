package bakery.workspace

import java.io.File

/**
 * BKY-CI-ISOLATION — single seam resolving the local workspace root.
 *
 * Several contract guards must read resources *outside* this repository (the real
 * JBake sites under `office/sites/`, the sibling `workspace-bom` catalog). An
 * isolated checkout (CI, fresh clone) has none of them. Hardcoding
 * `/home/cheroliv/workspace` made those guards throw instead of skip.
 *
 * Precedence: `-Dbakery.workspaceRoot` > `WORKSPACE_ROOT` env > local default.
 * Blank overrides are ignored so an empty CI variable never points at `/`.
 * Accessors degrade to `null` when the resource is absent — the caller decides
 * (typically `assumeTrue`) rather than the resolver throwing.
 */
object WorkspaceRoot {
    const val PROPERTY = "bakery.workspaceRoot"
    const val ENVIRONMENT = "WORKSPACE_ROOT"
    const val DEFAULT = "/home/cheroliv/workspace"

    fun resolve(
        property: () -> String? = { System.getProperty(PROPERTY) },
        environment: () -> String? = { System.getenv(ENVIRONMENT) },
        defaultRoot: String = DEFAULT,
    ): File {
        val chosen =
            property().takeIf { !it.isNullOrBlank() }
                ?: environment().takeIf { !it.isNullOrBlank() }
                ?: defaultRoot
        return File(chosen)
    }

    fun siteJbake(
        root: File,
        siteDirName: String,
    ): File? = root.resolve("office/sites/$siteDirName/jbake").takeIf { it.isDirectory }

    fun siteTemplates(
        root: File,
        siteDirName: String,
    ): File? = siteJbake(root, siteDirName)?.resolve("templates")?.takeIf { it.isDirectory }
}
