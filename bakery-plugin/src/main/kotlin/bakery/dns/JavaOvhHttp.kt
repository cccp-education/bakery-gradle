package bakery.dns

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Production OVH HTTP transport (EPIC BKY-DNS).
 *
 * Real adapter for [OvhHttp]: signs every request via [OvhSignature] and
 * sets the X-Ovh-Application / X-Ovh-Consumer / X-Ovh-Timestamp /
 * X-Ovh-Signature headers. Uses [java.net.http.HttpClient].
 */
class JavaOvhHttp(
    private val credentials: OvhCredentials,
    private val client: HttpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build(),
) : OvhHttp {

    override fun call(method: String, url: String, body: String?): OvhHttpResponse {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val signature =
            OvhSignature.sign(
                applicationSecret = credentials.applicationSecret,
                consumerKey = credentials.consumerKey,
                method = method,
                url = url,
                body = body ?: "",
                timestamp = timestamp,
            )

        val request =
            HttpRequest.newBuilder(URI.create(url))
                .method(method, bodyPublisher(body))
                .header("X-Ovh-Application", credentials.applicationKey)
                .header("X-Ovh-Consumer", credentials.consumerKey)
                .header("X-Ovh-Timestamp", timestamp)
                .header("X-Ovh-Signature", signature)
                .header("Content-Type", "application/json")
                .build()

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            OvhHttpResponse(response.statusCode(), response.body())
        } catch (e: Exception) {
            throw OvhDnsException("OVH request failed: $method $url — ${e.message}")
        }
    }

    private fun bodyPublisher(body: String?): HttpRequest.BodyPublisher =
        if (body == null) {
            HttpRequest.BodyPublishers.noBody()
        } else {
            HttpRequest.BodyPublishers.ofString(body)
        }
}
