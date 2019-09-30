package no.nav.familie.http.azure;

import java.time.Instant;

public class AccessTokenDto {
    public String access_token;
    public Instant expires_on;

    public AccessTokenDto() {
    }

    public AccessTokenDto(String access_token, Instant expires_on) {
        this.access_token = access_token;
        this.expires_on = expires_on;
    }

    String getAccess_token() {
        return access_token;
    }

    Instant getExpires_on() {
        return expires_on;
    }
}
