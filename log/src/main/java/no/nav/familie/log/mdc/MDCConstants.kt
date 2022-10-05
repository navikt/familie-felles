package no.nav.familie.log.mdc

object MDCConstants {
    const val MDC_USER_ID = "userId"
    const val MDC_CONSUMER_ID = "consumerId"
    const val MDC_CALL_ID = "callId"

    /**
     * trengs for å spore kall som går via nginx/controller - den bruker underscore
     **/
    const val MDC_REQUEST_ID = "request_Id"
}
