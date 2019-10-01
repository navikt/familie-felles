package no.nav.familie.http.azure;

import java.time.Instant;

public class AccessTokenDto {
    public String token_type;
    public String access_token;
    public Instant expires_on;

    public AccessTokenDto() {
    }

    public AccessTokenDto(String token_type, String access_token, Instant expires_on) {
        this.token_type = token_type;
        this.access_token = access_token;
        this.expires_on = expires_on;
    }

    public String getToken_type() {
        return token_type;
    }

    String getAccess_token() {
        return access_token;
    }

    Instant getExpires_on() {
        return expires_on;
    }
}
