package no.nav.familie.http.auditlogger

import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import javax.servlet.http.HttpServletRequest

private const val SPACE_SEPARATOR = ' '

object AuditLogger {
    fun log(clazz: Class<Any>, sporingsdata: Sporingsdata, type: AuditLoggerType, action: String) {
        LoggerFactory.getLogger("auditLogger" + "." + clazz.name).info(opprettMelding(sporingsdata, type, action))
    }

    fun logRequest(request: HttpServletRequest, ansvarligSaksbehandler: String) {
        val sporingsdata = Sporingsdata(verdier = mapOf(
                SporingsloggId.ANSVALIG_SAKSBEHANDLER to ansvarligSaksbehandler))

        LoggerFactory.getLogger("auditLogger")
                .info(opprettMelding(sporingsdata, AuditLoggerType.hentType(request.method), request.requestURI.toString()))
    }

    private fun opprettMelding(sporingsdata: Sporingsdata, type: AuditLoggerType, action: String): String {
        val msg: StringBuilder = StringBuilder()
                .append("action=").append(action).append(SPACE_SEPARATOR)
                .append("actionType=").append(type)
                .append(SPACE_SEPARATOR)

        sporingsdata.verdier.map {
            msg.append(it.key).append('=').append(it.value)
                    .append(SPACE_SEPARATOR)
        }

        return msg.toString().replace("([\\r\\n])".toRegex(), "").trim()
    }
}

data class Sporingsdata(
        val verdier: Map<SporingsloggId, String>
)

enum class SporingsloggId {
    ANSVALIG_SAKSBEHANDLER,
}

enum class AuditLoggerType(val httpMethod: HttpMethod) {
    READ(HttpMethod.GET),
    UPDATE(HttpMethod.PUT),
    CREATE(HttpMethod.POST),
    DELETE(HttpMethod.DELETE),
    PATCH(HttpMethod.PATCH);

    companion object {
        fun hentType(method: String): AuditLoggerType {
            return values().find { it.httpMethod.matches(method) } ?: throw IllegalStateException("Ikke godkjent http metode")
        }
    }
}
