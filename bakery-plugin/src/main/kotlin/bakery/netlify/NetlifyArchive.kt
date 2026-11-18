package bakery.netlify

import bakery.deploy.PayloadFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds the deployment archive (US-5).
 *
 * The archive is deterministic: entries are written in payload order (already
 * sorted) with a fixed timestamp, so the same tree yields the same bytes. That
 * is what makes the digest/archive pair debuggable and the tests stable.
 */
object NetlifyArchive {
    fun build(
        root: File,
        files: List<PayloadFile>,
    ): ByteArray {
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            files.forEach { file ->
                val entry = ZipEntry(file.relativePath).apply { time = 0L }
                zip.putNextEntry(entry)
                root.resolve(file.relativePath).inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return buffer.toByteArray()
    }
}
