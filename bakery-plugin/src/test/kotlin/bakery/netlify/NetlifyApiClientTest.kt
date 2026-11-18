package bakery.netlify

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-NETLIFY (US-4) — the API client is tested against a fake transport:
 * no network, explicit error mapping, bounded polling.
 */
class NetlifyApiClientTest {
    private class FakeHttp(
        private val postResponse: NetlifyHttpResponse,
        private val getResponses: List<NetlifyHttpResponse> = emptyList(),
    ) : NetlifyHttp {
        val postedUrls = mutableListOf<String>()
        var getCount: Int = 0

        override fun postZip(
            url: String,
            token: String,
            zip: ByteArray,
        ): NetlifyHttpResponse {
            postedUrls += url
            return postResponse
        }

        override fun get(
            url: String,
            token: String,
        ): NetlifyHttpResponse {
            val response = getResponses.getOrElse(getCount) { getResponses.last() }
            getCount++
            return response
        }
    }

    private fun client(
        http: NetlifyHttp,
        attempts: Int = 3,
    ) = NetlifyApiClient(
        http = http,
        token = "secret-token",
        baseUrl = "https://api.test/v1",
        pollAttempts = attempts,
        pollDelayMillis = 0,
        sleeper = {},
    )

    @Test
    @DisplayName("creates a deploy and prefers the ssl URL")
    fun createsDeploy() {
        val http =
            FakeHttp(
                postResponse =
                    NetlifyHttpResponse(
                        201,
                        """{"id":"dep-1","state":"uploading","deploy_ssl_url":"https://dep-1--site.netlify.app"}""",
                    ),
            )

        val result = client(http).createDeploy("site-1", byteArrayOf(1, 2))

        assertThat(http.postedUrls).containsExactly("https://api.test/v1/sites/site-1/deploys")
        assertThat(result).isInstanceOf(NetlifyApiResult.Success::class.java)
        val deploy = (result as NetlifyApiResult.Success).deploy
        assertThat(deploy.id).isEqualTo("dep-1")
        assertThat(deploy.url).isEqualTo("https://dep-1--site.netlify.app")
    }

    @Test
    @DisplayName("maps 401 to an explicit authentication failure")
    fun mapsUnauthorized() {
        val result = client(FakeHttp(NetlifyHttpResponse(401, "nope"))).createDeploy("site", byteArrayOf())

        assertThat(result).isInstanceOf(NetlifyApiResult.Failure::class.java)
        val failure = result as NetlifyApiResult.Failure
        assertThat(failure.status).isEqualTo(401)
        assertThat(failure.message).contains("authentication")
        assertThat(failure.message).doesNotContain("secret-token")
    }

    @Test
    @DisplayName("maps 404 to an explicit site/deploy failure")
    fun mapsNotFound() {
        val result = client(FakeHttp(NetlifyHttpResponse(404, "{}"))).createDeploy("missing", byteArrayOf())

        assertThat((result as NetlifyApiResult.Failure).message).contains("not found")
    }

    @Test
    @DisplayName("polls until the deploy is ready")
    fun pollsUntilReady() {
        val http =
            FakeHttp(
                postResponse = NetlifyHttpResponse(201, """{"id":"dep-2","state":"uploading","url":"https://deploy.test"}"""),
                getResponses =
                    listOf(
                        NetlifyHttpResponse(200, """{"id":"dep-2","state":"processing","url":"https://deploy.test"}"""),
                        NetlifyHttpResponse(200, """{"id":"dep-2","state":"ready","ssl_url":"https://site.netlify.app"}"""),
                    ),
            )

        val result = client(http).deploy("site", byteArrayOf())

        assertThat(http.getCount).isEqualTo(2)
        assertThat((result as NetlifyApiResult.Success).deploy.state).isEqualTo("ready")
        assertThat(result.deploy.url).isEqualTo("https://site.netlify.app")
    }

    @Test
    @DisplayName("an error state fails with Netlify's message")
    fun errorState() {
        val http =
            FakeHttp(
                postResponse = NetlifyHttpResponse(201, """{"id":"dep-3","state":"error","error_message":"invalid zip"}"""),
            )

        val result = client(http).deploy("site", byteArrayOf())

        assertThat(result).isInstanceOf(NetlifyApiResult.Failure::class.java)
        assertThat((result as NetlifyApiResult.Failure).message).contains("invalid zip")
    }

    @Test
    @DisplayName("polling is bounded")
    fun pollingIsBounded() {
        val http =
            FakeHttp(
                postResponse = NetlifyHttpResponse(201, """{"id":"dep-4","state":"uploading"}"""),
                getResponses = listOf(NetlifyHttpResponse(200, """{"id":"dep-4","state":"processing"}""")),
            )

        val result = client(http, attempts = 2).deploy("site", byteArrayOf())

        assertThat(result).isInstanceOf(NetlifyApiResult.Failure::class.java)
        assertThat((result as NetlifyApiResult.Failure).message).contains("did not become ready")
    }
}
