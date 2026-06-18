package contracts.pipeline

import java.io.File

interface ReleaseNotesRenderer {
    val format: String

    fun render(commits: List<ConventionalCommit>, config: ReleaseNotesConfig): String
    fun renderToFile(commits: List<ConventionalCommit>, config: ReleaseNotesConfig, outputFile: File): File
}
