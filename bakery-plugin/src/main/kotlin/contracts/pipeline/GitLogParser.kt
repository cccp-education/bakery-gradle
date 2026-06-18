package contracts.pipeline

import java.io.File

interface GitLogParser {
    fun parse(fromTag: String, toTag: String): List<ConventionalCommit>
    fun detectVersion(projectDir: File): String?
    fun detectFromTag(projectDir: File, toTag: String): String?
}
