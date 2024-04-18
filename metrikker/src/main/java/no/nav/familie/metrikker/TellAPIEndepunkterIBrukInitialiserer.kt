package no.nav.familie.metrikker

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@Component
@ConditionalOnProperty("familie.tellAPIEndepunkterIBruk")
class TellAPIEndepunkterIBrukInitialiserer(
    @Value("\${NAIS_APP_NAME}") private val applicationName: String,
    private val applicationContext: ApplicationContext,
) {
    init {
        metrikker.clear()
    }

    @PostConstruct
    fun populerMapMedCountersForRestEndepunkt() {
        val requestMappingHandlerMapping: RequestMappingHandlerMapping =
            applicationContext
                .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping::class.java)
        val requestMappings: Map<RequestMappingInfo, HandlerMethod> = requestMappingHandlerMapping.handlerMethods

        requestMappings.forEach { (info, handler) ->
            info.patternValues.forEach { path ->
                if (path.startsWith("/api")) {
                    val metrikknavn = "$applicationName.${info.methodsCondition}$path".tilMetrikknavn()
                    val key = "${info.methodsCondition}$path"
                    metrikker.put(
                        key,
                        Metrics.counter(
                            metrikknavn,
                            METRIKK_TAG_TYPE,
                            "endepunkt",
                            METRIKK_PATH_TYPE,
                            path,
                            METRIKK_REQUEST_METODE_TYPE,
                            "${info.methodsCondition}",
                        ),
                    )
                }
            }
        }
    }

    private fun String.tilMetrikknavn() =
        this
            .replace("[", "")
            .replace("]", "")
            .replace("{", "")
            .replace("}", "")
            .replace("/", ".")
            .replace("_", ".")

    companion object {
        private val metrikker = mutableMapOf<String, Counter>()
        val metrikkerForEndepunkter
            get() = metrikker.toMap()

        private const val METRIKK_TAG_TYPE = "type"
        private const val METRIKK_PATH_TYPE = "path"
        private const val METRIKK_REQUEST_METODE_TYPE = "requestMetode"
    }
}
