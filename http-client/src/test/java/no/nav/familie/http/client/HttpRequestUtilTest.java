package no.nav.familie.http.client;

import no.nav.familie.log.mdc.MDCConstants;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpRequestUtilTest {

    @Test
    public void MDC_skal_samsvare_med_standard() {
        assertThat(MDCConstants.MDC_CALL_ID).isEqualTo(MDCConstants.MDC_CALL_ID);
    }
}
