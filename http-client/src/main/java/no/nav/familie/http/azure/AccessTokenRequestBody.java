package no.nav.familie.http.azure;

class AccessTokenRequestBody {
    public String client_id;
    public String resource;
    public String grant_type;
    public String client_secret;

    AccessTokenRequestBody(String client_id, String resource, String grant_type, String client_secret) {
        this.client_id = client_id;
        this.resource = resource;
        this.grant_type = grant_type;
        this.client_secret = client_secret;
    }
}
