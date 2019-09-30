package no.nav.familie.http.azure;

import java.time.Instant;

class AccessTokenDto {
    public String access_token;
    public Instant expires_on;

    String getAccess_token() {
        return access_token;
    }

    Instant getExpires_on() {
        return expires_on;
    }
}
