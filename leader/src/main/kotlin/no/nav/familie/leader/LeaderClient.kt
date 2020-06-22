package no.nav.familie.leader

import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers


object LeaderClient {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * @return Om pod er leader eller ikke. null hvis leadersjekk ikke er implementert på pod
     */
    @JvmStatic
    fun isLeader(): Boolean? {
        val electorPath = Environment.hentLeaderSystemEnv() ?: return null

        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
                .uri(URI.create("http://$electorPath"))
                .GET()
                .build()

        val response: HttpResponse<String> = client.send(request, BodyHandlers.ofString())
        if (response.body().isNullOrBlank()) return null

        val hostname: String = InetAddress.getLocalHost().hostName
        log.info("Leader hostname $hostname ${response.body()}")
        return response.body().contains(hostname)
    }
}
