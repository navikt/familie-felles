package no.nav.familie.log

enum class NavHttpHeaders(
    private val header: String,
) {
    NAV_PERSONIDENT("Nav-Personident"),
    NAV_CALL_ID("Nav-Call-Id"),
    NGNINX_REQUEST_ID("X-Request-Id"),
    NAV_CONSUMER_ID("Nav-Consumer-Id"),
    NAV_USER_ID("Nav-User-Id"),
    ;

    fun asString(): String = header
}
