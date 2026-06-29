package bakery.i18n

import java.io.File

class ContentCopyService {

    fun copy(
        sourceDir: File,
        outputBaseDir: File,
        targetLanguages: List<String>,
        dryRun: Boolean = false,
        excludeRelativePaths: Set<String> = emptySet()
    ): ContentCopyResult {
        val copied = mutableListOf<String>()
        val skipped = mutableListOf<String>()

        sourceDir.walkTopDown().forEach { file ->
            val relativePath = file.relativeTo(sourceDir).path

            if (relativePath in excludeRelativePaths) return@forEach
            if (excludeRelativePaths.any { relativePath.startsWith("$it/") }) return@forEach

            if (file.isDirectory) {
                for (lang in targetLanguages) {
                    if (!dryRun) {
                        outputBaseDir.resolve(lang).resolve(relativePath).mkdirs()
                    }
                }
                return@forEach
            }

            for (lang in targetLanguages) {
                val target = outputBaseDir.resolve(lang).resolve(relativePath)
                val dirsToCheck = generateSequence(relativePath) { path ->
                    path.lastIndexOf('/').let { if (it > 0) path.substring(0, it) else null }
                }.toSet()
                if (excludeRelativePaths.any { it in dirsToCheck }) {
                    skipped.add("$lang/$relativePath")
                    return@forEach
                }
                if (dryRun) {
                    skipped.add("$lang/$relativePath")
                } else {
                    target.parentFile.mkdirs()
                    file.copyTo(target, overwrite = true)
                    copied.add("$lang/$relativePath")
                }
            }
        }

        return ContentCopyResult(copied, skipped)
    }
}

data class ContentCopyResult(
    val filesCopied: List<String>,
    val filesSkipped: List<String>
) {
    val totalFiles: Int get() = filesCopied.size + filesSkipped.size
}
