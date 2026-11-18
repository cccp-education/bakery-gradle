package bakery.netlify

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Production adapter for [NetlifyHttp] (US-4, D14).
 *
 * No new dependency: `java.net.http.HttpClient` is the same JDK transport
 * already used by the OVH DNS adapter. The token travels in the Authorization
 * header and is never logged by this class.
 */
class JavaNetlifyHttp(
    private val client: HttpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build(),
) : NetlifyHttp {
    override fun postZip(
        url: String,
        token: String,
        zip: ByteArray,
    ): NetlifyHttpResponse {
        val request =
            HttpRequest
                .newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "application/zip")
                .POST(HttpRequest.BodyPublishers.ofByteArray(zip))
        if (token.isNotBlank()) request.header("Authorization", "Bearer $token")
        return send(request.build())
    }

    override fun get(
        url: String,
        token: String,
    ): NetlifyHttpResponse {
        val request =
            HttpRequest
                .newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .GET()
        if (token.isNotBlank()) request.header("Authorization", "Bearer $token")
        return send(request.build())
    }

    private fun send(request: HttpRequest): NetlifyHttpResponse {
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return NetlifyHttpResponse(response.statusCode(), response.body() ?: "")
    }
}
