package contracts.pipeline

import java.io.File

interface ReleaseNotesGenerator {
    val parser: GitLogParser
    val renderer: ReleaseNotesRenderer

    fun generate(config: ReleaseNotesConfig): File
}
