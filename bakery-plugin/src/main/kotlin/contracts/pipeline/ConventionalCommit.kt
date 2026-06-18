package contracts.pipeline

data class ConventionalCommit(
    val type: String,
    val scope: String?,
    val message: String,
    val hash: String,
    val date: String
)
