package no.nav.familie.http.client;

public enum NavHttpHeaders {
    NAV_PERSONIDENT("Nav-Personident"),
    NAV_CALLID("Nav-Call-Id"),
    NAV_CONSUMER_ID("Nav-Consumer-Id");

    private final String header;

    NavHttpHeaders(String header) {
        this.header = header;
    }

    public String asString() {
        return header;
    }
}
