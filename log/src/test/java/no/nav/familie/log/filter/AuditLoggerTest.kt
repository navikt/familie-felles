package no.nav.familie.log.filter

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.familie.log.auditlogger.AuditLogger
import no.nav.familie.log.auditlogger.AuditLoggerType
import no.nav.familie.log.auditlogger.Sporingsdata
import no.nav.familie.log.auditlogger.SporingsloggId
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.slf4j.LoggerFactory


class AuditLoggerTest {

    @Test
    fun `Skal auditlogge henting av fagsak`() {
        val logger: Logger = LoggerFactory.getLogger("auditLogger" + "." + this::class.java.name) as Logger

        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()

        logger.addAppender(listAppender)
        AuditLogger.log(this.javaClass,
                        Sporingsdata(mapOf(SporingsloggId.ANSVALIG_SAKSBEHANDLER to "ansvarligSaksbehandler")),
                        AuditLoggerType.READ,
                        "FAGSAK")

        val logsList = listAppender.list
        assertThat(logsList.size).isEqualTo(1)
        assertThat(logsList[0]
                           .message).isEqualTo("action=FAGSAK actionType=READ ANSVALIG_SAKSBEHANDLER=ansvarligSaksbehandler"
        )
    }
}
