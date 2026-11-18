package bakery.netlify

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/**
 * Persists the last successful deployment so a re-run can skip the upload
 * without any network call (D7).
 *
 * A missing or unreadable file is not an error: it degrades to "no previous
 * deployment", which simply means the next run uploads.
 */
object NetlifyDeploymentRecordStore {
    private val mapper = jacksonObjectMapper()

    fun read(file: File): NetlifyDeploymentRecord? {
        if (!file.isFile) return null
        return runCatching { mapper.readValue<NetlifyDeploymentRecord>(file) }.getOrNull()
    }

    fun write(
        file: File,
        record: NetlifyDeploymentRecord,
    ) {
        file.parentFile?.mkdirs()
        file.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(record))
    }
}
