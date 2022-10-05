package no.nav.familie.log

enum class NavHttpHeaders(private val header: String) {
    NAV_PERSONIDENT("Nav-Personident"),
    NAV_CALL_ID("Nav-Call-Id"),
    NGNINX__REQUEST_ID("X_Request_Id"),
    NAV_CONSUMER_ID("Nav-Consumer-Id"),
    NAV_USER_ID("Nav-User-Id");

    fun asString(): String {
        return header
    }
}
