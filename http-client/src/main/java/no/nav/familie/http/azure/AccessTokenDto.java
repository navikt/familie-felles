package no.nav.familie.http.azure;

public class AccessTokenDto {

    private String token_type;
    private String access_token;
    private Integer expires_in;

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

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public void setExpires_in(Integer expires_in) {
        this.expires_in = expires_in;
    }
}
