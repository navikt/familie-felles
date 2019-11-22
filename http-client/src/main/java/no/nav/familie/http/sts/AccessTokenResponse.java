package no.nav.familie.http.sts;

class AccessTokenResponse {

    private String access_token;
    private String token_type;
    private Long expires_in;

    String getAccess_token() {
        return access_token;
    }

    public String getToken_type() {
        return token_type;
    }

    Long getExpires_in() {
        return expires_in;
    }
}
