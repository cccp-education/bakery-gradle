package bakery.dns

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * OVH DNS API client (EPIC BKY-DNS).
 *
 * Domain-level client: knows the endpoints and maps [DnsRecord] to/from
 * the OVH wire format. The transport ([OvhHttp]) and credentials are
 * injected — zero real I/O here, fully unit-testable. The production
 * transport [JavaOvhHttp] computes the HMAC signature and sets the
 * X-Ovh-* headers.
 */
class OvhDnsClient(
    private val credentials: OvhCredentials,
    private val http: OvhHttp,
    private val baseUrl: String = OvhEndpoint.BASE_URL,
) {
    private val json = jacksonObjectMapper()

    /** Lists the ids of the records in the zone, optionally filtered by type. */
    fun listRecordIds(domain: String, fieldType: String? = null): List<Long> {
        val query = if (fieldType.isNullOrBlank()) "" else "?fieldType=$fieldType"
        val response = call("GET", OvhEndpoint.recordListPath(domain) + query, null, "list records")
        return try {
            json.readValue(response.body, object : TypeReference<List<Long>>() {})
        } catch (e: Exception) {
            throw OvhDnsException(
                "OVH list records: cannot parse ids from '${response.body}'",
                response.status,
                response.body,
            )
        }
    }

    /** Fetches a single record and maps it back to a domain [DnsRecord]. */
    fun getRecord(domain: String, id: Long): DnsRecord {
        val response = call("GET", OvhEndpoint.recordPath(domain, id), null, "get record")
        return parseRecord(response, "get record").toDnsRecord()
    }

    /** Creates a record and returns the new record id. */
    fun createRecord(domain: String, record: DnsRecord): Long {
        val body = json.writeValueAsString(OvhRecord.fromDnsRecord(record))
        val response = call("POST", OvhEndpoint.recordListPath(domain), body, "create record")
        val wire = parseRecord(response, "create record")
        return wire.id
            ?: throw OvhDnsException(
                "OVH create record: response carried no record id",
                response.status,
                response.body,
            )
    }

    /** Updates an existing record (by id) to the desired [DnsRecord]. */
    fun updateRecord(domain: String, id: Long, record: DnsRecord) {
        val body = json.writeValueAsString(OvhRecord.fromDnsRecord(record))
        call("PUT", OvhEndpoint.recordPath(domain, id), body, "update record")
    }

    /** Deletes an existing record (by id). */
    fun deleteRecord(domain: String, id: Long) {
        call("DELETE", OvhEndpoint.recordPath(domain, id), null, "delete record")
    }

    /** Triggers an OVH zone refresh so pending changes propagate. */
    fun refreshZone(domain: String) {
        call("POST", OvhEndpoint.refreshPath(domain), "{}", "refresh zone")
    }

    private fun call(method: String, path: String, body: String?, action: String): OvhHttpResponse {
        val response = http.call(method, baseUrl + path, body)
        if (response.status !in 200..299) {
            throw OvhDnsException(
                "OVH $action failed: HTTP ${response.status} — ${response.body}",
                response.status,
                response.body,
            )
        }
        return response
    }

    private fun parseRecord(response: OvhHttpResponse, action: String): OvhRecord =
        try {
            json.readValue<OvhRecord>(response.body)
        } catch (e: Exception) {
            throw OvhDnsException(
                "OVH $action: cannot parse record from '${response.body}'",
                response.status,
                response.body,
            )
        }
}
