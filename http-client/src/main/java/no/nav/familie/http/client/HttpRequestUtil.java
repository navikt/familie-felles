package no.nav.familie.http.client;

import no.nav.familie.log.IdUtils;
import no.nav.familie.log.mdc.MDCConstants;
import org.slf4j.MDC;

import java.net.http.HttpRequest;
import java.time.Duration;

public final class HttpRequestUtil {

    private static final int SECONDS_IN_MINUTE = 60;

    private HttpRequestUtil() {
    }

    public static HttpRequest.Builder createRequest(String authorizationHeader) {
        return HttpRequest.newBuilder()
                          .header("Authorization", authorizationHeader)
                          .header(NavHttpHeaders.NAV_CALLID.asString(), hentEllerOpprettCallId())
                          .timeout(Duration.ofSeconds(SECONDS_IN_MINUTE));
    }

    private static String hentEllerOpprettCallId() {
        final var callId = MDC.get(MDCConstants.MDC_CALL_ID);
        if (callId == null) {
            return IdUtils.generateId();
        }
        return callId;
    }
}
