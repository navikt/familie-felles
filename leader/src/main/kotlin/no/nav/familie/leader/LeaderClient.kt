package no.nav.familie.leader

import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers

object LeaderClient {

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
        return response.body().contains(hostname)
    }
}
