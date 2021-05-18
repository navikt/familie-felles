package no.nav.familie.log.appender

import ch.qos.logback.core.AppenderBase
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class SecureLoggerRestAppender : AppenderBase<ch.qos.logback.classic.spi.ILoggingEvent>() {

    private val client: HttpClient = HttpClient.newHttpClient()
    private val objectMapper = ObjectMapper()

    override fun append(eventObject: ch.qos.logback.classic.spi.ILoggingEvent) {
        try {
            val logEvent = mutableMapOf<String, String>()
            logEvent["message"] = eventObject.formattedMessage
            logEvent["level"] = eventObject.level.levelStr
            logEvent["thread"] = eventObject.threadName
            val iThrowableProxy = eventObject.throwableProxy

            if (iThrowableProxy != null) {
                val stackTrace = "${iThrowableProxy.className}: ${iThrowableProxy.message}\n" +
                        iThrowableProxy
                            .stackTraceElementProxyArray
                            .joinToString { "${it.steAsString}\n" }

                logEvent["stack_trace"] = stackTrace
            }

            val mdc = eventObject.mdcPropertyMap
            mdc?.keys?.forEach {
                if (!mdc[it].isNullOrEmpty()) {
                    logEvent[it] = mdc[it]!!
                }
            }

            val request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create("http://localhost:19880/"))
                .timeout(Duration.ofSeconds(10))
                .setHeader("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(logEvent)))
                .build()

            val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                println(
                    "[securelogs] ERROR ved posting av melding til secureLog ${response.statusCode()} " +
                            "${response.body()} ${response.headers()}"
                )
            }
        } catch (e: Exception) {
            println("[securelogs] Ukjent feil ved securelogs $e")
        }
    }
}
