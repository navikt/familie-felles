package no.nav.familie.http.client;

import no.nav.familie.http.filter.LogFilter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NavHttpHeadersTest {

    @Test
    public void skal_ha_samme_verdi_som_standard() {
        assertThat(LogFilter.CONSUMER_ID_HEADER_NAME).isEqualTo(NavHttpHeaders.NAV_CONSUMER_ID.asString());
        assertThat(LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME).isEqualTo(NavHttpHeaders.NAV_CALLID.asString());
    }
}