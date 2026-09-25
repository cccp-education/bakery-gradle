package bakery.i18n.variant

import java.io.File

/**
 * BKY-LANG-NAV-8 — bake every deployable variant into a `{lang}/` sub-directory.
 *
 * The reference language is baked at the root by the normal `bake` path. Each
 * non-default deployable variant is baked from a throwaway assembled root
 * ([VariantBakeAssembler]) into `{dest}/{lang}/`, then the throwaway root is
 * removed. The output sub-directory is **wiped first**: `bake` never cleans its
 * output (S-049), so a stale page would shadow a real one.
 *
 * The actual bake is an injected `(sourceRoot, outputDir) -> Unit` — the domain
 * stays jbake-free, and the orchestration is unit-testable with a fake bake.
 */
class VariantBaker(
    private val layout: VariantLayout,
    private val bake: (sourceRoot: File, outputDir: File) -> Unit,
) {
    fun bakeAll(
        languages: List<String>,
        dest: File,
    ): List<String> {
        if (languages.isEmpty()) return emptyList()

        val assembledRoot = dest.parentFile.resolve(ASSEMBLED_DIR)
        val assembler = VariantBakeAssembler(layout)

        val baked = mutableListOf<String>()
        for (language in languages) {
            val assembled = assembledRoot.resolve(language)
            assembler.assemble(language, assembled)
            val outputDir = dest.resolve(language)
            outputDir.deleteRecursively()
            outputDir.mkdirs()
            bake(assembled, outputDir)
            baked += language
        }
        assembledRoot.deleteRecursively()
        return baked
    }

    private companion object {
        const val ASSEMBLED_DIR = "i18n-assembled"
    }
}
