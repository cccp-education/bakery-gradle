package contracts.pipeline

data class ReleaseNotesConfig(
    val fromTag: String? = null,
    val toTag: String = "HEAD",
    val categories: Map<String, String> = mapOf(
        "feat" to "Nouveautés",
        "fix" to "Corrections",
        "chore" to "Maintenance",
        "perf" to "Performance",
        "refactor" to "Refactoring",
        "docs" to "Documentation",
        "test" to "Tests"
    ),
    val outputDir: String = "build/release-notes",
    val version: String? = null,
    val rendererType: String = "asciidoc",
    val includeDownloads: Boolean = true
)
