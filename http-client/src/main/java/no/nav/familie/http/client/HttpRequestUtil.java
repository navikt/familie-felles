package no.nav.familie.http.client;

import org.slf4j.MDC;

import java.net.http.HttpRequest;
import java.time.Duration;

public final class HttpRequestUtil {

    static final String CALL_ID = "callId";

    private HttpRequestUtil() {
    }

    public static HttpRequest.Builder createRequest(String authorizationHeader) {
        return HttpRequest.newBuilder()
                .header("Authorization", authorizationHeader)
                .header(NavHttpHeaders.NAV_CALLID.asString(), hentEllerOpprettCallId())
                .timeout(Duration.ofSeconds(60));
    }

    private static String hentEllerOpprettCallId() {
        final var callId = MDC.get(CALL_ID);
        if (callId == null) {
            return IdUtils.generateId();
        }
        return callId;
    }
}
