package no.nav.familie.http.azure;

import java.time.Instant;

public class AccessTokenDto {
    public String token_type;
    public String access_token;
    public Integer expires_in;

    public AccessTokenDto() {
    }

    public AccessTokenDto(String token_type, String access_token, Integer expires_in) {
        this.token_type = token_type;
        this.access_token = access_token;
        this.expires_in = expires_in;
    }

    public String getToken_type() {
        return token_type;
    }

    String getAccess_token() {
        return access_token;
    }

    Integer getExpires_in() {
        return expires_in;
    }
}
